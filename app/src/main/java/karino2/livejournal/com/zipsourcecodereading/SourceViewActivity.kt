package karino2.livejournal.com.zipsourcecodereading

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Layout
import android.view.*
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import android.widget.ImageButton
import android.widget.EditText
import android.os.Build
import android.text.Spannable
import android.text.Selection
import android.text.Selection.getSelectionEnd




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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.source_view, menu)
        return true
    }

    val searchField : EditText by lazy {
        val sf = searchBar.findViewById(R.id.edittext) as EditText
        sf.setOnKeyListener {
            v, key, ev ->
            if(key == KeyEvent.KEYCODE_ENTER) {
                searchNext()
                true
            }else {
                false
            }
        }
        sf
    }


    val searchBar : View by lazy {
        layoutInflater.inflate(R.layout.search_bar, null)
    }

    val searchWindow: PopupWindow by lazy {

        val searchWindow = PopupWindow(searchBar, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        searchWindow.setFocusable(true)



        val cancelButton = searchBar.findViewById(R.id.cancel) as ImageButton
        cancelButton.setOnClickListener { hideSearchBar() }
        val prevButton = searchBar.findViewById(R.id.previous) as ImageButton
        prevButton.setOnClickListener{ searchPrevious() }
        val nextButton = searchBar.findViewById(R.id.next) as ImageButton
        nextButton.setOnClickListener{ searchNext() }
        searchWindow
    }

    private fun searchPrevious() {
        val text = textView.getText().toString()
        val search = searchField.text.toString()
        if (search.length == 0) {
            return
        }
        val selection = textView.getSelectionStart() - 1
        var previous = text.lastIndexOf(search, selection)
        if (previous > -1) {
            Selection.setSelection(textView.getText() as Spannable,
                    previous,
                    previous + search.length)
            if (!textView.isFocused()) {
                textView.requestFocus()
            }
        } else { // wrap
            previous = text.lastIndexOf(search)
            if (previous > -1) {
                Selection.setSelection(textView.getText() as Spannable,
                        previous,
                        previous + search.length)
                if (!textView.isFocused()) {
                    textView.requestFocus()
                }
            }
        }
    }


    private fun searchNext() {
        val text = textView.getText().toString()
        val search = searchField.text.toString()
        if (search.length == 0) {
            return
        }
        val selection = textView.getSelectionEnd()
        var next = text.indexOf(search, selection)
        if (next > -1) {
            Selection.setSelection(textView.getText() as Spannable,
                    next,
                    next + search.length)
            if (!textView.isFocused()) {
                textView.requestFocus()
            }
        } else { // wrap
            next = text.indexOf(search)
            if (next > -1) {
                Selection.setSelection(textView.getText() as Spannable,
                        next,
                        next + search.length)
                if (!textView.isFocused()) {
                    textView.requestFocus()
                }
            }
        }

    }

    val tempCoords = IntArray(2)
    fun showSearchBar() {
        scrollView.getLocationInWindow(tempCoords);
        searchWindow.showAtLocation(scrollView, Gravity.NO_GRAVITY, 0, tempCoords[1]);
    }

    fun hideSearchBar() {
        searchWindow.dismiss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.search_this -> {
                showSearchBar()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }


    var firstTime = true


    private val scrollView: ScrollView by lazy {
        findViewById(R.id.scrollView) as ScrollView
    }

    fun tryScroll(tv : TextView, lineNum: Int) : Boolean {
        val sv = scrollView
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


    private val textView: TextView by lazy {
        (findViewById(R.id.sourceTextView) as TextView)
    }

    private fun  openFile(zipEntryName: String, lineNum : Int) {
        val ent = ZipEntry(zipEntryName)
        supportActionBar!!.title = ent.name

        val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)

        val lines = reader.readLines()
        val tv = textView
        tv.text = lines.joinToString("\n")

        handler.post {
            startTryScroll(tv, lineNum)
        }

    }
}
