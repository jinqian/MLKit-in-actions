package fr.xebia.mlkitinactions.custom

import android.content.Context
import android.graphics.Bitmap
import android.support.text.emoji.EmojiCompat
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions
import fr.xebia.mlkitinactions.GraphicOverlay
import fr.xebia.mlkitinactions.R
import fr.xebia.mlkitinactions.VisionImageProcessor
import fr.xebia.mlkitinactions.emoji.FruitType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

// this classifier works for non quantized model which input/output format are Float32
class CustomImageClassifier
@Throws(FirebaseMLException::class)
constructor(private val context: Context) : VisionImageProcessor {

    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
    private val imgData = ByteBuffer.allocateDirect(
            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)

    private var interpreter: FirebaseModelInterpreter? = null

    /**
     * Data configuration of input & output data of model.
     */
    private lateinit var inputOutputOptions: FirebaseModelInputOutputOptions

    /**
     * Labels corresponding to the output of the vision model.
     */
    private lateinit var labelList: List<String>

    init {
        try {
            Log.d(TAG, "Created a Custom Image Classifier.")
            labelList = loadLabelList(context)
            val inputDims = intArrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
            val outputDims = intArrayOf(1, labelList.size)

            val conditions = FirebaseModelDownloadConditions.Builder()
                    .requireWifi()
                    .build()
            val localModelSource = FirebaseLocalModelSource.Builder(LOCAL_MODEL_NAME)
                    .setAssetFilePath(LOCAL_MODEL_PATH).build()
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
                    .setLocalModelName(LOCAL_MODEL_NAME)
                    .build()
            interpreter = FirebaseModelInterpreter.getInstance(modelOptions)

            // options for non-quantized model
            inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, inputDims)
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, outputDims)
                    .build()
            Log.d(TAG, "Configured input & output data for the custom image classifier.")
        } catch (e: FirebaseMLException) {
            Toast.makeText(context, "Error while setting up the model", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun process(bitmap: Bitmap, graphicOverlay: GraphicOverlay?, resultTextView: TextView) {
        if (interpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            val uninitialized = ArrayList<String>()
            uninitialized.add("Uninitialized Classifier.")
            Tasks.forResult<List<String>>(uninitialized)
        }

        Log.d(TAG, "classify frame")

        val imageByteBuffer = convertBitmapToByteBuffer(Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true))
        val inputs = FirebaseModelInputs.Builder().add(imageByteBuffer).build()

        interpreter?.run(inputs, inputOutputOptions)
                ?.addOnSuccessListener {
                    val labelProbArray = it.getOutput<Array<FloatArray>>(0)
                    val results = getTopLabel(labelProbArray)

                    // specific magritte
                    val fruitType = FruitType.getEmojiByName(results.first)
                    val confidence = "%.3f".format(results.second)
                    if (fruitType != FruitType.UNKNOWN && fruitType.emoji.isNotEmpty()) {
                        val processedText = EmojiCompat.get().process("${fruitType.emoji}: $confidence")
                        resultTextView.text = processedText
                    } else {
                        resultTextView.text = context.getString(R.string.custom_model_result, results.first, confidence)
                    }
                }
    }

    override fun stop() {
        // TODO how?
    }

    /**
     * Gets the top labels in the results.
     */
    @Synchronized
    private fun getTopLabel(labelProbArray: Array<FloatArray>): Pair<String, Float> {
        return labelList.asSequence().mapIndexed { i, label ->
            Pair(label, labelProbArray[0][i])
        }.sortedBy { it.second }.last()
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

        private const val HOSTED_MODEL_NAME = "magritte"
        private const val LOCAL_MODEL_NAME = "magritte"

        private const val LOCAL_MODEL_PATH = "magritte.tflite"
        private const val LABEL_PATH = "magritte_labels.txt"

        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224

        private const val MEAN = 128
        private const val STD = 128.0f
    }
}
