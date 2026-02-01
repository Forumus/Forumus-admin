package com.hcmus.forumus_admin.data.service

import android.util.Log
import com.hcmus.forumus_admin.data.api.RetrofitClient
import com.hcmus.forumus_admin.data.model.UserStatus
import com.hcmus.forumus_admin.data.model.email.ReportEmailRequest
import com.hcmus.forumus_admin.data.model.email.ReportedPost

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

    private fun mapStatusToString(status: UserStatus): String {
        return when (status) {
            UserStatus.NORMAL -> "NORMAL"
            UserStatus.REMINDED -> "REMINDED"
            UserStatus.WARNED -> "WARNED"
            UserStatus.BANNED -> "BANNED"
        }
    }

    private fun getPreviousStatus(currentStatus: UserStatus): UserStatus {
        return when (currentStatus) {
            UserStatus.BANNED -> UserStatus.WARNED
            UserStatus.WARNED -> UserStatus.REMINDED
            UserStatus.REMINDED -> UserStatus.NORMAL
            UserStatus.NORMAL -> UserStatus.NORMAL
        }
    }

    private fun getNextStatus(currentStatus: UserStatus): UserStatus {
        return when (currentStatus) {
            UserStatus.NORMAL -> UserStatus.REMINDED
            UserStatus.REMINDED -> UserStatus.WARNED
            UserStatus.WARNED -> UserStatus.BANNED
            UserStatus.BANNED -> UserStatus.BANNED
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun compareStatusSeverity(oldStatus: UserStatus, newStatus: UserStatus): Int {
        val oldLevel = getStatusSeverityLevel(oldStatus)
        val newLevel = getStatusSeverityLevel(newStatus)
        return newLevel - oldLevel
    }

    private fun getStatusSeverityLevel(status: UserStatus): Int {
        return when (status) {
            UserStatus.NORMAL -> 0
            UserStatus.REMINDED -> 1
            UserStatus.WARNED -> 2
            UserStatus.BANNED -> 3
        }
    }
}
