package com.hcmus.forumus_admin.data.model

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val status: UserStatus,
    val role: String = "Member",
    val createdAt: String = "Nov 18, 2023"
)

enum class UserStatus {
    BANNED,
    WARNED,
    REMINDED,
    NORMAL
}
