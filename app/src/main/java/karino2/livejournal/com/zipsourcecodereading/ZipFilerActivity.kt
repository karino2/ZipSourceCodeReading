package karino2.livejournal.com.zipsourcecodereading

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipFilerActivity : AppCompatActivity() {

    val lastZipPath by lazy {
        MainActivity.lastZipPath(this)
    }

    val zipArchive by lazy { SourceArchive(ZipFile(lastZipPath)) }

    val listView by lazy { findViewById(R.id.filerListView) as ListView }

    var currentFolder : ZipEntryAux? = null
    var entries : List<ZipEntryAux> = arrayListOf<ZipEntryAux>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zip_filer)


        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val item = entries[i]
            if(item.isDirectory) {
                showContent(item)
            } else {
                // dummy entry is always directory. so in this section, item always has original.
                openFile(item.original!!)
            }
        }

        showContent(currentFolder)
    }

    private fun openFile(item: ZipEntry) {
        val intent = Intent(this, SourceViewActivity::class.java)
        intent.putExtra("ZIP_FILE_ENTRY", item.toString())
        intent.putExtra("LINE_NUM", 1)
        startActivity(intent)
    }


    private fun showContent(dir : ZipEntryAux?) {

        currentFolder = dir
        entries = zipArchive.listFilesAt(currentFolder)
        val adapter = ArrayAdapter<String>(this, R.layout.list_item, entries.map { it.displayName })
        listView.adapter = adapter
    }
}
