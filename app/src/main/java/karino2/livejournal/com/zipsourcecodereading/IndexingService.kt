package karino2.livejournal.com.zipsourcecodereading

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.support.v7.app.NotificationCompat
import karino2.livejournal.com.zipsourcecodereading.index.IndexWriter
import java.io.DataInputStream
import java.io.File
import java.util.zip.ZipFile

/**
 * Created by _ on 2017/09/21.
 */
class IndexingService : IntentService("IndexingService") {
    companion object {
        const val NOTIFICATION_ID = 1
    }

    val handler = Handler()

    fun showMessage(msg : String) = handler.post { MainActivity.showMessage(this, msg) }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val zipFile = File(intent.getStringExtra("ZIP_PATH"))
            val indexCandidate = ZipChooseActivity.indexCandidate(zipFile)
            val tmpFile = ZipChooseActivity.getTempDirectory()

            val indexWriter = IndexWriter(tmpFile, indexCandidate)

            val zipArchive = SourceArchive(ZipFile(zipFile))
            var count = 0
            zipArchive.listFiles()
                    .filter{ !File(it.name).name.startsWith(".") }
                    .doOnNext { handler.post { updateNotification(count++) }}
                    .map { indexWriter.add(it.toString(), DataInputStream(zipArchive.getInputStream(it))) }
                    .subscribe()
            showMessageNotification("start flush")
            indexWriter.flush()
            showMessageNotification("end flush")
            showMessageNotification("start notification")
            handler.post { showReadyNotification(zipFile) }
            return
        }
        showMessage("recreate case, NYI.")
    }

    val notificationManager : NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    val notificationBuilder: android.support.v4.app.NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
    }

    fun showMessageNotification(msg : String) = handler.post { _showMessageNotification(msg) }
    fun _showMessageNotification(msg : String) {
        notificationBuilder
                .setContentTitle("Indexing status")
                .setContentText(msg)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }

    fun updateNotification(fileCount: Int) {
        notificationBuilder
                .setContentTitle("Indexing...")
                .setContentText("Indexing $fileCount file.")

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }

    private fun showReadyNotification(zipFile: File) {
        notificationBuilder
                .setContentTitle("Index ready")
                .setContentText("Index ready")

        val intent = Intent(this, SearchActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())


    }

}