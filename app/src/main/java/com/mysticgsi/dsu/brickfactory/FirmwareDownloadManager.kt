package com.mysticgsi.dsu.brickfactory

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.transition.Visibility
import com.mysticgsi.dsu.PopupUtil
import com.mysticgsi.dsu.R
import java.io.File

class FirmwareDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun isExist(url: String) : Boolean {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: "firmware.zip"
        val destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destinationFolder, fileName)
        return file.exists()
    }

    fun deleteFile(url: String) : Boolean {
        //todo
        return false
    }

    fun downloadFile(url: String, holder: FirmwareAdapter.FirmwareViewHolder) {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: return
        val destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destinationFolder, fileName)

        if (file.exists()) {
            return
        }

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Downloading $fileName")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

        val downloadId = downloadManager.enqueue(request)

        updateProgressBar(downloadId, holder)
    }

    fun restoreDownloadByUrl(holder: FirmwareAdapter.FirmwareViewHolder, url: String) {
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: return

        val downloadId = getDownloadIdByFileName(fileName)
        if (downloadId != -1L) {
            updateProgressBar(downloadId, holder)
        }
    }

    private fun getDownloadIdByFileName(fileName: String): Long {
        val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)
        val cursor: Cursor = downloadManager.query(query)

        while (cursor.moveToNext()) {
            val idColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val uriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)

            if (idColumnIndex != -1 && uriColumnIndex != -1) {
                val id = cursor.getLong(idColumnIndex)
                val fileUri = cursor.getString(uriColumnIndex)?.let { Uri.parse(it) }

                fileUri?.let {
                    val downloadedFileName = it.lastPathSegment
                    if (downloadedFileName == fileName) {
                        cursor.close()
                        return id
                    }
                }
            }
        }
        cursor.close()
        return -1L
    }

    @SuppressLint("Range")
    private fun updateProgressBar(downloadId: Long, holder: FirmwareAdapter.FirmwareViewHolder) {
        holder.deleteButton.visibility = View.GONE
        holder.downloadButton.visibility = View.GONE
        holder.flashButton.visibility = View.GONE
        holder.progressBar.visibility = View.VISIBLE
        holder.progressBar.progress = 0

        Thread {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val progress = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))


                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        (context as? Activity)?.runOnUiThread {
                            holder.progressBar.visibility = View.GONE
                            holder.flashButton.visibility = View.VISIBLE
                        }

                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        (context as? Activity)?.runOnUiThread {
                            holder.progressBar.progress = 50
                            holder.progressBar.progressDrawable.setTint(Color.RED)
                        }
                    } else {
                        val percent = (progress * 100L / total).toInt()
                        holder.progressBar.progress = percent
                    }
                }
                cursor.close()
                Thread.sleep(200)
            }
        }.start()
    }
}
