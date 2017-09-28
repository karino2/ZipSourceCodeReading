package karino2.livejournal.com.zipsourcecodereading

import android.content.Intent
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
import android.text.Spannable
import android.text.Selection
import android.view.inputmethod.EditorInfo


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
        sf.setOnEditorActionListener(fun(view, actionId, keyEvent)  : Boolean {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchNext()
                return true;
            }
            // for hardware keyboard.
            if(actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                searchNext();
                return true;
            }
        return false;
        })
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
        val text = sourceTextView.getText().toString()
        val search = searchField.text.toString()
        if (search.length == 0) {
            return
        }
        val selection = sourceTextView.getSelectionStart() - 1
        var previous = text.lastIndexOf(search, selection)
        if (previous > -1) {
            Selection.setSelection(sourceTextView.getText() as Spannable,
                    previous,
                    previous + search.length)
            if (!sourceTextView.isFocused()) {
                sourceTextView.requestFocus()
            }
        } else { // wrap
            previous = text.lastIndexOf(search)
            if (previous > -1) {
                Selection.setSelection(sourceTextView.getText() as Spannable,
                        previous,
                        previous + search.length)
                if (!sourceTextView.isFocused()) {
                    sourceTextView.requestFocus()
                }
            }
        }
    }


    private fun searchNext() {
        val text = sourceTextView.getText().toString()
        val search = searchField.text.toString()
        if (search.length == 0) {
            return
        }
        val selection = sourceTextView.getSelectionEnd()
        var next = text.indexOf(search, selection)
        if (next > -1) {
            Selection.setSelection(sourceTextView.getText() as Spannable,
                    next,
                    next + search.length)
            if (!sourceTextView.isFocused()) {
                sourceTextView.requestFocus()
            }
        } else { // wrap
            next = text.indexOf(search)
            if (next > -1) {
                Selection.setSelection(sourceTextView.getText() as Spannable,
                        next,
                        next + search.length)
                if (!sourceTextView.isFocused()) {
                    sourceTextView.requestFocus()
                }
            }
        }

    }

    val tempCoords = IntArray(2)
    fun showSearchBar() {
        scrollView.getLocationInWindow(tempCoords);
        searchWindow.showAtLocation(scrollView, Gravity.TOP or Gravity.RIGHT, 0, tempCoords[1]);
        searchField.requestFocus()

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
            R.id.search_global -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if(event.action ==  KeyEvent.ACTION_DOWN ) {
                if(event.isCtrlPressed) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_F -> {
                            showSearchBar()
                            return true
                        }
                    }
                }
        }
        return super.dispatchKeyEvent(event)
    }


    var firstTime = true


    private val scrollView: ScrollView by lazy {
        findViewById(R.id.scrollView) as ScrollView
    }

    private fun scrollPageUp() {
        scrollView.smoothScrollBy(0, -(2*scrollView.height)/3)
    }

    private fun scrollPageDown() {
        scrollView.smoothScrollBy(0, (2*scrollView.height)/3)
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


    private val sourceTextView: TextView by lazy {
        val sv = (findViewById(R.id.sourceTextView) as TextView)
        sv.setOnKeyListener { view, keyCode, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {

                when (keyCode) {
                    KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_SPACE -> {
                        scrollPageDown()
                        true
                    }
                    KeyEvent.KEYCODE_B ->{
                        scrollPageUp()
                        true
                    }
                    else ->false
                }

            } else {
                false
            }
        }
        sv
    }

    private fun  openFile(zipEntryName: String, lineNum : Int) {
        val ent = ZipEntry(zipEntryName)
        supportActionBar!!.title = ent.name

        val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)

        val lines = reader.readLines()
        val tv = sourceTextView
        tv.text = lines.joinToString("\n")

        handler.post {
            startTryScroll(tv, lineNum)
        }

    }
}
