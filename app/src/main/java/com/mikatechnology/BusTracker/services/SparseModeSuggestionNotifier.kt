package com.mikatechnology.BusTracker.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mikatechnology.BusTracker.MainActivity
import com.mikatechnology.BusTracker.R
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Seyrek kullanım önerisi — yerel bildirim (sheet ayrı gösterilir). */
object SparseModeSuggestionNotifier {

    /** Geriye dönük: eski bildirim intent anahtarı. */
    const val TYPE_OPEN_HOLIDAY_MODE = SparseModeSuggestion.INTENT_TYPE

    private const val NOTIFICATION_ID = 84_001

    suspend fun postNotificationIfNeeded(
        context: Context,
        memberID: String,
        prompt: SparseModeSuggestion.Prompt
    ) = withContext(Dispatchers.Default) {
        val appContext = context.applicationContext
        if (!SparseModeSuggestion.shouldSendNotification(appContext, memberID)) return@withContext
        if (!NotificationService.hasNotificationPermission(appContext)) return@withContext

        postNotification(appContext, prompt.comingDays)
        SparseModeSuggestion.markNotificationSent(appContext, memberID)
    }

    private fun postNotification(context: Context, comingDays: Int) {
        NotificationService.createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", SparseModeSuggestion.INTENT_TYPE)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, NotificationService.CHANNEL_TRIP)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(L10n.sparseModeSuggestionTitle)
            .setContentText(L10n.sparseModeSuggestionBody(comingDays))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(L10n.sparseModeSuggestionBody(comingDays))
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
