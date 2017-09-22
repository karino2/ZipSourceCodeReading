package karino2.livejournal.com.zipsourcecodereading

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        const val  LAST_ZIP_PATH_KEY = "last_zip_path"
        fun lastZipPath(ctx: Context) = sharedPreferences(ctx).getString(LAST_ZIP_PATH_KEY, null)
        fun writeLastZipPath(ctx: Context, path : String) = sharedPreferences(ctx).edit()
                .putString(LAST_ZIP_PATH_KEY, path)
                .commit()

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("ZSCR_PREFS", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zipPath = lastZipPath(this)
        zipPath?.let {
            val idx = ZipChooseActivity.findIndex(File(zipPath))
            idx?.let {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
        }

        val intent = Intent(this, ZipChooseActivity::class.java)
        startActivity(intent)

        finish();
    }
}
