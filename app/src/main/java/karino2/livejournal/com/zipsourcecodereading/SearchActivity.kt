package karino2.livejournal.com.zipsourcecodereading

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.google.re2j.Pattern
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class SearchActivity : AppCompatActivity() {

    val sourceArchive : SourceArchive by lazy {
        SourceArchive(ZipFile(MainActivity.lastZipPath(this)))
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
            // holder.lineNumberTV.text = (item.lineNumber!!).toString()
            holder.lineNumberTV.text = "___"
            holder.filePathTV.text = item.fentry
            holder.matchLineTV.text = item.line

            // todo onitem click listener
            with(holder.view) {
                tag = item
                setOnClickListener { showMessage("clicked: " + holder.filePathTV.text.toString() ) }
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
        (findViewById(R.id.progressBar)).visibility = View.VISIBLE
    }

    fun hideSearchingIndicator() {
        (findViewById(R.id.progressBar)).visibility = View.GONE

    }

    fun startSearch() {
        prevSearch?.dispose()
        prevSearch = null


        searchAdapter.items.clear()
        searchAdapter.notifyDataSetChanged()


        val fpat = (findViewById(R.id.fileEntryField) as EditText).text.toString()
        val spat =  (findViewById(R.id.searchEntryField) as EditText).text.toString()

        val ffilter = fun(path : String) : Boolean {
            if(fpat == "")
                return true
            return path.contains(fpat)
        }


        val reader = RegexpReader(Pattern.compile(spat))

        val obs = sourceArchive.listFiles()
                .filter{  ffilter(it.name) }
                .flatMap{ reader.Read(sourceArchive.getInputStream(it), it.toString(), null) }
                .subscribeOn(Schedulers.io())
                .buffer(1, TimeUnit.SECONDS, 5)

        showSearchingIndicator()

        prevSearch = obs.observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    prevSearch=null
                    hideSearchingIndicator()
                }
                .subscribe { matches ->
                    if (matches.size > 0)
                        searchAdapter.addAll(matches)
                }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = (findViewById(R.id.toolbar) as Toolbar)
        setSupportActionBar(toolbar)

        assert(MainActivity.lastZipPath(this) != null)

        val recycle = (findViewById(R.id.searchResult) as RecyclerView)
        recycle.adapter = searchAdapter
        searchAdapter.datasetChangedNotifier()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { ada ->
                    // ada.notifyItemRangeInserted(0, ada.items.size)
                    ada.notifyDataSetChanged()
                }

        findViewById(R.id.goButton).setOnClickListener {

            val cond = (findViewById(R.id.fileEntryField) as EditText).text.toString()

            sourceArchive.listFiles()
                    .filter{ it.name.contains(cond) }
                    .take(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { ent -> openFile(ent) }

        }

        (findViewById(R.id.searchEntryField) as EditText).setOnEditorActionListener(fun(view, actionId, keyEvent)  : Boolean {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch()
                return true;
            }
            return false;
        })

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
        val intent = Intent(this, SourceViewActivity::class.java)
        intent.putExtra("ZIP_FILE_ENTRY", ent.toString())
        startActivity(intent)
        /*
        val reader = BufferedReader(InputStreamReader(sourceArchive.getInputStream(ent)), 8*1024)

        val lines = reader.readLines()
        (findViewById(R.id.contentArea) as TextView).text = lines.joinToString("\n")
        */
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
