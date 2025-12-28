package com.hcmus.forumus_admin.data.api

import com.hcmus.forumus_admin.data.model.email.EmailResponse
import com.hcmus.forumus_admin.data.model.email.ReportEmailRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for email-related API endpoints.
 */
interface EmailApiService {
    
    /**
     * Send a report email to a user about their account status.
     * 
     * @param request The report email request containing recipient info and status
     * @return Response containing EmailResponse with success status
     */
    @POST("api/email/send-report")
    suspend fun sendReportEmail(
        @Body request: ReportEmailRequest
    ): Response<EmailResponse>
}
