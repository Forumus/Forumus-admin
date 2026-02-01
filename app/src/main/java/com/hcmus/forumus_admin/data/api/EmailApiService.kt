package com.hcmus.forumus_admin.data.api

import com.hcmus.forumus_admin.data.model.email.EmailResponse
import com.hcmus.forumus_admin.data.model.email.ReportEmailRequest
import com.hcmus.forumus_admin.data.model.notification.NotificationTriggerRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface EmailApiService {

    @POST("api/email/send-report")
    suspend fun sendReportEmail(
        @Body request: ReportEmailRequest
    ): Response<EmailResponse>
}

interface NotificationApiService {

    @POST("api/notifications")
    suspend fun triggerNotification(
        @Body request: NotificationTriggerRequest
    ): Response<String>
}
