package com.hcmus.forumus_admin.data.model.notification

import com.google.gson.annotations.SerializedName

/**
 * Request model for triggering notifications via backend API.
 * Matches the backend NotificationTriggerRequest DTO.
 */
data class NotificationTriggerRequest(
    @SerializedName("type")
    val type: String, // POST_DELETED, POST_APPROVED, STATUS_CHANGED, UPVOTE, COMMENT, REPLY
    
    @SerializedName("actorId")
    val actorId: String, // Admin ID or "SYSTEM"
    
    @SerializedName("actorName")
    val actorName: String, // Admin name or "System"
    
    @SerializedName("targetId")
    val targetId: String, // Post ID, Comment ID, or User ID
    
    @SerializedName("targetUserId")
    val targetUserId: String, // The user to notify
    
    @SerializedName("previewText")
    val previewText: String // Snippet of content or reason
)

/**
 * Notification types for admin actions.
 */
object NotificationType {
    const val POST_DELETED = "POST_DELETED"
    const val POST_APPROVED = "POST_APPROVED"
    const val STATUS_CHANGED = "STATUS_CHANGED"
    const val POST_REJECTED = "POST_REJECTED"
}
