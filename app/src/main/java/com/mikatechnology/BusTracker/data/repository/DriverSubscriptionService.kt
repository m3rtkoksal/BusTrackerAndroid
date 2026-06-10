package com.mikatechnology.BusTracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionInfo
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

object DriverSubscriptionConfig {
    const val RENEWAL_PAGE_BASE_URL = "https://mika.technology/subscribe"

    fun renewalPageUrl(serviceCode: String): String {
        val code = serviceCode.trim().uppercase()
        if (code.isBlank()) return RENEWAL_PAGE_BASE_URL
        return "$RENEWAL_PAGE_BASE_URL?code=${URLEncoder.encode(code, Charsets.UTF_8.name())}"
    }
}

object DriverSubscriptionShare {
    fun normalizedServiceCode(raw: String): String =
        raw.trim().uppercase()

    fun renewalUrl(serviceCode: String): String =
        DriverSubscriptionConfig.renewalPageUrl(serviceCode)

    fun shareMessage(serviceCode: String): String =
        L10n.subscriptionRenewalShareMessage(renewalUrl(serviceCode))
}

object DriverSubscriptionService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchSubscription(groupID: String): DriverSubscriptionInfo {
        if (groupID.isBlank()) return DriverSubscriptionInfo.Empty

        return try {
            val snapshot = db.collection("groups").document(groupID).get().await()
            if (!snapshot.exists()) return DriverSubscriptionInfo.Empty

            val startDate = snapshot.getTimestamp("subscriptionStartDate")?.toDate()
            val endDate = snapshot.getTimestamp("subscriptionEndDate")?.toDate()
            DriverSubscriptionInfo(startDate = startDate, endDate = endDate)
        } catch (_: Exception) {
            DriverSubscriptionInfo.Empty
        }
    }
}
