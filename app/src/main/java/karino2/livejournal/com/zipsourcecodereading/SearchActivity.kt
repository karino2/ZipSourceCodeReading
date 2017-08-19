package karino2.livejournal.com.zipsourcecodereading

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class SearchActivity : AppCompatActivity() {

    val sourceArchive : SourceArchive by lazy {
        SourceArchive(ZipFile(MainActivity.lastZipPath(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        assert(MainActivity.lastZipPath(this) != null)

        findViewById(R.id.goButton).setOnClickListener {

            val cond = (findViewById(R.id.fileEntryField) as EditText).text.toString()

            sourceArchive.listFiles()
                    .filter{ it.name.contains(cond) }
                    .take(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { ent -> openFile(ent) }

        }

        /*
        (findViewById(R.id.fileEntryField) as EditText).setOnEditorActionListener(fun (textView, actionId, keyEvent) : Boolean{
            keyEvent?.let {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    showMessage("deb: ")
                    // let system handle ime related operation.
                    return false
                }
            }
            when(keyEvent?.action) {
                KeyEvent.ACTION_DOWN -> {
                    showMessage("deb: ")
                    return true
                }
                null -> {
                    if(actionId == EditorInfo.IM)
                }
            }
            return false
        })
        */
    }

    private fun  openFile(ent: ZipEntry) {
        val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)

        val lines = reader.readLines()
        (findViewById(R.id.contentArea) as TextView).text = lines.joinToString("\n")
    }

    fun showMessage(msg : String) = MainActivity.showMessage(this, msg)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.action_choose -> {
                val intent = Intent(this, ZipChooseActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
