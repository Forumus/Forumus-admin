package com.hcmus.forumus_admin.data.service

import android.util.Log
import com.hcmus.forumus_admin.data.model.StatusEscalationResult
import com.hcmus.forumus_admin.data.model.UserStatus
import com.hcmus.forumus_admin.data.model.UserStatusLevel
import com.hcmus.forumus_admin.data.repository.UserRepository
import com.hcmus.forumus_admin.data.model.email.ReportedPost

/**
 * Service for handling user status escalation when posts are deleted.
 * 
 * Status escalation order:
 * NORMAL → REMINDED → WARNED → BANNED
 * 
 * Triggers:
 * - AI moderation rejection (AI reject feature)
 * - Admin deletion after report (reported post feature)
 */
class UserStatusEscalationService(
    private val userRepository: UserRepository = UserRepository(),
    private val emailNotificationService: EmailNotificationService = EmailNotificationService.getInstance()
) {
    companion object {
        private const val TAG = "UserStatusEscalation"
        
        // Singleton instance for easy access
        @Volatile
        private var instance: UserStatusEscalationService? = null
        
        fun getInstance(): UserStatusEscalationService {
            return instance ?: synchronized(this) {
                instance ?: UserStatusEscalationService().also { instance = it }
            }
        }
    }

    /**
     * Escalate a user's status to the next level when their post is deleted.
     * 
     * @param userId The ID of the user whose status should be escalated
     * @return StatusEscalationResult containing the result of the operation
     */
    suspend fun escalateUserStatus(userId: String): StatusEscalationResult {
        return try {
            Log.d(TAG, "Starting status escalation for user: $userId")
            
            // Step 1: Get current user status
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
            
            // Step 2: Get current status level
            val currentStatusLevel = UserStatusLevel.fromString(user.status)
            Log.d(TAG, "Current user status: ${currentStatusLevel.value}")
            
            // Step 3: Calculate next status level
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
            
            // Step 4: Update user status in Firebase
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
            
            // Step 5: Send email notification to user about status change
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
                    // Continue even if email fails - don't block status escalation
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error sending email notification (non-blocking)", e)
                // Continue even if email fails
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

    /**
     * Check if a user is banned.
     */
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

    /**
     * Convert UserStatusLevel to UserStatus enum used by UserRepository.
     */
    private fun convertToUserStatus(level: UserStatusLevel): UserStatus {
        return when (level) {
            UserStatusLevel.NORMAL -> UserStatus.NORMAL
            UserStatusLevel.REMINDED -> UserStatus.REMINDED
            UserStatusLevel.WARNED -> UserStatus.WARNED
            UserStatusLevel.BANNED -> UserStatus.BANNED
        }
    }
}
