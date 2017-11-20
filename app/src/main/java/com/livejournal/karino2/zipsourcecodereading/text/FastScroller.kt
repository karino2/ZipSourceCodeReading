package com.livejournal.karino2.zipsourcecodereading.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import com.livejournal.karino2.zipsourcecodereading.R

/**
 * Created by _ on 2017/10/04.
 */
class FastScroller(val target: LongTextView) {
    enum class State {
        NONE, ENTER, VISIBLE, DRAGGING, EXIT
    }

    val thumbDrawable : Drawable by lazy {
        target.resources.getDrawable(R.drawable.scrollbar_handle)
    }

    val dpi : Float by lazy {
        val dm = DisplayMetrics()
        val wm = target.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(dm)
        (dm.xdpi+dm.ydpi)/2
    }

    val thumbH : Int by lazy {
        ((48*dpi)/160).toInt()
    }
    val thumbW  : Int by lazy {
        ((52*dpi)/160).toInt()
    }
    var thumbY = 0

    var state = State.NONE

    fun gotoState(newState: State) {
        when(newState) {
            State.NONE -> {
                target.removeCallbacks(scrollFade)
                target.invalidate()
            }
            State.VISIBLE -> {
                if(state != State.VISIBLE) {
                    resetThumbPos()
                }
                target.removeCallbacks(scrollFade)
            }
            State.DRAGGING -> {
                target.removeCallbacks(scrollFade)
            }
            State.EXIT -> {
                invalidateSelf()
            }
        }
        state = newState
    }

    fun resetThumbPos() {
        val vw = target.getWidth()
        thumbDrawable.setBounds(vw - thumbW, 0, vw, thumbH);
        thumbDrawable.setAlpha(ALPHA_MAX);

    }

    val ALPHA_MAX = 200

    val paint : Paint by lazy {
        val pai = Paint()
        pai.setAntiAlias(true)
        pai.setTextAlign(Paint.Align.CENTER)
        pai
    }

    fun invalidateSelf() {
        val vw = target.getWidth()
        target.invalidate(vw - thumbW, thumbY, vw, thumbY+thumbH)
    }

    fun stop() = gotoState(State.NONE)

    val isVisible : Boolean
        get() = state != State.NONE

    fun draw(canvas : Canvas) {
        if(state == State.NONE)
            return
        val y = (thumbY + target.scrollY).toFloat()
        val viewWidth = target.width
        val x = target.scrollX.toFloat()

        var alpha = -1
        if(state == State.EXIT) {
            alpha = scrollFade.alpha
            if(alpha < ALPHA_MAX/2) {
                thumbDrawable.setAlpha(alpha*2)
            }

        }

        canvas.translate(x, y)
        thumbDrawable.draw(canvas)
        canvas.translate(-x, -y)

        if(alpha == 0) {
            gotoState(State.NONE)
        } else {
            // I think this must be the same as invalidateSelf(), invalidate self does not have scrollY, and here is. Which is correct?
            target.invalidate(viewWidth - thumbW, y.toInt(), viewWidth, (y+thumbH).toInt())
        }
    }


    fun onSizeChanged(w: Int, h:Int, oldW:Int, oldH : Int) {
        thumbDrawable.setBounds(w-thumbW, 0, w, thumbH)
    }

    var scrollCompleted = true
    var currentVScrollOrigin = -1

    fun onScroll(vscrollOrigin : Int, visibleHeight: Int, wholeHeight : Int) {
        if(visibleHeight <= 0)
            return

        if(wholeHeight - visibleHeight > 0 && state != State.DRAGGING ) {
            thumbY = ((visibleHeight - thumbH)*vscrollOrigin)/(wholeHeight- visibleHeight)
        }
        scrollCompleted = true
        if(currentVScrollOrigin == vscrollOrigin)
            return

        currentVScrollOrigin = vscrollOrigin
        if(state != State.DRAGGING) {
            gotoState(State.VISIBLE)
            target.postDelayed(scrollFade, 1500)
        }

    }

    fun isPointInside(x: Float, y: Float) = x > target.width - thumbW && y >= thumbY && y <= thumbY+thumbH


    var lastEventTime  = 0L

    fun onTouchEvent(ev : MotionEvent) : Boolean {
        if(state == State.NONE)
            return false

        val x = ev.x
        val y = ev.y
        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if(isPointInside(x, y)) {
                    gotoState(State.DRAGGING)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                if(state == State.DRAGGING) {
                    gotoState(State.VISIBLE)
                    target.removeCallbacks(scrollFade)
                    target.postDelayed(scrollFade, 1000)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if(state != State.DRAGGING)
                    return false

                val now = System.currentTimeMillis()
                val diff = now - lastEventTime
                if(diff > 30) {
                    lastEventTime = now
                    val viewHeight = target.height
                    val newThumbY = Math.min(Math.max(0, (ev.y - thumbH/2).toInt()), viewHeight - thumbH)
                    if(Math.abs(newThumbY - thumbY) < 2)
                        return true

                    thumbY = newThumbY
                    scrollTo(thumbY.toFloat()/(viewHeight - thumbH))
                    return true

                }
                return true

            }
            else -> return false
        }
    }

    fun scrollTo(pos : Float) {
        target.moveToVLineNow((target.lineCount*pos).toInt())
    }


    inner class ScrollFade : Runnable {
        var startTime = 0L
        val FADE_DURATION = 200L

        fun startFade() {
            startTime = SystemClock.uptimeMillis()
            gotoState(State.EXIT)
        }

        val alpha : Int
            get() {
                if(state != State.EXIT)
                    return ALPHA_MAX
                val dur = SystemClock.uptimeMillis() - startTime

                if(dur > FADE_DURATION) {
                    return 0
                }
                return (ALPHA_MAX - (dur*ALPHA_MAX)/FADE_DURATION).toInt()
            }



        override fun run() {
            if(state != State.EXIT) {
                startFade()
                return
            }
            if(alpha > 0) {
                invalidateSelf()
            } else {
                gotoState(State.NONE)
            }
        }

    }

    val scrollFade = ScrollFade()



}