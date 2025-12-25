package com.hcmus.forumus_admin.data.model

/**
 * Represents the user status levels in order of escalation.
 * Each status escalates to the next level when a violation occurs.
 */
enum class UserStatusLevel(val value: String, val displayName: String) {
    NORMAL("NORMAL", "Normal"),
    REMINDED("REMINDED", "Reminded"),
    WARNED("WARNED", "Warned"),
    BANNED("BANNED", "Banned");

    companion object {
        fun fromString(value: String): UserStatusLevel {
            return values().find { it.value.equals(value, ignoreCase = true) } ?: NORMAL
        }

        /**
         * Get the next escalation level for a given status.
         * Returns BANNED if already at the highest level.
         */
        fun getNextLevel(currentLevel: UserStatusLevel): UserStatusLevel {
            return when (currentLevel) {
                NORMAL -> REMINDED
                REMINDED -> WARNED
                WARNED -> BANNED
                BANNED -> BANNED // Already at max level
            }
        }

        /**
         * Get the previous level for de-escalation (if needed in the future).
         * Returns NORMAL if already at the lowest level.
         */
        fun getPreviousLevel(currentLevel: UserStatusLevel): UserStatusLevel {
            return when (currentLevel) {
                BANNED -> WARNED
                WARNED -> REMINDED
                REMINDED -> NORMAL
                NORMAL -> NORMAL // Already at min level
            }
        }
    }
}

/**
 * Result class for status escalation operations
 */
data class StatusEscalationResult(
    val success: Boolean,
    val userId: String,
    val previousStatus: UserStatusLevel,
    val newStatus: UserStatusLevel,
    val wasEscalated: Boolean, // false if user was already at max level
    val historyId: String? = null,
    val error: String? = null
)
