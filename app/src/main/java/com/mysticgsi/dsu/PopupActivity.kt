package com.mysticgsi.dsu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

object PopupUtil {
    fun showPopup(context: Context, title: String, desc: String) {
        val intent = Intent(context, PopupActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("description", desc)
        context.startActivity(intent)
    }
}

class PopupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.PopupActivityTheme)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.activity_popup)

        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")

        if (title == null || description == null) {
            finish()
        }

        findViewById<TextView>(R.id.title).text = title
        findViewById<TextView>(R.id.description).text = description

        val closeButton: Button = findViewById(R.id.button)
        closeButton.setOnClickListener {
            finish()
        }
    }

}
