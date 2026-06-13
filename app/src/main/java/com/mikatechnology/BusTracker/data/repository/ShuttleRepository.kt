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

    suspend fun passengerIsMember(code: String, profile: com.mikatechnology.BusTracker.data.model.UserProfile): Boolean {
        if (profile.role != com.mikatechnology.BusTracker.data.model.MemberRole.Passenger) return false
        val groupID = groupIdForServiceCode(code) ?: return false
        val memberGroups = profile.groupIDs.ifEmpty {
            profile.groupID.takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
        }
        return memberGroups.contains(groupID)
    }

    private suspend fun groupIdForServiceCode(code: String): String? {
        val trimmed = code.trim().uppercase()
        if (trimmed.length < 4) return null
        return try {
            db.collection("groups")
                .whereEqualTo("code", trimmed)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id
        } catch (_: Exception) {
            null
        }
    }

    suspend fun joinAdditionalGroup(code: String, currentProfile: com.mikatechnology.BusTracker.data.model.UserProfile): com.mikatechnology.BusTracker.data.model.UserProfile {
        val user = auth.currentUser ?: throw ShuttleError.NotAuthenticated()
        if (currentProfile.role != com.mikatechnology.BusTracker.data.model.MemberRole.Passenger) {
            throw ShuttleError.InvalidInput("Yalnızca yolcu hesabı servis ekleyebilir.")
        }

        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length < 4) {
            throw ShuttleError.InvalidInput("Servis kodu en az 4 karakter olmalı.")
        }

        val memberGroups = currentProfile.groupIDs.ifEmpty {
            currentProfile.groupID.takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
        }

        _isLoading.value = true
        try {
            val snapshot = db.collection("groups")
                .whereEqualTo("code", trimmedCode)
                .limit(1)
                .get()
                .await()

            val groupDoc = snapshot.documents.firstOrNull() ?: throw ShuttleError.GroupNotFound()
            val newGroupID = groupDoc.id
            if (memberGroups.contains(newGroupID)) {
                throw ShuttleError.InvalidInput("Bu servise zaten kayıtlısınız.")
            }

            val groupData = groupDoc.data ?: emptyMap()
            val groupName = groupData["name"] as? String ?: "Servis"
            val groupCode = groupData["code"] as? String ?: trimmedCode

            groupDoc.reference.collection("members").document(user.uid).set(
                mapOf(
                    "userID" to user.uid,
                    "name" to currentProfile.name.trim(),
                    "googleUserID" to currentProfile.authUserId,
                    "role" to com.mikatechnology.BusTracker.data.model.MemberRole.Passenger.rawValue,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val mergedGroupIDs = memberGroups + newGroupID
            val profile = currentProfile.copy(
                groupIDs = mergedGroupIDs,
                activeGroupIDs = listOf(newGroupID),
                groupID = newGroupID,
                groupCode = groupCode,
                groupName = groupName
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

    suspend fun resolveGroupIDForCode(code: String): String? {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.length < 4) return null
        val snapshot = db.collection("groups")
            .whereEqualTo("code", trimmedCode)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id
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
                groupIDs = listOf(groupDoc.id),
                activeGroupIDs = listOf(groupDoc.id),
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
        val payload = mutableMapOf<String, Any>(
            "memberID" to profile.memberID,
            "name" to profile.name,
            "googleUserID" to profile.authUserId,
            "role" to profile.role.rawValue,
            "groupID" to profile.groupID,
            "groupCode" to profile.groupCode,
            "groupName" to profile.groupName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (profile.groupIDs.isNotEmpty()) {
            payload["groupIDs"] = profile.groupIDs
        }
        if (profile.activeGroupIDs.isNotEmpty()) {
            payload["activeGroupIDs"] = profile.activeGroupIDs
        }
        db.collection("users").document(profile.userID).set(payload).await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun userProfileFrom(data: Map<String, Any>, userID: String): UserProfile? {
        val memberID = data["memberID"] as? String ?: return null
        val name = data["name"] as? String ?: return null
        val roleRaw = data["role"] as? String ?: return null
        val role = MemberRole.entries.firstOrNull { it.rawValue == roleRaw } ?: return null

        val authUserId = (data["googleUserID"] as? String)
            ?: (data["appleUserID"] as? String)
            ?: (data["phoneNumber"] as? String)?.let { "legacy:$it" }
            ?: return null

        val groupIDs: List<String>
        val activeGroupIDs: List<String>
        val legacyGroupID: String
        val legacyGroupCode: String
        val legacyGroupName: String

        val ids = data["groupIDs"] as? List<*>
        if (ids != null) {
            groupIDs = ids.mapNotNull { it as? String }
            activeGroupIDs = (data["activeGroupIDs"] as? List<*>)?.mapNotNull { it as? String } ?: groupIDs
            legacyGroupID = data["groupID"] as? String ?: ""
            legacyGroupCode = data["groupCode"] as? String ?: ""
            legacyGroupName = data["groupName"] as? String ?: ""
        } else {
            val groupID = data["groupID"] as? String ?: return null
            groupIDs = listOf(groupID)
            activeGroupIDs = listOf(groupID)
            legacyGroupID = groupID
            legacyGroupCode = data["groupCode"] as? String ?: ""
            legacyGroupName = data["groupName"] as? String ?: ""
        }

        return UserProfile(
            userID = userID,
            memberID = memberID,
            name = name,
            authUserId = authUserId,
            role = role,
            groupIDs = groupIDs,
            activeGroupIDs = activeGroupIDs,
            groupID = legacyGroupID,
            groupCode = legacyGroupCode,
            groupName = legacyGroupName
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
