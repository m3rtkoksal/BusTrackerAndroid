package com.mikatechnology.BusTracker.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BusTrackerMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        NotificationService.createNotificationChannel(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token, oturum açıldığında AppRoot üzerinden profile yazılır.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Bildirim payload'ı sistem tarafından gösterilir.
    }
}
