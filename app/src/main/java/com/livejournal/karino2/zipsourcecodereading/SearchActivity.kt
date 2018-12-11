package com.livejournal.karino2.zipsourcecodereading

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.google.re2j.Parser
import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import com.google.re2j.RE2
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import com.livejournal.karino2.zipsourcecodereading.index.Index
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class SearchActivity : AppCompatActivity() {

    val sourceArchive : SourceArchive by lazy {
        SourceArchive(ZipFile(MainActivity.lastZipPath(this)))
    }

    val index : Index by lazy {
        Index.open(ZipChooseActivity.findIndex(File(MainActivity.lastZipPath(this)))!!)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val lineNumberTV = (view.findViewById(R.id.lineNumber) as TextView)
        val filePathTV = (view.findViewById(R.id.filePath) as TextView)
        val matchLineTV = (view.findViewById(R.id.matchLine) as TextView)
    }

    // fun showMessage(msg : String) = MainActivity.showMessage(this, msg)

    inner class MatchEntryAdapter : ObserverAdapter<RegexpReader.MatchEntry, ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.lineNumberTV.text = (item.lineNumber!!).toString()
            // holder.lineNumberTV.text = "___"
            holder.filePathTV.text = item.fentry
            holder.matchLineTV.text = item.line

            with(holder.view) {
                tag = item
                setOnClickListener { openFile(holder.filePathTV.text.toString(),  holder.lineNumberTV.text.toString().toInt()) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.search_result_item, parent, false))
        }

    }

    val searchAdapter = MatchEntryAdapter()

    var prevSearch : Disposable? = null

    fun showSearchingIndicator() {
        (findViewById<View>(R.id.progressBar)).visibility = View.VISIBLE
    }

    fun hideSearchingIndicator() {
        (findViewById<View>(R.id.progressBar)).visibility = View.GONE

    }

    val searchEntryField by lazy {
        findViewById<EditText>(R.id.searchEntryField)
    }

    fun startSearch() {
        prevSearch?.dispose()
        prevSearch = null


        searchAdapter.items.clear()
        searchAdapter.notifyDataSetChanged()


        val fpat = findViewById<EditText>(R.id.fileEntryField).text.toString()
        val spat =  searchEntryField.text.toString()

        val ffilter = fun(path : String) : Boolean {
            if(fpat == "")
                return true
            return path.contains(fpat)
        }


        try {
            val reader = RegexpReader(Pattern.compile(spat))

            // we should get regexp from pattern, but
            val query = Query.fromRegexp(Parser.parse(spat, RE2.PERL))

            val obs = Observable.defer { Observable.fromIterable(index.postingQuery(query)) }
                    .map { index.readName(it) }
                    .filter { ffilter(it) }
                    .flatMap { reader.Read(sourceArchive.getInputStream(ZipEntry(it)), it, 0) }
                    .subscribeOn(Schedulers.io())
                    .buffer(1, TimeUnit.SECONDS, 5)

            showSearchingIndicator()

            prevSearch = obs.observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete {
                        prevSearch = null
                        hideSearchingIndicator()
                    }
                    .subscribe { matches ->
                        if (matches.size > 0)
                            searchAdapter.addAll(matches)
                    }
        }catch(e: PatternSyntaxException) {
            showMessage("Rexp compile fail: ${e.toString()}")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = (findViewById(R.id.toolbar) as Toolbar)
        setSupportActionBar(toolbar)

        supportActionBar!!.title = sourceArchive.title

        assert(MainActivity.lastZipPath(this) != null)

        val recycle = (findViewById(R.id.searchResult) as RecyclerView)
        recycle.adapter = searchAdapter
        searchAdapter.datasetChangedNotifier()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { ada ->
                    // ada.notifyItemRangeInserted(0, ada.items.size)
                    ada.notifyDataSetChanged()
                }

        val divider = DividerItemDecoration(this, (recycle.layoutManager as LinearLayoutManager).orientation)
        recycle.addItemDecoration(divider)


        var searchPublisher = PublishSubject.create<Int>()
        searchPublisher.throttleFirst(500L, TimeUnit.MILLISECONDS)
                .subscribe() {
                    when(it) {
                        EditorInfo.IME_ACTION_SEARCH-> {
                            hideSoftkey()
                            startSearch()
                        }
                        EditorInfo.IME_ACTION_UNSPECIFIED -> {
                            startSearch();
                        }
                    }
                }


        (findViewById(R.id.searchEntryField) as EditText).setOnEditorActionListener(fun(_, actionId, _)  : Boolean {
            when(actionId) {
                EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_UNSPECIFIED /* for hardware keyboard. */ -> {
                    searchPublisher.onNext(actionId)
                    return true
                }

            }
            return false;
        })

        intent?.let {
            val word = intent.getStringExtra("SEARCH_WORD")
            if(!word.isNullOrEmpty()) {
                searchEntryField.setText(word)
                handler.post { startSearch() }
            }
        }

    }

    val handler by lazy { Handler() }

    fun hideSoftkey() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    private fun  openFile(ent: String, lineNum: Int) {
        val intent = Intent(this, SourceViewActivity::class.java)
        intent.putExtra("ZIP_FILE_ENTRY", ent)
        intent.putExtra("LINE_NUM", lineNum)
        startActivity(intent)
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
            R.id.action_filer -> {
                val intent = Intent(this, ZipFilerActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
