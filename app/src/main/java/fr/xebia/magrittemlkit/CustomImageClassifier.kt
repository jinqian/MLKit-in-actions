package fr.xebia.magrittemlkit

import android.content.Context
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

// this classifier works for non quantized model which input/output format are Float32
class CustomImageClassifier
@Throws(FirebaseMLException::class)
constructor(context: Context) {

    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
    private val imgData = ByteBuffer.allocateDirect(
            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)

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
            labelList = loadLabelList(context)
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
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, inputDims)
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, outputDims)
                    .build()
            Log.d(TAG, "Configured input & output data for the custom image classifier.")
        } catch (e: FirebaseMLException) {
            Toast.makeText(context, "Error while setting up the model", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
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
                    val labelProbArray = task.result.getOutput<Array<FloatArray>>(0)
                    getTopLabels(labelProbArray)
                }
    }

    /**
     * Gets the top labels in the results.
     */
    @Synchronized
    private fun getTopLabels(labelProbArray: Array<FloatArray>): List<String> {
        for (i in labelList.indices) {
            sortedLabels.add(
                    AbstractMap.SimpleEntry<String, Float>(labelList[i], labelProbArray[0][i]))
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
    private fun loadLabelList(context: Context): List<String> {
        val labels = ArrayList<String>()
        labels.addAll(context.assets.open(LABEL_PATH).bufferedReader().readLines())
        return labels
    }

    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        imgData.apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        intValues.forEach {
            imgData.putFloat(((it shr 16 and 0xFF) - MEAN) / STD)
            imgData.putFloat(((it shr 8 and 0xFF) - MEAN) / STD)
            imgData.putFloat(((it and 0xFF) - MEAN) / STD)
        }

        return imgData
    }

    companion object {
        private const val TAG = "CustomImageClassifier"

        const val HOSTED_MODEL_NAME = "magritte"
        const val LOCAL_MODEL_ASSET = "magritte.tflite"
        /**
         * Name of the label file stored in Assets.
         */
        private const val LABEL_PATH = "magritte_labels.txt"
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

        private const val MEAN = 128
        private const val STD = 128.0f
    }
}
