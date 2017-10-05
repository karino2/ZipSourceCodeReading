package karino2.livejournal.com.zipsourcecodereading.text

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.text.method.MetaKeyKeyListener.resetLockedMeta
import android.text.method.MetaKeyKeyListener
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.view.VelocityTracker
import android.text.NoCopySpan
import android.widget.Scroller
import android.widget.TextView

import android.text.Layout.Alignment
import android.text.SpannableString
import java.lang.Character.UnicodeBlock

/**
 * Created by _ on 2017/10/04.
 */
class MovementMethod {

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

    fun initialize(widget: LongTextView, text: Spannable) {
        Selection.setSelection(text, 0)
    }



    private class FlingRunnable internal constructor(context: Context) : Runnable {

        internal var mTouchMode = TOUCH_MODE_REST

        private val mScroller: Scroller

        private var mLastFlingY: Int = 0

        private var mWidget: LongTextView? = null

        init {
            mScroller = Scroller(context)
        }

        internal fun start(parent: LongTextView, initialVelocity: Int) {
            mWidget = parent
            val initialX = parent.scrollX //initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            val initialY = parent.scrollY //initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingY = initialY
            mScroller.fling(initialX, initialY, 0, initialVelocity,
                    0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE)
            mTouchMode = TOUCH_MODE_FLING

            mWidget!!.post(this)

        }

        fun endFling() {
            mTouchMode = TOUCH_MODE_REST

            if (mWidget != null) {
                mWidget!!.removeCallbacks(this)
                mWidget!!.moveCursorToVisibleOffset()

                mWidget = null
            }

        }

        override fun run() {
            when (mTouchMode) {

                TOUCH_MODE_FLING -> {

                    val scroller = mScroller
                    val more = scroller.computeScrollOffset()

                    val x = scroller.currX
                    var y = scroller.currY



                    val layout = mWidget!!.layout!!

                    val padding = mWidget!!.paddingTop + mWidget!!.paddingBottom

                    y = Math.min(y, layout.height - (mWidget!!.height - padding))
                    y = Math.max(y, 0)
                    //                final boolean atEnd = trackMotionScroll(delta, delta);

                    scrollTo(mWidget!!, layout, x, y)
                    val delta = mLastFlingY - y

                    if (more && delta != 0) {
                        mWidget!!.invalidate()
                        mLastFlingY = y
                        mWidget!!.post(this)
                    } else {
                        endFling()
                    }
                }
                else -> return
            }

        }

        companion object {

            internal val TOUCH_MODE_REST = -1
            internal val TOUCH_MODE_FLING = 3
        }
    }

    private class DragState(var mX: Float, var mY: Float, var mScrollX: Int, var mScrollY: Int) : NoCopySpan {
        var mFarEnough: Boolean = false
        var mUsed: Boolean = false
        var mVelocityTracker: VelocityTracker? = null
        var mFlingRunnable: FlingRunnable? = null

        init {
            mVelocityTracker = null
            mFlingRunnable = null
        }
    }

    /* We check for a onepointfive tap. This is similar to
    *  doubletap gesture (where a finger goes down, up, down, up, in a short
    *  time period), except in the onepointfive tap, a users finger only needs
    *  to go down, up, down in a short time period. We detect this type of tap
    *  to implement the onepointfivetap-and-swipe selection gesture.
    *  This gesture allows users to select a segment of text without going
    *  through the "select text" option in the context menu.
    */
    private class OnePointFiveTapState : NoCopySpan {
        internal var mWhen: Long = 0
        internal var active: Boolean = false
    }

    private class DoubleTapState : NoCopySpan {
        var mWhen: Long = 0
    }


    val LAST_TAP_DOWN = Object()

    fun getInitialScrollX(widget: LongTextView, buffer: Spannable): Int {
        val ds = buffer.getSpans<DragState>(0, buffer.length, DragState::class.java)
        return if (ds.size > 0) ds[0].mScrollX else -1
    }

    fun getInitialScrollY(widget: LongTextView, buffer: Spannable): Int {
        val ds = buffer.getSpans<DragState>(0, buffer.length, DragState::class.java)
        return if (ds.size > 0) ds[0].mScrollY else -1
    }

    /**
     * Handles touch events for dragging.  You may want to do other actions
     * like moving the cursor on touch as well.
     */
    fun handleTouchEvent(widget: LongTextView, buffer: Spannable,
                         event: MotionEvent): Boolean {
        val ds: Array<DragState>

        ds = buffer.getSpans(0, buffer.length, DragState::class.java)

        if (ds.size > 0) {
            if (ds[0].mVelocityTracker == null) {
                ds[0].mVelocityTracker = VelocityTracker.obtain()
            }
            ds[0].mVelocityTracker!!.addMovement(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (ds.size > 0) {
                    if (ds[0].mFlingRunnable != null) {
                        ds[0].mFlingRunnable!!.endFling()
                        widget.cancelLongPress()
                    }
                }
                for (i in ds.indices) {
                    buffer.removeSpan(ds[i])
                }

                buffer.setSpan(DragState(event.x, event.y,
                        widget.scrollX, widget.scrollY),
                        0, 0, Spannable.SPAN_MARK_MARK)

                return true
            }

            MotionEvent.ACTION_UP -> {
                var result = false
                val cap = false
                /*
                val cap = MetaKeyKeyListener.getMetaState(buffer,
                        KeyEvent.META_SHIFT_ON) == 1 || JotaTextKeyListener.getMetaStateSelecting(buffer) !== 0
                        */

                if (ds.size > 0 && ds[0].mUsed) {
                    result = true
                    if (!cap) {
                        val velocityTracker = ds[0].mVelocityTracker
                        val mMinimumVelocity = ViewConfiguration.get(widget.context).scaledMinimumFlingVelocity
                        val mMaximumVelocity = ViewConfiguration.get(widget.context).scaledMaximumFlingVelocity
                        velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                        val initialVelocity = velocityTracker.getYVelocity().toInt()

                        if (Math.abs(initialVelocity) > mMinimumVelocity) {
                            if (ds[0].mFlingRunnable == null) {
                                ds[0].mFlingRunnable = FlingRunnable(widget.context)
                            }

                            ds[0].mFlingRunnable!!.start(widget, -initialVelocity)
                        } else {
                            widget.moveCursorToVisibleOffset()
                        }
                    } else {
                        widget.moveCursorToVisibleOffset()
                    }
                } else {
                    widget.moveCursorToVisibleOffset()
                }

                if (ds[0].mVelocityTracker != null) {
                    ds[0].mVelocityTracker!!.recycle()
                    ds[0].mVelocityTracker = null
                }

                return result
            }
            MotionEvent.ACTION_MOVE ->

                if (ds.size > 0) {
                    if (ds[0].mFarEnough === false) {
                        val slop = ViewConfiguration.get(widget.context).scaledTouchSlop

                        if (Math.abs(event.x - ds[0].mX) >= slop || Math.abs(event.y - ds[0].mY) >= slop) {
                            ds[0].mFarEnough = true
                        }
                    }

                    if (ds[0].mFarEnough) {
                        ds[0].mUsed = true
                        val cap = false
                        /*
                        val cap = MetaKeyKeyListener.getMetaState(buffer,
                                KeyEvent.META_SHIFT_ON) == 1 || MetaKeyKeyListener.getMetaState(buffer,
                                MetaKeyKeyListener.META_SELECTING) != 0
                                */
                        val dx: Float
                        val dy: Float
                        if (cap) {
                            // if we're selecting, we want the scroll to go in
                            // the direction of the drag
                            dx = event.x - ds[0].mX
                            dy = event.y - ds[0].mY
                        } else {
                            dx = ds[0].mX - event.x
                            dy = ds[0].mY - event.y
                        }
                        ds[0].mX = event.x
                        ds[0].mY = event.y

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

                        return true
                    }
                }
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

    // TODO: Unify with TextView.getWordForDictionary()
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
            initialScrollX = getInitialScrollX(widget, buffer)
            initialScrollY = getInitialScrollY(widget, buffer)
        }

        val handled = handleTouchEvent(widget, buffer, event)

        if (widget.isFocused && !widget.didTouchFocusSelect){
            if (event.action == MotionEvent.ACTION_DOWN) {
                val cap = false
                /*
                val cap = MetaKeyKeyListener.getMetaState(buffer,
                        KeyEvent.META_SHIFT_ON) == 1 || JotaTextKeyListener.getMetaStateSelecting(buffer) !== 0
                        */
                val x = event.x.toInt() - lineNumberWidth
                val y = event.y.toInt()
                val offset = getOffset(x, y, widget)

                if (cap) {
                    buffer.setSpan(LAST_TAP_DOWN, offset, offset,
                            Spannable.SPAN_POINT_POINT)

                    // Disallow intercepting of the touch events, so that
                    // users can scroll and select at the same time.
                    // without this, users would get booted out of select
                    // mode once the view detected it needed to scroll.
                    widget.parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    val tap = buffer.getSpans<OnePointFiveTapState>(0, buffer.length,
                            OnePointFiveTapState::class.java)

                    if (tap.size > 0) {
                        if (event.eventTime - tap[0].mWhen <= ViewConfiguration.getDoubleTapTimeout() && sameWord(buffer, offset, Selection.getSelectionEnd(buffer))) {

                            tap[0].active = true
                            // MetaKeyKeyListener.startSelecting(widget, buffer)
                            widget.parent.requestDisallowInterceptTouchEvent(true)
                            buffer.setSpan(LAST_TAP_DOWN, offset, offset,
                                    Spannable.SPAN_POINT_POINT)
                        }

                        tap[0].mWhen = event.eventTime
                    } else {
                        val newtap = OnePointFiveTapState()
                        newtap.mWhen = event.eventTime
                        newtap.active = false
                        buffer.setSpan(newtap, 0, buffer.length,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                }
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val cap = false
                /*
                val cap = MetaKeyKeyListener.getMetaState(buffer,
                        KeyEvent.META_SHIFT_ON) == 1 || JotaTextKeyListener.getMetaStateSelecting(buffer) !== 0
                        */

                if (cap && handled) {
                    // Before selecting, make sure we've moved out of the "slop".
                    // handled will be true, if we're in select mode AND we're
                    // OUT of the slop

                    // Turn long press off while we're selecting. User needs to
                    // re-tap on the selection to enable longpress
                    widget.cancelLongPress()

                    // Update selection as we're moving the selection area.

                    // Get the current touch position
                    val x = event.x.toInt() - lineNumberWidth    // Jota Text Editor
                    val y = event.y.toInt()
                    val offset = getOffset(x, y, widget)

                    val tap = buffer.getSpans<OnePointFiveTapState>(0, buffer.length,
                            OnePointFiveTapState::class.java)

                    if (tap.size > 0 && tap[0].active) {
                        // Get the last down touch position (the position at which the
                        // user started the selection)
                        val lastDownOffset = buffer.getSpanStart(LAST_TAP_DOWN)

                        // Compute the selection boundaries
                        val spanstart: Int
                        val spanend: Int
                        if (offset >= lastDownOffset) {
                            // Expand from word start of the original tap to new word
                            // end, since we are selecting "forwards"
                            spanstart = findWordStart(buffer, lastDownOffset)
                            spanend = findWordEnd(buffer, offset)
                        } else {
                            // Expand to from new word start to word end of the original
                            // tap since we are selecting "backwards".
                            // The spanend will always need to be associated with the touch
                            // up position, so that refining the selection with the
                            // trackball will work as expected.
                            spanstart = findWordEnd(buffer, lastDownOffset)
                            spanend = findWordStart(buffer, offset)
                        }
                        Selection.setSelection(buffer, spanstart, spanend)
                    } else {
                        Selection.extendSelection(buffer, offset)
                    }
                    return true
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                // If we have scrolled, then the up shouldn't move the cursor,
                // but we do need to make sure the cursor is still visible at
                // the current scroll offset to avoid the scroll jumping later
                // to show it.
                if (initialScrollY >= 0 && initialScrollY != widget.scrollY || initialScrollX >= 0 && initialScrollX != widget.scrollX) {
                    //                    widget.moveCursorToVisibleOffset();
                    return true
                }

                val x = event.x.toInt() - lineNumberWidth    // Jota Text Editor
                val y = event.y.toInt()
                val off = getOffset(x, y, widget)

                // XXX should do the same adjust for x as we do for the line.

                val onepointfivetap = buffer.getSpans<OnePointFiveTapState>(0, buffer.length,
                        OnePointFiveTapState::class.java)
                if (onepointfivetap.size > 0 && onepointfivetap[0].active &&
                        Selection.getSelectionStart(buffer) == Selection.getSelectionEnd(buffer)) {
                    // If we've set select mode, because there was a onepointfivetap,
                    // but there was no ensuing swipe gesture, undo the select mode
                    // and remove reference to the last onepointfivetap.
                    // MetaKeyKeyListener.stopSelecting(widget, buffer)

                    for (i in onepointfivetap.indices) {
                        buffer.removeSpan(onepointfivetap[i])
                    }
                    buffer.removeSpan(LAST_TAP_DOWN)
                }

                val cap = false
                /*
                val cap = MetaKeyKeyListener.getMetaState(buffer,
                        KeyEvent.META_SHIFT_ON) == 1 || JotaTextKeyListener.getMetaStateSelecting(buffer) !== 0
                        */

                val tap = buffer.getSpans<DoubleTapState>(0, buffer.length,
                        DoubleTapState::class.java)
                var doubletap = false

                if (tap.size > 0) {
                    if (event.eventTime - tap[0].mWhen <= ViewConfiguration.getDoubleTapTimeout() && sameWord(buffer, off, Selection.getSelectionEnd(buffer))) {

                        doubletap = true
                    }

                    tap[0].mWhen = event.eventTime
                } else {
                    val newtap = DoubleTapState()
                    newtap.mWhen = event.eventTime
                    buffer.setSpan(newtap, 0, buffer.length,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }

                if (cap) {
                    buffer.removeSpan(LAST_TAP_DOWN)
                    if (onepointfivetap.size > 0 && onepointfivetap[0].active) {
                        // If we selecting something with the onepointfivetap-and
                        // swipe gesture, stop it on finger up.
                        // MetaKeyKeyListener.stopSelecting(widget, buffer)
                    } else {
                        Selection.extendSelection(buffer, off)
                    }
                } else if (doubletap) {
                    selectWord(buffer, off)
                } else {
                    Selection.setSelection(buffer, off)
                }

                MetaKeyKeyListener.adjustMetaAfterKeypress(buffer)
                // resetLockedMeta(buffer.toLong())

                return true
            }
        }

        return handled
    }

    fun selectWord(buffer: Spannable, off: Int) : Boolean {
        val start = findWordStart(buffer, off)
        val end = findWordEnd(buffer, off)
        if(start == end) return false
        Selection.setSelection(buffer, start, end)
        return true
    }


}