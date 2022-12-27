package me.zeroest.push_basic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService: FirebaseMessagingService() {
    companion object {
        private const val CHANNEL_NAME = "push_basic"
        private const val CHANNEL_DESCRIPTION = "Push basic test"
        private const val CHANNEL_ID = "CH_ID"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Apps that use Firebase Cloud Messaging should implement onNewToken()
        // in order to observe token changes
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val messageBody: String = message.notification?.body ?: "empty"
        val messageTitle: String = message.notification?.title ?: "empty"

        Log.i("body", messageBody)
        Log.i("title", messageTitle)

        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.google.android.material.R.drawable.ic_clock_black_24dp)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)

        NotificationManagerCompat.from(this)
            .notify(1, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = CHANNEL_DESCRIPTION

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}