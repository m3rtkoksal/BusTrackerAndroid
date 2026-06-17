package com.mikatechnology.BusTracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionInfo
import com.mikatechnology.BusTracker.data.model.PoolContributionHistoryItem
import com.mikatechnology.BusTracker.data.model.PoolContributionResult
import com.mikatechnology.BusTracker.data.model.ShuttlePoolMode
import com.mikatechnology.BusTracker.data.model.ShuttlePoolState
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.tasks.await

object DriverSubscriptionService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchPoolState(
        groupID: String,
        preferServer: Boolean = false
    ): ShuttlePoolState {
        if (groupID.isBlank()) return ShuttlePoolState.Empty

        return try {
            val source = if (preferServer) Source.SERVER else Source.DEFAULT
            val snapshot = db.collection("groups").document(groupID).get(source).await()
            if (!snapshot.exists()) return ShuttlePoolState.Empty

            val startDate = snapshot.getTimestamp("subscriptionStartDate")?.toDate()
            val endDate = snapshot.getTimestamp("subscriptionEndDate")?.toDate()
            val poolMode = ShuttlePoolMode.fromRaw(snapshot.getString("poolMode"))
            val poolTarget = snapshot.getLong("poolTarget")?.toInt() ?: poolMode.targetAmount
            val poolCollected = snapshot.getLong("poolCollected")?.toInt() ?: 0

            ShuttlePoolState(
                subscription = DriverSubscriptionInfo(startDate = startDate, endDate = endDate),
                poolMode = poolMode,
                poolTarget = poolTarget,
                poolCollected = poolCollected
            )
        } catch (_: Exception) {
            ShuttlePoolState.Empty
        }
    }

    suspend fun fetchSubscription(groupID: String): DriverSubscriptionInfo =
        fetchPoolState(groupID).subscription

    suspend fun updatePoolMode(groupID: String, mode: ShuttlePoolMode) {
        if (groupID.isBlank()) return
        db.collection("groups").document(groupID)
            .set(
                mapOf(
                    "poolMode" to mode.rawValue,
                    "poolTarget" to mode.targetAmount
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    suspend fun purchaseMembership(groupID: String, mode: ShuttlePoolMode): PoolContributionResult {
        val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west1")
        val callable = functions.getHttpsCallable("purchasePoolMembership")
        try {
            val result = callable
                .call(
                    mapOf(
                        "groupId" to groupID,
                        "poolMode" to mode.rawValue
                    )
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?> ?: throw PoolMembershipException.BackendFailed(
                L10n.poolPurchaseBackendFailed
            )
            if (payload["success"] as? Boolean != true) {
                throw PoolMembershipException.BackendFailed(L10n.poolPurchaseBackendFailed)
            }

            val poolCollected = (payload["poolCollected"] as? Number)?.toInt()
                ?: throw PoolMembershipException.BackendFailed(L10n.poolPurchaseBackendFailed)
            val poolTarget = (payload["poolTarget"] as? Number)?.toInt()
                ?: throw PoolMembershipException.BackendFailed(L10n.poolPurchaseBackendFailed)
            val activated = payload["activated"] as? Boolean ?: false

            return PoolContributionResult(
                poolCollected = poolCollected,
                poolTarget = poolTarget,
                activated = activated
            )
        } catch (error: PoolMembershipException) {
            throw error
        } catch (error: com.google.firebase.functions.FirebaseFunctionsException) {
            throw PoolMembershipException.BackendFailed(poolMembershipErrorMessage(error))
        } catch (error: Exception) {
            throw PoolMembershipException.BackendFailed(poolMembershipErrorMessage(error))
        }
    }

    suspend fun fetchContributionHistory(groupID: String): List<PoolContributionHistoryItem> {
        if (groupID.isBlank()) return emptyList()

        val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west1")
        val callable = functions.getHttpsCallable("listPoolContributions")
        return try {
            val result = callable.call(mapOf("groupId" to groupID)).await()
            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?> ?: return emptyList()
            if (payload["success"] as? Boolean != true) return emptyList()

            @Suppress("UNCHECKED_CAST")
            val rows = payload["contributions"] as? List<Map<String, Any?>> ?: return emptyList()
            rows.mapNotNull { row ->
                val id = row["id"] as? String ?: return@mapNotNull null
                val memberName = (row["memberName"] as? String)?.trim().orEmpty().ifBlank { "—" }
                val amount = (row["amount"] as? Number)?.toInt() ?: 0
                PoolContributionHistoryItem(
                    id = id,
                    memberName = memberName,
                    amount = amount
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun poolMembershipErrorMessage(error: Exception): String {
        if (error is com.google.firebase.functions.FirebaseFunctionsException) {
            val details = error.message.orEmpty()
            if (details.contains("insufficient_pool_balance")) return L10n.poolInsufficientBalance
            if (error.code == com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND) {
                return L10n.poolFunctionNotDeployed
            }
        }
        val message = error.message.orEmpty()
        if (message.contains("insufficient_pool_balance")) return L10n.poolInsufficientBalance
        if (message.uppercase() == "NOT FOUND") return L10n.poolFunctionNotDeployed
        return message.ifBlank { L10n.poolPurchaseBackendFailed }
    }
}

sealed class PoolMembershipException(message: String) : Exception(message) {
    class BackendFailed(message: String) : PoolMembershipException(message)
}
