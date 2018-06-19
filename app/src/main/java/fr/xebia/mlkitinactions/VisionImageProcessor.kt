package fr.xebia.mlkitinactions

import android.graphics.Bitmap
import android.widget.TextView

interface VisionImageProcessor {

    fun process(bitmap: Bitmap, graphicOverlay: GraphicOverlay?, resultTextView: TextView)

    fun stop()
}