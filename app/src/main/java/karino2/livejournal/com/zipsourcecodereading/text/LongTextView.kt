package karino2.livejournal.com.zipsourcecodereading.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.Gravity
import android.util.FloatMath
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.text.BoringLayout
import android.text.method.Touch
import android.text.method.ArrowKeyMovementMethod





/**
 * Created by _ on 2017/10/03.
 *
 * LongTextView for long text with coloring. Do not support WRAP_CONTENT and make resize faster.
 */
class LongTextView(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener{

    var text = SpannableString("")
    set(newText) {
        textPaint.textScaleX = 1F

        field = newText

        layout?.let {
            // This view does not support wrap_content, so chnaging text does not cause relayout.
            val want = layout!!.width
            makeNewLayout(want, false)

            invalidate()
        }

    }


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

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private fun makeNewLayout(width: Int,  bringIntoView: Boolean) {
        mHighlightPathBogus = true

        var w = width
        if (w < 0) {
            w = 0;
        }

        val spacingMulti = 1F
        val spacingAdd = 0F
        val includePad = true

        layout = Layout(text, textPaint, w, spacingMulti, spacingAdd,  includePad)

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
    override fun onPreDraw(): Boolean {
        if(preDrawState != PREDRAW_STATE.PENDING)
            return true

        if(layout == null) {
            assumeLayout()
        }

        // TODO: goto line here.

        preDrawState = PREDRAW_STATE.DONE
        val changed = false
        return changed
    }

    /**
     * Returns true if anything changed.
     */
    // this function seems nothing for our case. just keep for documentation purpose.
    /*
    private fun bringTextIntoView(): Boolean {
        var line = 0

        val a = layout!!.getParagraphAlignment(line)
        val hspace = right - left - paddingLeft - paddingRight

        val extendedPaddingTop = paddingTop
        val extendedPaddingBottom = paddingBottom

        val vspace = bottom - top - extendedPaddingTop - extendedPaddingBottom
        val ht = layout!!.height

        var scrollx = Math.floor(layout!!.getLineLeft(line).toDouble()).toInt()
        var scrolly = 0


        if (scrollx != scrollX || scrolly != scrollY) {
            scrollTo(scrollx, scrolly)
            return true
        } else {
            return false
        }
    }
    */

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
            lineNumberWidth = lineNumberPaint.measureText("888888|") as Int
        } else {
            lineNumberWidth = 0
        }

        /*
        ArrowKeyMovementMethod.setLineNumberWidth(mLineNumberWidth)
        Touch.setLineNumberWidth(mLineNumberWidth)
        */
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
                mLayout.getWidth() > unpaddedWidth ||
                mLayout.getHeight() > unpaddedHeight) {
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

        val selLine = layout!!.getLineForOffset(selEnd)


        layout!!.draw(canvas, voffsetCursor-voffsetText, selLine, lineNumberWidth, lineNumberPaint, spacePaths)

    }

    private val ideographicalSpacePath = Path()
    private val lineBreakPath = Path()
    private val tabPath = Path()
    private val spacePaths = arrayOf<Path>(tabPath, lineBreakPath, ideographicalSpacePath)

    var lineNumberWidth = 0

    val lineNumberPaint : Paint by lazy {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG);
        paint.textSize = 10F;
        paint.setTypeface(Typeface.MONOSPACE);
        paint.strokeWidth = 1F;
        paint
    }

}