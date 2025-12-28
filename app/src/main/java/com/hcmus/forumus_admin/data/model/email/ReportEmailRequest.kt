package com.hcmus.forumus_admin.data.model.email

import com.google.gson.annotations.SerializedName

/**
 * Request model for sending report emails via backend API.
 * Matches the backend ReportEmailRequest DTO.
 */
data class ReportEmailRequest(
    @SerializedName("recipientEmail")
    val recipientEmail: String,
    
    @SerializedName("userName")
    val userName: String,
    
    @SerializedName("userStatus")
    val userStatus: String,
    
    @SerializedName("reportedPosts")
    val reportedPosts: List<ReportedPost>? = null
)

/**
 * Represents a reported post included in the email.
 */
data class ReportedPost(
    @SerializedName("postId")
    val postId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("reason")
    val reason: String? = null
)
