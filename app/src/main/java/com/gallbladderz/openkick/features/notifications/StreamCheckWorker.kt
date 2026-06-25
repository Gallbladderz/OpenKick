package com.gallbladderz.openkick.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gallbladderz.openkick.MainActivity
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.data.local.FollowType
import com.gallbladderz.openkick.data.local.FollowsDao
import com.gallbladderz.openkick.features.following.FollowingRepository
import kotlinx.coroutines.flow.first

class StreamCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val followsDao: FollowsDao,
    private val followingRepository: FollowingRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val followedStreamers = followsDao.getAllFollows().first().filter { it.type == FollowType.STREAMER }

        for (entity in followedStreamers) {
            val details = followingRepository.fetchChannelDetails(entity.slug) ?: continue


            if (details.isLive && !entity.isLive) {
                sendNotification(details.username, details.streamTitle, details.slug)
            }


            if (details.isLive != entity.isLive) {
                followsDao.updateLiveStatus(entity.slug, FollowType.STREAMER, details.isLive)
            }
        }
        return Result.success()
    }

    private fun sendNotification(username: String, title: String, slug: String) {

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "openkick_streams"
        val notificationManager = NotificationManagerCompat.from(context)


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Live streams", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }


        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔴 $username is live!")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(slug.hashCode(), notification)
    }
}