package com.gallbladderz.openkick.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gallbladderz.openkick.MainActivity
import com.gallbladderz.openkick.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamKeepaliveService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val NOTIFICATION_ID = 1337
    private val CHANNEL_ID = "keepalive_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenKick работает в фоне")
            .setContentText("Мониторим стримы для моментальных пушей")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)


        startMonitoringStreams()



        return START_STICKY
    }

    private fun startMonitoringStreams() {

        serviceScope.coroutineContext.cancelChildren()

        serviceScope.launch {

            while (isActive) {

                android.util.Log.d("StreamKeepalive", "Я живой и чекаю стримы!")
                delay(30000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Фоновая работа",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Поддерживает соединение для получения уведомлений"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}