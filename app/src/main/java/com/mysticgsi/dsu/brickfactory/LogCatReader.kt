package com.mysticgsi.dsu.brickfactory

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.mysticgsi.dsu.MainActivity
import com.mysticgsi.dsu.R
import java.io.IOException

class LogCatReader(private val mainActivity: MainActivity) {
    fun start() { //TODO: do it better
        Thread {
            try {
                val command = arrayOf(
                    "logcat",
                    "-T 0",
                    "-v", "tag",
                    "gsid:*", "*:S",
                    "DynamicSystemService:*", "*:S",
                    "DynamicSystemInstallationService:*", "*:S",
                    "DynSystemInstallationService:*", "*:S"
                )

                val commandString = command.joinToString(" ")

                val process = Runtime.getRuntime().exec("su -c $commandString")

                val reader = process.inputStream.bufferedReader()

                var line: String?
                while (true) {
                    line = reader.readLine()
                    if (line != null) {
                        Log.d("MysticDSU", line)
                        process(line)
                    }
                }
            } catch (e: IOException) {
                Log.d("MysticDSU", "ERROR: ${e.message}")
            }
        }.start()
    }

    fun process(line: String) {
        if (line.contains("DynamicSystemInstallationService") && line.contains("NOT_STARTED")){
            if (line.contains("INSTALL_CANCELLED")) {
                (mainActivity as? Activity)?.runOnUiThread {
                    mainActivity.findViewById<CardView>(R.id.install_card).visibility = View.GONE
                }
            } else if (line.contains("detail:")) {
                (mainActivity as? Activity)?.runOnUiThread {
                    mainActivity.findViewById<TextView>(R.id.install_status_text).text = "Error!"
                    mainActivity.findViewById<TextView>(R.id.install_status_sub_text).text = line.substringAfter("detail:").trim()
                    mainActivity.findViewById<MaterialButton>(R.id.abort_button).visibility =
                        View.GONE
                    mainActivity.findViewById<MaterialButton>(R.id.reboot_button).visibility =
                        View.GONE
                    mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progressDrawable.setTint(Color.RED)
                }
            }
        }

        if (line.contains("DynamicSystemInstallationService") && line.contains("READY")) {
            if (line.contains("INSTALL_COMPLETED")) {
                (mainActivity as? Activity)?.runOnUiThread {
                    mainActivity.findViewById<TextView>(R.id.install_status_text).text =
                        "Install is done!"
                    mainActivity.findViewById<TextView>(R.id.install_status_sub_text).text =
                        "Reboot and test it!"
                    mainActivity.findViewById<MaterialButton>(R.id.abort_button).text = "Discard"
                    mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progress = 100
                    mainActivity.findViewById<MaterialButton>(R.id.abort_button).visibility =
                        View.VISIBLE
                    mainActivity.findViewById<MaterialButton>(R.id.reboot_button).visibility =
                        View.VISIBLE
                    mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progressDrawable.setTintList(null)
                }
            }
        }

        if (line.contains("isInstalled(): true")) {
            (mainActivity as? Activity)?.runOnUiThread {
                mainActivity.findViewById<TextView>(R.id.install_status_text).text =
                    "Install is done!"
                mainActivity.findViewById<TextView>(R.id.install_status_sub_text).text =
                    "Reboot and test it!"
                mainActivity.findViewById<MaterialButton>(R.id.abort_button).text = "Discard"
                mainActivity.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                mainActivity.findViewById<MaterialButton>(R.id.abort_button).visibility =
                    View.VISIBLE
                mainActivity.findViewById<MaterialButton>(R.id.reboot_button).visibility =
                    View.VISIBLE
                mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progressDrawable.setTintList(null)
            }
        }

        if (line.contains("DynamicSystemInstallationService") && line.contains("IN_PROGRESS")) {
            val regex = Regex(
                """status:\s*(\w+),\s*cause:\s*(\w+),\s*partition\s*name:\s*(\w+),\s*progress:\s*(\d+)/(\d+),\s*total_progress:\s*(\d+)%"""
            )
            val matchResult = regex.find(line)

            matchResult?.let {
                val (status, cause, partitionName, progress, totalProgress, totalProgressPercent) = it.destructured

                (mainActivity as? Activity)?.runOnUiThread {
                    mainActivity.findViewById<CardView>(R.id.install_card).visibility = View.VISIBLE
                    mainActivity.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
                    mainActivity.findViewById<MaterialButton>(R.id.reboot_button).visibility = View.GONE
                    mainActivity.findViewById<MaterialButton>(R.id.abort_button).text = "Cancel"
                    mainActivity.findViewById<MaterialButton>(R.id.abort_button).visibility = View.VISIBLE
                    val progressBomb = totalProgressPercent.replace("%", "").toInt();
                    if (progressBomb > 0) {
                        mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progress = progressBomb
                        mainActivity.findViewById<ProgressBar>(R.id.progress_bar).isIndeterminate = false
                        mainActivity.findViewById<TextView>(R.id.install_status_text).text = "Installing..."
                    } else {
                        mainActivity.findViewById<ProgressBar>(R.id.progress_bar).isIndeterminate = true
                        mainActivity.findViewById<TextView>(R.id.install_status_text).text = "Starting..."
                    }

                    mainActivity.findViewById<TextView>(R.id.install_status_sub_text).text = "${partitionName} ${progress}/${totalProgress}"
                    mainActivity.findViewById<ProgressBar>(R.id.progress_bar).progressDrawable.setTintList(null)
                }

            }
        }
    }
}