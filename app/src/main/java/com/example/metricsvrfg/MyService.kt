package com.example.metricsvrfg

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.metricsvrfg.MainActivity.Companion.ACTION_STOP_FOREGROUND
import android.net.TrafficStats

import java.io.*
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*



class MyService :Service() {
    val DATA_LOCK = arrayOfNulls<Any>(0)
    lateinit var context: Context
    lateinit var bm : BatteryManager
    val delay = 1000L
    val handler: Handler = Handler()
    lateinit var runnable: Runnable
    lateinit var file: File

    fun getVoltage(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        runnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {
                //current in mA
                var currentNow: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                currentNow = (currentNow * 0.001).toInt()*-1
                //voltage in mV
                var voltage = getVoltage(context)
                //wattage in mW
                var currentInAmperes = (currentNow * 0.001)
                var wattage = voltage * currentInAmperes


                //printable metrics
                var myCurrent = currentNow.toString()
                var myVoltage = voltage.toString()
                var myWattage= wattage.toString()




                val now = Calendar.getInstance()
                val time = now[Calendar.HOUR_OF_DAY].toString()+ ":"+ now[Calendar.MINUTE].toString()+ ":"+now[Calendar.SECOND]

                var metricsString = time+","+myCurrent+","+myVoltage+","+myWattage+"\r\n"

                try {
                    synchronized(DATA_LOCK) {
                        if (file != null && file.canWrite()) {
                            val out: Writer = BufferedWriter(FileWriter(file, true), 1024)
                            out.write(metricsString)
                            out.close()
                        }
                    }
                } catch (e: IOException) {
                    Log.d("ERRORRRRRRRRR", "Impossible writing file")
                }

                //end
                handler!!.postDelayed(this, delay)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != null && intent.action.equals(
                ACTION_STOP_FOREGROUND, ignoreCase = true)) {
            Log.d("AAAAAAAAAAAAAAAA", "terminatoooooooooooooooooooooo")
            stopForeground(true)
            stopSelf()
            handler!!.removeCallbacks(runnable)
            return START_STICKY
        }
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateAndTime: String = sdf.format(Date())
        val external = Environment.getExternalStorageDirectory()
        val sdcardPath = external.path
        file = File(sdcardPath+"/Metrics/"+currentDateAndTime+".txt")
        file.createNewFile()

        var metricsNames = "Time,BatteryCurrent_Now(mA),Battery_Voltage(mV),Wattage(mW)\r\n"
        try {
            synchronized(DATA_LOCK) {
                if (file != null && file.canWrite()) {
                    val out: Writer = BufferedWriter(FileWriter(file, true), 1024)
                    out.write(metricsNames)
                    out.close()
                }
            }
        } catch (e: IOException) {
            Log.d("ERRORRRRRRRRR", "Impossible writing file")
        }


        generateForegroundNotification()


        handler!!.postDelayed(runnable, delay)

        return START_STICKY

    }

    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123



    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, FLAG_IMMUTABLE)
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel("service_channel", "Service Notifications",
                        NotificationManager.IMPORTANCE_MIN)
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(this, "service_channel")

            builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" service is running").toString())
                .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
                .setContentText("Touch to open") //                    , swipe down for more options.
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
            if (iconNotification != null) {
                builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
            }
            builder.color = resources.getColor(R.color.purple_200)
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }

    }

}