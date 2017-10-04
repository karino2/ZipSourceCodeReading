package karino2.livejournal.com.zipsourcecodereading.text

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.Spanned
import android.text.SpannedString
import android.text.style.ReplacementSpan
import android.text.Layout.DIR_LEFT_TO_RIGHT






/**
 * Created by _ on 2017/10/03.
 */
class Styled {
    companion object {
        fun drawText(canvas: Canvas, text: SpannableString, start: Int, end: Int, x: Float, top: Int, y: Float, bottom: Int, textPaint: TextPaint, workPaint: TextPaint, needWidth: Boolean) : Float {
            return drawDirectionalRun(canvas, text, start, end,
                    x, top, y, bottom, null, textPaint, workPaint,
                    needWidth)
        }

        private fun drawDirectionalRun(canvas: Canvas?,
                                       text: SpannableString, start: Int, end: Int,
                                       x: Float, top: Int, y: Float, bottom: Int,
                                       fmi: Paint.FontMetricsInt?,
                                       paint: TextPaint,
                                       workPaint: TextPaint,
                                       needWidth: Boolean): Float {
            var x = x

            val ox = x
            var minAscent = 0
            var maxDescent = 0
            var minTop = 0
            var maxBottom = 0

            val division: Class<*>

            if (canvas == null)
                division = MetricAffectingSpan::class.java
            else
                division = CharacterStyle::class.java

            var next: Int
            var i = start
            while (i < end) {
                next = text.nextSpanTransition(i, end, division)

                x += drawUniformRun(canvas, text, i, next,
                        x, top, y, bottom, fmi, paint, workPaint,
                        needWidth || next != end)

                if (fmi != null) {
                    if (fmi!!.ascent < minAscent)
                        minAscent = fmi!!.ascent
                    if (fmi!!.descent > maxDescent)
                        maxDescent = fmi!!.descent

                    if (fmi!!.top < minTop)
                        minTop = fmi!!.top
                    if (fmi!!.bottom > maxBottom)
                        maxBottom = fmi!!.bottom
                }
                i = next
            }

            if (fmi != null) {
                if (start == end) {
                    paint.getFontMetricsInt(fmi)
                } else {
                    fmi!!.ascent = minAscent
                    fmi!!.descent = maxDescent
                    fmi!!.top = minTop
                    fmi!!.bottom = maxBottom
                }
            }

            return x - ox
        }

        private fun drawUniformRun(canvas: Canvas?,
                                   text: Spanned, start: Int, end: Int,
                                   x: Float, top: Int, y: Float, bottom: Int,
                                   fmi: Paint.FontMetricsInt?,
                                   paint: TextPaint,
                                   workPaint: TextPaint,
                                   needWidth: Boolean): Float {

            var haveWidth = false
            var ret = 0f
            val spans = text.getSpans(start, end, CharacterStyle::class.java)

            var replacement: ReplacementSpan? = null

            // XXX: This shouldn't be modifying paint, only workPaint.
            // However, the members belonging to TextPaint should have default
            // values anyway.  Better to ensure this in the Layout constructor.
            paint.bgColor = 0
            paint.baselineShift = 0
            workPaint.set(paint)

            if (spans.size > 0) {
                for (i in spans.indices) {
                    val span = spans[i]

                    if (span is ReplacementSpan) {
                        replacement = span
                    } else {
                        span.updateDrawState(workPaint)
                    }
                }
            }

            if (replacement == null) {
                val tmp: CharSequence
                val tmpstart: Int
                val tmpend: Int

                tmp = text
                tmpstart = start
                tmpend = end

                if (fmi != null) {
                    workPaint.getFontMetricsInt(fmi)
                }

                if (canvas != null) {
                    if (workPaint.bgColor != 0) {
                        val c = workPaint.color
                        val s = workPaint.style
                        workPaint.color = workPaint.bgColor
                        workPaint.style = Paint.Style.FILL

                        if (!haveWidth) {
                            ret = workPaint.measureText(tmp, tmpstart, tmpend)
                            haveWidth = true
                        }


                        canvas.drawRect(x, top.toFloat(), x + ret, bottom.toFloat(), workPaint)

                        workPaint.style = s
                        workPaint.color = c
                    }


                    if (needWidth) {
                        if (!haveWidth) {
                            ret = workPaint.measureText(tmp, tmpstart, tmpend)
                            haveWidth = true
                        }
                    }
                    canvas.drawText(tmp, tmpstart, tmpend,
                            x, (y + workPaint.baselineShift).toFloat(), workPaint)
                } else {
                    if (needWidth && !haveWidth) {
                        ret = workPaint.measureText(tmp, tmpstart, tmpend)
                        haveWidth = true
                    }
                }
            } else {
                ret = replacement.getSize(workPaint, text, start, end, fmi).toFloat()

                if (canvas != null) {

                    replacement.draw(canvas, text, start, end,
                            x, top, y.toInt(), bottom, workPaint)
                }
            }

            return ret
        }

        /**
         * Returns the width of a run of left-to-right text on a single line,
         * considering style information in the text (e.g. even when text is an
         * instance of [android.text.Spanned], this method correctly measures
         * the width of the text).
         *
         * @param paint the main [TextPaint] object; will not be modified
         * @param workPaint the [TextPaint] object available for modification;
         * will not necessarily be used
         * @param text the text to measure
         * @param start the index of the first character to start measuring
         * @param end 1 beyond the index of the last character to measure
         * @param fmi FontMetrics information; can be null
         * @return The width of the text
         */
        fun measureText(paint: TextPaint,
                        workPaint: TextPaint,
                        text: SpannableString, start: Int, end: Int,
                        fmi: Paint.FontMetricsInt?): Float {
            return drawDirectionalRun(null, text, start, end,
                    0F, 0, 0F, 0, fmi, paint, workPaint, true)
        }
    }
}