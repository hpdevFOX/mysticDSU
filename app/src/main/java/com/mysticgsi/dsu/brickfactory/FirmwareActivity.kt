package com.mysticgsi.dsu.brickfactory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mysticgsi.dsu.PopupUtil
import com.mysticgsi.dsu.networking.ApiClient
import com.mysticgsi.dsu.networking.FirmwareResponse
import com.mysticgsi.dsu.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.animation.ObjectAnimator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.animation.Animator


class FirmwareActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var firmwareAdapter: FirmwareAdapter
    private lateinit var firmwareDownloadManager: FirmwareDownloadManager
    private var userDataSize : Long = 17179869184L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware)

        val sharedPreferences = getSharedPreferences("MysticDSUPrefs", MODE_PRIVATE)
        userDataSize = sharedPreferences.getInt("userdata", 16) * 1024L * 1024L * 1024L
        val authKey = sharedPreferences.getString("authkey", "")

        val mysticMain = "https://mysticcloudmain.hpdevfox.ru/dsu.json?key=${authKey}"
        recyclerView = findViewById(R.id.firmware_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        firmwareDownloadManager = FirmwareDownloadManager(this)

        firmwareAdapter = FirmwareAdapter(this, listOf(), firmwareDownloadManager, userDataSize)
        recyclerView.adapter = firmwareAdapter

        ApiClient.apiService.getFirmwareList(mysticMain).enqueue(object : Callback<FirmwareResponse> {
            override fun onResponse(
                call: Call<FirmwareResponse>,
                response: Response<FirmwareResponse>
            ) {
                if (response.isSuccessful) {
                    findViewById<CardView?>(R.id.load_card).visibility = View.GONE;

                    val firmwareResponse = response.body()
                    firmwareResponse?.images?.let {
                        firmwareAdapter.updateFirmwareList(it)
                    }
                    try {

                    } catch (_ : Exception) {

                    }
                }
            }

            override fun onFailure(call: Call<FirmwareResponse>, t: Throwable) {
                PopupUtil.showPopup(this@FirmwareActivity,"NETWORK ERROR!", "Unable to fetch data from the DSU server!")
                finish()
            }
        })
    }
}