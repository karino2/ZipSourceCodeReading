package karino2.livejournal.com.zipsourcecodereading

import android.widget.EditText
import io.reactivex.Observable
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by _ on 2017/08/19.
 */
class SourceArchive(val zipFile : ZipFile) {

    fun listFiles() : Observable<ZipEntry> {
        return Observable.create<ZipEntry> {
            emitter ->

            val entries = zipFile.entries()
            while(entries.hasMoreElements()) {
                if(emitter.isDisposed)
                    return@create
                val next = entries.nextElement()
                emitter.onNext(next)
            }
            emitter.onComplete()
        }
    }

    fun  getInputStream(ent: ZipEntry): InputStream {
        return zipFile.getInputStream(ent)
    }

}