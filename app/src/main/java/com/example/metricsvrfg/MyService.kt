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
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    val initialRxBytes = TrafficStats.getTotalRxBytes()
    val initialTxBytes = TrafficStats.getTotalTxBytes()

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
                //RTD in ms
                var rtd = getRoundTripDelay()
                //Bandwidth available in kbps
                //var bandAval = getAvailableBandwidth(this@MyService)
                //Bandwidth utilization in %
                var bandUtil = getBandwidthUtilization()

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()


                val downloadSpeed = ((currentRxBytes - previousRxBytes) * 8) / 1000.0 // kbps
                val uploadSpeed = ((currentTxBytes - previousTxBytes) * 8) /  1000.0 // kbps

                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes


                //printable metrics
                var myCurrent = currentNow.toString()
                var myVoltage = voltage.toString()
                var myWattage= wattage.toString()
                var myDownloadSpeed = downloadSpeed.toString()
                var myUploadSpeed = uploadSpeed.toString()
                var myTotalRxBytes = (currentRxBytes-initialRxBytes).toString()
                var myTotalTxBytes = (currentTxBytes-initialTxBytes).toString()
                var myRtd = rtd.toString()
               // var myBandAval = bandAval.toString()
                var myBandUtil = bandUtil.toString()


                Log.d("METRICS1", myDownloadSpeed+" mbpsDown")
                Log.d("METRICS2", myUploadSpeed+" mbpsUp")
                Log.d("METRICS3", myTotalRxBytes+" bytesDown")
                Log.d("METRICS4", myTotalTxBytes+" bytesUp")
                Log.d("METRICS5", myRtd+" RTD ms")
                Log.d("METRICS6", myBandUtil+" % util")


                val now = Calendar.getInstance()
                val time = now[Calendar.HOUR_OF_DAY].toString()+ ":"+ now[Calendar.MINUTE].toString()+ ":"+now[Calendar.SECOND]

                var metricsString = time+","+myCurrent+","+myVoltage+","+myWattage+","+myDownloadSpeed+","+myUploadSpeed+","+myTotalRxBytes+","+myTotalTxBytes+","+myRtd+","+myBandUtil+"\r\n"

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

        var metricsNames = "Time,BatteryCurrent_Now(mA),Battery_Voltage(mV),Wattage(mW),DownloadSpeed(kbps),UploadSpeed(kbps),Total_downloaded_bytes,Total_uploaded_bytes,Rtd(ms),Bandwidth_Utilization(%)\r\n"
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

    fun getRoundTripDelay(): Long {
        val hostname = "http://192.168.50.62:5000/" // Replace with the remote server you want to ping
        val timeout = 5000 // Timeout in milliseconds

        try {
            val inetAddress = InetAddress.getByName(hostname)
            val startTime = System.currentTimeMillis()
            if (inetAddress.isReachable(timeout)) {
                val endTime = System.currentTimeMillis()
                return endTime - startTime
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return -1 // Return -1 if there was an error or the host is unreachable
    }

    /**
    @RequiresApi(Build.VERSION_CODES.M)
    fun getAvailableBandwidth(context: Context): Long {
        val url = "http://192.168.50.231:5000/" // Replace with the URL of a file to download
        val bufferSize = 4096
        val maxDuration = 5000 // Maximum duration for the download in milliseconds

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            val startTime = System.currentTimeMillis()

            val connection = URL(url).openConnection()
            connection.connectTimeout = maxDuration
            connection.readTimeout = maxDuration

            val inputStream: InputStream = BufferedInputStream(connection.getInputStream())
            val buffer = ByteArray(bufferSize)
            var bytesRead = 0
            var totalBytesRead = 0L

            while (System.currentTimeMillis() - startTime < maxDuration && inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
            }

            inputStream.close()

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            return totalBytesRead * 8 / duration * 1000 / 1000 // Convert to bits per second
        }

        return -1 // Return -1 if the network is not available or doesn't have internet capability
    }
    */

    fun getBandwidthUtilization(): Double {
        val totalRxBytes = TrafficStats.getTotalRxBytes()
        val totalTxBytes = TrafficStats.getTotalTxBytes()
        val totalBytes = totalRxBytes + totalTxBytes

        val totalRxBytesPerSecond = TrafficStats.getUidRxBytes(android.os.Process.myUid()) / 1000 // Convert to kilobytes
        val totalTxBytesPerSecond = TrafficStats.getUidTxBytes(android.os.Process.myUid()) / 1000 // Convert to kilobytes
        val totalBytesPerSecond = totalRxBytesPerSecond + totalTxBytesPerSecond

        return if (totalBytes > 0) totalBytesPerSecond.toDouble() / totalBytes * 100 else 0.0
    }













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