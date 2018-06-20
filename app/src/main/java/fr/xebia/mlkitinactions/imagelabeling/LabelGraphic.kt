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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import com.google.firebase.ml.vision.label.FirebaseVisionLabel

import fr.xebia.mlkitinactions.GraphicOverlay

/**
 * Graphic instance for rendering a label within an associated graphic overlay view.
 */
class LabelGraphic internal constructor(private val overlay: GraphicOverlay, private val labels: List<FirebaseVisionLabel>) : GraphicOverlay.Graphic(overlay) {

    private val textPaint: Paint = Paint()

    init {
        textPaint.color = Color.WHITE
        textPaint.textSize = 60.0f
        postInvalidate()
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        val x = overlay.width / 4.0f
        var y = overlay.height / 2.0f

        for (label in labels) {
            canvas.drawText(label.label, x, y, textPaint)
            y -= 62.0f
        }
    }
}
