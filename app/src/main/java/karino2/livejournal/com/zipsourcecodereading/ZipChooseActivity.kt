package karino2.livejournal.com.zipsourcecodereading

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast

class ZipChooseActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_PICK_ZIP = 1
    }

    private val zipPathField: EditText
        get() {
            val et = findViewById(R.id.zipPathField) as EditText
            return et
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zip_choose)

        val zipPath = getSharedPreferences("ZSCR_PREFS", Context.MODE_PRIVATE).getString(MainActivity.LAST_ZIP_PATH_KEY, "")
        if(zipPath != "") {
            zipPathField.setText(zipPath)
        }

        findViewById(R.id.browseZipButton).setOnClickListener { _ ->
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("application/zip")
            startActivityForResult(intent, REQUEST_PICK_ZIP);
        }

        findViewById(R.id.indexStartButton).setOnClickListener { _ ->
            showMessage("deb: " + zipPathField.text.toString())
        }

    }
    fun showMessage(msg : String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_PICK_ZIP ->{
                if(resultCode == RESULT_OK) {
                    data?.getData()?.getPath()?.let { zipPathField.setText(it) }
                }
                return
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
