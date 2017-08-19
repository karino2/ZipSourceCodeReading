package karino2.livejournal.com.zipsourcecodereading

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        assert(MainActivity.lastZipPath(this) != null)

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
