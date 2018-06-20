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
package fr.xebia.mlkitinactions.imagelabeling

import android.util.Log
import android.widget.TextView
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector
import fr.xebia.mlkitinactions.GraphicOverlay
import fr.xebia.mlkitinactions.VisionProcessorBase
import java.io.IOException

/**
 * Custom Image Classifier Demo.
 */
class ImageLabelingProcessor : VisionProcessorBase<List<FirebaseVisionLabel>>() {

    private val detector: FirebaseVisionLabelDetector = FirebaseVision.getInstance().visionLabelDetector

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: $e")
        }

    }

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionLabel>> {
        return detector.detectInImage(image)
    }

    override fun onSuccess(
            labels: List<FirebaseVisionLabel>,
            graphicOverlay: GraphicOverlay,
            resultTextView: TextView) {
        graphicOverlay.clear()
        val labelGraphic = LabelGraphic(graphicOverlay, labels)
        graphicOverlay.add(labelGraphic)
        val stringBuilder = StringBuilder()
        
        for (label in labels) {
            val confidence = "%.3f".format(label.confidence)
            stringBuilder.append("${label.label}: $confidence").append("\n")
        }
        resultTextView.text = stringBuilder.toString()
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Label detection failed.$e")
    }

    companion object {

        private val TAG = "ImageLabelingProcessor"
    }
}
