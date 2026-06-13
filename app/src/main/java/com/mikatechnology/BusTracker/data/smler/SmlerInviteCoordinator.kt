package com.mikatechnology.BusTracker.data.smler

import android.content.Context
import android.net.Uri
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SmlerInviteCoordinator {
    private const val PREFS_NAME = "smler"
    private const val PERSISTED_REGISTRATION_CODE_KEY = "smler_pending_registration_code_v1"

    private val _pendingRegistrationCode = MutableStateFlow<String?>(null)
    val pendingRegistrationCode: StateFlow<String?> = _pendingRegistrationCode.asStateFlow()

    private val _pendingAddServiceCode = MutableStateFlow<String?>(null)
    val pendingAddServiceCode: StateFlow<String?> = _pendingAddServiceCode.asStateFlow()

    private val _alreadyMemberMessage = MutableStateFlow<String?>(null)
    val alreadyMemberMessage: StateFlow<String?> = _alreadyMemberMessage.asStateFlow()

    private val _inviteRevision = MutableStateFlow(0)
    val inviteRevision: StateFlow<Int> = _inviteRevision.asStateFlow()

    private var deferredInviteCode: String? = null

    fun ingest(context: Context, serviceCode: String) {
        val normalized = SmlerConfig.normalizedCode(serviceCode)
        if (normalized.length < 4) return
        deferredInviteCode = normalized
        _pendingRegistrationCode.value = normalized
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PERSISTED_REGISTRATION_CODE_KEY, normalized)
            .apply()
        bumpRevision()
    }

    suspend fun processIncomingURL(context: Context, url: Uri) {
        val code = SmlerDeepLinkService.serviceCodeFrom(url) ?: return
        ingest(context, code)
    }

    fun restorePersistedRegistrationInviteIfNeeded(context: Context) {
        if (_pendingRegistrationCode.value != null || deferredInviteCode != null) return
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PERSISTED_REGISTRATION_CODE_KEY, null)
            ?: return
        ingest(context, stored)
    }

    fun hasPassengerRegistrationInvite(): Boolean {
        return (deferredInviteCode?.length ?: 0) >= 4 || (_pendingRegistrationCode.value?.length ?: 0) >= 4
    }

    fun preparePassengerRegistrationFromDeferred() {
        deferredInviteCode?.let { _pendingRegistrationCode.value = it }
    }

    fun consumeRegistrationInvite(context: Context) {
        _pendingRegistrationCode.value = null
        deferredInviteCode = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PERSISTED_REGISTRATION_CODE_KEY)
            .apply()
    }

    fun clearAddServicePending() {
        _pendingAddServiceCode.value = null
    }

    fun dismissAlreadyMemberMessage() {
        _alreadyMemberMessage.value = null
    }

    suspend fun handleIfReady(profile: UserProfile?, isSignedIn: Boolean) {
        val code = deferredInviteCode ?: return

        if (profile?.role == MemberRole.Driver) {
            discardInvite()
            return
        }

        if (!isSignedIn || profile == null) return

        if (isAlreadyMember(code, profile)) {
            _alreadyMemberMessage.value = L10n.alreadyMemberOfShuttle
            discardInvite()
            return
        }

        _pendingRegistrationCode.value = null
        _pendingAddServiceCode.value = code
        deferredInviteCode = null
        bumpRevision()
    }

    private fun discardInvite() {
        deferredInviteCode = null
        _pendingRegistrationCode.value = null
        _pendingAddServiceCode.value = null
        bumpRevision()
    }

    private suspend fun isAlreadyMember(code: String, profile: UserProfile): Boolean {
        val groupID = ShuttleRepository.shared.resolveGroupIDForCode(code) ?: return false
        val ids = profile.groupIDs.ifEmpty {
            profile.groupID.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
        }
        return ids.contains(groupID)
    }

    private fun bumpRevision() {
        _inviteRevision.update { it + 1 }
    }
}
