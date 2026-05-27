package com.mikatechnology.BusTracker.data.model

data class UserProfile(
    val userID: String,
    val memberID: String,
    val name: String,
    val phoneNumber: String,
    val role: MemberRole,

    // Yeni çoklu grup desteği
    val groupIDs: List<String> = emptyList(),
    val activeGroupIDs: List<String> = emptyList(),

    // Geriye uyumluluk için (geçici olarak korunuyor)
    @Deprecated("Use groupIDs instead")
    val groupID: String = "",
    @Deprecated("Use groupCode from active group")
    val groupCode: String = "",
    @Deprecated("Use groupName from active group")
    val groupName: String = ""
) {
    // Kolay erişim için yardımcı property'ler (geriye uyumluluk)
    val primaryGroupID: String
        get() = activeGroupIDs.firstOrNull() ?: groupIDs.firstOrNull() ?: groupID
}
