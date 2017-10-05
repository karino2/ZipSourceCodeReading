package karino2.livejournal.com.zipsourcecodereading.text

import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.text.Selection
import android.text.SpannableString
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import karino2.livejournal.com.zipsourcecodereading.R


/**
 * Created by _ on 2017/10/03.
 *
 * LongTextView for long text with coloring. Do not support WRAP_CONTENT and make resize faster.
 */
class LongTextView(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener{

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isLongClickable = true
    }



    val handleLeftDrawable by lazy {
        getResources().getDrawable(R.drawable.text_select_handle_left)
    }

    val handleRightDrawable by lazy {
        getResources().getDrawable(R.drawable.text_select_handle_right)
    }

    val lastDownPositionX
    get() = selectionController.lastTapPositionX

    val lastDownPositionY
    get() = selectionController.lastTapPositionY


    fun isPositionOnText(x: Float, y: Float): Boolean {
        if(layout == null) { return false }

        val line = getLineFromViewportY(y.toInt())

        val contX = x + viewportToContentHorizontalOffset()
        if (contX < layout!!.getLineLeft(line))
            return false

        val end = layout!!.getLineRight(line)
        if (contX > layout!!.getLineRight(line))
            return false
        return true
    }

    var actionMode : ActionMode? = null

    fun touchPositionInSelection() : Boolean {
        if(selectionStart == selectionEnd) { return false }

        val start = Math.min(selectionStart, selectionEnd)
        val end = Math.max(selectionStart, selectionEnd)

        return selectionController.lastTouchOffset in start..end
    }

    override fun performLongClick(): Boolean {
        var handled = super.performLongClick()

        if(!handled && !isPositionOnText(lastDownPositionX.toFloat(), lastDownPositionY.toFloat())) {
            val offset = getOffset(lastDownPositionX, lastDownPositionY)
            stopSelectionActionMode()
            Selection.setSelection(text, offset)
            handled = true
        }

        if(!handled && actionMode != null) {
            if(touchPositionInSelection()) {
                // start drag in original TextView, but we do nothing in this case.
                // just keep action mode as is.
            } else {
                selectionController.hide()
                selectCurrentWord()
                selectionController.show()
            }
            handled = true
        }

        if(! handled) {
            handled = startSelectionActionMode()
        }

        if(handled)
        {
            eatTouchRelease = true
        }
        return handled
    }

    fun selectCurrentWord() = movement.selectWord(text, selectionController.lastTouchOffset)

    inner class SelectionActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.setTitle("Selecting...")
            mode.subtitle = null
            mode.titleOptionalHint = true

            menu.add(0, R.id.ID_COPY, 0, R.string.copy).setAlphabeticShortcut('c').setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
            menu.add(0, R.id.ID_SEARCH, 0, R.string.search).setAlphabeticShortcut('s').setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT
            )
            menu.add(0, R.id.ID_GSEARCH, 0, R.string.gsearch).setAlphabeticShortcut('g').setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_WITH_TEXT
            )

            if (menu.hasVisibleItems()) {
                selectionController.show()
                // mTextView.setHasTransientState(true)
                return true
            } else {
                return false
            }
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = true

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = onTextContextMenuItem(item.itemId)

        override fun onDestroyActionMode(mode: ActionMode) {
            Selection.setSelection(text, selectionEnd)
            selectionController.hide()

            actionMode = null
        }
    }

    private fun onTextContextMenuItem(itemId: Int): Boolean {
        when(itemId) {
            R.id.ID_COPY ->{
                copySelection()
                return true
            }
            R.id.ID_SEARCH-> {

            }
            R.id.ID_GSEARCH-> {

            }
        }
        return false
    }

    fun startSelectionActionMode(): Boolean {
        if(actionMode != null) {
            // already there
            return false
        }

        if(!requestFocus()) {
            // not support text selection. Is this occur in our case?
            return false
        }

        if(!hasSelection) {
            if(!selectCurrentWord()) {
                return false
            }
        }
        actionMode = startActionMode(SelectionActionModeCallback())
        return actionMode != null
    }

    val hasSelection
    get() = selectionStart >=0 && selectionStart != selectionEnd

    fun stopSelectionActionMode() {
        actionMode?.let {
            actionMode!!.finish()
        }
    }

    /*
    override fun onCreateContextMenu(menu: ContextMenu) {
        super.onCreateContextMenu(menu)

        val selected = selectionStart != selectionEnd

        if(!selected) {

        }


        if(selected) {
            menu.add(0, R.id.ID_CANCEL_SELECTION, 0, R.string.cancel_select)
                    .setOnMenuItemClickListener{ cancelSelection(); true }
            menu.add(0, R.id.ID_COPY, 0, R.string.copy)
                    .setOnMenuItemClickListener { copySelection(); true }
            menu.add(0, R.id.ID_SEARCH, 0, R.string.search)
            menu.add(0, R.id.ID_GSEARCH, 0, R.string.gsearch)
        } else {
            menu.add(0, R.id.ID_START_SELECTION, 0, R.string.start_select)
        }

        // hideControloers()



    }
    */

    val clipManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private fun copySelection(){
        clipManager.setText(selectedText)
        stopSelectionActionMode()
    }

    private val selectedText: CharSequence
    get() {
        val minsel = Math.max(0, Math.min(selectionStart, selectionEnd))
        val maxsel = Math.max(0, Math.max(selectionStart, selectionEnd))
        val selected = text.subSequence(minsel, maxsel)
        return selected
    }

    var text = SpannableString("Loading...")
    set(newText) {
        textPaint.textScaleX = 1F

        field = newText

        movement.initialize(this, text)

        layout?.let {
            // This view does not support wrap_content, so chnaging text does not cause relayout.
            val want = layout!!.width
            makeNewLayout(want, false)

            invalidate()
        }

    }

    val movement = MovementMethod()

    var layout : Layout? = null


    var horizontallyScrolling = false
    private val VERY_WIDE = 16384 * 4

    var mHighlightPathBogus = false

    fun assumeLayout() {
        var width = Math.max(0, right - left - paddingLeft - paddingRight)
        if(horizontallyScrolling) {
            width = VERY_WIDE
        }
        makeNewLayout(width, false)
    }

    val textPaint  by lazy {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = Typeface.MONOSPACE
        tp.textSize = resolveSp(18F)
        tp
    }

    fun resolveSp(sp : Float) =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)

    private fun makeNewLayout(width: Int,  bringIntoView: Boolean) {
        mHighlightPathBogus = true

        var w = width
        if (w < 0) {
            w = 0;
        }

        val spacingMulti = 1F
        val spacingAdd = 0F
        val includePad = true

        layout = Layout.create(text, textPaint, w, spacingMulti, spacingAdd,  includePad)

        setupIdeographicalSpacePath()
        setupLineBreakPath()
        setupTabPath()

        if(bringIntoView) {
            registerForPreDraw()
        }
    }


    enum class PREDRAW_STATE {
        NOT_REGISTERED,
        PENDING,
        DONE
    }

    var  preDrawState: PREDRAW_STATE = PREDRAW_STATE.NOT_REGISTERED



    private fun registerForPreDraw() {
        val observer = viewTreeObserver
        observer?.let {
            when(preDrawState) {
                PREDRAW_STATE.NOT_REGISTERED -> {
                    observer.addOnPreDrawListener(this)
                    preDrawState = PREDRAW_STATE.PENDING
                }
                PREDRAW_STATE.DONE -> {
                    preDrawState = PREDRAW_STATE.PENDING
                }
                PREDRAW_STATE.PENDING -> {}
            }
        }

    }


    private fun getInterestingRect(r: Rect, line: Int) {
        convertFromViewportToContentCoordinates(r)

        // Rectangle can can be expanded on first and last line to take
        // padding into account.
        if (line == 0) r.top -= paddingTop
        if (line == layout!!.lineCount - 1) r.bottom += paddingBottom
    }

    fun convertFromViewportToContentCoordinates(r: Rect) {
        val horizontalOffset = viewportToContentHorizontalOffset()
        r.left += horizontalOffset
        r.right += horizontalOffset

        val verticalOffset = viewportToContentVerticalOffset()
        r.top += verticalOffset
        r.bottom += verticalOffset
    }

    private fun viewportToContentHorizontalOffset(): Int {
        return paddingLeft - scrollX
    }

    private fun viewportToContentVerticalOffset(): Int {
        var offset = paddingTop - scrollY
        /*
        if (mGravity and Gravity.VERTICAL_GRAVITY_MASK !== Gravity.TOP) {
            offset += getVerticalOffset(false)
        }
        */
        return offset
    }

    /**
     * Move the point, specified by the offset, into the view if it is needed.
     * This has to be called after layout. Returns true if anything changed.
     */
    fun bringPointIntoView(offset: Int): Boolean {
        var changed = false

        val line = layout!!.getLineForOffset(offset)

        val x = layout!!.getPrimaryHorizontal(offset).toInt() + lineNumberWidth
        val top = layout!!.getLineTop(line)
        val bottom = layout!!.getLineTop(line + 1)

        val left = Math.floor(layout!!.getLineLeft(line).toDouble()).toInt()
        val right = Math.ceil((layout!!.getLineRight(line) + lineNumberWidth).toDouble()).toInt()
        var ht = layout!!.height

        val forceScroll = true
        if (forceScroll) {
            ht += layout!!.getLineTop(7)
        }

        val grav: Int

        when (layout!!.getParagraphAlignment(line)) {
            android.text.Layout.Alignment.ALIGN_NORMAL -> grav = 1

            android.text.Layout.Alignment.ALIGN_OPPOSITE -> grav = -1

            else -> grav = 0
        }

        // grav *= layout!!.getParagraphDirection(line)

        val hspace = this.right - this.left - paddingLeft - paddingRight
        val vspace = this.bottom - this.top - paddingTop - paddingBottom

        var hslack = (bottom - top) / 2
        var vslack = hslack

        if (vslack > vspace / 4)
            vslack = vspace / 4
        if (hslack > hspace / 4)
            hslack = hspace / 4

        var hs = scrollX
        var vs = scrollY

        if (top - vs < vslack)
            vs = top - vslack
        if (bottom - vs > vspace - vslack)
            vs = bottom - (vspace - vslack)
        if (ht - vs < vspace)
            vs = ht - vspace
        if (0 - vs > 0)
            vs = 0

        if (grav != 0) {
            if (x - hs < hslack) {
                hs = x - hslack
            }
            if (x - hs > hspace - hslack) {
                hs = x - (hspace - hslack)
            }
        }

        if (grav < 0) {
            if (left - hs > 0)
                hs = left
            if (right - hs < hspace)
                hs = right - hspace
        } else if (grav > 0) {
            if (right - hs < hspace)
                hs = right - hspace
            if (left - hs > 0)
                hs = left
        } else
        /* grav == 0 */ {
            if (right - left <= hspace) {
                /*
                 * If the entire text fits, center it exactly.
                 */
                hs = left - (hspace - (right - left)) / 2
            } else if (x > right - hslack) {
                /*
                 * If we are near the right edge, keep the right edge
                 * at the edge of the view.
                 */
                hs = right - hspace
            } else if (x < left + hslack) {
                /*
                 * If we are near the left edge, keep the left edge
                 * at the edge of the view.
                 */
                hs = left
            } else if (left > hs) {
                /*
                 * Is there whitespace visible at the left?  Fix it if so.
                 */
                hs = left
            } else if (right < hs + hspace) {
                /*
                 * Is there whitespace visible at the right?  Fix it if so.
                 */
                hs = right - hspace
            } else {
                /*
                 * Otherwise, float as needed.
                 */
                if (x - hs < hslack) {
                    hs = x - hslack
                }
                if (x - hs > hspace - hslack) {
                    hs = x - (hspace - hslack)
                }
            }
        }

        if (hs != scrollX || vs != scrollY) {
            scrollTo(hs, vs)
            /*
            if (mScroller == null) {
                scrollTo(hs, vs)
            } else {
                val duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll
                val dx = hs - mScrollX
                val dy = vs - mScrollY

                if (duration > ANIMATED_SCROLL_GAP) {
                    mScroller.startScroll(mScrollX, mScrollY, dx, dy)
                    // Jota Text Editor
                    //awakenScrollBars(mScroller.getDuration());
                    invalidate()
                } else {
                    if (!mScroller.isFinished()) {
                        mScroller.abortAnimation()
                    }

                    scrollBy(dx, dy)
                }

                mLastScroll = AnimationUtils.currentAnimationTimeMillis()
            }
            */

            changed = true
        }

        if (isFocused) {
            // This offsets because getInterestingRect() is in terms of
            // viewport coordinates, but requestRectangleOnScreen()
            // is in terms of content coordinates.

            val r = Rect(x, top, x + 1, bottom)
            getInterestingRect(r, line)
            r.offset(scrollX, scrollY)

            if (requestRectangleOnScreen(r)) {
                changed = true
            }
        }

        return changed
    }

    override fun onPreDraw(): Boolean {
        if(preDrawState != PREDRAW_STATE.PENDING)
            return true

        if(layout == null) {
            assumeLayout()
        }

        var curs = selectionEnd
        /*
        if (selectionController != null && selectionController.isSelectionStartDragged()) {
            curs = getSelectionStart()
        }
        */

        var changed = false

        if (curs >= 0) {
            changed = bringPointIntoView(curs)
        }


        preDrawState = PREDRAW_STATE.DONE
        return changed
    }

    var eatTouchRelease = false
    var scrolled = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action

        selectionController.onTouchEvent(event)

        if(action == MotionEvent.ACTION_DOWN) {
            // Reset this state; it will be re-set if super.onTouchEvent
            // causes focus to move to the view.
            touchFocusSelected = false
            scrolled = false
        }

        val superHandled = super.onTouchEvent(event)

        /*
          * Don't handle the release after a long press, because it will
          * move the selection away from whatever the menu action was
          * trying to affect.
          */
        if(eatTouchRelease && action == MotionEvent.ACTION_UP) {
            eatTouchRelease = false
            return superHandled
        }

        if(layout != null){
            var handled = false

            // Save previous selection, in case this event is used to show the IME.
            val oldSelStart = selectionStart
            val oldSelEnd = selectionEnd

            val oldScrollX = scrollX
            val oldScrollY = scrollY

            handled = handled or movement.onTouchEvent(this, text, event)

            if (action == MotionEvent.ACTION_UP && isFocused && !scrolled) {
                // Cannot be done by CommitSelectionReceiver, which might not always be called,
                // for instance when dealing with an ExtractEditText.
                onTapUpEvent(oldSelStart, oldSelEnd)
            }
            if (handled) {
                return true
            }
        }

        return superHandled
    }

    private fun onTapUpEvent(prevStart: Int, prevEnd: Int) {
        val start = selectionStart
        val end = selectionEnd

        if (start == end) {
            val selectAllOnFocus = false

            val tapInsideSelectAllOnFocus = selectAllOnFocus && prevStart == 0 &&
                    prevEnd == text.length
            if (start >= prevStart && start < prevEnd && !tapInsideSelectAllOnFocus) {
                // Restore previous selection
                Selection.setSelection(text, prevStart, prevEnd)

                // Tapping inside the selection displays the cut/copy/paste context menu
                startSelectionActionMode()
                selectionController.show()
            } else {
                // Tapping outside stops selection mode, if any
                stopSelectionActionMode()

                /*
                val selectAllGotFocus = selectAllOnFocus && touchFocusSelected
                if (hasInsertionController() && !selectAllGotFocus) {
                    getInsertionController().show()
                    hideSelectionModifierCursorController()
                }
                */
            }
        }
    }

    private fun setupTabPath() {
        tabPath.reset()
        val textHeight = -textPaint.ascent() / 2
        tabPath.moveTo(1F, -textHeight);
        tabPath.lineTo(1F + textHeight / 2, -textHeight / 2);
        tabPath.lineTo(1F, 0F);
    }

    private fun setupLineBreakPath() {
        lineBreakPath.reset()
        val mw = textPaint.measureText("m")
        val textHeight = -textPaint.descent() - textPaint.ascent()
        lineBreakPath.moveTo(mw / 4, -textHeight);
        lineBreakPath.lineTo(mw / 4, 0F);
        lineBreakPath.moveTo(mw / 4, -1F);
        lineBreakPath.lineTo(1F, -mw / 4 - 1);
        lineBreakPath.moveTo(mw / 4, -1F);
        lineBreakPath.lineTo(mw / 2 - 1F, -mw / 4 - 1F);
    }

    private fun setupIdeographicalSpacePath() {
        ideographicalSpacePath.reset()

        val ideoWidth = textPaint.measureText("\u3000")
        val textHeight = -textPaint.descent() - textPaint.ascent()

        ideographicalSpacePath.addRect(2F, -textHeight, ideoWidth - 2F, 0F, Path.Direction.CW)
    }

    val showLineNumber = true
    val wrapWidthNumber = 0
    val wrapWidthChar = "m"

    val tabWidthNumber = 0
    val tabWidthChar = "m"

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        var width: Int
        var height: Int



        if (widthMode == View.MeasureSpec.EXACTLY) {
            width = widthSize
        } else {
            // do not support wrap content, so just pass dummy value
            width = 50
        }

        if (showLineNumber) {
            lineNumberWidth = lineNumberPaint.measureText("888888|").toInt()
        } else {
            lineNumberWidth = 0
        }

        /*
        ArrowKeyMovementMethod.setLineNumberWidth(mLineNumberWidth)
        Touch.setLineNumberWidth(mLineNumberWidth)
        */
        MovementMethod.lineNumberWidth = lineNumberWidth
        var want = width - paddingLeft - paddingRight - lineNumberWidth

        if (wrapWidthNumber > 0) {
            val widths = FloatArray(1)
            textPaint.getTextWidths(wrapWidthChar, widths)
            want = wrapWidthNumber * widths[0].toInt()
        }
        if (tabWidthNumber > 0) {
            val widths = FloatArray(1)
            textPaint.getTextWidths(tabWidthChar, widths)
            layout!!.tabSize = tabWidthNumber * widths[0].toInt()
        }

        val unpaddedWidth = want

        if (horizontallyScrolling)
            want = VERY_WIDE

        if (layout == null) {
            makeNewLayout(want, false)
        } else if (layout!!.width !== want) {
            if (want > layout!!.width) {
                layout!!.increaseWidthTo(want)
            } else {
                makeNewLayout(want, false)
            }
        } else {
            // Width has not changed.
        }

        if (heightMode == View.MeasureSpec.EXACTLY) {
            height = heightSize
        } else {
            // do not support wrap content. just put dummy value
            height = 50
        }

        var unpaddedHeight = height - paddingTop - paddingBottom

        /*
         * We didn't let makeNewLayout() register to bring the cursor into view,
         * so do it here if there is any possibility that it is needed.
         */
        /*
        if (mMovement != null ||
                layout.getWidth() > unpaddedWidth ||
                layout.getHeight() > unpaddedHeight) {
                */
        if(layout!!.width > unpaddedWidth ||
                layout!!.height > unpaddedHeight) {
            registerForPreDraw()
        } else {
            scrollTo(0, 0)
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val selStart = -1
        val selEnd = -1
        val voffsetCursor = 0
        val voffsetText = 0

        if(preDrawState == PREDRAW_STATE.DONE) {
            val observer = viewTreeObserver
            observer?.let {
                observer.removeOnPreDrawListener(this)
                preDrawState = PREDRAW_STATE.NOT_REGISTERED
            }
        }

        if(layout == null) {
            assumeLayout()
        }

        val selLine = layout!!.getLineForOffset(selEnd)


        layout!!.draw(canvas, voffsetCursor-voffsetText, selLine, lineNumberWidth, lineNumberPaint, spacePaths)

    }


    val selectionController by lazy {
        val cont = SelectionController(this)
        val observer = viewTreeObserver
        observer?.let {
            observer.addOnTouchModeChangeListener(cont)
        }
        cont
    }

    private val ideographicalSpacePath = Path()
    private val lineBreakPath = Path()
    private val tabPath = Path()
    private val spacePaths = arrayOf<Path>(tabPath, lineBreakPath, ideographicalSpacePath)

    var lineNumberWidth = 0

    val lineNumberPaint : Paint by lazy {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = resolveSp(10F)
        paint.typeface = Typeface.MONOSPACE
        paint.strokeWidth = 1F
        paint
    }

    val selectionStart : Int
    get() = Selection.getSelectionStart(text)

    val selectionEnd : Int
    get() = Selection.getSelectionEnd(text)


    var touchFocusSelected = false

    /**
     * Returns true, only while processing a touch gesture, if the initial
     * touch down event caused focus to move to the text view and as a result
     * its selection changed.  Only valid while processing the touch gesture
     * of interest.
     */
    val didTouchFocusSelect : Boolean
    get() = touchFocusSelected

    fun moveCursorToVisibleOffset() : Boolean {
        val end = selectionEnd
        val endline = layout!!.getLineForOffset(end)
        val top = layout!!.getLineTop(endline)
        val bottom = layout!!.getLineTop(endline+1)
        val vspace = this.bottom - this.top - paddingTop - paddingBottom

        val vslack = 0

        val vs = scrollY

        val line = if(top < (vs+vslack)) {
            layout!!.getLineForVertical(vs+vslack+(bottom-top))
        } else {
            layout!!.getLineForVertical(vspace+vs-vslack-(bottom-top))
        }

        val hspace = this.right - this.left - paddingLeft - paddingRight
        val hs = scrollX
        val leftChar = layout!!.getOffsetForHorizontal(line, hs.toFloat())
        val rightChar = layout!!.getOffsetForHorizontal(line, (hspace+hs).toFloat())

        var newEnd = end
        if (newEnd < leftChar) {
            newEnd = leftChar
        } else if (newEnd > rightChar) {
            newEnd = rightChar
        }

        if (newEnd != end) {
            Selection.setSelection(text, newEnd)
            stopSelectionActionMode()
            return true
        }

        return false

    }



    fun moveToLine(line : Int) {
        val offset = layout!!.getLineStart(line)
        Selection.setSelection(text, offset, offset)

        registerForPreDraw()
        invalidate()
    }


    /**
     * Get the offset character closest to the specified absolute position.
     *
     * @param x The horizontal absolute position of a point on screen
     * @param y The vertical absolute position of a point on screen
     * @return the character offset for the character whose position is closest to the specified
     * position. Returns -1 if there is no layout.
     *
     * @hide
     */
    fun getOffset(x: Int, y: Int): Int {
        if (layout == null) return -1

        val line = getLineFromViewportY(y)
        return getOffsetForHorizontal(line, x)
    }

    private fun getLineFromViewportY(y: Int): Int {
        var y = y

        y -= paddingTop
        // Clamp the position to inside of the view.
        y = Math.max(0, y)
        y = Math.min(height - paddingBottom - 1, y)
        y += scrollY

        return layout!!.getLineForVertical(y)
    }


    private fun getOffsetForHorizontal(line: Int, x: Int): Int {
        var x = x
        x -= paddingLeft

        // Clamp the position to inside of the view.
        x = Math.max(0, x)
        x = Math.min(width - paddingRight - 1, x)
        x += scrollX

        return layout!!.getOffsetForHorizontal(line, x.toFloat())
    }

    fun getHysteresisOffset(x: Int, y: Int, previousOffset: Int): Int {
        var y = y
        val layout = layout ?: return -1

        y -= paddingTop

        // Clamp the position to inside of the view.
        y = Math.max(0, y)
        y = Math.min(height - paddingBottom - 1, y)
        y += scrollY

        var line = layout!!.getLineForVertical(y)

        val previousLine = layout!!.getLineForOffset(previousOffset)
        val previousLineTop = layout!!.getLineTop(previousLine)
        val previousLineBottom = layout!!.getLineBottom(previousLine)
        val hysteresisThreshold = (previousLineBottom - previousLineTop) / 8

        // If new line is just before or after previous line and y position is less than
        // hysteresisThreshold away from previous line, keep cursor on previous line.
        if (line == previousLine + 1 && y - previousLineBottom < hysteresisThreshold || line == previousLine - 1 && previousLineTop - y < hysteresisThreshold) {
            line = previousLine
        }

        return getOffsetForHorizontal(line, x)
    }


}