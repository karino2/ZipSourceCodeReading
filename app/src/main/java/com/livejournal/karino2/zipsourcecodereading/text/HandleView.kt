package com.livejournal.karino2.zipsourcecodereading.text

import android.graphics.Canvas
import android.graphics.Rect
import android.view.*
import android.widget.PopupWindow


/**
 * Created by _ on 2017/10/05.
 */
abstract class HandleView(val parent: LongTextView,  pos: HandleSide) : View(parent.context) {
    enum class HandleSide {
        LEFT,
        RIGHT
    }

    val drawable by lazy {
        when(pos) {
            HandleSide.LEFT -> parent.handleLeftDrawable
            HandleSide.RIGHT -> parent.handleRightDrawable
        }
    }
    private val container: PopupWindow by lazy {
        val pop = PopupWindow(parent.context, null,
                android.R.attr.textSelectHandleWindowStyle)
        pop.setClippingEnabled(false)
        pop
    }

    private var positionX: Int = 0
    private var positionY: Int = 0

    var isDragging: Boolean = false
    private set

    private var touchToWindowOffsetX: Float = 0.toFloat()
    private var touchToWindowOffsetY: Float = 0.toFloat()

    val hotspotX: Float by lazy {
        when(pos) {
            HandleSide.LEFT -> {
                val handleWidth = drawable.intrinsicWidth
                (handleWidth * 3 / 4).toFloat()
            }
            HandleSide.RIGHT -> {
                val handleWidth = drawable.intrinsicWidth
                (handleWidth / 4).toFloat()
            }
        }
    }


    val hotspotY = 0f
    val _height by lazy { drawable.intrinsicHeight }
    val touchOffsetY: Float by lazy { -drawable.intrinsicHeight * 0.3f }
    private var lastParentX: Int = 0
    private var lastParentY: Int = 0

    val isShowing: Boolean
    get() = container.isShowing

    val tempRect = Rect()
    var tempCoords = IntArray(2)

    private
    val isPositionVisible: Boolean
    get() {
        if (isDragging) {
            return true
        }

        val extendedPaddingTop = parent.paddingTop
        val extendedPaddingBottom = parent.paddingBottom
        val compoundPaddingLeft = parent.paddingLeft
        val compoundPaddingRight = parent.paddingRight

        val hostView = parent
        val left = 0
        val right = hostView.width
        val top = 0
        val bottom = hostView.height

        val clip = tempRect
        clip.left = left + compoundPaddingLeft
        clip.top = top + extendedPaddingTop
        clip.right = right - compoundPaddingRight
        clip.bottom = bottom - extendedPaddingBottom

        val _parent = hostView.parent
        if (_parent == null || !_parent!!.getChildVisibleRect(hostView, clip, null)) {
            return false
        }

        val coords = tempCoords
        hostView.getLocationInWindow(coords)
        val posX = coords[0] + positionX + hotspotX.toInt()
        val posY = coords[1] + positionY + hotspotY.toInt()

        return posX >= clip.left && posX <= clip.right &&
                posY >= clip.top && posY <= clip.bottom
    }

    init {
        invalidate()
    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(drawable!!.intrinsicWidth,
                drawable!!.intrinsicHeight)
    }

    fun show() {
        if (!isPositionVisible) {
            hide()
            return
        }
        container.setContentView(this)
        val coords = tempCoords
        parent.getLocationInWindow(coords)
        coords[0] += positionX
        coords[1] += positionY
        container.showAtLocation(parent, Gravity.NO_GRAVITY, coords[0], coords[1])
    }

    fun hide() {
        isDragging = false
        container.dismiss()
    }

    private fun moveTo(x: Int, y: Int) {
        positionX = x - parent.scrollX
        positionY = y - parent.scrollY
        if (isPositionVisible) {
            var coords: IntArray? = null
            if (container.isShowing()) {
                coords = tempCoords
                parent.getLocationInWindow(coords)
                container.update(coords!![0] + positionX, coords[1] + positionY,
                        parent.right - parent.left, parent.bottom - parent.top)
            } else {
                show()
            }

            if (isDragging) {
                if (coords == null) {
                    coords = tempCoords
                    parent.getLocationInWindow(coords)
                }
                if (coords!![0] != lastParentX || coords[1] != lastParentY) {
                    touchToWindowOffsetX += (coords[0] - lastParentX).toFloat()
                    touchToWindowOffsetY += (coords[1] - lastParentY).toFloat()
                    lastParentX = coords[0]
                    lastParentY = coords[1]
                }
            }
        } else {
            hide()
        }
    }

    override fun onDraw(c: Canvas) {
        // drawable!!.setBounds(0, 0, parent.right - parent.left, parent.bottom - parent.top)
        drawable!!.setBounds(0, 0, drawable!!.intrinsicWidth, drawable!!.intrinsicHeight)
        drawable!!.draw(c)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val rawX = ev.rawX
                val rawY = ev.rawY
                touchToWindowOffsetX = rawX - positionX
                touchToWindowOffsetY = rawY - positionY
                val coords = tempCoords
                parent.getLocationInWindow(coords)
                lastParentX = coords[0]
                lastParentY = coords[1]
                isDragging = true
            }

            MotionEvent.ACTION_MOVE -> {
                val rawX = ev.rawX
                val rawY = ev.rawY
                val newPosX = rawX - touchToWindowOffsetX + hotspotX - parent.lineNumberWidth
                val newPosY = rawY - touchToWindowOffsetY + hotspotY + touchOffsetY

                updatePosition(this, Math.round(newPosX+offsetX), Math.round(newPosY))
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return true
    }

    abstract fun updatePosition(handleView: HandleView, x: Int, y: Int)

    val layout: Layout
    get() = parent.layout!!

    fun positionAtCursor(offset: Int, bottom: Boolean) {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val line = layout.getLineForOffset(offset)
        val lineTop = layout.getLineTop(line)
        val lineBottom = layout.getLineBottom(line)

        val bounds = tempRect
        bounds.left = ((layout.getPrimaryHorizontal(offset) - hotspotX).toInt()
                + parent.scrollX + parent.lineNumberWidth)
        bounds.top = (if (bottom) lineBottom else lineTop - _height) + parent.scrollY

        bounds.right = bounds.left + width
        bounds.bottom = bounds.top + height

        parent.convertFromViewportToContentCoordinates(bounds)
        moveTo(bounds.left+offsetX, bounds.top)
    }
    val offsetX = -20
}