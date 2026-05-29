package com.mikatechnology.BusTracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class ShuttleError(message: String) : Exception(message) {
    class NotAuthenticated : ShuttleError("Giriş yapmanız gerekiyor.")
    class GroupNotFound : ShuttleError("Bu servis kodu bulunamadı.")
    class AlreadyInGroup : ShuttleError("Zaten bir servise kayıtlısınız.")
    class InvalidInput(message: String) : ShuttleError(message)
}

class ShuttleRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun fetchUserProfile(userID: String): UserProfile? {
        val doc = db.collection("users").document(userID).get().await()
        val data = doc.data ?: return null
        return userProfileFrom(data, userID)
    }

    suspend fun createGroup(name: String, driverName: String): UserProfile {
        val user = auth.currentUser ?: throw ShuttleError.NotAuthenticated()
        val authUserId = AuthRepository.resolveAuthUserId()
            ?: throw ShuttleError.InvalidInput("Google hesap kimliği bulunamadı.")

        val trimmedName = name.trim()
        val trimmedDriver = driverName.trim()
        if (trimmedName.isEmpty()) throw ShuttleError.InvalidInput("Servis adı boş olamaz.")
        if (trimmedDriver.isEmpty()) throw ShuttleError.InvalidInput("Adınız boş olamaz.")

        if (fetchUserProfile(user.uid) != null) {
            throw ShuttleError.AlreadyInGroup()
        }

        _isLoading.value = true
        try {
            val groupID = UUID.randomUUID().toString()
            val memberID = user.uid
            val code = generateGroupCode()

            val groupRef = db.collection("groups").document(groupID)
            groupRef.set(
                mapOf(
                    "name" to trimmedName,
                    "code" to code,
                    "driverMemberID" to memberID,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            groupRef.collection("members").document(memberID).set(
                mapOf(
                    "userID" to user.uid,
                    "name" to trimmedDriver,
                    "googleUserID" to authUserId,
                    "role" to MemberRole.Driver.rawValue,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val profile = UserProfile(
                userID = user.uid,
                memberID = memberID,
                name = trimmedDriver,
                authUserId = authUserId,
                role = MemberRole.Driver,
                groupID = groupID,
                groupCode = code,
                groupName = trimmedName
            )

            saveUserDocument(profile)
            return profile
        } finally {
            _isLoading.value = false
        }
    }

    /** Yolcu kaydı: kod Firestore'da var mı (Google öncesi kontrol). */
    suspend fun validatePassengerGroupCode(code: String) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.isEmpty()) {
            throw ShuttleError.InvalidInput("Servis kodu girmedin.")
        }
        if (trimmedCode.length < 4) {
            throw ShuttleError.InvalidInput("Servis kodu en az 4 karakter olmalı.")
        }
        val snapshot = db.collection("groups")
            .whereEqualTo("code", trimmedCode)
            .limit(1)
            .get()
            .await()
        if (snapshot.documents.isEmpty()) {
            throw ShuttleError.GroupNotFound()
        }
    }

    suspend fun joinGroup(code: String, passengerName: String): UserProfile {
        val user = auth.currentUser ?: throw ShuttleError.NotAuthenticated()
        val authUserId = AuthRepository.resolveAuthUserId()
            ?: throw ShuttleError.InvalidInput("Google hesap kimliği bulunamadı.")

        val trimmedCode = code.trim().uppercase()
        val trimmedName = passengerName.trim()
        if (trimmedCode.length < 4) {
            throw ShuttleError.InvalidInput("Servis kodu en az 4 karakter olmalı.")
        }
        if (trimmedName.isEmpty()) {
            throw ShuttleError.InvalidInput("Adınız boş olamaz.")
        }

        if (fetchUserProfile(user.uid) != null) {
            throw ShuttleError.AlreadyInGroup()
        }

        _isLoading.value = true
        try {
            val snapshot = db.collection("groups")
                .whereEqualTo("code", trimmedCode)
                .limit(1)
                .get()
                .await()

            val groupDoc = snapshot.documents.firstOrNull()
                ?: throw ShuttleError.GroupNotFound()

            val memberID = user.uid
            val groupData = groupDoc.data ?: emptyMap()
            val groupName = groupData["name"] as? String ?: "Servis"
            val groupCode = groupData["code"] as? String ?: trimmedCode

            groupDoc.reference.collection("members").document(memberID).set(
                mapOf(
                    "userID" to user.uid,
                    "name" to trimmedName,
                    "googleUserID" to authUserId,
                    "role" to MemberRole.Passenger.rawValue,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val profile = UserProfile(
                userID = user.uid,
                memberID = memberID,
                name = trimmedName,
                authUserId = authUserId,
                role = MemberRole.Passenger,
                groupID = groupDoc.id,
                groupCode = groupCode,
                groupName = groupName
            )

            saveUserDocument(profile)
            return profile
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun saveUserDocument(profile: UserProfile) {
        db.collection("users").document(profile.userID).set(
            mapOf(
                "memberID" to profile.memberID,
                "name" to profile.name,
                "googleUserID" to profile.authUserId,
                "role" to profile.role.rawValue,
                "groupID" to profile.groupID,
                "groupCode" to profile.groupCode,
                "groupName" to profile.groupName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun userProfileFrom(data: Map<String, Any>, userID: String): UserProfile? {
        val memberID = data["memberID"] as? String ?: return null
        val name = data["name"] as? String ?: return null
        val roleRaw = data["role"] as? String ?: return null
        val groupID = data["groupID"] as? String ?: return null
        val groupCode = data["groupCode"] as? String ?: return null
        val groupName = data["groupName"] as? String ?: return null
        val role = MemberRole.entries.firstOrNull { it.rawValue == roleRaw } ?: return null

        val authUserId = (data["googleUserID"] as? String)
            ?: (data["appleUserID"] as? String)
            ?: (data["phoneNumber"] as? String)?.let { "legacy:$it" }
            ?: return null

        return UserProfile(
            userID = userID,
            memberID = memberID,
            name = name,
            authUserId = authUserId,
            role = role,
            groupID = groupID,
            groupCode = groupCode,
            groupName = groupName
        )
    }

    private fun generateGroupCode(): String {
        val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { characters.random() }.joinToString("")
    }

    companion object {
        val shared = ShuttleRepository()
    }
}
