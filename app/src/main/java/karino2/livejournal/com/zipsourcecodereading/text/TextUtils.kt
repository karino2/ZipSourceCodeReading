package karino2.livejournal.com.zipsourcecodereading.text

import android.text.GetChars
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan




/**
 * Created by _ on 2017/10/03.
 */
class TextUtils {
    companion object {
        val lock = Object()
        var tempBuf :CharArray? = null

        fun obtain(len : Int) : CharArray {
            var buf :CharArray? = null
            synchronized(lock) {
                buf = tempBuf
                tempBuf = null
            }
            if((buf == null) || (buf!!.size < len)) {
                buf = CharArray(ArrayUtils.idealCharArraySize(len))
            }
            return buf!!
        }

        fun recycle(temp: CharArray) {
            if (temp.size > 1000)
                return

            synchronized(lock) {
                tempBuf = temp
            }
        }

        fun getOffsetAfter(text: SpannableString, offset: Int): Int {
            var offset = offset
            val len = text.length

            if (offset == len)
                return len
            if (offset == len - 1)
                return len

            val c = text[offset]

            if (c in '\uD800'..'\uDBFF') {
                val c1 = text[offset + 1]

                if (c1 in '\uDC00'..'\uDFFF')
                    offset += 2
                else
                    offset += 1
            } else {
                offset += 1
            }

            val spans = text.getSpans(offset, offset,
                    ReplacementSpan::class.java)

            for (i in spans.indices) {
                val start = text.getSpanStart(spans[i])
                val end = text.getSpanEnd(spans[i])

                if (start < offset && end > offset)
                    offset = end
            }

            return offset
        }

        fun indexOf(s: SpannableString, ch: Char, start: Int, end: Int): Int {
            var start = start
            val c = s.javaClass

            val INDEX_INCREMENT = 500
            val temp = obtain(INDEX_INCREMENT)

            while (start < end) {
                var segend = start + INDEX_INCREMENT
                if (segend > end)
                    segend = end

                s.getChars(start, segend, temp, 0)

                val count = segend - start
                for (i in 0 until count) {
                    if (temp[i] == ch) {
                        recycle(temp)
                        return i + start
                    }
                }

                start = segend
            }

            recycle(temp)
            return -1
        }


    }
}