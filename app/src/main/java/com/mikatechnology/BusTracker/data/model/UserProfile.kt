package com.mikatechnology.BusTracker.data.model

data class UserProfile(
    val userID: String,
    val memberID: String,
    val name: String,
    val authUserId: String,
    val role: MemberRole,

    val groupIDs: List<String> = emptyList(),
    val activeGroupIDs: List<String> = emptyList(),

    @Deprecated("Use groupIDs instead")
    val groupID: String = "",
    @Deprecated("Use groupCode from active group")
    val groupCode: String = "",
    @Deprecated("Use groupName from active group")
    val groupName: String = ""
) {
    val primaryGroupID: String
        get() = activeGroupIDs.firstOrNull() ?: groupIDs.firstOrNull() ?: groupID
}
