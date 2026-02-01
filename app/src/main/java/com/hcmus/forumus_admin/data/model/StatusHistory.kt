package com.hcmus.forumus_admin.data.model

enum class UserStatusLevel(val value: String, val displayName: String) {
    NORMAL("NORMAL", "Normal"),
    REMINDED("REMINDED", "Reminded"),
    WARNED("WARNED", "Warned"),
    BANNED("BANNED", "Banned");

    companion object {
        fun fromString(value: String): UserStatusLevel {
            return values().find { it.value.equals(value, ignoreCase = true) } ?: NORMAL
        }

        fun getNextLevel(currentLevel: UserStatusLevel): UserStatusLevel {
            return when (currentLevel) {
                NORMAL -> REMINDED
                REMINDED -> WARNED
                WARNED -> BANNED
                BANNED -> BANNED
            }
        }

        fun getPreviousLevel(currentLevel: UserStatusLevel): UserStatusLevel {
            return when (currentLevel) {
                BANNED -> WARNED
                WARNED -> REMINDED
                REMINDED -> NORMAL
                NORMAL -> NORMAL
            }
        }
    }
}

data class StatusEscalationResult(
    val success: Boolean,
    val userId: String,
    val previousStatus: UserStatusLevel,
    val newStatus: UserStatusLevel,
    val wasEscalated: Boolean,
    val historyId: String? = null,
    val error: String? = null
)
