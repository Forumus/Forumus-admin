package com.hcmus.forumus_admin.data.model.email

import com.google.gson.annotations.SerializedName

data class EmailResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)
