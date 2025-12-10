# AI Moderation Mock API - Quick Reference

## Overview
Mock API implementation for AI content moderation, ready for backend integration.

## Key Components

### 1. Data Models (`data/model/AiModerationResult.kt`)
- **AiModerationResult**: Main result object with approval status, score, violations
- **ViolationType**: Individual violation with category, confidence score, description
- **ViolationCategory**: 10 types (TOXICITY, SPAM, INSULT, THREAT, etc.)

### 2. Service Interface (`data/service/AiModerationService.kt`)
```kotlin
interface AiModerationService {
    suspend fun analyzePost(request: AiModerationRequest): AiModerationResponse
    suspend fun getModerationResults(limit: Int, offset: Int): Result<List<AiModerationResult>>
    suspend fun getFilteredResults(isApproved: Boolean, limit: Int, offset: Int): Result<List<AiModerationResult>>
    suspend fun overrideDecision(postId: String, isApproved: Boolean): Result<AiModerationResult>
}
```

### 3. Mock Service (`data/service/MockAiModerationService.kt`)
- 12 pre-configured sample posts (6 approved, 6 rejected)
- Simulates network delays (300-1500ms)
- Keyword-based content analysis
- Confidence scores (0.0-1.0)

### 4. Repository (`data/repository/AiModerationRepository.kt`)
```kotlin
class AiModerationRepository(
    private val service: AiModerationService = MockAiModerationService()
)
```

**Key Methods:**
- `analyzePost()`: Analyze new content
- `getApprovedPosts()` / `getRejectedPosts()`: Get filtered lists
- `overrideModerationDecision()`: Manual approval/rejection
- `getModerationStats()`: Get statistics

### 5. ViewModel (`ui/assistant/AssistantViewModel.kt`)
- Uses repository for data operations
- Async loading with coroutines
- Handles loading states and errors
- Reactive UI updates

## Sample Posts

### Approved (6 posts)
- React Hooks tutorial
- TypeScript best practices
- CSS techniques
- JavaScript async/await
- Microservices architecture
- Machine learning intro

### Rejected (6 posts)
- Get rich quick spam (score: 0.78)
- Toxic rant (score: 0.89)
- Course spam (score: 0.95)
- Insulting content (score: 0.87)
- Giveaway spam (score: 0.93)
- Hate speech (score: 0.96)

## Usage

### Load Posts
```kotlin
// Automatic on ViewModel init
private fun loadPosts() {
    viewModelScope.launch {
        val result = repository.getAllModerationResults()
        result.onSuccess { /* Update UI */ }
    }
}
```

### Approve/Reject Post
```kotlin
// Already implemented in ViewModel
fun approvePost(postId: String) // Calls repository
fun rejectPost(postId: String)  // Calls repository
```

### Search Posts
```kotlin
fun searchPosts(query: String) // Filters by title/description/author
```

## Switching to Real API

1. Create `RealAiModerationService` implementing `AiModerationService`
2. Update `AiModerationRepository` default service
3. Add Retrofit dependencies
4. Define API endpoints

**No changes needed in ViewModel or UI!**

## API Response Format

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
      }
    ],
    "analyzedAt": 1733868400000
  }
}
```

## Files Created
- ✅ `data/model/AiModerationResult.kt`
- ✅ `data/service/AiModerationService.kt`
- ✅ `data/service/MockAiModerationService.kt`
- ✅ `data/repository/AiModerationRepository.kt`
- ✅ `AI_MODERATION_MOCK_API.md` (full documentation)
- ✅ `AI_MODERATION_MOCK_API_QUICK_REF.md` (this file)

## Files Modified
- ✅ `ui/assistant/AssistantViewModel.kt`

## Benefits
- ✨ Clean architecture (Service → Repository → ViewModel → UI)
- 🔄 Easy to switch mock ↔ real API
- 🧪 Perfect for testing
- 🚀 Non-blocking async operations
- 📊 Realistic sample data with violation scores
- 🎯 Type-safe with Kotlin

## No Additional Dependencies Required
Uses existing:
- Kotlin Coroutines
- AndroidX Lifecycle
- Kotlin Standard Library

Ready for development and testing!
