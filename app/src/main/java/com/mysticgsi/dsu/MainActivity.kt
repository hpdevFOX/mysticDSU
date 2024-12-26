package com.mysticgsi.dsu

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mysticgsi.dsu.brickfactory.FirmwareActivity
import com.mysticgsi.dsu.brickfactory.GSITool
import com.mysticgsi.dsu.brickfactory.LogCatReader
import com.mysticgsi.dsu.networking.ApiClient
import com.mysticgsi.dsu.networking.FirmwareResponse
import com.mysticgsi.dsu.networking.VersionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private var clickCount = 0
    private var updateLink : String = "";
    val gsiTool = GSITool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTexts()
        validateRoot()

        val logCatReader = LogCatReader(this);
        logCatReader.start()

        validateVersion()

        try {
            loadData()
        } catch (_: Exception) {}
    }

    private fun validateVersion() {
        findViewById<CardView>(R.id.update_card).visibility = View.GONE
        ApiClient.apiService.getVersion().enqueue(object :
            Callback<VersionResponse> {
            override fun onResponse(
                call: Call<VersionResponse>,
                response: Response<VersionResponse>)
            {
                if (response.isSuccessful) {
                    val versionResponse = response.body()
                    versionResponse?.let {
                        val version = it.version
                        updateLink = it.url

                        if (version != "1.1BETA") {
                            findViewById<CardView>(R.id.update_card).visibility = View.VISIBLE
                        }
                    }

                }
            }

            override fun onFailure(call: Call<VersionResponse>, t: Throwable) {
                findViewById<CardView>(R.id.update_card).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.update_card_icon).setImageResource(R.drawable.ic_warn)
                findViewById<TextView>(R.id.update_card_text).text = "Failed to connect to the server."


            }

        })
    }

    private fun drawInstalled() {
        val (isInstalled, isEnabled) = gsiTool.getStatus()

        if (isInstalled) {
            findViewById<CardView>(R.id.install_card).visibility = View.VISIBLE
            findViewById<TextView>(R.id.install_status_text).text =
                "Install is done!"
            findViewById<TextView>(R.id.install_status_sub_text).text =
                "Reboot and test it!"
            findViewById<MaterialButton>(R.id.abort_button).text = "Discard"
            findViewById<ProgressBar>(R.id.progress_bar).progress = 100
            findViewById<MaterialButton>(R.id.abort_button).visibility =
                View.VISIBLE
            findViewById<MaterialButton>(R.id.reboot_button).visibility =
                View.VISIBLE
            findViewById<ProgressBar>(R.id.progress_bar).progressDrawable.setTintList(null)
        }
    }

    private fun validateRoot() {
        val rootCard = findViewById<CardView>(R.id.root_card)
        try {
            drawInstalled()
            rootCard.visibility = View.GONE;
        } catch (e: Exception) {
            rootCard.visibility = View.VISIBLE;
        }
    }

    private fun buildFooter() {
        var textView = findViewById<TextView>(R.id.brick_factory_project_what_you_forgot_here)
        val bestBrick = "TG: @mysticgsi | MysticDSU v1.1BETA\nProvided for you by \uD83E\uDD8A & \uD83C\uDF3C"
        val spannableString = SpannableString(bestBrick)
        val fontSize = textView.textSize

        val hpdevDrawable: Drawable = ContextCompat.getDrawable(this, R.drawable.emoji_hpdevfox)!!
        val romashkaDrawable: Drawable = ContextCompat.getDrawable(this, R.drawable.emoji_romashka)!!

        hpdevDrawable.setBounds(0, -10, fontSize.toInt(), fontSize.toInt() -10)
        romashkaDrawable.setBounds(0, -10, fontSize.toInt(), fontSize.toInt() -10)

        spannableString.setSpan(ImageSpan(hpdevDrawable), bestBrick.indexOf("🦊"), bestBrick.indexOf("🦊") + 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ImageSpan(romashkaDrawable), bestBrick.indexOf("\uD83C\uDF3C"), bestBrick.indexOf("\uD83C\uDF3C") + 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
    }

    private fun setTexts() {
        buildFooter()

        val textView = findViewById<TextView>(R.id.title_text)
        textView.text = "MysticDSU";
        textView.setOnClickListener {
            clickCount++
            if (clickCount == 10) {
                findViewById<TextInputLayout>(R.id.auth_input_layout).visibility = View.VISIBLE;
            }
        }
    }

    private fun loadData() {
        try {
            val sharedPreferences = getSharedPreferences("MysticDSUPrefs", MODE_PRIVATE)
            findViewById<TextInputEditText>(R.id.userdata_text_input_layout).setText(
                sharedPreferences.getInt("userdata", 16).toString()
            )
            findViewById<TextInputEditText>(R.id.auth_text_input_layout).setText(
                sharedPreferences.getString(
                    "authkey",
                    ""
                )
            )
        } catch (_ : Exception) {}
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("MysticDSUPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("userdata", findViewById<TextInputEditText>(R.id.userdata_text_input_layout).text.toString().toInt());
        editor.putString("authkey", findViewById<TextInputEditText>(R.id.auth_text_input_layout).text.toString())
        editor.apply()
    }

    fun onRebootButton(view: View) {
        gsiTool.rebootIntoDSU()
    }

    fun onAbortButton(view: View) {
        gsiTool.abortInstall()
    }

    fun onOpenDSUButton(view: View) {
        val intent = Intent(this, FirmwareActivity::class.java)
        startActivity(intent)
    }

    fun onSaveButton(view: View) {
        findViewById<TextInputEditText>(R.id.auth_text_input_layout).clearFocus()
        findViewById<TextInputEditText>(R.id.userdata_text_input_layout).clearFocus()
        try {
            findViewById<TextInputEditText>(R.id.userdata_text_input_layout).text.toString().toInt();
            saveData()
            Toast.makeText(this, "Data saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Incorrect userdata size", Toast.LENGTH_SHORT).show()
        }
    }

    fun onFooter(view: View) {
        PopupUtil.showPopup(this, "Easter egg", "you found me, gg!")
    }

    fun onMysticButton(view: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/mysticgsi"))
        startActivity(intent)
    }

    fun onUpdateButton(view: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateLink))
        startActivity(intent)
    }

    fun onDebugButton(view: View) {}
}
