package com.mikatechnology.BusTracker.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object NotificationService {
    private const val CHANNEL_ID = "bustracker_trip"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Servis bildirimleri",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Servis ve sürücü yaklaşma bildirimleri"
        }
        manager?.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun requestPermissionIfNeeded(context: Context): Boolean {
        createNotificationChannel(context)
        return hasNotificationPermission(context)
    }

    suspend fun fetchAndSaveToken(groupID: String, memberID: String) {
        if (groupID.isBlank() || memberID.isBlank()) return
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val token = runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrNull() ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userID)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        db.collection("groups").document(groupID)
            .collection("members").document(memberID)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    suspend fun syncTokenForProfile(context: Context, groupID: String, memberID: String) {
        createNotificationChannel(context)
        fetchAndSaveToken(groupID, memberID)
    }
}
