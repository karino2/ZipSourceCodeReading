package karino2.livejournal.com.zipsourcecodereading.text

import android.text.Selection
import android.text.Spannable
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.view.VelocityTracker
import android.text.NoCopySpan
import android.widget.Scroller

import android.text.Layout.Alignment
import android.util.Log
import java.lang.Character.UnicodeBlock

/**
 * Created by _ on 2017/10/04.
 */
abstract class MovementMethod(val widget : LongTextView) {

    companion object {

        var lineNumberWidth = 0

        /**
         * Scrolls the specified widget to the specified coordinates, except
         * constrains the X scrolling position to the horizontal regions of
         * the text that will be visible after scrolling to the specified
         * Y position.
         */
        fun scrollTo(widget: LongTextView, layout: Layout, x: Int, y: Int) {
            var x = x
            var padding = widget.paddingTop + widget.paddingBottom
            val top = layout.getLineForVertical(y)
            val bottom = layout.getLineForVertical(y + widget.height - padding)

            var left = Integer.MAX_VALUE
            var right = 0
            var a: Alignment? = null

            for (i in top..bottom) {
                left = Math.min(left.toFloat(), layout.getLineLeft(i)).toInt()
                right = Math.max(right.toFloat(), layout.getLineRight(i) + lineNumberWidth).toInt()

                if (a == null) {
                    a = layout.getParagraphAlignment(i)
                }
            }

            padding = widget.paddingLeft + widget.paddingRight
            val width = widget.width
            var diff = 0

            if (right - left < width - padding) {
                if (a === android.text.Layout.Alignment.ALIGN_CENTER) {
                    diff = (width - padding - (right - left)) / 2
                } else if (a === Alignment.ALIGN_OPPOSITE) {
                    diff = width - padding - (right - left)
                }
            }

            x = Math.min(x, right - (width - padding) - diff)
            x = Math.max(x, left - diff)

            widget.scrollTo(x, y)
        }

    }

    fun initialize(text: Spannable) {
        Selection.setSelection(text, 0)
    }

    abstract fun notifyScroll()

    var touchMode = TouchMode.REST

    enum class TouchMode {
        REST, FLING, DOWN,
        TAP, SCROLL,
    }

    val flingRunnable by lazy { FlingRunnable() }

    inner class FlingRunnable : Runnable {

        private val mScroller: Scroller

        private var mLastFlingY: Int = 0

        init {
            mScroller = Scroller(widget.context)
        }

        internal fun start(initialVelocity: Int) {
            val initialX = widget.scrollX //initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            val initialY = widget.scrollY //initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingY = initialY
            mScroller.fling(initialX, initialY, 0, initialVelocity,
                    0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE)
            touchMode = TouchMode.FLING
            notifyScroll()

            widget.postOnAnimation(this)

        }

        fun endFling() {
            touchMode = TouchMode.REST
            widget.removeCallbacks(this)
            widget.removeCallbacks(checkFlyWheel)

            widget.moveCursorToVisibleOffset()
        }

        override fun run() {
            when (touchMode) {
                TouchMode.SCROLL, TouchMode.FLING -> {
                    if(touchMode == TouchMode.SCROLL && mScroller.isFinished) {
                        return
                    }

                    val scroller = mScroller
                    val more = scroller.computeScrollOffset()

                    val x = scroller.currX
                    var y = scroller.currY

                    val delta2 = mLastFlingY - y


                    val layout = widget.layout!!

                    val padding = widget.paddingTop + widget.paddingBottom

                    y = Math.min(y, layout.height - (widget.height - padding))
                    y = Math.max(y, 0)

                    scrollTo(widget, layout, x, y)
                    val delta = mLastFlingY - y

                    // deleta becomes zero for round, so should not used as finish check.
                    // keep this code as comment for document purpose.
                    if (more /*  && delta != 0 */) {
                        widget.invalidate()
                        mLastFlingY = y
                        widget.postOnAnimation(this)
                    } else {
                        endFling()
                    }
                }
                else -> endFling()
            }

        }

        val FLYWHEEL_TIMEOUT = 40L; // milliseconds

        fun checkFlyWheelAgain() = widget.postDelayed(checkFlyWheel, FLYWHEEL_TIMEOUT)

        val checkFlyWheel : Runnable = Runnable() {
            val vt = velocityTracker ?: return@Runnable

            vt.computeCurrentVelocity(1000, maximumVelocity.toFloat());
            val yvel = -vt.yVelocity
            if (Math.abs(yvel) >= minimumVelocity
                /*
                    && mScroller.isScrollingInDirection(0, yvel) */ ) {
                // Keep the fling alive a little longer
                checkFlyWheelAgain()
            } else {
                endFling();
                touchMode = TouchMode.SCROLL;
                notifyScroll()
            }
        }

        fun flywheelTouch() {
            widget.postDelayed(checkFlyWheel, FLYWHEEL_TIMEOUT)
        }


    }


    var velocityTracker : VelocityTracker? = null

    class DragState() {
        var x = 0f
        var y = 0f
        var scrollX = 0
        var scrollY = 0
        var farEnough = false
        var used = false

        fun init(widget: LongTextView, event: MotionEvent) {
            used = true
            farEnough = false
            x = event.x
            y = event.y
            scrollX = widget.scrollX
            scrollY = widget.scrollY

        }
    }

    val dragState = DragState()

    fun getInitialScrollX(buffer: Spannable): Int {
        return if (dragState.used) dragState.scrollX else -1
    }

    fun getInitialScrollY(buffer: Spannable): Int {
        return if (dragState.used) dragState.scrollY else -1
    }


    val configuration by lazy { ViewConfiguration.get(widget.context) }

    val maximumVelocity by lazy { configuration.scaledMaximumFlingVelocity }
    val minimumVelocity by lazy { configuration.scaledMinimumFlingVelocity }


    inner class CheckForLongPress : Runnable {
        override fun run() {
            widget.performLongClick()
            touchMode = TouchMode.REST
        }

    }

    val checkForLongPress by lazy {
        CheckForLongPress()
    }

    val longPressTimeout by lazy {
        ViewConfiguration.getLongPressTimeout()
    }

    inner class CheckForTap : Runnable {
        var x = 0f
        var y = 0f

        override fun run() {
            if(touchMode == TouchMode.DOWN) {
                touchMode = TouchMode.TAP

                widget.postDelayed(checkForLongPress, longPressTimeout.toLong())

            }
        }

    }

    val checkForTap by lazy { CheckForTap() }

    fun initOrResetVelocityTracker() {
        if(velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker!!.clear()
        }
    }

    /**
     * Handles touch events for dragging.  You may want to do other actions
     * like moving the cursor on touch as well.
     */
    fun handleTouchEvent(buffer: Spannable,
                         event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initOrResetVelocityTracker()
                // I think this code add twice, but AbsListView code is this way.
                velocityTracker!!.addMovement(event)

                when(touchMode) {
                    TouchMode.FLING -> {
                        // createScrollingCache()
                        touchMode = TouchMode.SCROLL
                        flingRunnable.flywheelTouch()
                    }
                    else -> {
                        touchMode = TouchMode.DOWN

                        dragState.init(widget, event)

                        checkForTap.x = event.getX()
                        checkForTap.y = event.getY()
                        widget.postDelayed(checkForTap, ViewConfiguration.getTapTimeout().toLong())
                    }

                }

                if(touchMode == TouchMode.DOWN) {
                    widget.removeCallbacks(checkForTap)
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                var result = false
                when(touchMode) {
                    TouchMode.DOWN, TouchMode.TAP-> {

                        if(touchMode == TouchMode.DOWN || touchMode == TouchMode.TAP) {
                            widget.removeCallbacks(if(touchMode == TouchMode.DOWN) checkForTap else checkForLongPress)

                            // invoke tap
                            widget.moveCursorToVisibleOffset()
                            touchMode = TouchMode.REST
                        }
                        return true
                    }
                    TouchMode.SCROLL -> {
                        velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                        val initialVelocity = velocityTracker!!.getYVelocity().toInt()

                        if(Math.abs(initialVelocity) > minimumVelocity) {
                            flingRunnable!!.start(-initialVelocity)
                        }
                    }
                    else -> {
                        touchMode = TouchMode.REST
                    }
                }

            }
            MotionEvent.ACTION_MOVE ->
                when(touchMode) {
                    TouchMode.DOWN, TouchMode.TAP -> {
                        startScrollIfNeeded(widget, event)
                    }
                    TouchMode.SCROLL -> {
                        scrollByTouch(widget, event)
                    }
                }

        }

        velocityTracker?.let {
            velocityTracker!!.addMovement(event)
        }

        return true

    }

    private fun scrollByTouch(widget: LongTextView, event: MotionEvent) {
        val dx: Float = dragState.x - event.x
        val dy: Float = dragState.y - event.y
        dragState.x = event.x
        dragState.y = event.y


        val nx = widget.scrollX + dx.toInt()
        var ny = widget.scrollY + dy.toInt()

        val padding = widget.paddingTop + widget.paddingBottom
        val layout = widget.layout

        ny = Math.min(ny, layout!!.height - (widget.height - padding))
        ny = Math.max(ny, 0)

        val oldX = widget.scrollX
        val oldY = widget.scrollY

        scrollTo(widget, layout, nx, ny)

        // If we actually scrolled, then cancel the up action.
        if (oldX != widget.scrollX || oldY != widget.scrollY) {
            widget.cancelLongPress()
        }
    }

    fun startScrollIfNeeded(widget: LongTextView, event: MotionEvent) : Boolean {
        val slop = ViewConfiguration.get(widget.context).scaledTouchSlop

        if (Math.abs(event.x - dragState.x) >= slop || Math.abs(event.y - dragState.y) >= slop) {
            touchMode = TouchMode.SCROLL
            notifyScroll()

            widget.removeCallbacks(checkForLongPress)

            scrollByTouch(widget, event)
            return true
        }
        return false

    }

    private fun checkFarEnough(widget: LongTextView, event: MotionEvent) :Boolean {
        val slop = ViewConfiguration.get(widget.context).scaledTouchSlop

        if (Math.abs(event.x - dragState.x) >= slop || Math.abs(event.y - dragState.y) >= slop) {
            dragState.farEnough = true
            dragState.used = true
            return true
        }
        return false
    }

    private fun getOffset(x: Int, y: Int, widget: LongTextView): Int {
        var x = x
        var y = y
        // Converts the absolute X,Y coordinates to the character offset for the
        // character whose position is closest to the specified
        // horizontal position.
        x -= widget.paddingLeft
        y -= widget.paddingTop

        // Clamp the position to inside of the view.
        if (x < 0) {
            x = 0
        } else if (x >= widget.width - widget.paddingRight) {
            x = widget.width - widget.paddingRight - 1
        }
        if (y < 0) {
            y = 0
        } else if (y >= widget.height - widget.paddingBottom) {
            y = widget.height - widget.paddingBottom - 1
        }

        x += widget.scrollX
        y += widget.scrollY

        val layout = widget.layout!!
        val line = layout.getLineForVertical(y)

        return layout.getOffsetForHorizontal(line, x.toFloat())
    }

    private fun sameWord(text: Spannable, one: Int, two: Int): Boolean {
        val start = findWordStart(text, one)
        val end = findWordEnd(text, one)

        return if (end == start) {
            false
        } else start == findWordStart(text, two) && end == findWordEnd(text, two)

    }

    private fun findWordStart(text: CharSequence, start: Int): Int {
        var start = start
        if (text.length <= start) {
            return start
        }

        val c0 = UnicodeBlock.of(text[start])

        while (start > 0) {
            val c = text[start - 1]
            val cb = UnicodeBlock.of(c)
            if (c0 === UnicodeBlock.BASIC_LATIN) {
                val type = Character.getType(c)

                if (c != '\'' &&
                        type != Character.UPPERCASE_LETTER.toInt() &&
                        type != Character.LOWERCASE_LETTER.toInt() &&
                        type != Character.TITLECASE_LETTER.toInt() &&
                        type != Character.MODIFIER_LETTER.toInt() &&
                        type != Character.DECIMAL_DIGIT_NUMBER.toInt()) {
                    break
                }
            } else if (c0 !== cb) {
                break
            }
            start--
        }

        return start
    }

    private fun findWordEnd(text: Spannable, end: Int): Int {
        var end = end
        val len = text.length

        if (len <= end) {
            return end
        }

        val c0 = UnicodeBlock.of(text[end])

        while (end < len) {
            val c = text[end]
            val cb = UnicodeBlock.of(c)
            if (c0 === UnicodeBlock.BASIC_LATIN) {
                val type = Character.getType(c)

                if (c != '\'' &&
                        type != Character.UPPERCASE_LETTER.toInt() &&
                        type != Character.LOWERCASE_LETTER.toInt() &&
                        type != Character.TITLECASE_LETTER.toInt() &&
                        type != Character.MODIFIER_LETTER.toInt() &&
                        type != Character.DECIMAL_DIGIT_NUMBER.toInt()) {
                    break
                }
            } else if (c0 !== cb) {
                break
            }
            end++
        }

        return end
    }


    fun onTouchEvent(widget: LongTextView, buffer: Spannable,
                     event: MotionEvent): Boolean {
        var initialScrollX = -1
        var initialScrollY = -1
        if (event.action == MotionEvent.ACTION_UP) {
            initialScrollX = getInitialScrollX(buffer)
            initialScrollY = getInitialScrollY(buffer)
        }

        return handleTouchEvent(buffer, event)
    }

    fun selectWord(buffer: Spannable, off: Int) : Boolean {
        val start = findWordStart(buffer, off)
        val end = findWordEnd(buffer, off)
        if(start == end) return false
        Selection.setSelection(buffer, start, end)
        return true
    }


}