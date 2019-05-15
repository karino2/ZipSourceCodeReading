package com.livejournal.karino2.zipsourcecodereading

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.EditText
import java.io.File
import java.io.IOException


class ZipChooseActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_PICK_ZIP = 1
        const val FOLDER_NAME = "ZipSourceCodeReading"

        @Throws(IOException::class)
        fun ensureDirExist(dir: File) {
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw IOException()
                }
            }
        }

        @Throws(IOException::class)
        fun getStoreDirectory(): File {
            val dir = File(Environment.getExternalStorageDirectory(), FOLDER_NAME)
            ensureDirExist(dir)
            return dir
        }

        @Throws(IOException::class)
        fun getTempDirectory(): File {
            val dir = File(getStoreDirectory(), "tmp")
            ensureDirExist(dir)
            return dir
        }

        fun findIndex(zipPath: File) : File? {
            val cand1 = indexCandidate(zipPath)
            if(cand1.exists())
                return cand1
            val cand2 = File(zipPath.absolutePath + ".idx")
            if(cand2.exists())
                return cand2
            return null
        }

        fun indexCandidate(zipPath: File): File {
            return File(getStoreDirectory(), zipPath.name + ".idx")
        }

    }

    override fun onNewIntent(intent: Intent?) {
        // indexing finished. goto search activity.
        replaceToSearchActivity()
    }

    private val zipPathField: EditText
        get() {
            val et = findViewById(R.id.zipPathField) as EditText
            return et
        }

    val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zip_choose)

        val zipPath = MainActivity.lastZipPath(this)
        zipPath?.let{ zipPathField.setText(zipPath) }

        findViewById<View>(R.id.browseZipButton).setOnClickListener { _ ->
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("application/zip")
            startActivityForResult(intent, REQUEST_PICK_ZIP);
        }

        findViewById<View>(R.id.indexStartButton).setOnClickListener { _ ->
            val path = zipPathField.text.toString()
            onZipPathChosen(path)
        }

        requestReadExternalStorage()

    }

    private fun onZipPathChosen(path: String) {
        MainActivity.writeLastZipPath(this, path)

        // val zipIS = ZipInputStream(contentResolver.openInputStream(Uri.parse(path)))

        val zipFile = File(path)
        val indexFile = findIndex(zipFile)
        indexFile?.let {
            replaceToSearchActivity()
            return
        }
        startIndexingService(zipFile)

    }

    private fun replaceToSearchActivity() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startIndexingService(zipFile: File) {
        findViewById<View>(R.id.indexStartButton).isEnabled = false
        showMessage("Start indexing...")

        val intent = Intent(this, IndexingService::class.java)

        intent.putExtra("ZIP_PATH", zipFile.absolutePath)
        startService(intent)
    }

    fun showMessage(msg : String) = MainActivity.showMessage(this, msg)

    fun externalStorageDir(storageType : String) : File {
        val primary = Environment.getExternalStorageDirectory()
        if(storageType == "primary") {
            return primary
        }
        val extdirs = getExternalFilesDirs(null)
        val suffix = extdirs[0].absolutePath.substring(primary.absolutePath.length)
        for(file in getExternalFilesDirs(null)) {
            val abspath = file.absolutePath
            val dirstr = abspath.substring(0 until (abspath.length - suffix.length))
            val dir = File(dirstr)
            if(dir.name.equals(storageType))
                return dir;

        }

        throw IllegalArgumentException("No storage found.")

    }

    fun Uri.toPath() : String {
        // val sel = "${MediaStore.Files.FileColumns._ID}=?"

        if("com.android.externalstorage.documents".equals(this.authority)) {
            val docId = DocumentsContract.getDocumentId(this)
            val split = docId.split(":")

            return externalStorageDir(split[0]).absolutePath + "/${split[1]}"
        }
        return this.path

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_PICK_ZIP ->{
                if(resultCode == RESULT_OK) {
                    data?.getData()?.let { zipPathField.setText(it.toPath()) }
                }
                return
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    fun requestReadExternalStorage(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_READ_EXTERNAL_STORAGE_ID)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE_ID ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

        }

        findViewById<View>(R.id.browseZipButton).isEnabled = false

    }
}


