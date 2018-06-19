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
package fr.xebia.mlkitinactions.text

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

import com.google.firebase.ml.vision.text.FirebaseVisionText

import fr.xebia.mlkitinactions.GraphicOverlay

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class TextGraphic internal constructor(overlay: GraphicOverlay, private val textElement: FirebaseVisionText.Element?) : GraphicOverlay.Graphic(overlay) {

    private val rectPaint: Paint = Paint()
    private val textPaint: Paint = Paint()

    init {
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH

        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_SIZE
        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
    }

    /**
     * Draws the textElement block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        if (textElement == null) {
            throw IllegalStateException("Attempting to draw a null textElement.")
        }

        // Draws the bounding box around the TextBlock.
        val rect = RectF(textElement.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, rectPaint)

        // Renders the textElement at the bottom of the box.
        canvas.drawText(textElement.text, rect.left, rect.bottom, textPaint)
    }

    companion object {

        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
    }
}
