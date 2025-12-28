package com.hcmus.forumus_admin.data.service

import android.util.Log
import com.hcmus.forumus_admin.data.api.RetrofitClient
import com.hcmus.forumus_admin.data.model.UserStatus
import com.hcmus.forumus_admin.data.model.email.ReportEmailRequest
import com.hcmus.forumus_admin.data.model.email.ReportedPost

/**
 * Service for sending email notifications to users about their account status changes.
 * 
 * Email types based on status:
 * - NORMAL: Congratulatory email for improved behavior
 * - REMINDED: Gentle reminder about community guidelines
 * - WARNED: Warning about serious violations
 * - BANNED: Account suspension notification
 */
class EmailNotificationService {
    
    companion object {
        private const val TAG = "EmailNotificationService"
        
        @Volatile
        private var instance: EmailNotificationService? = null
        
        fun getInstance(): EmailNotificationService {
            return instance ?: synchronized(this) {
                instance ?: EmailNotificationService().also { instance = it }
            }
        }
    }
    
    /**
     * Send email notification to user about their status change.
     * 
     * @param userEmail The email address of the user
     * @param userName The name of the user
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @param reportedPosts List of posts that triggered the status change (optional)
     * @return Result indicating success or failure
     */
    suspend fun sendStatusChangeEmail(
        userEmail: String,
        userName: String,
        oldStatus: UserStatus,
        newStatus: UserStatus,
        reportedPosts: List<ReportedPost>? = null
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending email notification to $userEmail for status change: ${oldStatus.name} -> ${newStatus.name}")
            
            // Validate email
            if (userEmail.isBlank() || !isValidEmail(userEmail)) {
                Log.e(TAG, "Invalid email address: $userEmail")
                return Result.failure(IllegalArgumentException("Invalid email address"))
            }
            
            // Prepare request
            val request = ReportEmailRequest(
                recipientEmail = userEmail,
                userName = userName.ifBlank { userEmail.substringBefore("@") },
                userStatus = mapStatusToString(newStatus),
                reportedPosts = reportedPosts
            )
            
            // Send email via API
            val response = RetrofitClient.emailApi.sendReportEmail(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Email sent successfully to $userEmail: ${response.body()?.message}")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to send email"
                Log.e(TAG, "Failed to send email to $userEmail: $errorMsg (HTTP ${response.code()})")
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email notification to $userEmail", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send email notification when user status is escalated (increased severity).
     */
    suspend fun sendEscalationEmail(
        userEmail: String,
        userName: String,
        newStatus: UserStatus,
        reportedPosts: List<ReportedPost>? = null
    ): Result<Unit> {
        Log.d(TAG, "Sending escalation email for status: ${newStatus.name}")
        return sendStatusChangeEmail(
            userEmail = userEmail,
            userName = userName,
            oldStatus = getPreviousStatus(newStatus),
            newStatus = newStatus,
            reportedPosts = reportedPosts
        )
    }
    
    /**
     * Send email notification when user status is de-escalated (decreased severity).
     */
    suspend fun sendDeEscalationEmail(
        userEmail: String,
        userName: String,
        oldStatus: UserStatus,
        newStatus: UserStatus
    ): Result<Unit> {
        Log.d(TAG, "Sending de-escalation (congratulatory) email for status change: ${oldStatus.name} -> ${newStatus.name}")
        return sendStatusChangeEmail(
            userEmail = userEmail,
            userName = userName,
            oldStatus = oldStatus,
            newStatus = newStatus,
            reportedPosts = null // No reported posts for de-escalation
        )
    }
    
    /**
     * Map UserStatus enum to string for API.
     */
    private fun mapStatusToString(status: UserStatus): String {
        return when (status) {
            UserStatus.NORMAL -> "NORMAL"
            UserStatus.REMINDED -> "REMINDED"
            UserStatus.WARNED -> "WARNED"
            UserStatus.BANNED -> "BANNED"
        }
    }
    
    /**
     * Get the previous status level (for escalation tracking).
     */
    private fun getPreviousStatus(currentStatus: UserStatus): UserStatus {
        return when (currentStatus) {
            UserStatus.BANNED -> UserStatus.WARNED
            UserStatus.WARNED -> UserStatus.REMINDED
            UserStatus.REMINDED -> UserStatus.NORMAL
            UserStatus.NORMAL -> UserStatus.NORMAL
        }
    }
    
    /**
     * Get the next status level (for de-escalation tracking).
     */
    private fun getNextStatus(currentStatus: UserStatus): UserStatus {
        return when (currentStatus) {
            UserStatus.NORMAL -> UserStatus.REMINDED
            UserStatus.REMINDED -> UserStatus.WARNED
            UserStatus.WARNED -> UserStatus.BANNED
            UserStatus.BANNED -> UserStatus.BANNED
        }
    }
    
    /**
     * Validate email format.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Compare severity levels of two statuses.
     * @return positive if newStatus is more severe, negative if less severe, 0 if same
     */
    fun compareStatusSeverity(oldStatus: UserStatus, newStatus: UserStatus): Int {
        val oldLevel = getStatusSeverityLevel(oldStatus)
        val newLevel = getStatusSeverityLevel(newStatus)
        return newLevel - oldLevel
    }
    
    /**
     * Get numeric severity level for status.
     * NORMAL = 0, REMINDED = 1, WARNED = 2, BANNED = 3
     */
    private fun getStatusSeverityLevel(status: UserStatus): Int {
        return when (status) {
            UserStatus.NORMAL -> 0
            UserStatus.REMINDED -> 1
            UserStatus.WARNED -> 2
            UserStatus.BANNED -> 3
        }
    }
}
