package com.hcmus.forumus_admin.data.model

/**
 * Represents the result of AI moderation analysis for a post
 */
data class AiModerationResult(
    val postData: FirestorePost,
    val isApproved: Boolean,
    val overallScore: Double, // 0.0 to 1.0, where 1.0 is most toxic/inappropriate
    val violations: List<ViolationType>,
    val analyzedAt: Long = System.currentTimeMillis()
)

/**
 * Types of content violations detected by AI
 */
data class ViolationType(
    val type: ViolationCategory,
    val score: Double, // Confidence score 0.0 to 1.0
    val description: String
)

/**
 * Categories of content violations
 */
enum class ViolationCategory(val displayName: String) {
    TOXICITY("Toxic Content"),
    SEVERE_TOXICITY("Severe Toxicity"),
    IDENTITY_ATTACK("Identity Attack"),
    INSULT("Insult"),
    PROFANITY("Profanity"),
    THREAT("Threat"),
    SPAM("Spam"),
    SEXUALLY_EXPLICIT("Sexually Explicit"),
    FLIRTATION("Flirtation"),
    MISINFORMATION("Misinformation");
    
    companion object {
        fun fromString(value: String): ViolationCategory? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Request for analyzing a post with AI moderation
 */
data class AiModerationRequest(
    val postId: String,
    val content: String,
    val title: String,
    val authorId: String
)

/**
 * Response from AI moderation API
 */
data class AiModerationResponse(
    val success: Boolean,
    val result: AiModerationResult?,
    val error: String? = null
)
