package com.example.bluejoysticktruck

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlin.math.PI
import kotlin.math.atan

fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    return Math.sqrt( Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0) )
}

class JoyStick @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var strength: Double = 0.0; var angle = -1
    var centerX = 0F; var centerY = 0F
    var cx = 0F; var cy = 0F; var r = 0F; var w = 0F
    val oval = RectF()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG); val paintBounds = Paint(Paint.ANTI_ALIAS_FLAG)
    val paintDot = Paint(Paint.ANTI_ALIAS_FLAG)
    val mBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_joystick_bg)
//    lateinit var imgRect: Rect

    init {
        paint.apply {
            style = Paint.Style.FILL
            color = Color.rgb(255, 255, 255)
        }
        paintBounds.apply {
            style = Paint.Style.FILL
            color = Color.rgb(235, 235, 235)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w.toFloat()
        r = this.w/6 // diameter is 1/3 of width of canvas
        centerX = this.w/2; centerY = centerX
        cx = centerX; cy = centerY
//        imgRect = Rect(0, 0, w, w)
    }

    var tr = 0F

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

//        canvas?.drawColor(Color.BLUE)

//        canvas?.drawBitmap(mBitmap, null, imgRect, null)

        tr = (2.2*r).toFloat()
        oval.set(centerX - tr, centerY - tr, centerX + tr, centerY + tr)
        canvas?.drawArc(oval, 0F, 360F, false, paintBounds)

        oval.set(cx - r, cy - r, cx + r, cy + r)
        canvas?.drawArc(oval, 0F, 360F, false, paint)
    }

    var cdX = 0F; var cdY = 0F; var isInContact = false
    var m  = 0F; var tconst = 0F

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // when touch starts
                cdX = event.x - centerX; cdY = event.y - centerY

                if (distance(centerX, centerY, event.x, event.y) < r)
                    isInContact = true

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // when touch moves
                if (!isInContact) return true

                cx = event.x - cdX; cy = event.y - cdY

                strength = distance(centerX, centerY, cx, cy)

                // slope or gradient of line joining center and origin
                m = (centerY - cy)/(centerX - cx)
                angle = (atan( if (m < 0) -m else m )*180/PI).toInt()

                if (strength> r) {
                    if (centerX - cx != 0F) {
                        tconst = r / Math.sqrt((1 + m * m).toDouble()).toFloat()

                        if (cx > centerX) {
                            cx = centerX + tconst
                        } else if (cx < centerX) {
                            cx = centerX - tconst
                        }
                        
                        cy = m * (cx - centerX) + centerY

                    } else {
                        cx = centerX
                        cy = if (cy > centerY) r + centerY else centerY - r
                    }

                    strength = distance(centerX, centerY, cx, cy)
                }

                postInvalidate()

                return true
            }
            MotionEvent.ACTION_UP -> {
                // when touch ends
                cx = centerX; cy = centerY
                isInContact = false
                postInvalidate()

                angle = -1
                strength = 0.0

                return true
            }
            else -> return super.onTouchEvent(event)
        }

    }

    fun getJoystickAngle(): Int {
        if (angle == -1) return -1
        if (cx <= centerX) {
            if (cy < centerY) {
                return 180 - angle // 90 + (90 - angle)
            } else {
                return 180 + angle
            }
        } else if (cy > centerY) {
            return 360 - angle - 1
        }
        return angle
    }

}