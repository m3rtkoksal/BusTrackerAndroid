package com.mikatechnology.BusTracker.data.model

data class UserProfile(
    val userID: String,
    val memberID: String,
    val name: String,
    val phoneNumber: String,
    val role: MemberRole,
    val groupID: String,
    val groupCode: String,
    val groupName: String
)
