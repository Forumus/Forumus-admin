package com.hcmus.forumus_admin.data.service

import android.util.Log
import com.hcmus.forumus_admin.data.model.StatusEscalationResult
import com.hcmus.forumus_admin.data.model.UserStatus
import com.hcmus.forumus_admin.data.model.UserStatusLevel
import com.hcmus.forumus_admin.data.repository.UserRepository
import com.hcmus.forumus_admin.data.model.email.ReportedPost

class UserStatusEscalationService(
    private val userRepository: UserRepository = UserRepository(),
    private val emailNotificationService: EmailNotificationService = EmailNotificationService.getInstance(),
    private val pushNotificationService: PushNotificationService = PushNotificationService.getInstance()
) {
    companion object {
        private const val TAG = "UserStatusEscalation"

        @Volatile
        private var instance: UserStatusEscalationService? = null
        
        fun getInstance(): UserStatusEscalationService {
            return instance ?: synchronized(this) {
                instance ?: UserStatusEscalationService().also { instance = it }
            }
        }
    }

    suspend fun escalateUserStatus(userId: String): StatusEscalationResult {
        return try {
            Log.d(TAG, "Starting status escalation for user: $userId")

            val userResult = userRepository.getUserById(userId)
            if (userResult.isFailure) {
                Log.e(TAG, "Failed to get user: $userId")
                return StatusEscalationResult(
                    success = false,
                    userId = userId,
                    previousStatus = UserStatusLevel.NORMAL,
                    newStatus = UserStatusLevel.NORMAL,
                    wasEscalated = false,
                    error = "Failed to get user: ${userResult.exceptionOrNull()?.message}"
                )
            }
            
            val user = userResult.getOrNull()
            if (user == null) {
                Log.e(TAG, "User not found: $userId")
                return StatusEscalationResult(
                    success = false,
                    userId = userId,
                    previousStatus = UserStatusLevel.NORMAL,
                    newStatus = UserStatusLevel.NORMAL,
                    wasEscalated = false,
                    error = "User not found: $userId"
                )
            }

            val currentStatusLevel = UserStatusLevel.fromString(user.status)
            Log.d(TAG, "Current user status: ${currentStatusLevel.value}")

            val nextStatusLevel = UserStatusLevel.getNextLevel(currentStatusLevel)
            val wasEscalated = nextStatusLevel != currentStatusLevel
            
            if (!wasEscalated) {
                Log.d(TAG, "User $userId is already at maximum status level (BANNED)")
                return StatusEscalationResult(
                    success = true,
                    userId = userId,
                    previousStatus = currentStatusLevel,
                    newStatus = currentStatusLevel,
                    wasEscalated = false
                )
            }
            
            Log.d(TAG, "Escalating status: ${currentStatusLevel.value} -> ${nextStatusLevel.value}")

            val newUserStatus = convertToUserStatus(nextStatusLevel)
            val updateResult = userRepository.updateUserStatus(userId, newUserStatus)
            
            if (updateResult.isFailure) {
                Log.e(TAG, "Failed to update user status", updateResult.exceptionOrNull())
                return StatusEscalationResult(
                    success = false,
                    userId = userId,
                    previousStatus = currentStatusLevel,
                    newStatus = nextStatusLevel,
                    wasEscalated = false,
                    error = "Failed to update user status: ${updateResult.exceptionOrNull()?.message}"
                )
            }

            try {
                val emailResult = emailNotificationService.sendEscalationEmail(
                    userEmail = user.email,
                    userName = user.fullName,
                    newStatus = newUserStatus,
                    reportedPosts = null // Can be extended to include specific posts
                )
                
                if (emailResult.isSuccess) {
                    Log.d(TAG, "Email notification sent successfully to ${user.email}")
                } else {
                    Log.w(TAG, "Failed to send email notification: ${emailResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error sending email notification (non-blocking)", e)
            }

            try {
                val pushResult = pushNotificationService.sendStatusChangedNotification(
                    userId = userId,
                    oldStatus = currentStatusLevel.value,
                    newStatus = nextStatusLevel.value
                )
                
                if (pushResult.isSuccess) {
                    Log.d(TAG, "Push notification sent successfully to user $userId")
                } else {
                    Log.w(TAG, "Failed to send push notification: ${pushResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error sending push notification (non-blocking)", e)
            }
            
            Log.d(TAG, "Status escalation completed successfully for user: $userId")
            
            StatusEscalationResult(
                success = true,
                userId = userId,
                previousStatus = currentStatusLevel,
                newStatus = nextStatusLevel,
                wasEscalated = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during status escalation for user: $userId", e)
            StatusEscalationResult(
                success = false,
                userId = userId,
                previousStatus = UserStatusLevel.NORMAL,
                newStatus = UserStatusLevel.NORMAL,
                wasEscalated = false,
                error = e.message
            )
        }
    }

    suspend fun isUserBanned(userId: String): Boolean {
        return try {
            val userResult = userRepository.getUserById(userId)
            val user = userResult.getOrNull() ?: return false
            UserStatusLevel.fromString(user.status) == UserStatusLevel.BANNED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user is banned: $userId", e)
            false
        }
    }

    private fun convertToUserStatus(level: UserStatusLevel): UserStatus {
        return when (level) {
            UserStatusLevel.NORMAL -> UserStatus.NORMAL
            UserStatusLevel.REMINDED -> UserStatus.REMINDED
            UserStatusLevel.WARNED -> UserStatus.WARNED
            UserStatusLevel.BANNED -> UserStatus.BANNED
        }
    }
}
