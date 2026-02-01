package com.hcmus.forumus_admin.data.model.notification

import com.google.gson.annotations.SerializedName

data class NotificationTriggerRequest(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("actorId")
    val actorId: String,
    
    @SerializedName("actorName")
    val actorName: String,
    
    @SerializedName("targetId")
    val targetId: String,
    
    @SerializedName("targetUserId")
    val targetUserId: String,
    
    @SerializedName("previewText")
    val previewText: String,

    @SerializedName("originalPostTitle")
    val originalPostTitle: String? = null,

    @SerializedName("originalPostContent")
    val originalPostContent: String? = null
)

object NotificationType {
    const val POST_DELETED = "POST_DELETED"
    const val POST_APPROVED = "POST_APPROVED"
    const val STATUS_CHANGED = "STATUS_CHANGED"
    const val POST_REJECTED = "POST_REJECTED"
}
