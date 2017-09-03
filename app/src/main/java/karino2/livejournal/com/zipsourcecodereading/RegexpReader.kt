package karino2.livejournal.com.zipsourcecodereading

import com.google.re2j.Matcher
import com.google.re2j.Pattern
import io.reactivex.Observable
import java.io.InputStream
import java.io.Reader

/**
 * Created by _ on 2017/08/22.
 */
class RegexpReader(val pat : Pattern) {
    val buf  : ByteArray by lazy {
        ByteArray(1024*1024)
    }

    val nl = '\n'.toByte()

    fun findEol(buf: ByteArray, startpos: Int, upperbound: Int, endText : Boolean) : Int? {
        for( i in startpos until upperbound) {
            if(buf[i] == nl)
                return i
        }
        if(endText)
            return upperbound
        return null
    }

    fun findLastIndexOfRange(buf: ByteArray, elem : Byte, beg : Int, end : Int) : Int {
        for (i in (end-1) downTo beg) {
            if(buf[i] == elem)
                return i
        }
        return -1
    }

    data class MatchEntry(val fentry: String, val lineNumber: Int?, val line: String)

    // port of codesearch/regexp/match.go Read()
    fun Read(inp: InputStream, fentry : String, lineno : Int? = null) : Observable<MatchEntry> = Observable.create { emitter ->

        var bufUsed = 0
        var endText = false
        var end = 0
        var lineNumber: Int? = lineno

        while (!emitter.isDisposed) {

            val readLen = inp.read(buf, bufUsed, buf.size - bufUsed)
            end = bufUsed

            if (readLen == -1) {
                endText = true
            } else {
                bufUsed += readLen
                end = findLastIndexOfRange(buf, nl, 0, bufUsed) + 1
            }

            var chunkStart = 0
            val slice = Slice.wrappedBuffer(buf, chunkStart, end-chunkStart)

            while (chunkStart < end) {
                slice.set(buf, chunkStart, end-chunkStart)
                val matcher = pat.matcher(slice)

                if (!matcher.find())
                    break

                val matchpos = matcher.start() + chunkStart
                val lineEnd = findEol(buf, matchpos, end, endText) ?: break

                var lineStart = findLastIndexOfRange(buf, nl, chunkStart, matchpos)+1
                if(lineStart == 0) {
                    lineStart = chunkStart
                }


                lineNumber?.let { lineNumber += countNL(buf, chunkStart, lineStart) }

                val line = buf.copyOfRange(lineStart, lineEnd).toString(Charsets.UTF_8)

                emitter.onNext(MatchEntry(fentry, lineNumber, line))
                lineNumber?.let { lineNumber++ }

                chunkStart = lineEnd+1
            }

            lineNumber?.let { lineNumber += countNL(buf, chunkStart, end) }


            System.arraycopy(buf, end, buf, 0, bufUsed - end)
            bufUsed -= end

            if(bufUsed ==0 && endText) {
                break
            }
        }
        emitter.onComplete()
    }

    private fun  countNL(buf: ByteArray, start: Int, end: Int): Int {
        return buf.slice(start until end).count { it == nl }
    }

}