package karino2.livejournal.com.zipsourcecodereading.text

import android.text.GetChars
import android.text.SpannableString


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
            if((buf == null) or (buf!!.size < len)) {
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