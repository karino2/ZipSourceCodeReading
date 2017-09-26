package karino2.livejournal.com.zipsourcecodereading

import io.reactivex.Observable
import java.io.File
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

    fun ZipEntry.isUnder(dir: ZipEntryAux) : Boolean {
        return this.name.startsWith(dir.name)
    }

    fun ZipEntry.isDirectlyUnder(dir: ZipEntryAux): Boolean {
        if(!this.isUnder(dir))
            return false

        if(this.name.slice(dir.nameWithSlash.length until this.name.length).contains("/"))
            return false
        return true
    }

    /*
    fun listFilesAtRoot() : List<ZipEntryAux> {
        val res = ArrayList<ZipEntryAux>()
        val entries = zipFile.entries()

        while(entries.hasMoreElements()) {
            val next = entries.nextElement()

            val name = next.name
            if(!name.slice(1 until name.length).contains("/")){
                res.add(ZipEntryAux(next))
            }
        }
        return res
    }
    */

    fun ZipEntry.relative(parent: ZipEntryAux) = this.name.slice(parent.nameWithSlash.length until name.length)

    val File.topDirectory
        get() : File {
            var res = this
            while(res.parentFile != null){
                res = res.parentFile
            }
            return res
        }

    fun listFilesAtDir(dir: ZipEntryAux) : List<ZipEntryAux> {
        val files = ArrayList<ZipEntryAux>()
        val entries = zipFile.entries()



        val dirs = mutableSetOf<String>()

        while(entries.hasMoreElements()) {
            val next = entries.nextElement()
            if (next.isUnder(dir)) {
                if(next.isDirectlyUnder(dir)) {
                    files.add(ZipEntryAux(next))
                } else {
                    if(dir.isRoot) {
                        dirs.add(File(next.name).topDirectory.name)

                    } else {
                        dirs.add(File(dir.nameWithSlash, File(next.relative(dir)).topDirectory.name).path)
                    }
                }
            }
        }
        return dirs.sorted().map { ZipEntryAux(it) } + files

    }
    fun listFilesAt(dir: ZipEntryAux?) : List<ZipEntryAux> {
        return if(dir == null) listFilesAtDir(ZipEntryAux("")) else listFilesAtDir(dir)
    }

    fun  getInputStream(ent: ZipEntry): InputStream {
        return zipFile.getInputStream(ent)
    }

    val title: String
        get() = zipFile.name

}