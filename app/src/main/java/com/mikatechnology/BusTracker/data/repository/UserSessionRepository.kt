package com.mikatechnology.BusTracker.data.repository

import android.content.Context
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSessionRepository {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "userID"
    private const val KEY_MEMBER_ID = "memberID"
    private const val KEY_NAME = "name"
    private const val KEY_PHONE = "phoneNumber"
    private const val KEY_ROLE = "role"
    private const val KEY_GROUP_ID = "groupID"
    private const val KEY_GROUP_CODE = "groupCode"
    private const val KEY_GROUP_NAME = "groupName"

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userID = prefs.getString(KEY_USER_ID, null)
        val roleRaw = prefs.getString(KEY_ROLE, null)

        if (userID == null || roleRaw == null) {
            _isSessionLoaded.value = true
            _profile.value = null
            return
        }

        val role = MemberRole.entries.firstOrNull { it.rawValue == roleRaw } ?: run {
            _isSessionLoaded.value = true
            _profile.value = null
            return
        }

        _profile.value = UserProfile(
            userID = userID,
            memberID = prefs.getString(KEY_MEMBER_ID, userID) ?: userID,
            name = prefs.getString(KEY_NAME, "") ?: "",
            phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
            role = role,
            groupID = prefs.getString(KEY_GROUP_ID, "") ?: "",
            groupCode = prefs.getString(KEY_GROUP_CODE, "") ?: "",
            groupName = prefs.getString(KEY_GROUP_NAME, "") ?: ""
        )

        _isSessionLoaded.value = true
    }

    fun save(context: Context, profile: UserProfile) {
        _profile.value = profile
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, profile.userID)
            .putString(KEY_MEMBER_ID, profile.memberID)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_PHONE, profile.phoneNumber)
            .putString(KEY_ROLE, profile.role.rawValue)
            .putString(KEY_GROUP_ID, profile.groupID)
            .putString(KEY_GROUP_CODE, profile.groupCode)
            .putString(KEY_GROUP_NAME, profile.groupName)
            .apply()
    }

    fun clear(context: Context) {
        _profile.value = null
        _isSessionLoaded.value = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    suspend fun signOut(context: Context) {
        clear(context)
        AuthRepository.signOut()
    }
}
