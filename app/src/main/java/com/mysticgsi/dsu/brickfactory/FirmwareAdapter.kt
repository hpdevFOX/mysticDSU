package com.mysticgsi.dsu.brickfactory

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mysticgsi.dsu.PopupUtil
import com.mysticgsi.dsu.networking.Image
import com.mysticgsi.dsu.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class FirmwareAdapter(private var context: Context, private var firmwareList: List<Image>, private var firmwareDownloadManager: FirmwareDownloadManager, private var userDataSize : Long) :
    RecyclerView.Adapter<FirmwareAdapter.FirmwareViewHolder>() {

    class FirmwareViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.name)
        val detailsTextView: TextView = view.findViewById(R.id.details)
        val downloadButton: TextView = view.findViewById(R.id.download_button)
        val flashButton: TextView = view.findViewById(R.id.install_button)
        val deleteButton: TextView = view.findViewById(R.id.delete_button)
        val progressBar: ProgressBar = view.findViewById(R.id.download_bar)
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FirmwareViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_firmware, parent, false)
        return FirmwareViewHolder(view)
    }

    override fun onBindViewHolder(holder: FirmwareViewHolder, position: Int) {
        val firmware = firmwareList[position]
        if (firmwareDownloadManager.isExist(firmware.uri)) {
            holder.downloadButton.visibility = View.GONE
            holder.flashButton.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.GONE
        }

        Glide.with(context)
            .load("https://mysticcloudmain.hpdevfox.ru/icons/${firmware.os_version}.png")
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_warn)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imageView)

        val gsiTool = GSITool()

        holder.nameTextView.text = firmware.name
        holder.detailsTextView.text = firmware.details
        holder.downloadButton.setOnClickListener {
            firmwareDownloadManager.downloadFile(firmware.uri, holder)
        }
        firmwareDownloadManager.restoreDownloadByUrl(holder, firmware.uri)
        holder.flashButton.setOnClickListener {
            val client = OkHttpClient()
            val request = Request.Builder().url(firmware.tos).build()

            Thread {
                try {
                    val response: okhttp3.Response = client.newCall(request).execute()
                    val tosText = response.body?.string() ?: "Failed to load ToS"

                    (context as? Activity)?.runOnUiThread {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("MysticDSU | ToS")
                            .setMessage(tosText)
                            .setPositiveButton("FLASH") { _, _ ->
                                holder.flashButton.visibility = View.GONE
                                val uri = Uri.parse(firmware.uri)
                                val fileName = uri.lastPathSegment ?: "firmware.zip"
                                gsiTool.wipe()
                                val destinationFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val file = File(destinationFolder, fileName)

                                val command = """am start-activity \
-n com.android.dynsystem/com.android.dynsystem.VerificationActivity \
-a android.os.image.action.START_INSTALL \
-d file://${file.absolutePath} \
--el KEY_SYSTEM_SIZE 13958643712 \
--el KEY_USERDATA_SIZE ${userDataSize}
""".trimIndent()

                                try {
                                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                    process.waitFor()
                                } catch (e: Exception) {
                                    PopupUtil.showPopup(context, "ROOT REQUIRED!", "You can't install DSU without root!")
                                }
                                (context as Activity).finish()
                            }
                            .setNegativeButton("ABORT") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setCancelable(true)
                            .show()
                    }
                } catch (_: Exception) {
                    PopupUtil.showPopup(context, "NETWORK ERROR!", "Unable to fetch data from the DSU server!")
                }
            }.start()
        }
        holder.deleteButton.setOnClickListener {
            //if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1001)
            //}

            if (firmwareDownloadManager.deleteFile(firmware.uri)) //FirmwareDownloadManager.deleteFile(firmware.uri)
            {
                holder.downloadButton.visibility = View.VISIBLE
                holder.flashButton.visibility = View.GONE
                holder.deleteButton.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return firmwareList.size
    }

    fun updateFirmwareList(newFirmwareList: List<Image>) {
        firmwareList = newFirmwareList
        notifyDataSetChanged()
    }
}
