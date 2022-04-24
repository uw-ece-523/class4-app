package edu.uw.ee523.customviewactivity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap

class CustomImageView : androidx.appcompat.widget.AppCompatImageView{

    var curX = 0
    var curY = 0

    var isBitmapInited = false
    lateinit var myBitmap: Bitmap
    lateinit var bitmapCanvas: Canvas
    var myPaint: Paint

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        myPaint = Paint()
        myPaint.setARGB(255, 150, 200, 215)

        setOnTouchListener { v, event ->
            val action = event.action
            when(action){
                MotionEvent.ACTION_DOWN -> {
                    curX= event.x.toInt()
                    curY= event.y.toInt()
                    bitmapCanvas.drawOval(RectF(curX.toFloat(), curY.toFloat(), curX + 50f, curY + 50f), myPaint)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    curX = event.x.toInt()
                    curY= event.y.toInt()
                    bitmapCanvas.drawOval(RectF(curX.toFloat(), curY.toFloat(), curX + 50f, curY + 50f), myPaint)
                    invalidate()
                }
                else ->{

                }
            }
            true
        }
    }

    fun initBitmap()
    {
        if (width > 0 && height > 0) {
            myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(myBitmap)
            isBitmapInited = true
            this.setImageBitmap(myBitmap)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        if (!isBitmapInited) {
            initBitmap()
        }
    }
}