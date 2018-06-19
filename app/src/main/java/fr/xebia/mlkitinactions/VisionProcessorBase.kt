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
package fr.xebia.mlkitinactions

import android.graphics.Bitmap
import android.widget.TextView
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement [ ][.onSuccess] to define what they want to with the detection
 * results and [.detectInImage] to specify the detector object.
 *
 * @param <T> The type of the detected feature.
</T> */
abstract class VisionProcessorBase<T> : VisionImageProcessor {

    // Whether we should ignore process(). This is usually caused by feeding input data faster than
    // the model can handle.
    private val shouldThrottle = AtomicBoolean(false)

    // Bitmap version
    override fun process(bitmap: Bitmap, graphicOverlay: GraphicOverlay?, resultTextView: TextView) {
        if (shouldThrottle.get()) {
            return
        }
        graphicOverlay?.let {
            detectInVisionImage(FirebaseVisionImage.fromBitmap(bitmap), it, resultTextView)
        }
    }

    private fun detectInVisionImage(
            image: FirebaseVisionImage,
            graphicOverlay: GraphicOverlay,
            resultTextView: TextView) {
        detectInImage(image)
                .addOnSuccessListener { results ->
                    shouldThrottle.set(false)
                    onSuccess(results, graphicOverlay, resultTextView)
                }
                .addOnFailureListener { e ->
                    shouldThrottle.set(false)
                    onFailure(e)
                }
        // Begin throttling until this frame of input has been processed, either in onSuccess or
        // onFailure.
        shouldThrottle.set(true)
    }

    override fun stop() {}

    protected abstract fun detectInImage(image: FirebaseVisionImage): Task<T>

    protected abstract fun onSuccess(
            results: T,
            graphicOverlay: GraphicOverlay,
            resultTextView: TextView)

    protected abstract fun onFailure(e: Exception)
}
