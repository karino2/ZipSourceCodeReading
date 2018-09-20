package com.livejournal.karino2.zipsourcecodereading

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.*
import android.view.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import android.text.style.ForegroundColorSpan
import android.view.inputmethod.EditorInfo
import android.widget.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import com.livejournal.karino2.zipsourcecodereading.text.LongTextView
import syntaxhighlight.ParseResult
import java.io.File
import java.util.concurrent.TimeUnit


class SourceViewActivity : AppCompatActivity() {

    val sourceArchive : SourceArchive by lazy {
        SourceArchive(ZipFile(MainActivity.lastZipPath(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_view)
        sourceTextView.setString("Loading...")

        intent?.let {
            val zipEntryName = intent.getStringExtra("ZIP_FILE_ENTRY")
            val lineNum = intent.getIntExtra("LINE_NUM", 0)
            openFile(zipEntryName, lineNum)
        }
        sourceTextView.requestFocus()

        sourceTextView.onSearch = {sword ->
            showSearchBar()
            searchField.setText(sword)
            searchNext()
        }
        sourceTextView.onGSearch = {gword ->
            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra("SEARCH_WORD", gword)
            startActivity(intent)
        }


    }

    val handler by lazy {
        Handler()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.source_view, menu)
        return true
    }

    val searchField : EditText
    get() {
        return supportActionBar!!.customView.findViewById(R.id.edittext) as EditText
    }


    var isSearchOpened = false
    private fun showSearchBar() {
        val actionBar = supportActionBar!!
        if(!isSearchOpened) {
            isSearchOpened = true
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setCustomView(R.layout.search_bar)
            actionBar.setDisplayShowTitleEnabled(false)

            val searchBar = actionBar.customView
            val et = searchBar.findViewById<EditText>(R.id.edittext)
            et.setOnEditorActionListener(fun(_, actionId, _)  : Boolean {
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

            val cancelButton = searchBar.findViewById<ImageButton>(R.id.cancel)
            cancelButton.setOnClickListener { hideSearchBar() }
            val prevButton = searchBar.findViewById<ImageButton>(R.id.previous)
            prevButton.setOnClickListener { searchPrevious() }
            val nextButton = searchBar.findViewById<ImageButton>(R.id.next)
            nextButton.setOnClickListener { searchNext() }


        }
    }

    private fun hideSearchBar() {
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowCustomEnabled(false)
        actionBar.setDisplayShowTitleEnabled(true)
        isSearchOpened = false
    }


    private fun searchPrevious() {
        val text = sourceTextView.text
        val search = searchField.text.toString()
        if (search.isEmpty()) {
            return
        }
        val selection = sourceTextView.selectionStart - 1
        var previous = text.lastIndexOf(search, selection)
        if (previous > -1) {
            Selection.setSelection(sourceTextView.text as Spannable,
                    previous,
                    previous + search.length)
            if (!sourceTextView.isFocused()) {
                sourceTextView.requestFocus()
            }
            // sourceTextView.moveCursorToVisibleOffset()
        } else { // wrap
            previous = text.lastIndexOf(search)
            if (previous > -1) {
                Selection.setSelection(sourceTextView.text as Spannable,
                        previous,
                        previous + search.length)
                if (!sourceTextView.isFocused()) {
                    sourceTextView.requestFocus()
                }
                // sourceTextView.moveCursorToVisibleOffset()
            }
        }
    }


    private fun searchNext() {
        val text = sourceTextView.text
        val search = searchField.text.toString()
        if (search.isEmpty()) {
            return
        }
        val selection = sourceTextView.selectionEnd
        var next = text.indexOf(search, selection)
        if (next > -1) {
            Selection.setSelection(sourceTextView.text as Spannable,
                    next,
                    next + search.length)
            if (!sourceTextView.isFocused) {
                sourceTextView.requestFocus()
            }
        } else { // wrap
            next = text.indexOf(search)
            if (next > -1) {
                Selection.setSelection(sourceTextView.text as Spannable,
                        next,
                        next + search.length)
                if (!sourceTextView.isFocused) {
                    sourceTextView.requestFocus()
                }
            }
        }

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


    /*
    private val scrollView: ScrollView by lazy {
        findViewById(R.id.scrollView) as ScrollView
    }
    */

    /*
    private fun scrollPageUp() {
        scrollView.smoothScrollBy(0, -(2*scrollView.height)/3)
    }

    private fun scrollPageDown() {
        scrollView.smoothScrollBy(0, (2*scrollView.height)/3)
    }
    */

    fun tryScroll(tv : LongTextView, lineNum: Int) : Boolean {
        val layout = tv.layout ?: return false

        sourceTextView.moveToLine(lineNum)
        return true
    }

    private fun scrollToLine(sv: ScrollView, layout: Layout, lineNum: Int) {
        val pos = layout.getLineTop(lineNum)
        sv.smoothScrollTo(0, layout.getLineTop(lineNum))
    }

    fun startTryScroll(tv: LongTextView, lineNum: Int) {
        if(!tryScroll(tv, lineNum)) {
            handler.postDelayed({ startTryScroll(tv, lineNum)}, 50)
        }

    }


    private val sourceTextView: LongTextView by lazy {
        val sv = (findViewById(R.id.sourceTextView) as LongTextView)
        /*
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
        */
        sv
    }

    val parser = RxPrettifyParser()

    var parsing : Disposable = object: Disposable{
        override fun dispose() {
        }

        override fun isDisposed(): Boolean {
            return true
        }
    }

    fun showMessage(msg: String) = MainActivity.showMessage(this, msg)

    override fun onStop() {
        // this might cause partial code as non-coloring. But it's rare and also not fatal.
        parsing.dispose()

        super.onStop()
    }

    private fun  openFile(zipEntryName: String, lineNum : Int) {
        val ent = ZipEntry(zipEntryName)
        supportActionBar!!.title = ent.name

        parsing.dispose()
        Single.fromCallable {
            val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)
            val lines = reader.readLines()
            lines.joinToString("\n")
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { content ->

                    val tooLarge = content.count() > 500*1024

                    sourceTextView.setString(content)

                    if(!tooLarge) {
                        startColoring(zipEntryName, content)
                    } else {
                        handler.postDelayed({ showMessage("Too large file. Turn off coloring.") }, 1500)
                    }

                    handler.post {
                        startTryScroll(sourceTextView, lineNum)
                    }
                }
    }

    private fun startColoring(zipEntryName: String, content: String) {
        Observable.timer(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    parsing = Observable.zip(
                            parser.parse(File(zipEntryName).extension, content)
                                    .buffer(2000),
                            Observable.interval(200, TimeUnit.MILLISECONDS),
                            BiFunction { obs: List<ParseResult>, _/*time*/: Long->obs}
                    )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                it.map { oneResult ->
                                    val color = when (oneResult.styleKeys.get(0)) {
                                        "typ" -> 0xff763405
                                        "kwd" -> 0xff000088
                                        "lit" -> 0xff0000ff
                                        "com" -> 0xff666666
                                        "str" -> 0xffbb0000
                                        "pun" -> 0xff111111
                                        else -> 0xff000000
                                    }
                                    (sourceTextView.text as Spannable).setSpan(ForegroundColorSpan(color.toInt()), oneResult.offset, oneResult.offset + oneResult.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                            }
                }
    }
}
