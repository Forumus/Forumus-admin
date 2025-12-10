# AI Moderation Mock API Implementation

## Overview
This implementation provides a mock API architecture for the AI moderation feature, making it easy to integrate with a real backend API later. The mock service simulates realistic API responses with network delays and provides comprehensive moderation results.

## Architecture

### 1. Service Layer (`data/service/`)

#### `AiModerationService.kt`
Interface defining the contract for AI moderation API operations:
- `analyzePost()`: Analyze a post for content moderation
- `getModerationResults()`: Get all moderation results with pagination
- `getFilteredResults()`: Get results filtered by approval status
- `overrideDecision()`: Manually override AI moderation decisions

#### `MockAiModerationService.kt`
Mock implementation of the service interface that:
- Simulates realistic network delays (300-1500ms)
- Provides 12 pre-configured sample posts (6 approved, 6 rejected)
- Implements content analysis with keyword detection for:
  - **Spam**: Promotional content, excessive emojis, urgency tactics
  - **Toxicity**: Hate speech, aggressive language
  - **Insults**: Direct personal attacks
  - **Other violations**: Identity attacks, threats, misinformation
- Returns confidence scores (0.0-1.0) for each violation type
- Supports CRUD operations on moderation results

### 2. Data Models (`data/model/`)

#### `AiModerationResult.kt`
Contains all data models for AI moderation:

**AiModerationResult**
```kotlin
data class AiModerationResult(
    val postId: String,
    val isApproved: Boolean,
    val overallScore: Double,        // 0.0-1.0 toxicity score
    val violations: List<ViolationType>,
    val analyzedAt: Long
)
```

**ViolationType**
```kotlin
data class ViolationType(
    val type: ViolationCategory,
    val score: Double,               // Confidence: 0.0-1.0
    val description: String
)
```

**ViolationCategory** (enum)
- TOXICITY
- SEVERE_TOXICITY
- IDENTITY_ATTACK
- INSULT
- PROFANITY
- THREAT
- SPAM
- SEXUALLY_EXPLICIT
- FLIRTATION
- MISINFORMATION

### 3. Repository Layer (`data/repository/`)

#### `AiModerationRepository.kt`
Acts as a single source of truth for AI moderation data:
- Abstracts the service implementation (mock or real)
- Provides high-level operations for the UI layer
- Handles data transformation and error handling
- Includes utility methods like `searchModerationResults()` and `getModerationStats()`

### 4. ViewModel Updates (`ui/assistant/`)

#### `AiModerationViewModel.kt`
Updated to use the repository pattern:
- Loads posts asynchronously using coroutines
- Handles loading states and errors
- Converts moderation results to UI-friendly Post objects
- Implements approve/reject actions through the repository
- Maintains reactive state for the UI

## Sample Data

### Approved Posts (6)
1. "Getting Started with React Hooks" - Programming tutorial
2. "Best Practices for TypeScript" - Development guide
3. "Modern CSS Techniques" - Design tutorial
4. "Understanding Async/Await in JavaScript" - Technical content
5. "Building Scalable Microservices" - Architecture guide
6. "Introduction to Machine Learning" - Educational content

### Rejected Posts (6)
1. "10 Ways to Get Rich Quick Online" - Spam/Misinformation (score: 0.78)
2. "Why This Framework is GARBAGE" - Toxicity/Insults (score: 0.89)
3. "BUY MY COURSE NOW!!!" - Spam (score: 0.95)
4. "You're all stupid if you don't agree" - Toxicity/Insults (score: 0.87)
5. "FREE iPHONE GIVEAWAY" - Spam (score: 0.93)
6. Hate speech example - Severe Toxicity (score: 0.96)

## Usage Examples

### Analyzing a New Post
```kotlin
val repository = AiModerationRepository()

lifecycleScope.launch {
    val response = repository.analyzePost(
        postId = "new-post-123",
        content = "Post content here",
        title = "Post Title",
        authorId = "user-456"
    )
    
    if (response.success) {
        val result = response.result
        println("Approved: ${result?.isApproved}")
        println("Score: ${result?.overallScore}")
        result?.violations?.forEach { violation ->
            println("${violation.type}: ${violation.score}")
        }
    }
}
```

### Getting Filtered Results
```kotlin
// Get all rejected posts
val rejectedResult = repository.getRejectedPosts(limit = 20)

rejectedResult.onSuccess { posts ->
    posts.forEach { result ->
        println("Post ${result.postId}: ${result.violations.size} violations")
    }
}

// Get all approved posts
val approvedResult = repository.getApprovedPosts(limit = 20)
```

### Overriding Decisions
```kotlin
// Admin manually approves a rejected post
val result = repository.overrideModerationDecision(
    postId = "4",
    isApproved = true
)

result.onSuccess { updated ->
    println("Decision updated: ${updated.isApproved}")
}
```

### Getting Statistics
```kotlin
val statsResult = repository.getModerationStats()

statsResult.onSuccess { stats ->
    println("Total: ${stats.totalPosts}")
    println("Approved: ${stats.approvedCount}")
    println("Rejected: ${stats.rejectedCount}")
    println("Average Score: ${stats.averageScore}")
}
```

## Transition to Real API

When integrating with a real backend API, follow these steps:

### 1. Create Real API Service
```kotlin
class RealAiModerationService(
    private val apiClient: RetrofitClient
) : AiModerationService {
    
    override suspend fun analyzePost(
        request: AiModerationRequest
    ): AiModerationResponse {
        return apiClient.analyzePost(request)
    }
    
    // Implement other methods...
}
```

### 2. Update Repository
```kotlin
class AiModerationRepository(
    // Change default to real service
    private val service: AiModerationService = RealAiModerationService()
) {
    // No other changes needed!
}
```

### 3. Add Retrofit Dependencies
In `app/build.gradle.kts`:
```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 4. Define API Endpoints
```kotlin
interface AiModerationApi {
    @POST("/api/v1/moderation/analyze")
    suspend fun analyzePost(
        @Body request: AiModerationRequest
    ): AiModerationResponse
    
    @GET("/api/v1/moderation/results")
    suspend fun getModerationResults(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): List<AiModerationResult>
    
    @GET("/api/v1/moderation/results/filtered")
    suspend fun getFilteredResults(
        @Query("approved") isApproved: Boolean,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): List<AiModerationResult>
    
    @PUT("/api/v1/moderation/override/{postId}")
    suspend fun overrideDecision(
        @Path("postId") postId: String,
        @Body request: OverrideRequest
    ): AiModerationResult
}

data class OverrideRequest(val isApproved: Boolean)
```

## Benefits of This Architecture

1. **Separation of Concerns**: Clear separation between service, repository, and UI layers
2. **Easy Testing**: Mock service can be used for unit tests without network calls
3. **Flexible**: Easy to switch between mock and real implementations
4. **Type-Safe**: Strong typing with Kotlin data classes
5. **Asynchronous**: Proper coroutine support for non-blocking operations
6. **Scalable**: Repository pattern allows for caching, offline support, etc.
7. **Maintainable**: Interface-based design makes code easy to understand and modify

## API Response Format

The mock API returns responses in this format:

```json
{
  "success": true,
  "result": {
    "postId": "4",
    "isApproved": false,
    "overallScore": 0.78,
    "violations": [
      {
        "type": "SPAM",
        "score": 0.92,
        "description": "Content appears to be spam or clickbait"
      },
      {
        "type": "MISINFORMATION",
        "score": 0.71,
        "description": "Contains potentially misleading claims"
      }
    ],
    "analyzedAt": 1733868400000
  },
  "error": null
}
```

## Future Enhancements

1. **Caching**: Add local database (Room) for offline support
2. **Real-time Updates**: WebSocket support for live moderation results
3. **Batch Analysis**: Support for analyzing multiple posts at once
4. **User Feedback**: Allow admins to provide feedback on AI decisions
5. **Analytics**: Track moderation accuracy and performance metrics
6. **Custom Rules**: Allow configuration of moderation thresholds
7. **Multi-language**: Support for content moderation in multiple languages

## Testing

The mock service is perfect for testing:

```kotlin
@Test
fun testMockService() = runBlocking {
    val service = MockAiModerationService()
    
    val response = service.analyzePost(
        AiModerationRequest(
            postId = "test-1",
            content = "This is spam! Buy now!!!",
            title = "Test",
            authorId = "user-1"
        )
    )
    
    assertTrue(response.success)
    assertFalse(response.result?.isApproved ?: true)
    assertTrue(response.result?.violations?.any { 
        it.type == ViolationCategory.SPAM 
    } ?: false)
}
```

## Dependencies

No additional dependencies required! The implementation uses:
- Kotlin Coroutines (already in project)
- Kotlin Standard Library
- AndroidX Lifecycle (already in project)

## Files Created

1. `data/model/AiModerationResult.kt` - Data models
2. `data/service/AiModerationService.kt` - Service interface
3. `data/service/MockAiModerationService.kt` - Mock implementation
4. `data/repository/AiModerationRepository.kt` - Repository layer

## Files Modified

1. `ui/assistant/AssistantViewModel.kt` - Updated to use repository
2. (ViewModel automatically integrates with existing UI)

The existing UI components (`AssistantFragment`, `AiPostsAdapter`, layouts) work seamlessly with the new architecture without any changes!
