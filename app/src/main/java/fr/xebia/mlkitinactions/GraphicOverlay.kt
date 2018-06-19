package fr.xebia.mlkitinactions

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

class GraphicOverlay @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lock = Any()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private val graphics = HashSet<Graphic>()

    var widthScaleFactor = 1.0f
    var heightScaleFactor = 1.0f

    /** Removes all graphics from the overlay.  */
    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    /** Adds a graphic to the overlay.  */
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
        postInvalidate()
    }

    /** Removes a graphic from the overlay.  */
    fun remove(graphic: Graphic) {
        synchronized(lock) {
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform image
     * coordinates later.
     */
    fun setCameraInfo(previewWidth: Int, previewHeight: Int) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
        }
        postInvalidate()
    }

    /** Draws the overlay with its associated graphic objects.  */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor = canvas.width.toFloat() / previewWidth.toFloat()
                heightScaleFactor = canvas.height.toFloat() / previewHeight.toFloat()
            }

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }

    abstract class Graphic(private val overlay: GraphicOverlay) {

        /**
         * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
         * to view coordinates for the graphics that are drawn:
         *
         *
         *  1. [Graphic.scaleX] and [Graphic.scaleY] adjust the size of the
         * supplied value from the preview scale to the view scale.
         *  1. [Graphic.translateX] and [Graphic.translateY] adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.
         *
         *
         * @param canvas drawing canvas
         */
        abstract fun draw(canvas: Canvas)

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view scale.
         */
        fun scaleX(horizontal: Float): Float {
            return horizontal * overlay.widthScaleFactor
        }

        /** Adjusts a vertical value of the supplied value from the preview scale to the view scale.  */
        fun scaleY(vertical: Float): Float {
            return vertical * overlay.heightScaleFactor
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate system.
         */
        fun translateX(x: Float): Float = scaleX(x)

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate system.
         */
        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            overlay.postInvalidate()
        }
    }
}
