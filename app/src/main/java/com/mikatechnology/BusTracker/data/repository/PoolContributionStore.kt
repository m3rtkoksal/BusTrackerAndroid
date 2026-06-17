package com.mikatechnology.BusTracker.data.repository

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.mikatechnology.BusTracker.data.model.PoolContributionResult
import com.mikatechnology.BusTracker.data.model.ShuttlePoolBilling
import com.mikatechnology.BusTracker.data.model.ShuttlePoolDisplay
import com.mikatechnology.BusTracker.data.model.ShuttlePoolProduct
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class PoolContributionException(message: String? = null) : Exception(message) {
    class ProductUnavailable : PoolContributionException(L10n.poolPurchaseProductUnavailable)
    class PurchasePending : PoolContributionException(L10n.poolPurchasePending)
    class PurchaseCancelled : PoolContributionException()
    class VerificationFailed : PoolContributionException(L10n.poolPurchaseVerificationFailed)
    class BackendFailed(message: String) : PoolContributionException(message)
}

class PoolContributionStore(private val activity: Activity) {
    var selectedTier by mutableStateOf<ShuttlePoolProduct?>(null)
        private set
    var isPurchasing by mutableStateOf(false)
        private set
    var isLoadingProducts by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    private var poolProductDetails: ProductDetails? = null
    private val offerDetailsByTier = mutableMapOf<ShuttlePoolProduct, ProductDetails.OneTimePurchaseOfferDetails>()
    private var tierPrices by mutableStateOf<Map<ShuttlePoolProduct, String>>(emptyMap())

    private var purchaseContinuation: CancellableContinuation<Purchase>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val continuation = purchaseContinuation ?: return@PurchasesUpdatedListener
        purchaseContinuation = null

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (purchase != null) {
                    continuation.resume(purchase)
                } else {
                    continuation.resumeWithException(PoolContributionException.VerificationFailed())
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                continuation.resumeWithException(PoolContributionException.PurchaseCancelled())
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                purchases?.firstOrNull()?.let { owned ->
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(owned.purchaseToken)
                        .build()
                    billingClient.consumeAsync(consumeParams) { _, _ -> }
                }
                continuation.resumeWithException(
                    PoolContributionException.BackendFailed(L10n.poolPurchaseProductUnavailable)
                )
            }
            else ->
                continuation.resumeWithException(
                    PoolContributionException.BackendFailed(billingErrorMessage(billingResult))
                )
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(activity.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    val selectedTierState: ShuttlePoolProduct?
        get() = selectedTier

    val isPurchasingState: Boolean
        get() = isPurchasing

    val lastErrorState: String?
        get() = lastError

    fun selectTier(tier: ShuttlePoolProduct) {
        selectedTier = tier
        clearError()
    }

    fun reportError(message: String) {
        lastError = message
    }

    fun clearError() {
        lastError = null
    }

    fun displayPrice(tier: ShuttlePoolProduct): String =
        ShuttlePoolDisplay.formatCurrency(tier.amount)

    fun canPurchaseTier(tier: ShuttlePoolProduct): Boolean =
        offerDetailsByTier.containsKey(tier)

    suspend fun loadProducts(forceRefresh: Boolean = false) {
        if (isPurchasing) return
        isLoadingProducts = true
        try {
            if (forceRefresh && billingClient.isReady) {
                billingClient.endConnection()
            }
            connectBillingClient()
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(ShuttlePoolBilling.PLAY_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            val productDetails = suspendCancellableCoroutine { continuation ->
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(productDetailsResult.productDetailsList.firstOrNull())
                    } else {
                        continuation.resume(null)
                    }
                }
            }

            poolProductDetails = productDetails
            offerDetailsByTier.clear()
            productDetails?.oneTimePurchaseOfferDetailsList.orEmpty().forEach { offer ->
                val tier = resolveOfferTier(offer) ?: return@forEach
                val existing = offerDetailsByTier[tier]
                if (existing == null || shouldPreferOffer(offer, existing)) {
                    offerDetailsByTier[tier] = offer
                }
            }

            tierPrices = ShuttlePoolProduct.entries.associateWith { tier ->
                offerDetailsByTier[tier]?.formattedPrice ?: ShuttlePoolDisplay.formatCurrency(tier.amount)
            }

            lastError = if (offerDetailsByTier.size < ShuttlePoolProduct.entries.size) {
                L10n.poolPurchaseProductUnavailable
            } else {
                null
            }
        } finally {
            isLoadingProducts = false
        }
    }

    suspend fun purchaseSelectedTier(groupID: String): PoolContributionResult {
        val tier = selectedTier ?: throw PoolContributionException.ProductUnavailable()
        return purchase(tier, groupID)
    }

    suspend fun purchase(tier: ShuttlePoolProduct, groupID: String): PoolContributionResult {
        if (groupID.isBlank()) {
            throw PoolContributionException.BackendFailed(L10n.poolMissingGroup)
        }

        val productDetails = poolProductDetails
            ?: throw PoolContributionException.ProductUnavailable()
        val offerDetails = offerDetailsByTier[tier]
            ?: throw PoolContributionException.ProductUnavailable()

        isPurchasing = true
        lastError = null
        try {
            connectBillingClient()
            consumeUnfinishedPoolPurchases()
            val offerToken = offerDetails.offerToken
                ?: throw PoolContributionException.ProductUnavailable()
            val purchase = launchBillingFlow(productDetails, offerToken)
            val result = ShuttlePoolService.recordContribution(
                groupID = groupID,
                productID = tier.backendProductId,
                transactionID = purchase.purchaseToken,
                contributionAmount = tier.amount
            )
            acknowledgeAndConsume(purchase)
            selectedTier = null
            return result
        } finally {
            isPurchasing = false
        }
    }

    fun destroy() {
        if (!isPurchasing) {
            billingClient.endConnection()
        }
    }

    private fun resolveOfferTier(offer: ProductDetails.OneTimePurchaseOfferDetails): ShuttlePoolProduct? {
        val optionId = offer.purchaseOptionId ?: return null
        ShuttlePoolProduct.entries.firstOrNull { it.purchaseOptionId == optionId }?.let { return it }
        if (!optionId.startsWith("tier-")) return null
        val amount = optionId.removePrefix("tier-").toIntOrNull() ?: return null
        return ShuttlePoolProduct.matching(amount)
    }

    private fun shouldPreferOffer(
        candidate: ProductDetails.OneTimePurchaseOfferDetails,
        current: ProductDetails.OneTimePurchaseOfferDetails
    ): Boolean {
        val candidateIsBase = candidate.offerId.isNullOrBlank()
        val currentIsBase = current.offerId.isNullOrBlank()
        if (candidateIsBase != currentIsBase) return candidateIsBase
        return false
    }

    private suspend fun connectBillingClient() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(
                            PoolContributionException.BackendFailed(billingErrorMessage(billingResult))
                        )
                    }
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    private suspend fun launchBillingFlow(
        productDetails: ProductDetails,
        offerToken: String
    ): Purchase = suspendCancellableCoroutine { continuation ->
        purchaseContinuation = continuation
        continuation.invokeOnCancellation { purchaseContinuation = null }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseContinuation = null
            continuation.resumeWithException(
                PoolContributionException.BackendFailed(billingErrorMessage(launchResult))
            )
        }
    }

    private suspend fun consumeUnfinishedPoolPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val purchases = suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchaseList)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
        purchases
            .filter { purchase ->
                purchase.products.any { productId ->
                    productId == ShuttlePoolBilling.PLAY_PRODUCT_ID ||
                        productId.contains("bustracker.pool")
                }
            }
            .forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeAndConsume(purchase)
                }
            }
    }

    private suspend fun acknowledgeAndConsume(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            suspendCancellableCoroutine { continuation ->
                billingClient.acknowledgePurchase(acknowledgeParams) { _ ->
                    continuation.resume(Unit)
                }
            }
        }
        consumePurchase(purchase)
    }

    private suspend fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCancellableCoroutine { continuation ->
            billingClient.consumeAsync(consumeParams) { _, _ ->
                continuation.resume(Unit)
            }
        }
    }

    private fun billingErrorMessage(result: BillingResult): String {
        val debug = result.debugMessage?.trim().orEmpty()
        val base = when (result.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                L10n.poolPurchaseBillingUnavailable
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR ->
                L10n.poolPurchasePlaySheetFailed
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED ->
                L10n.poolPurchaseProductUnavailable
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
                L10n.poolPurchaseBillingUnavailable
            else -> L10n.poolPurchasePlaySheetFailed
        }
        return if (debug.isNotBlank()) "$base ($debug)" else base
    }
}

private object ShuttlePoolService {
    private val functions = FirebaseFunctions.getInstance("europe-west1")

    suspend fun recordContribution(
        groupID: String,
        productID: String,
        transactionID: String,
        contributionAmount: Int
    ): PoolContributionResult {
        val callable = functions.getHttpsCallable("recordPoolIAP")
        try {
            val result = callable
                .call(
                    mapOf(
                        "groupId" to groupID,
                        "productId" to productID,
                        "transactionId" to transactionID,
                        "contributionAmount" to contributionAmount
                    )
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?> ?: throw backendFailed()
            if (payload["success"] as? Boolean != true) throw backendFailed()

            val poolCollected = intFrom(payload["poolCollected"]) ?: throw backendFailed()
            val poolTarget = intFrom(payload["poolTarget"]) ?: throw backendFailed()
            val activated = payload["activated"] as? Boolean ?: false

            return PoolContributionResult(
                poolCollected = poolCollected,
                poolTarget = poolTarget,
                activated = activated
            )
        } catch (error: PoolContributionException) {
            throw error
        } catch (error: FirebaseFunctionsException) {
            throw PoolContributionException.BackendFailed(userFacingMessage(error))
        } catch (error: Exception) {
            throw PoolContributionException.BackendFailed(userFacingMessage(error))
        }
    }

    private fun backendFailed(): PoolContributionException.BackendFailed =
        PoolContributionException.BackendFailed(L10n.poolPurchaseBackendFailed)

    private fun intFrom(value: Any?): Int? = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Number -> value.toInt()
        else -> null
    }

    private fun userFacingMessage(error: Exception): String {
        if (error is FirebaseFunctionsException) {
            val code = error.code.name.lowercase()
            val details = error.message.orEmpty()
            if (details.contains("insufficient_pool_balance")) return L10n.poolInsufficientBalance
            return when (code) {
                "not-found" -> L10n.poolFunctionNotDeployed
                "failed-precondition" -> if (details.contains("insufficient")) {
                    L10n.poolInsufficientBalance
                } else {
                    details.ifBlank { L10n.poolPurchaseBackendFailed }
                }
                else -> when (details.lowercase()) {
                    "group_not_found" -> L10n.poolMissingGroup
                    "not_group_member" -> L10n.poolNotGroupMember
                    "invalid_pool_payment_payload" -> L10n.poolPurchaseBackendFailed
                    "auth_required" -> L10n.signInRequired
                    else -> error.message?.takeIf { it.isNotBlank() } ?: L10n.poolPurchaseBackendFailed
                }
            }
        }

        val message = error.message.orEmpty()
        if (message.uppercase() == "NOT FOUND") return L10n.poolFunctionNotDeployed
        return message.ifBlank { L10n.poolPurchaseBackendFailed }
    }
}
