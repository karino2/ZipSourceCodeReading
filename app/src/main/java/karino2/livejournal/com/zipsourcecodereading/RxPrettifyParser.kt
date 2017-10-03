package karino2.livejournal.com.zipsourcecodereading

/**
 * Created by _ on 2017/10/03.
 */

import io.reactivex.Observable
import java.util.Arrays
import prettify.parser.Job
import prettify.parser.Prettify
import syntaxhighlight.ParseResult

/**
 * The cancelable prettify parser for syntax highlight.
 */
class RxPrettifyParser {
    /**
     * The prettify parser.
     */
    val prettify = Prettify()

    fun parse(fileExtension: String, content: String): Observable<ParseResult> {
        return Observable.create<ParseResult> {
            emitter ->

            val job = Job(0, content)
            prettify.langHandlerForExtension(fileExtension, content).decorate(job)
            val decorations = job.decorations
            var i = 0
            val iEnd = decorations.size
            while (i < iEnd) {
                if(emitter.isDisposed) {
                    return@create
                }
                val endPos = if (i + 2 < iEnd) decorations[i + 2] as Int else content.length
                val startPos = decorations[i] as Int
                emitter.onNext(ParseResult(startPos, endPos - startPos, Arrays.asList(*arrayOf(decorations[i + 1] as String))))
                i += 2
            }
            emitter.onComplete()
        }
    }
}