package karino2.livejournal.com.zipsourcecodereading

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Layout
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class SourceViewActivity : AppCompatActivity() {

    val sourceArchive : SourceArchive by lazy {
        SourceArchive(ZipFile(MainActivity.lastZipPath(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_view)

        intent?.let {
            val zipEntryName = intent.getStringExtra("ZIP_FILE_ENTRY")
            val lineNum = intent.getIntExtra("LINE_NUM", 0)
            openFile(zipEntryName, lineNum)
        }

    }

    val handler by lazy {
        Handler()
    }


    var firstTime = true



    fun tryScroll(tv : TextView, lineNum: Int) : Boolean {
        val sv = findViewById(R.id.scrollView) as ScrollView
        val layout = tv.layout
        if(layout == null)
            return false
        if(firstTime) {
            firstTime = false
            var onlayout : View.OnLayoutChangeListener = View.OnLayoutChangeListener{_, _, _, _, _, _, _, _, _ ->}
            onlayout = View.OnLayoutChangeListener {_, _, _, _, _, _, _, _, _ ->
                scrollToLine(sv, layout, lineNum)
                sv.removeOnLayoutChangeListener(onlayout)
            }
            sv.addOnLayoutChangeListener(onlayout)
            return true
        }
        scrollToLine(sv, layout, lineNum)
        return true
    }

    private fun scrollToLine(sv: ScrollView, layout: Layout, lineNum: Int) {
        val pos = layout.getLineTop(lineNum)
        sv.smoothScrollTo(0, layout.getLineTop(lineNum))
    }

    fun startTryScroll(tv: TextView, lineNum: Int) {
        if(!tryScroll(tv, lineNum)) {
            handler.postDelayed({ startTryScroll(tv, lineNum)}, 50)
        }

    }


    private fun  openFile(zipEntryName: String, lineNum : Int) {
        val ent = ZipEntry(zipEntryName)
        supportActionBar!!.title = ent.name

        val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)

        val lines = reader.readLines()
        val tv = (findViewById(R.id.sourceTextView) as TextView)
        tv.text = lines.joinToString("\n")

        handler.post {
            startTryScroll(tv, lineNum)
        }

    }
}
