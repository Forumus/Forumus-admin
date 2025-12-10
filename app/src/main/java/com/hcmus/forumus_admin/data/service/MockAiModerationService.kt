package com.hcmus.forumus_admin.data.service

import com.hcmus.forumus_admin.data.model.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock implementation of AI Moderation Service
 * Simulates API responses for development and testing
 * This will be replaced with actual API implementation later
 */
class MockAiModerationService : AiModerationService {
    
    // Simulated database of moderation results
    private val moderationResults = mutableMapOf<String, AiModerationResult>()
    
    // Sample post data with predefined moderation results
    private val samplePosts = listOf(
        // Approved posts
        Triple("1", "Getting Started with React Hooks", "A comprehensive guide to understanding and implementing React Hooks in your applications. Learn about useState, useEffect, and custom hooks."),
        Triple("2", "Best Practices for TypeScript", "Explore the best practices for writing clean, maintainable TypeScript code. Cover type safety, interfaces, and advanced patterns."),
        Triple("3", "Modern CSS Techniques", "Discover the latest CSS techniques including Grid, Flexbox, and custom properties to create responsive layouts."),
        Triple("7", "Understanding Async/Await in JavaScript", "Deep dive into asynchronous programming in JavaScript with practical examples and common pitfalls to avoid."),
        Triple("8", "Building Scalable Microservices", "Learn how to design and implement microservices architecture with best practices for scalability and maintainability."),
        Triple("9", "Introduction to Machine Learning", "Beginner-friendly guide to machine learning concepts, algorithms, and practical applications in real-world scenarios."),
        
        // Rejected posts
        Triple("4", "10 Ways to Get Rich Quick Online", "Make money fast with these amazing tricks! No experience needed. Click here to learn the secret methods that gurus don't want you to know!"),
        Triple("5", "Why This Framework is GARBAGE", "This framework is absolute trash and anyone who uses it is an idiot. I can't believe people actually waste their time with this garbage."),
        Triple("6", "BUY MY COURSE NOW!!! LIMITED OFFER!!!", "🔥🔥🔥 EXCLUSIVE OFFER!!! Learn web development in 24 hours!!! Buy now for $9999! Limited slots available! Don't miss out! 🚀💰"),
        Triple("10", "You're all stupid if you don't agree", "Everyone who disagrees with me is a complete moron. This community is full of idiots who don't know what they're talking about."),
        Triple("11", "FREE iPHONE GIVEAWAY CLICK HERE", "GET FREE STUFF NOW!!! LIMITED TIME OFFER!!! CLICK THIS LINK IMMEDIATELY!!! DON'T MISS OUT ON THIS AMAZING OPPORTUNITY!!!"),
        Triple("12", "Hate speech content example", "This post contains inappropriate content targeting specific groups with discriminatory language and hate speech.")
    )
    
    init {
        // Pre-populate with sample moderation results
        initializeSampleResults()
    }
    
    private fun initializeSampleResults() {
        // Generate approved posts
        listOf("1", "2", "3", "7", "8", "9").forEach { postId ->
            moderationResults[postId] = generateApprovedResult(postId)
        }
        
        // Generate rejected posts
        moderationResults["4"] = AiModerationResult(
            postId = "4",
            isApproved = false,
            overallScore = 0.78,
            violations = listOf(
                ViolationType(ViolationCategory.SPAM, 0.92, "Content appears to be spam or clickbait"),
                ViolationType(ViolationCategory.MISINFORMATION, 0.71, "Contains potentially misleading claims")
            )
        )
        
        moderationResults["5"] = AiModerationResult(
            postId = "5",
            isApproved = false,
            overallScore = 0.89,
            violations = listOf(
                ViolationType(ViolationCategory.TOXICITY, 0.85, "Toxic language detected"),
                ViolationType(ViolationCategory.INSULT, 0.91, "Contains insulting language"),
                ViolationType(ViolationCategory.PROFANITY, 0.73, "Profanity detected")
            )
        )
        
        moderationResults["6"] = AiModerationResult(
            postId = "6",
            isApproved = false,
            overallScore = 0.95,
            violations = listOf(
                ViolationType(ViolationCategory.SPAM, 0.98, "High spam confidence - excessive promotional content"),
                ViolationType(ViolationCategory.FLIRTATION, 0.12, "Minor marketing language detected")
            )
        )
        
        moderationResults["10"] = AiModerationResult(
            postId = "10",
            isApproved = false,
            overallScore = 0.87,
            violations = listOf(
                ViolationType(ViolationCategory.TOXICITY, 0.88, "Toxic language toward community"),
                ViolationType(ViolationCategory.INSULT, 0.94, "Direct insults detected"),
                ViolationType(ViolationCategory.SEVERE_TOXICITY, 0.62, "Moderate toxicity level")
            )
        )
        
        moderationResults["11"] = AiModerationResult(
            postId = "11",
            isApproved = false,
            overallScore = 0.93,
            violations = listOf(
                ViolationType(ViolationCategory.SPAM, 0.99, "Clear spam pattern detected"),
                ViolationType(ViolationCategory.MISINFORMATION, 0.45, "Suspicious promotional claims")
            )
        )
        
        moderationResults["12"] = AiModerationResult(
            postId = "12",
            isApproved = false,
            overallScore = 0.96,
            violations = listOf(
                ViolationType(ViolationCategory.SEVERE_TOXICITY, 0.97, "Severe toxic content"),
                ViolationType(ViolationCategory.IDENTITY_ATTACK, 0.94, "Identity-based attack detected"),
                ViolationType(ViolationCategory.THREAT, 0.68, "Potentially threatening language")
            )
        )
    }
    
    private fun generateApprovedResult(postId: String): AiModerationResult {
        return AiModerationResult(
            postId = postId,
            isApproved = true,
            overallScore = Random.nextDouble(0.0, 0.25), // Low toxicity score
            violations = emptyList() // No violations for approved posts
        )
    }
    
    override suspend fun analyzePost(request: AiModerationRequest): AiModerationResponse {
        // Simulate network delay
        delay(Random.nextLong(500, 1500))
        
        try {
            // Check if we already have a result
            val existingResult = moderationResults[request.postId]
            if (existingResult != null) {
                return AiModerationResponse(
                    success = true,
                    result = existingResult
                )
            }
            
            // Simulate AI analysis based on content
            val result = simulateAiAnalysis(request)
            moderationResults[request.postId] = result
            
            return AiModerationResponse(
                success = true,
                result = result
            )
        } catch (e: Exception) {
            return AiModerationResponse(
                success = false,
                result = null,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }
    
    private fun simulateAiAnalysis(request: AiModerationRequest): AiModerationResult {
        val content = request.content.lowercase() + " " + request.title.lowercase()
        val violations = mutableListOf<ViolationType>()
        var maxScore = 0.0
        
        // Check for spam indicators
        val spamKeywords = listOf("buy now", "limited offer", "click here", "free", "!!!!", "🔥", "💰")
        val spamCount = spamKeywords.count { content.contains(it) }
        if (spamCount > 0) {
            val spamScore = minOf(0.99, 0.5 + (spamCount * 0.15))
            violations.add(ViolationType(
                ViolationCategory.SPAM,
                spamScore,
                "Promotional or spam content detected"
            ))
            maxScore = maxOf(maxScore, spamScore)
        }
        
        // Check for toxic language
        val toxicKeywords = listOf("garbage", "trash", "stupid", "idiot", "hate", "awful")
        val toxicCount = toxicKeywords.count { content.contains(it) }
        if (toxicCount > 0) {
            val toxicScore = minOf(0.95, 0.6 + (toxicCount * 0.12))
            violations.add(ViolationType(
                ViolationCategory.TOXICITY,
                toxicScore,
                "Toxic language detected"
            ))
            maxScore = maxOf(maxScore, toxicScore)
        }
        
        // Check for insults
        val insultKeywords = listOf("idiot", "moron", "dumb", "stupid")
        if (insultKeywords.any { content.contains(it) }) {
            val insultScore = 0.85
            violations.add(ViolationType(
                ViolationCategory.INSULT,
                insultScore,
                "Insulting language detected"
            ))
            maxScore = maxOf(maxScore, insultScore)
        }
        
        // Determine approval based on violations
        val threshold = 0.5
        val isApproved = violations.isEmpty() || maxScore < threshold
        
        return AiModerationResult(
            postId = request.postId,
            isApproved = isApproved,
            overallScore = maxScore,
            violations = violations
        )
    }
    
    override suspend fun getModerationResults(
        limit: Int,
        offset: Int
    ): Result<List<AiModerationResult>> {
        // Simulate network delay
        delay(Random.nextLong(300, 800))
        
        return try {
            val results = moderationResults.values
                .sortedByDescending { it.analyzedAt }
                .drop(offset)
                .take(limit)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getFilteredResults(
        isApproved: Boolean,
        limit: Int,
        offset: Int
    ): Result<List<AiModerationResult>> {
        // Simulate network delay
        delay(Random.nextLong(300, 800))
        
        return try {
            val results = moderationResults.values
                .filter { it.isApproved == isApproved }
                .sortedByDescending { it.analyzedAt }
                .drop(offset)
                .take(limit)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun overrideDecision(
        postId: String,
        isApproved: Boolean
    ): Result<AiModerationResult> {
        // Simulate network delay
        delay(Random.nextLong(200, 600))
        
        return try {
            val existing = moderationResults[postId]
                ?: return Result.failure(Exception("Post not found"))
            
            val updated = existing.copy(
                isApproved = isApproved,
                analyzedAt = System.currentTimeMillis()
            )
            moderationResults[postId] = updated
            
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Additional helper method to get sample post info
     * Useful for displaying post details in the UI
     */
    fun getSamplePostInfo(postId: String): Triple<String, String, String>? {
        return samplePosts.find { it.first == postId }
    }
}
