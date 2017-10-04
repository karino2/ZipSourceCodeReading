package karino2.livejournal.com.zipsourcecodereading.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.Layout
import android.text.SpannableString
import android.text.TextPaint
import android.text.Layout.DIR_RIGHT_TO_LEFT
import android.text.style.AlignmentSpan
import android.text.Spanned










/**
 * Created by _ on 2017/10/03.
 * ReadOnly, no emoji support, no rtl support.
 */
class Layout(val text: SpannableString, val textPaint: TextPaint, var width: Int, val spacingmult: Float, val spacingadd: Float, val includepad: Boolean) {
    var mFontMetricsInt: Paint.FontMetricsInt?
    private var mChs: CharArray?
    private var mWidths: FloatArray?

    init {
        mFontMetricsInt = Paint.FontMetricsInt()
        mChs = null
        mWidths = null


        generate(text, 0, text.length, textPaint, width, spacingmult, spacingadd, includepad, includepad, false, false)

        mChs = null
        mWidths = null
        mFontMetricsInt = null
    }

    var lineCount = 0

    var lines : IntArray = IntArray(ArrayUtils.idealIntArraySize(2))

    val START = 0
    val TAB = 0
    val START_MASK = 0x1FFFFFFF
    val TAB_MASK = 0x20000000


    fun getLineStart(line : Int) = lines[line+START] and START_MASK
    fun getLineContainsTab(line: Int) =  lines[line + TAB] and TAB_MASK !== 0


    // for non-rtl support and gravity is always normal, this function always return 0
    fun getLineLeft(line: Int) = 0f

    /**
     * Return the text offset after the last character on the specified line.
     */
    fun getLineEnd(line: Int) = getLineStart(line + 1)

    /**
     * Get the alignment of the specified paragraph, taking into account
     * markup attached to it.
     */
    fun getParagraphAlignment(line: Int): Layout.Alignment {
        var align = Layout.Alignment.ALIGN_NORMAL

        val sp = text as Spanned
        val spans = sp.getSpans(getLineStart(line),
                getLineEnd(line),
                AlignmentSpan::class.java)

        val spanLength = spans.size
        if (spanLength > 0) {
            align = spans[spanLength - 1].alignment
        }

        return align
    }

    private val FIRST_CJK = '\u2E80'
    /**
     * Returns true if the specified character is one of those specified
     * as being Ideographic (class ID) by the Unicode Line Breaking Algorithm
     * (http://www.unicode.org/unicode/reports/tr14/), and is therefore OK
     * to break between a pair of.
     *
     * @param includeNonStarters also return true for category NS
     * (non-starters), which can be broken
     * after but not before.
     */
    private fun isIdeographic(c: Char, includeNonStarters: Boolean): Boolean {
        if (c in '\u2E80'..'\u2FFF') {
            return true // CJK, KANGXI RADICALS, DESCRIPTION SYMBOLS
        }
        if (c == '\u3000') {
            return true // IDEOGRAPHIC SPACE
        }
        if (c in '\u3001'..'\u303F') {
            if (!includeNonStarters) {
                when (c) {
                    '\u3001' //  # IDEOGRAPHIC COMMA
                        , '\u3002' //  # IDEOGRAPHIC FULL STOP
                    -> return false
                }
            }
            return true // Japanese Symbols
        }
        if (c in '\u3040'..'\u309F') {
            if (!includeNonStarters) {
                when (c) {
                    '\u3041' //  # HIRAGANA LETTER SMALL A
                        , '\u3043' //  # HIRAGANA LETTER SMALL I
                        , '\u3045' //  # HIRAGANA LETTER SMALL U
                        , '\u3047' //  # HIRAGANA LETTER SMALL E
                        , '\u3049' //  # HIRAGANA LETTER SMALL O
                        , '\u3063' //  # HIRAGANA LETTER SMALL TU
                        , '\u3083' //  # HIRAGANA LETTER SMALL YA
                        , '\u3085' //  # HIRAGANA LETTER SMALL YU
                        , '\u3087' //  # HIRAGANA LETTER SMALL YO
                        , '\u308E' //  # HIRAGANA LETTER SMALL WA
                        , '\u3095' //  # HIRAGANA LETTER SMALL KA
                        , '\u3096' //  # HIRAGANA LETTER SMALL KE
                        , '\u309B' //  # KATAKANA-HIRAGANA VOICED SOUND MARK
                        , '\u309C' //  # KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
                        , '\u309D' //  # HIRAGANA ITERATION MARK
                        , '\u309E' //  # HIRAGANA VOICED ITERATION MARK
                    -> return false
                }
            }
            return true // Hiragana (except small characters)
        }
        if (c in '\u30A0'..'\u30FF') {
            if (!includeNonStarters) {
                when (c) {
                    '\u30A0' //  # KATAKANA-HIRAGANA DOUBLE HYPHEN
                        , '\u30A1' //  # KATAKANA LETTER SMALL A
                        , '\u30A3' //  # KATAKANA LETTER SMALL I
                        , '\u30A5' //  # KATAKANA LETTER SMALL U
                        , '\u30A7' //  # KATAKANA LETTER SMALL E
                        , '\u30A9' //  # KATAKANA LETTER SMALL O
                        , '\u30C3' //  # KATAKANA LETTER SMALL TU
                        , '\u30E3' //  # KATAKANA LETTER SMALL YA
                        , '\u30E5' //  # KATAKANA LETTER SMALL YU
                        , '\u30E7' //  # KATAKANA LETTER SMALL YO
                        , '\u30EE' //  # KATAKANA LETTER SMALL WA
                        , '\u30F5' //  # KATAKANA LETTER SMALL KA
                        , '\u30F6' //  # KATAKANA LETTER SMALL KE
                        , '\u30FB' //  # KATAKANA MIDDLE DOT
                        , '\u30FC' //  # KATAKANA-HIRAGANA PROLONGED SOUND MARK
                        , '\u30FD' //  # KATAKANA ITERATION MARK
                        , '\u30FE' //  # KATAKANA VOICED ITERATION MARK
                    -> return false
                }
            }
            return true // Katakana (except small characters)
        }
        if (c in '\u3400'..'\u4DB5') {
            return true // CJK UNIFIED IDEOGRAPHS EXTENSION A
        }
        if (c in '\u4E00'..'\u9FBB') {
            return true // CJK UNIFIED IDEOGRAPHS
        }
        if (c in '\uF900'..'\uFAD9') {
            return true // CJK COMPATIBILITY IDEOGRAPHS
        }
        if (c in '\uA000'..'\uA48F') {
            return true // YI SYLLABLES
        }
        if (c in '\uA490'..'\uA4CF') {
            return true // YI RADICALS
        }
        if (c in '\uFE62'..'\uFE66') {
            return true // SMALL PLUS SIGN to SMALL EQUALS SIGN
        }
        return if (c in '\uFF10'..'\uFF19') {
            true // WIDE DIGITS
        } else false

    }

    fun generate(source:SpannableString, bufstart:Int, bufend:Int, paint:TextPaint, outerwidth:Int,
        spacingmult:Float, spacingadd:Float,
        includepad:Boolean, trackpad:Boolean,
        breakOnlyAtSpaces:Boolean, showTab:Boolean) {
        lineCount = 0

        var v = 0
        val needMultiply = spacingmult != 1f || spacingadd != 0f

        val fm = mFontMetricsInt!!

        var end = TextUtils.indexOf(source, '\n', bufstart, bufend)
        val bufsiz = if (end >= 0) end - bufstart else bufend - bufstart
        var first = true


        if (mChs == null)
        {
            mChs = CharArray(ArrayUtils.idealCharArraySize(bufsiz + 1))
            mWidths = FloatArray(ArrayUtils.idealIntArraySize((bufsiz + 1) * 2))
        }

        var chs = mChs
        var widths = mWidths

        var start = bufstart
        while (start <= bufend)
        {
            if (first)
                first = false
            else
                end = TextUtils.indexOf(source, '\n', start, bufend)

            if (end < 0)
                end = bufend
            else
                end++

            var firstWidthLineCount = 1


            if (end - start > chs!!.size)
            {
                chs = CharArray(ArrayUtils.idealCharArraySize(end - start))
                mChs = chs
            }
            if ((end - start) * 2 > widths!!.size)
            {
                widths = FloatArray(ArrayUtils.idealIntArraySize((end - start) * 2))
                mWidths = widths
            }

            source.getChars(start, end, chs, 0)
            val n = end - start


            // Do not supp;ort rtl


            val sub = source



            var width = outerwidth

            var w = 0f
            var here = start

            var ok = start
            var okascent = 0
            var okdescent = 0
            var oktop = 0
            var okbottom = 0

            var fit = start
            var fitascent = 0
            var fitdescent = 0
            var fittop = 0
            var fitbottom = 0

            var tab = false

            var next:Int
            var i = start
            while (i < end)
            {
                next = end

                paint.getTextWidths(sub, i, next, widths)
                System.arraycopy(widths, 0, widths,
                end - start + (i - start), next - i)

                paint.getFontMetricsInt(fm)


                val fmtop = fm.top
                val fmbottom = fm.bottom
                val fmascent = fm.ascent
                val fmdescent = fm.descent


                var j = i
                while (j < next)
                {
                     val c = chs[j - start]


                    if (c == '\n')
                    {
                        if (!tab) tab = showTab      // Jota Text Editor
                    }
                    else if (c == '\t')
                    {
                        w = nextTab(w)
                        tab = true
                    } /* emoji not supported */
                    else
                    {

	                    if (c.toInt() == 0x3000)
                        { // ideographic space ( for Japanese )
                            if (!tab) tab = showTab
                        }
                        w += widths[j - start + (end - start)]
                    }

                    if (w <= width)
                    {
                        fit = j + 1

                        if (fmtop < fittop)
                            fittop = fmtop
                        if (fmascent < fitascent)
                            fitascent = fmascent
                        if (fmdescent > fitdescent)
                            fitdescent = fmdescent
                        if (fmbottom > fitbottom)
                            fitbottom = fmbottom

                        /*
                         * From the Unicode Line Breaking Algorithm:
                         * (at least approximately)
                         *
                         * .,:; are class IS: breakpoints
                         *      except when adjacent to digits
                         * /    is class SY: a breakpoint
                         *      except when followed by a digit.
                         * -    is class HY: a breakpoint
                         *      except when followed by a digit.
                         *
                         * Ideographs are class ID: breakpoints when adjacent,
                         * except for NS (non-starters), which can be broken
                         * after but not before.
                         */

                        if (((c == ' ') or (c == '\t')
                            or
                                (((c == '.') or (c == ',') or (c == ':') or (c == ';')) and
                                    ((j - 1 < here) or !Character.isDigit(chs[j - 1 - start])) and
                                    ((j + 1 >= next) or !Character.isDigit(chs[j + 1 - start])))
                            or
                                ((((c == '/') or (c == '-')) and ((j + 1 >= next) or !Character.isDigit(chs[j + 1 - start]))))
                            or
                                (((c >= FIRST_CJK) and isIdeographic(c, true) and
                                        (j + 1 < next) and isIdeographic(chs[j + 1 - start], false)))))
                        {
                            ok = j + 1

                            if (fittop < oktop)
                                oktop = fittop
                            if (fitascent < okascent)
                                okascent = fitascent
                            if (fitdescent > okdescent)
                                okdescent = fitdescent
                            if (fitbottom > okbottom)
                                okbottom = fitbottom
                        }
                    }
                    else if (breakOnlyAtSpaces)
                    {
                        if (ok != here)
                        {
                         // Log.e("text", "output ok " + here + " to " +ok);

                            while (ok < next && chs[ok - start] == ' ')
                            {
                                ok++
                            }

                            v = out(source,
                                here, ok,
                                okascent, okdescent, oktop, okbottom,
                                v,
                                spacingmult, spacingadd, fm, tab,
                                needMultiply,
                                ok == bufend, includepad, trackpad)

                            here = ok
                        }
                        else
                        {
                         // Act like it fit even though it didn't.

                            fit = j + 1

                            if (fmtop < fittop)
                                fittop = fmtop
                            if (fmascent < fitascent)
                                fitascent = fmascent
                            if (fmdescent > fitdescent)
                                fitdescent = fmdescent
                            if (fmbottom > fitbottom)
                                fitbottom = fmbottom
                        }
                    }
                    else
                    {
                        if (ok != here)
                        {
                         // Log.e("text", "output ok " + here + " to " +ok);

                            while (ok < next && chs[ok - start] == ' ')
                            {
                                ok++
                            }

                            v = out(source,
                                here, ok,
                                okascent, okdescent, oktop, okbottom,
                                v,
                                spacingmult, spacingadd, fm, tab,
                                needMultiply,
                                ok == bufend, includepad, trackpad)

                            here = ok
                        }
                        else if (fit != here)
                        {
                     // Log.e("text", "output fit " + here + " to " +fit);

                            v = out(source,
                                here, fit,
                                fitascent, fitdescent,
                                fittop, fitbottom,
                                v,
                                spacingmult, spacingadd, fm, tab,
                                needMultiply,
                                fit == bufend, includepad, trackpad)

                            here = fit
                        }
                        else
                        {
                         // Log.e("text", "output one " + here + " to " +(here + 1));
                            measureText(paint, workPaint,
                                source, here, here + 1, fm, tab)

                            v = out(source,
                                here, here + 1,
                                fm.ascent, fm.descent,
                                fm.top, fm.bottom,
                                v,
                                spacingmult, spacingadd, fm, tab,
                                needMultiply,
                                here + 1 == bufend, includepad,
                                trackpad)

                            here = here + 1
                        }

                        if (here < i)
                        {
                            next = here
                            j = next // must remeasure
                        }
                        else
                        {
                            j = here - 1    // continue looping
                        }

                        fit = here
                        ok = fit
                        w = 0f
                        fitbottom = 0
                        fittop = fitbottom
                        fitdescent = fittop
                        fitascent = fitdescent
                        okbottom = 0
                        oktop = okbottom
                        okdescent = oktop
                        okascent = okdescent

                        if (--firstWidthLineCount <= 0)
                        {
                            width = outerwidth
                        }
                    }
                    j++
                }
                i = next
            }

            if (end != here)
            {
                if ((fittop or fitbottom or fitdescent or fitascent) == 0)
                {
                    paint.getFontMetricsInt(fm)

                    fittop = fm.top
                    fitbottom = fm.bottom
                    fitascent = fm.ascent
                    fitdescent = fm.descent
                }

                 // Log.e("text", "output rest " + here + " to " + end);

                v = out(source,
                    here, end, fitascent, fitdescent,
                    fittop, fitbottom,
                    v,
                    spacingmult, spacingadd, fm, tab,
                    needMultiply,
                    end == bufend, includepad, trackpad)
            }

            start = end

            if (end == bufend)
                break
            start = end
        }

        if (bufend == bufstart || source[bufend - 1] == '\n')
        {
             // Log.e("text", "output last " + bufend);

            paint.getFontMetricsInt(fm)

            v = out(source,
                bufend, bufend, fm.ascent, fm.descent,
                fm.top, fm.bottom,
                v,
                spacingmult, spacingadd, fm, false,
                needMultiply,
                true, includepad, trackpad)
        }
    }

    private val TOP = 1
    var mTopPadding = 0
    var mBottomPadding = 0


    private fun out(text: SpannableString, start: Int, end: Int,
                    above: Int, below: Int, top: Int, bottom: Int, v: Int,
                    spacingmult: Float, spacingadd: Float,
                    fm: Paint.FontMetricsInt?, tab: Boolean,
                    needMultiply: Boolean, last: Boolean,
                    includepad: Boolean, trackpad: Boolean): Int {
        var above = above
        var below = below
        var top = top
        var bottom = bottom
        val j = lineCount
        val off = j * 1
        val want = off + 1 + TOP
        var lines = this.lines

        // Log.e("text", "line " + start + " to " + end + (last ? "===" : ""));

        if (want >= lines.size) {
            val nlen = ArrayUtils.idealIntArraySize(want + 1)
            val grow = IntArray(nlen)
            System.arraycopy(lines, 0, grow, 0, lines.size)
            this.lines = grow
            lines = grow
        }

        if (j == 0) {
            if (trackpad) {
                mTopPadding = top - above
            }

            if (includepad) {
                above = top
            }
        }
        if (last) {
            if (trackpad) {
                mBottomPadding = bottom - below
            }

            if (includepad) {
                below = bottom
            }
        }

        val extra: Int

        if (needMultiply) {
            val ex = ((below - above) * (spacingmult - 1) + spacingadd).toDouble()
            if (ex >= 0) {
                extra = (ex + 0.5).toInt()
            } else {
                extra = -(-ex + 0.5).toInt()
            }
        } else {
            extra = 0
        }

        lines[off + START] = start

        height = below - above + extra
        descent = below + extra

        if (tab)
            lines[off + TAB] = lines[off + TAB] or TAB_MASK

        // do not support RTL
        // lines[off + DIR] = lines[off + DIR] or (dir shl DIR_SHIFT)

        var cur = Character.DIRECTIONALITY_LEFT_TO_RIGHT.toInt()
        var count = 0

        lineCount++
        return v
    }

    /**
     * Measure width of a run of text on a single line that is known to all be
     * in the same direction as the paragraph base direction. Returns the width,
     * and the line metrics in fm if fm is not null.
     *
     * @param paint the paint for the text; will not be modified
     * @param workPaint paint available for modification
     * @param text text
     * @param start start of the line
     * @param end limit of the line
     * @param fm object to return integer metrics in, can be null
     * @param hasTabs true if it is known that the line has tabs
     * @return the width of the text from start to end
     */
    fun measureText(paint: TextPaint,
                                   workPaint: TextPaint,
                                   text: SpannableString,
                                   start: Int, end: Int,
                                   fm: Paint.FontMetricsInt?,
                                   hasTabs: Boolean): Float {
        var buf: CharArray? = null

        if (hasTabs) {
            buf = TextUtils.obtain(end - start)
            text.getChars(start, end, buf, 0)
        }

        val len = end - start

        var lastPos = 0
        var width = 0f
        var ascent = 0
        var descent = 0
        var top = 0
        var bottom = 0

        if (fm != null) {
            fm.ascent = 0
            fm.descent = 0
        }

        var pos = if (hasTabs) 0 else len
        while (pos <= len) {
            var codept = 0

            if (hasTabs && pos < len) {
                codept = buf!![pos].toInt()
            }

            if (codept in 0xD800..0xDFFF && pos < len) {
                codept = Character.codePointAt(buf, pos)
            }

            if (pos == len || codept == '\t'.toInt()) {
                workPaint.baselineShift = 0

                width += Styled.measureText(paint, workPaint, text,
                        start + lastPos, start + pos,
                        fm)

                if (fm != null) {
                    if (workPaint.baselineShift < 0) {
                        fm.ascent += workPaint.baselineShift
                        fm.top += workPaint.baselineShift
                    } else {
                        fm.descent += workPaint.baselineShift
                        fm.bottom += workPaint.baselineShift
                    }
                }

                if (pos != len) {
                    width = nextTab(width)
                }

                if (fm != null) {
                    if (fm.ascent < ascent) {
                        ascent = fm.ascent
                    }
                    if (fm.descent > descent) {
                        descent = fm.descent
                    }

                    if (fm.top < top) {
                        top = fm.top
                    }
                    if (fm.bottom > bottom) {
                        bottom = fm.bottom
                    }

                    // No need to take bitmap height into account here,
                    // since it is scaled to match the text height.
                }

                lastPos = pos + 1
            }
            pos++
        }

        if (fm != null) {
            fm.ascent = ascent
            fm.descent = descent
            fm.top = top
            fm.bottom = bottom
        }

        if (hasTabs)
            TextUtils.recycle(buf!!)

        return width
    }

    fun getLineForOffset(offset: Int): Int {
        var high = lineCount
        var low = -1
        var guess =0

        while(high - low > 1) {
            guess = (high+low)/2

            if(getLineStart(guess)>offset) {
                high = guess
            } else {
                low = guess
            }
        }

        return Math.max(0, low)
    }

    val tempRect by lazy { Rect() }

    var height = 0
    fun getLineTop(line: Int) = height * line


    fun getLineForVertical(vertical: Int): Int {
        var high = lineCount
        var low = -1
        var guess: Int
        while (high - low > 1) {
            guess = high + low shr 1
            if (guess * height > vertical) {
                high = guess
            } else {
                low = guess
            }
        }
        return Math.max(0, low)
    }

    private fun getLineVisibleEnd(line: Int, start: Int, endArg: Int): Int {
        var end = endArg

        var ch: Char
        if (line == lineCount - 1) {
            return end
        }

        while (end > start) {
            ch = text.get(end - 1)

            if (ch == '\n') {
                return end - 1
            }

            if (ch != ' ' && ch != '\t') {
                break
            }
            end--

        }

        return end
    }


    fun getLineBottom(line: Int) =  getLineTop(line + 1)

    val workPaint = TextPaint()

    var descent = 0


    fun draw(canvas: Canvas, cursorOffsetVertical: Int, selLine: Int, lineNumberWidth: Int, lineNumberPaint: Paint, spacePaths: Array<Path>) {
        if(!canvas.getClipBounds(tempRect)) {
            return
        }

        val dtop = tempRect.top
        val dbottom = tempRect.bottom

        val top = Math.max(0, dtop)
        val bottom = Math.min(dbottom, getLineTop(lineCount))

        val first = getLineForVertical(top)
        val last = getLineForVertical(bottom)

        var previousLineBottom = getLineTop(first)
        var previousLineEnd = getLineStart(first)

        for(i in first..last) {
            val start = previousLineEnd
            previousLineEnd = getLineStart(i+1)

            val end = getLineVisibleEnd(i, start, previousLineEnd)

            val ltop = previousLineBottom
            val lbottom = getLineTop(i+1)

            previousLineBottom = lbottom

            val lbaseline = lbottom - descent

            val left = 0
            val right = width

            val x = left

            if(lineNumberWidth != 0) {
                val linenum = "      ${i+1}"
                canvas.drawText(linenum, linenum.length-5, linenum.length, x.toFloat(), lbaseline.toFloat(), lineNumberPaint)

                val linebottom = if(i < lineCount -1) { getLineTop(i+1) } else { getLineBottom(i) }
                canvas.drawLine((lineNumberWidth-4).toFloat(), getLineTop(i).toFloat(), (lineNumberWidth-4).toFloat(), linebottom.toFloat(), lineNumberPaint);
                canvas.translate(lineNumberWidth.toFloat(), 0F);
            }

            val hasTab = getLineContainsTab(i)

            // if not spanned, this is faster.
            //  canvas.drawText(text, start, end, x, lbaseline, paint);

            val spacePaint :Paint? = null


            drawText(canvas, text, start, end, x.toFloat(), ltop, lbaseline.toFloat(), lbottom, textPaint, workPaint, hasTab,/* noparaspans, */ spacePaint,  spacePaths)



        }



    }

    var TAB_INCREMENT = 20

    var tabSize:Int
    get() = TAB_INCREMENT
    set(newsize) {
        TAB_INCREMENT = newsize
    }

    /**
     * Increase the width of this layout to the specified width.
     * Be careful to use this only when you know it is appropriate
     * it does not cause the text to reflow to use the full new width.
     */
    fun increaseWidthTo(wid: Int) {
        if (wid < width) {
            throw RuntimeException("attempted to reduce Layout width")
        }

        width = wid
    }

    fun nextTab(h: Float) :Float =   ( ((h + TAB_INCREMENT).toInt() / TAB_INCREMENT) * TAB_INCREMENT).toFloat()

    fun drawText(canvas: Canvas, text: SpannableString, start: Int, end: Int, x: Float, top: Int, y: Float, bottom: Int, textPaint: TextPaint, workPaint: TextPaint, hasTab: Boolean, spacePaint: Paint?, spacePaths: Array<Path>) {
        if(!hasTab) {
            Styled.drawText(canvas, text, start, end, x, top, y, bottom, textPaint, workPaint, false)
            return
        }

        val buf = TextUtils.obtain(end - start)
        text.getChars(start, end, buf, 0)

        var h = 0F

        // Do not support bidi
        val here = 0
        val there = end-start

        var segstart = here

        for(j in here..there) {
            if((j == there) or (buf[j] == '\t')){
                h += Styled.drawText(canvas, text,
                        start + segstart, start + j,
                        x + h,
                        top, y, bottom, textPaint, workPaint,
                        (start + j !== end) or hasTab)
                if((j != there) and (buf[j] == '\t')) {
                    spacePaint?.let {
                        canvas.translate(x+h, y)
                        canvas.drawPath(spacePaths[0], spacePaint)
                        canvas.translate(-x-h, -y)
                    }
                    h = nextTab(h)

                }

                if ( spacePaint != null && j== there ){
                    // IDE messed up for text.charAt, so I add cast....
                    if ( (end < text.length) and  ((text as java.lang.CharSequence).charAt(end)=='\n')){
                        canvas.translate(x+h, y);
                        canvas.drawPath(spacePaths[1], spacePaint);
                        canvas.translate(-x-h, -y);
                    }
                }
                segstart = j + 1;
            } else if ( (spacePaint!=null) and  (buf[j]==0x3000.toChar()) ){    // Ideographic Space ( for Japanese charset )
                h += Styled.drawText(canvas, text,
                        start + segstart, start + j,
                         x + h,
                top, y, bottom, textPaint, workPaint,
                start + j != end);

                val width = textPaint.measureText("\u3000");
                canvas.translate(x+h, y);
                canvas.drawPath(spacePaths[2], spacePaint);
                canvas.translate(-x-h, -y);
                h += width;

                segstart = j + 1;
            } /* emoji support here if you wantj */
        }
        TextUtils.recycle(buf)
    }

}