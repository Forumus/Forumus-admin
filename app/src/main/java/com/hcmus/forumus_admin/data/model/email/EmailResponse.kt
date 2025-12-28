package com.hcmus.forumus_admin.data.model.email

import com.google.gson.annotations.SerializedName

/**
 * Response model for email API calls.
 * Matches the backend EmailResponse DTO.
 */
data class EmailResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)
