package karino2.livejournal.com.zipsourcecodereading

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    companion object {
        const val  LAST_ZIP_PATH_KEY = "last_zip_path"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zipPath = getSharedPreferences("ZSCR_PREFS", Context.MODE_PRIVATE).getString(LAST_ZIP_PATH_KEY, "")
        if("" != zipPath) {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val intent = Intent(this, ZipChooseActivity::class.java)
        startActivity(intent)

        finish();
    }
}
