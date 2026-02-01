package com.hcmus.forumus_admin.data.model.email

import com.google.gson.annotations.SerializedName

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

data class ReportedPost(
    @SerializedName("postId")
    val postId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("reason")
    val reason: String? = null
)
