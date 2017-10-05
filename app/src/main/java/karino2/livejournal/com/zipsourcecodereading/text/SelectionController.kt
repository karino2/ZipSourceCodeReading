package karino2.livejournal.com.zipsourcecodereading.text

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.content.ContentValues.TAG
import android.text.Selection
import android.util.Log
import android.view.ViewTreeObserver


/**
 * Created by _ on 2017/10/05.
 */
class SelectionController(val parent: LongTextView) : ViewTreeObserver.OnTouchModeChangeListener {
    // The cursor controller images
    private val mStartHandle by lazy {
        object : HandleView(parent, HandleView.HandleSide.LEFT) {
            override fun updatePosition(handleView: HandleView, x: Int, y: Int) {
                updatePosition(handleView, x, y)
            }

        }
    }


    private val mEndHandle by lazy {
        object : HandleView(parent, HandleView.HandleSide.RIGHT) {
            override fun updatePosition(handleView: HandleView, x: Int, y: Int) {
                updatePosition(handleView, x, y)
            }
       }
    }

    var lastTouchOffset: Int = 0
    // Whether selection anchors are active
    var isShowing: Boolean = false
        private set
    // Double tap detection
    private var mPreviousTapUpTime: Long = 0
    var lastTapPositionX: Int = 0
    var lastTapPositionY: Int = 0

    /**
     * @return true iff this controller is currently used to move the selection start.
     */
    val isSelectionStartDragged: Boolean
        get() = mStartHandle.isDragging

    init {
        resetTouchOffsets()
    }

    fun show() {
        isShowing = true
        updatePosition()
        mStartHandle.show()
        mEndHandle.show()
    }

    fun hide() {
        mStartHandle.hide()
        mEndHandle.hide()
        isShowing = false
    }

    fun updatePosition(handle: HandleView, x: Int, y: Int) {
        var selectionStart = parent.selectionStart
        var selectionEnd = parent.selectionEnd

        val previousOffset = if (handle === mStartHandle) selectionStart else selectionEnd
        var offset = parent.getHysteresisOffset(x, y, previousOffset)

        // Handle the case where start and end are swapped, making sure start <= end
        if (handle === mStartHandle) {
            if (selectionStart == offset || offset > selectionEnd) {
                return  // no change, no need to redraw;
            }
            // If the user "closes" the selection entirely they were probably trying to
            // select a single character. Help them out.
            if (offset == selectionEnd) {
                offset = selectionEnd - 1
            }
            selectionStart = offset
        } else {
            if (selectionEnd == offset || offset < selectionStart) {
                return  // no change, no need to redraw;
            }
            // If the user "closes" the selection entirely they were probably trying to
            // select a single character. Help them out.
            if (offset == selectionStart) {
                offset = selectionStart + 1
            }
            selectionEnd = offset
        }

        Selection.setSelection(parent.text, selectionStart, selectionEnd)
        updatePosition()
    }

    fun updatePosition() {
        if (!isShowing) {
            return
        }

        val selectionStart = parent.selectionStart
        val selectionEnd = parent.selectionEnd

        if (selectionStart < 0 || selectionEnd < 0) {
            // Should never happen, safety check.
            Log.w(TAG, "Update selection controller position called with no cursor")
            hide()
            return
        }

        mStartHandle.positionAtCursor(selectionStart, true)
        mEndHandle.positionAtCursor(selectionEnd, true)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        // This is done even when the View does not have focus, so that long presses can start
        // selection and tap can move cursor from this tap position.
        //            if (isTextEditable()) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt()
                val y = event.y.toInt()

                // Remember finger down position, to be able to start selection from there
                lastTouchOffset = parent.getOffset(x, y)

                // Double tap detection
                val duration = SystemClock.uptimeMillis() - mPreviousTapUpTime
                if (duration <= ViewConfiguration.getDoubleTapTimeout()) {
                    val deltaX = x - lastTapPositionX
                    val deltaY = y - lastTapPositionY
                    val distanceSquared = deltaX * deltaX + deltaY * deltaY
                    val doubleTapSlop = ViewConfiguration.get(parent.context).scaledDoubleTapSlop
                    val slopSquared = doubleTapSlop * doubleTapSlop
                    if (distanceSquared < slopSquared) {
                        parent.startSelectionActionMode()
                    }
                }
                lastTapPositionX = x
                lastTapPositionY = y
            }

            MotionEvent.ACTION_UP -> mPreviousTapUpTime = SystemClock.uptimeMillis()
        }
        return false
    }

    fun resetTouchOffsets() {
        lastTouchOffset = -1
    }

    override fun onTouchModeChanged(isInTouchMode: Boolean) {
        if (!isInTouchMode) {
            hide()
        }
    }

    fun onDetached() {}
}
