package com.hcmus.forumus_admin.data.service

import android.util.Log
import com.hcmus.forumus_admin.data.api.RetrofitClient
import com.hcmus.forumus_admin.data.model.notification.NotificationTriggerRequest
import com.hcmus.forumus_admin.data.model.notification.NotificationType

class PushNotificationService {
    
    companion object {
        private const val TAG = "PushNotificationService"
        private const val ADMIN_ACTOR_ID = "ADMIN"
        private const val SYSTEM_ACTOR_ID = "SYSTEM"
        private const val ADMIN_ACTOR_NAME = "Admin"
        private const val SYSTEM_ACTOR_NAME = "Forumus System"
        
        @Volatile
        private var instance: PushNotificationService? = null
        
        fun getInstance(): PushNotificationService {
            return instance ?: synchronized(this) {
                instance ?: PushNotificationService().also { instance = it }
            }
        }
    }

    suspend fun sendPostDeletedNotification(
        postId: String,
        postAuthorId: String,
        postTitle: String,
        postContent: String,
        reason: String = "Community guidelines violation",
        isAiDeleted: Boolean = false
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending post deleted notification to user: $postAuthorId")
            
            val actorId = if (isAiDeleted) SYSTEM_ACTOR_ID else ADMIN_ACTOR_ID
            val actorName = if (isAiDeleted) SYSTEM_ACTOR_NAME else ADMIN_ACTOR_NAME
            
            val request = NotificationTriggerRequest(
                type = NotificationType.POST_DELETED,
                actorId = actorId,
                actorName = actorName,
                targetId = postId,
                targetUserId = postAuthorId,
                previewText = "Your post \"${postTitle.take(50)}\" was removed: $reason",
                originalPostTitle = postTitle,
                originalPostContent = postContent
            )
            
            val response = RetrofitClient.notificationApi.triggerNotification(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Post deleted notification sent successfully")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to send notification (HTTP ${response.code()})"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending post deleted notification", e)
            Result.failure(e)
        }
    }

    suspend fun sendPostApprovedNotification(
        postId: String,
        postAuthorId: String,
        postTitle: String,
        isAiApproved: Boolean = false
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending post approved notification to user: $postAuthorId")
            
            val actorId = if (isAiApproved) SYSTEM_ACTOR_ID else ADMIN_ACTOR_ID
            val actorName = if (isAiApproved) SYSTEM_ACTOR_NAME else ADMIN_ACTOR_NAME
            
            val request = NotificationTriggerRequest(
                type = NotificationType.POST_APPROVED,
                actorId = actorId,
                actorName = actorName,
                targetId = postId,
                targetUserId = postAuthorId,
                previewText = "Your post \"${postTitle.take(50)}\" has been approved and is now live!"
            )
            
            val response = RetrofitClient.notificationApi.triggerNotification(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Post approved notification sent successfully")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to send notification (HTTP ${response.code()})"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending post approved notification", e)
            Result.failure(e)
        }
    }

    suspend fun sendPostRejectedNotification(
        postId: String,
        postAuthorId: String,
        postTitle: String,
        postContent: String,
        reason: String = "Content policy violation",
        isAiRejected: Boolean = false
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending post rejected notification to user: $postAuthorId")
            
            val actorId = if (isAiRejected) SYSTEM_ACTOR_ID else ADMIN_ACTOR_ID
            val actorName = if (isAiRejected) SYSTEM_ACTOR_NAME else ADMIN_ACTOR_NAME
            
            val request = NotificationTriggerRequest(
                type = NotificationType.POST_REJECTED,
                actorId = actorId,
                actorName = actorName,
                targetId = postId,
                targetUserId = postAuthorId,
                previewText = "Your post \"${postTitle.take(50)}\" has been rejected by the verification system",
                originalPostTitle = postTitle,
                originalPostContent = postContent
            )
            
            val response = RetrofitClient.notificationApi.triggerNotification(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Post rejected notification sent successfully")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to send notification (HTTP ${response.code()})"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending post rejected notification", e)
            Result.failure(e)
        }
    }

    suspend fun sendStatusChangedNotification(
        userId: String,
        oldStatus: String,
        newStatus: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending status changed notification to user: $userId")
            
            val statusMessage = when (newStatus.uppercase()) {
                "NORMAL" -> "Your account status has been restored to normal. Welcome back!"
                "REMINDED" -> "You've received a reminder about community guidelines."
                "WARNED" -> "Your account has received a warning. Please review our guidelines."
                "BANNED" -> "Your account has been suspended due to policy violations."
                else -> "Your account status has been updated to $newStatus."
            }
            
            val request = NotificationTriggerRequest(
                type = NotificationType.STATUS_CHANGED,
                actorId = ADMIN_ACTOR_ID,
                actorName = ADMIN_ACTOR_NAME,
                targetId = userId,
                targetUserId = userId,
                previewText = statusMessage
            )
            
            val response = RetrofitClient.notificationApi.triggerNotification(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Status changed notification sent successfully")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to send notification (HTTP ${response.code()})"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending status changed notification", e)
            Result.failure(e)
        }
    }
}
