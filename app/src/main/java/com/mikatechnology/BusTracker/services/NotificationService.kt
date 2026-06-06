package com.mikatechnology.BusTracker.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.mikatechnology.BusTracker.R
import kotlinx.coroutines.tasks.await

object NotificationService {

    enum class AccessResult {
        Granted,
        Prompted,
        NeedsSettings
    }
    /** Servis başladı ve hatırlatmalar — özel ses (approach_tink). */
    const val CHANNEL_TRIP = "bustracker_trip_v2"

    private const val CHANNEL_TRIP_LEGACY = "bustracker_trip"

    /** Servis çağrısı — korna sesi (sürücü biniş noktana yaklaşınca). */
    const val CHANNEL_APPROACHING = "bustracker_approaching"

    private const val PREFS_NAME = "bustracker_notification_prefs"
    private const val KEY_POST_NOTIFICATIONS_REQUESTED = "post_notifications_requested"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
            ?: return

        manager.deleteNotificationChannel(CHANNEL_TRIP_LEGACY)

        val tripSound = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.approach_tink}"
        )
        val tripChannel = android.app.NotificationChannel(
            CHANNEL_TRIP,
            "Servis bildirimleri",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Servis başladı ve hatırlatmalar"
            setSound(
                tripSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
        }
        manager.createNotificationChannel(tripChannel)

        val approachSound = tripSound
        val approachChannel = android.app.NotificationChannel(
            CHANNEL_APPROACHING,
            "Servis çağrısı",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Van biniş noktana gelince korna ile çağrı — bu ses = senin servisin seni alıyor"
            setSound(
                approachSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 140, 90, 140, 90, 220)
        }
        manager.createNotificationChannel(approachChannel)
    }

    @Deprecated("Use createNotificationChannels", ReplaceWith("createNotificationChannels(context)"))
    fun createNotificationChannel(context: Context) = createNotificationChannels(context)

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun markNotificationPermissionRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_POST_NOTIFICATIONS_REQUESTED, true)
            .apply()
    }

    /** Sistem diyaloğu bir kez gösterildiyse ve izin hâlâ yoksa → ayarlar bottom sheet (iOS sürücü parity). */
    fun shouldShowNotificationSettingsGuide(context: Context): Boolean {
        if (hasNotificationPermission(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return !NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_POST_NOTIFICATIONS_REQUESTED, false)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** İzin varsa token kaydeder; yoksa çağıran Activity launcher ile istemeli. */
    suspend fun ensureNotificationsEnabled(
        context: Context,
        groupID: String?,
        memberID: String?
    ): AccessResult {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            return AccessResult.NeedsSettings
        }
        if (!groupID.isNullOrBlank() && !memberID.isNullOrBlank()) {
            fetchAndSaveToken(groupID, memberID)
        }
        return AccessResult.Granted
    }

    @Deprecated("Launcher ile ensureNotificationsEnabled kullanın")
    suspend fun requestPermissionIfNeeded(context: Context): Boolean {
        createNotificationChannels(context)
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
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return
        fetchAndSaveToken(groupID, memberID)
    }

    /** Yolcu aksiyonu öncesi: izin varsa token senkronize eder; yoksa çağıran launcher ile istemeli. */
    fun shouldPromptPassengerNotification(context: Context): Boolean {
        createNotificationChannels(context)
        return !hasNotificationPermission(context)
    }
}
