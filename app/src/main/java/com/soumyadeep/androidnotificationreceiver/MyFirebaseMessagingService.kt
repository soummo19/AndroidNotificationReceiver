package com.soumyadeep.androidnotificationreceiver

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Legacy callback — still works during the migration period
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        NotificationRepository.updateToken(token)
    }

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.d(TAG, "New FCM Installation ID: $installationId")
        NotificationRepository.updateInstallationId(installationId)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        val notificationTitle = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "New Push Notification"

        val notificationBody = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You received a message from backend"

        Log.d(TAG, "Message Notification Title: $notificationTitle")
        Log.d(TAG, "Message Notification Body: $notificationBody")
        Log.d(TAG, "Message Data Payload: ${remoteMessage.data}")

        // Ensure channel exists before showing
        NotificationHelper.createNotificationChannel(this)

        // Broadcast to UI first to guarantee the app registers it
        NotificationRepository.onNotificationReceived(
            ReceivedNotification(
                title = notificationTitle,
                body = notificationBody,
                data = remoteMessage.data
            )
        )

        // Show local notification banner
        NotificationHelper.showNotification(
            context = this,
            title = notificationTitle,
            body = notificationBody
        )
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
