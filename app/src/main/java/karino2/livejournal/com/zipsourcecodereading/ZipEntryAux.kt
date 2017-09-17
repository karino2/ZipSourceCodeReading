package karino2.livejournal.com.zipsourcecodereading

import java.io.File
import java.util.zip.ZipEntry

/**
 * Created by _ on 2017/09/17.
 */
data class ZipEntryAux(val original : ZipEntry?, val fullPath: String) {

    companion object {
        fun ZipEntry.toDisplayName() : String {
            val suffix = if(this.isDirectory) "/" else ""
            return File(this.name).name + suffix
        }
    }

    val isRoot = (original == null) and (fullPath == "")

    constructor(org: ZipEntry) : this(org, org.name)
    constructor(fullPath: String): this(null, fullPath)

    val isDirectory = if(original == null) true else original.isDirectory
    val name = if(original == null) fullPath else original.name

    val nameWithSlash = if(name.endsWith("/")) name else name + "/"

    val _displayName = File(name).name
    val displayName = if(!isDirectory or _displayName.endsWith("/")) _displayName else _displayName + "/"

    val parent : ZipEntryAux
        get() = ZipEntryAux(File(fullPath).parentFile?.path ?: "")

}