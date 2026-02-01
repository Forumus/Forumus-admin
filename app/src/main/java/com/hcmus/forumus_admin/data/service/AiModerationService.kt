package com.hcmus.forumus_admin.data.service

import com.hcmus.forumus_admin.data.model.AiModerationRequest
import com.hcmus.forumus_admin.data.model.AiModerationResponse
import com.hcmus.forumus_admin.data.model.AiModerationResult

interface AiModerationService {

    suspend fun analyzePost(request: AiModerationRequest): AiModerationResponse

    suspend fun getModerationResults(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>>

    suspend fun getFilteredResults(
        isApproved: Boolean,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>>

    suspend fun overrideDecision(
        postId: String,
        isApproved: Boolean
    ): Result<AiModerationResult>
}
