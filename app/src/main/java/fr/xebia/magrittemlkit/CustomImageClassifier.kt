// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package fr.xebia.magrittemlkit

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.experimental.and

/**
 * A `FirebaseModelInterpreter` based image classifier.
 */
class CustomImageClassifier
/**
 * Initializes an `CustomImageClassifier`.
 */
@Throws(FirebaseMLException::class)
constructor(activity: Activity) {

    /* Preallocated buffers for storing image data in. */
    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    private var interpreter: FirebaseModelInterpreter? = null

    /**
     * Data configuration of input & output data of model.
     */
    private lateinit var dataOptions: FirebaseModelInputOutputOptions

    /**
     * Labels corresponding to the output of the vision model.
     */
    private lateinit var labelList: List<String>

    private val sortedLabels = PriorityQueue(
            RESULTS_TO_SHOW,
            Comparator<Map.Entry<String, Float>> { o1, o2 -> o1.value.compareTo(o2.value) })

    init {
        try {
            Log.d(TAG, "Created a Custom Image Classifier.")
            labelList = loadLabelList(activity)
            val inputDims = intArrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
            val outputDims = intArrayOf(1, labelList.size)

            val conditions = FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build()
            val localModelSource = FirebaseLocalModelSource.Builder("asset")
                    .setAssetFilePath(LOCAL_MODEL_ASSET).build()
            val cloudSource = FirebaseCloudModelSource.Builder(HOSTED_MODEL_NAME)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)
                    .build()
            val manager = FirebaseModelManager.getInstance()
            manager.registerLocalModelSource(localModelSource)
            manager.registerCloudModelSource(cloudSource)
            val modelOptions = FirebaseModelOptions.Builder()
                    .setCloudModelName(HOSTED_MODEL_NAME)
                    .setLocalModelName("asset")
                    .build()
            interpreter = FirebaseModelInterpreter.getInstance(modelOptions)

            dataOptions = FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.BYTE, inputDims)
                    .setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims)
                    .build()
            Log.d(TAG, "Configured input & output data for the custom image classifier.")
        } catch (e: FirebaseMLException) {
            Toast.makeText(activity, "Error while setting up the model", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun stop() {
    }

    /**
     * Classifies a frame from the preview stream.
     */
    @Throws(FirebaseMLException::class)
    fun classifyFrame(bitmap: Bitmap): Task<List<String>>? {
        if (interpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            val uninitialized = ArrayList<String>()
            uninitialized.add("Uninitialized Classifier.")
            Tasks.forResult<List<String>>(uninitialized)
        }

        Log.d(TAG, "classify frame")
        val imgData = convertBitmapToByteBuffer(bitmap)
        val inputs = FirebaseModelInputs.Builder().add(imgData).build()

        return interpreter
                ?.run(inputs, dataOptions)
                ?.continueWith { task ->
                    val labelProbArray = task.result.getOutput<Array<ByteArray>>(0)
                    getTopLabels(labelProbArray)
                }

    }

    /**
     * Gets the top labels in the results.
     */
    @Synchronized
    private fun getTopLabels(labelProbArray: Array<ByteArray>): List<String> {
        for (i in labelList.indices) {
            sortedLabels.add(
                    AbstractMap.SimpleEntry<String, Float>(labelList[i], (labelProbArray[0][i] and 0xff.toByte()) / 255.0f))
            if (sortedLabels.size > RESULTS_TO_SHOW) {
                sortedLabels.poll()
            }
        }
        val result = ArrayList<String>()
        val size = sortedLabels.size
        for (i in 0 until size) {
            val label = sortedLabels.poll()
            result.add(label.key + ":" + label.value)
        }
        Log.d(TAG, "labels: " + result.toString())
        return result
    }

    /**
     * Reads label list from Assets.
     */
    private fun loadLabelList(activity: Activity): List<String> {
        val labelList = ArrayList<String>()
        try {
            BufferedReader(InputStreamReader(activity.assets.open(LABEL_PATH))).use { reader ->
                do {
                    val line = reader.readLine()
                    if (line != null) {
                        labelList.add(line)
                    }
                } while (line != null)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read label list.", e)
        }

        return labelList
    }

    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // TODO need to *4 if using non-quantized model?
        val imgData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y,
                true)
        imgData.rewind()
        bitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0,
                scaledBitmap.width, scaledBitmap.height)
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val currentValue = intValues[pixel++]
                imgData.put((currentValue shr 16 and 0xFF).toByte())
                imgData.put((currentValue shr 8 and 0xFF).toByte())
                imgData.put((currentValue and 0xFF).toByte())
            }
        }
        return imgData
    }

    companion object {
        private const val TAG = "CustomImageClassifier"

        // mobile net 224 quant
//        const val HOSTED_MODEL_NAME = "mobilenet_v1_224_quant"
//        const val LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224_quant.tflite"

        const val HOSTED_MODEL_NAME = "mobilenet_v1_224"
        const val LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224.tflite"

//        const val HOSTED_MODEL_NAME = "magritte"
//        const val LOCAL_MODEL_ASSET = "magritte.tflite"
        /**
         * Name of the label file stored in Assets.
         */
        private const val LABEL_PATH = "labels.txt"
//        private const val LABEL_PATH = "magritte_labels.txt"
        /**
         * Number of results to show in the UI.
         */
        private const val RESULTS_TO_SHOW = 3
        /**
         * Dimensions of inputs.
         */
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
    }
}
