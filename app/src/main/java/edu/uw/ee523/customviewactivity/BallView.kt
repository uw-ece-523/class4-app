package edu.uw.ee523.customviewactivity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BallView(context: Context, attrs: AttributeSet) : View(context, attrs) {


    //This method draws in the View
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) return

        //Parameters to draw
        val mPaint = Paint()
        mPaint.setARGB(120, 150, 200, 215)
//        canvas.drawOval(RectF(x, y, width.toFloat(), height.toFloat()), mPaint)
//        canvas.drawOval(RectF(x, y, 50f, 50f), mPaint)

        canvas.drawText("Foobar", 15f, 15f, mPaint)
    }
}