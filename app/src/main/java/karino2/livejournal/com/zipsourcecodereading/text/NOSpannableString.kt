package karino2.livejournal.com.zipsourcecodereading.text

import android.text.GetChars
import android.text.Spannable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.text.Selection
import android.text.SpanWatcher
import android.util.Log


/**
 * Created by _ on 2017/10/06.
 *
 * Non Overlap Spannable String(readonly).
 * It usage is restrict, but getSpans is much much faster.
 * So, SpanWatcher is not supported (because it overlap by design)
 */
class NOSpannableString(val source : CharSequence) : CharSequence by source, GetChars, Spannable {
    val sourceStr = source.toString()
    override fun getChars(start: Int, end: Int, dest: CharArray?, offset: Int) = (sourceStr as java.lang.String).getChars(start, end, dest, offset)


    val spans: MutableList<Triple<Int, Int, Any>> = mutableListOf()

    var spanWatcher : SpanWatcher  = object : SpanWatcher {
        override fun onSpanChanged(p0: Spannable?, what: Any?, oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
        }

        override fun onSpanRemoved(p0: Spannable?, what: Any?, start: Int, end: Int) {
        }

        override fun onSpanAdded(p0: Spannable?, what: Any?, start: Int, end: Int) {
        }
    }

    fun MutableList<Triple<Int, Int, Any>>.findInsertPos(start: Int) : Int {
        if(spans.size == 0) {
            return 0
        }
        if(spans[0].first > start) {
            return 0
        }
        if(spans.last().first < start) {
            return spans.size
        }
        var low = 0
        var high = spans.size

        while(high - low > 1) {
            val guess = (high+low)/2
            // for debug
            val span = spans[guess]
            if(spans[guess].first > start)
                high = guess
            else
                low = guess
        }
        return high

    }

    fun addSpanSorted(span: Triple<Int, Int, Any>) {
        val pos = spans.findInsertPos(span.first)
        spans.add(pos, span)
    }

    var selectionStart = -1
    var selectionEnd = -1


    fun notifySelectionChange(oldVal: Int, what: Any, start:Int, end:Int) {
        if(oldVal == -1) {
            spanWatcher.onSpanAdded(this, what, start, end)
        } else {
            spanWatcher.onSpanChanged(this, what, oldVal, oldVal, start, end)
        }
    }

    override fun setSpan(what: Any, start: Int, end: Int, flagas: Int) {
        // not care flags now.
        if(what == Selection.SELECTION_START) {
            val prev = selectionStart
            selectionStart = start
            notifySelectionChange(prev, what, start, end)

            return
        }
        if(what == Selection.SELECTION_END) {
            val prev = selectionEnd
            selectionEnd = start
            notifySelectionChange(prev, what, start, end)

            return
        }

        if(start == 0) {
            Log.d("ZSCR", "start==0")
        }

        // adding is happen in end most of the case.
        if(spans.isEmpty()) {
            spanWatcher.onSpanAdded(this, what, start, end)
            spans.add(0, Triple(start, end, what))
            return
        }
        val last = spans.last()
        if(last.second <= start) {
            spanWatcher.onSpanAdded(this, what, start, end)
            spans.add(spans.size, Triple(start, end, what))
            return
        }

        val pos = spans.findInsertPos(start)
        if(spans.size > 0) {
            val before = Math.max(0, pos-1)
            if (spans[before].first == start)
                throw IllegalArgumentException("overlap spans, not allowed")
        }

        spanWatcher.onSpanAdded(this, what, start, end)
        spans.add(pos, Triple(start, end, what))
    }


    fun firstOverlapSpanIndex(start: Int, end: Int) :Int {
        if(spans.isEmpty())
            return -1

        val pos = Math.max(0, spans.findInsertPos(start))
        // pos must be only two case

        // case1: overlap
        //  <-spans[pos]->
        //        |<- start   end-> |

        // case2: largest pos which is just before start
        //  <-spans[pos]->
        //                 |<- start   end-> |



        if(spans[pos].second < start) {
            // case2

            //  <-spans[pos]->
            //                  |<- start   end-> |
            if(spans.size == pos+1)
                return -1
            // must be start < spans[pos+1].first, but check for sure
            if(spans[pos+1].first in start..end)
                return pos+1

            //                     <-spans[pos+1]->
            // |<- start   end-> |
            //
            return -1
        }

        // spans[pos].second >= start
        // case1: overlap
        //  <-spans[pos]->
        //        |<- start   end-> |

        // must be always true, but check for sure.
        if(spans[pos].first <= start)
            return pos
        return -1
    }

    fun getSpanAt(index: Int) = spans[index]

    val spanCount
    get() = spans.size

    val emptyCache = mutableMapOf<Any, Any>()


    // Do not return selection! take care!
    override fun <T : Any?> getSpans(start: Int, end: Int, kind: Class<T>?): Array<T> {
        if(spans.isEmpty()) {
            return getEmptyArray(kind)
        }

        val ret : MutableList<T> by lazy {
            mutableListOf<T>()
        }

        val startPos = Math.max(0, spans.findInsertPos(start)-1)

        var added = false
        var pos = startPos
        while(pos < spans.size && end >= spans[pos].first) {
            // for debug.
            val span = spans[pos]
            if(span.first != span.second &&
                    start != end) {
                if(span.first == end ||
                        span.second == start) {
                    pos++
                    continue
                }
            }

            if(start <= span.second) {
                if(kind != null &&!(kind.isInstance(span.third))) {
                    pos++
                    continue
                }
                added = true
                ret.add(span.third as T)
            }
            pos++
        }
        if(!added)
            return getEmptyArray(kind)

        val array = java.lang.reflect.Array.newInstance(kind, ret.size) as Array<T>
        ret.forEachIndexed { index, obj -> array[index] = obj }

        return array
    }

    private fun <T : Any?> getEmptyArray(kind: Class<T>?): Array<T> {

        if(!emptyCache.containsKey(kind as Any)) {
            emptyCache.put(kind as Any, java.lang.reflect.Array.newInstance(kind, 0))
        }
        return emptyCache.get(kind as Any) as Array<T>
    }

    override fun removeSpan(what: Any?) {
        when(what) {
            Selection.SELECTION_START -> {
                val prev = selectionStart
                selectionStart = -1
                if(prev != -1) {
                    spanWatcher.onSpanRemoved(this, what, prev, prev)
                }
            }
            Selection.SELECTION_END -> {
                val prev = selectionEnd
                selectionEnd = -1
                if (prev != -1) {
                    spanWatcher.onSpanRemoved(this, what, prev, prev)
                }
            }
        }
        throw RuntimeException("removeSpan: not supported")
    }

    override fun nextSpanTransition(start: Int, limit: Int, kind: Class<*>?): Int {
        if(spans.isEmpty()) {
            return limit
        }

        val startPos = Math.max(0, spans.findInsertPos(start)-1)
        var pos = startPos
        var limit2 = limit
        while(pos < spans.size && spans[pos].first < limit2) {
            if(spans[pos].first >start) {
                if(kind == null || kind.isInstance(spans[pos].third)) {
                    limit2 =spans[pos].first
                    // I guess never happend this case.
                }
            }

            if(spans[pos].second in (start + 1)..(limit2 - 1)) {
                 if(kind == null || kind.isInstance(spans[pos].third)) {
                     limit2 =spans[pos].second
                     return limit2
                 }
            }
            pos++
        }
        return limit2
    }

    override fun getSpanEnd(what: Any?): Int {
        when(what) {
            Selection.SELECTION_START ->
                    return selectionStart
            Selection.SELECTION_END ->
                    return selectionEnd
        }
        throw RuntimeException("getSpanEnd: not supported")
    }

    override fun getSpanFlags(what: Any?): Int {
        throw RuntimeException("getSpanFlags: not supported")
    }

    override fun getSpanStart(what: Any?): Int {
        when(what) {
            Selection.SELECTION_START ->
                return selectionStart
            Selection.SELECTION_END ->
                return selectionEnd
        }
        throw RuntimeException("getSpanStart: not supported")
    }

}