package com.example.metricsvrfg

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

        startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                uri
            )
        )
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_start)?.setOnClickListener {
            startService(Intent(this, MyService::class.java))
            updateTextStatus()
        }
        findViewById<View>(R.id.btn_stop)?.setOnClickListener {
            val intentStop = Intent(this, MyService::class.java)
            intentStop.action = ACTION_STOP_FOREGROUND
            startService(intentStop)
            Handler().postDelayed({
                updateTextStatus()
            },100)

        }
        updateTextStatus()

    }

    private fun updateTextStatus() {
        if(isMyServiceRunning(MyService::class.java)){
            findViewById<TextView>(R.id.txt_service_status)?.text = "Metrics recording running"
        }else{
            findViewById<TextView>(R.id.txt_service_status)?.text = "Metrics recording stopped"
        }
    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            val manager =
                getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(
                Int.MAX_VALUE
            )) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    companion object{
         const val  ACTION_STOP_FOREGROUND = "${BuildConfig.APPLICATION_ID}.stopforeground"
    }
}
