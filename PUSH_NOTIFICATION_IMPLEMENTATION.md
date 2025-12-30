# Push Notification Feature Implementation

## Overview
This feature automatically sends push notifications to users via Firebase Cloud Messaging (FCM) whenever:
- **Admin or AI deletes a post** - User is notified about post removal
- **Admin or AI approves a post** - User is notified about post approval
- **Admin or AI changes user status** - User is notified about account status change

## Backend Integration

### Available API Endpoint
```
POST /api/notifications
```

### Request Payload
```json
{
  "type": "POST_DELETED | POST_APPROVED | STATUS_CHANGED | POST_REJECTED",
  "actorId": "ADMIN | SYSTEM | <userId>",
  "actorName": "Admin | Forumus System",
  "targetId": "<postId | userId>",
  "targetUserId": "<userId to notify>",
  "previewText": "Notification message preview"
}
```

## Implementation Components

### 1. Data Models (`data/model/notification/`)

#### NotificationTriggerRequest.kt
Request payload matching backend DTO:
```kotlin
data class NotificationTriggerRequest(
    val type: String,
    val actorId: String,
    val actorName: String,
    val targetId: String,
    val targetUserId: String,
    val previewText: String
)
```

#### NotificationType Object
Notification type constants:
- `POST_DELETED` - Post removed by admin/AI
- `POST_APPROVED` - Post approved by admin/AI
- `POST_REJECTED` - Post rejected by admin/AI
- `STATUS_CHANGED` - User account status changed

### 2. API Layer (`data/api/`)

#### NotificationApiService.kt
Retrofit interface for notification endpoint:
```kotlin
interface NotificationApiService {
    @POST("api/notifications")
    suspend fun triggerNotification(
        @Body request: NotificationTriggerRequest
    ): Response<String>
}
```

#### RetrofitClient.kt
Added notification API instance:
```kotlin
val notificationApi: NotificationApiService by lazy {
    retrofit.create(NotificationApiService::class.java)
}
```

### 3. Service Layer (`data/service/`)

#### PushNotificationService.kt
Core service for sending push notifications with methods:

**sendPostDeletedNotification()**
- Notifies user when their post is deleted
- Parameters: postId, postAuthorId, postTitle, reason, isAiDeleted
- Actor: "Admin" or "Forumus System" (AI)

**sendPostApprovedNotification()**
- Notifies user when their post is approved
- Parameters: postId, postAuthorId, postTitle, isAiApproved
- Actor: "Admin" or "Forumus System" (AI)

**sendPostRejectedNotification()**
- Notifies user when their post is rejected
- Parameters: postId, postAuthorId, postTitle, reason, isAiRejected
- Actor: "Admin" or "Forumus System" (AI)

**sendStatusChangedNotification()**
- Notifies user when account status changes
- Parameters: userId, oldStatus, newStatus
- Generates context-aware messages based on new status:
  - NORMAL: "Your account status has been restored to normal. Welcome back!"
  - REMINDED: "You've received a reminder about community guidelines."
  - WARNED: "Your account has received a warning. Please review our guidelines."
  - BANNED: "Your account has been suspended due to policy violations."

## Integration Points

### 1. Reported Posts - Admin Deletes Post
**File**: [ReportedPostsFragment.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/ui/reported/ReportedPostsFragment.kt)

```kotlin
// After successful post deletion
pushNotificationService.sendPostDeletedNotification(
    postId = post.id,
    postAuthorId = post.authorId,
    postTitle = post.title,
    reason = "Reported by community members",
    isAiDeleted = false
)
```

### 2. AI Assistant - Admin Approves Post
**File**: [AssistantViewModel.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/ui/assistant/AssistantViewModel.kt)

```kotlin
// After successful post approval
pushNotificationService.sendPostApprovedNotification(
    postId = post.id,
    postAuthorId = post.author,
    postTitle = post.title,
    isAiApproved = false // Admin approved
)
```

### 3. Blacklist - Admin Changes User Status
**File**: [BlacklistFragment.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/ui/blacklist/BlacklistFragment.kt)

```kotlin
// After successful status update
pushNotificationService.sendStatusChangedNotification(
    userId = user.uid,
    oldStatus = oldStatus.name,
    newStatus = newStatus.name
)
```

### 4. Auto-Escalation - AI/System Changes Status
**File**: [UserStatusEscalationService.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/service/UserStatusEscalationService.kt)

```kotlin
// After automatic status escalation
pushNotificationService.sendStatusChangedNotification(
    userId = userId,
    oldStatus = currentStatusLevel.value,
    newStatus = nextStatusLevel.value
)
```

## Notification Flow

### Post Deletion Flow
```
1. Admin clicks "Delete" on reported post
2. Post deleted from Firestore
3. Push notification sent to post author
4. User status escalated (if applicable)
5. Email notification sent (if applicable)
6. UI updated with success message
```

### Post Approval Flow
```
1. Admin clicks "Approve" in AI Assistant
2. Post status updated in Firestore
3. Push notification sent to post author
4. Post appears in user's feed
5. UI updated with success message
```

### Status Change Flow
```
1. Admin changes user status in Blacklist
2. Status updated in Firestore
3. Push notification sent to user
4. Email notification sent to user
5. UI updated with new status
```

## Error Handling

### Non-Blocking Design
All notification operations are non-blocking:
```kotlin
try {
    pushNotificationService.sendPostDeletedNotification(...)
} catch (e: Exception) {
    Log.w(TAG, "Failed to send notification (non-blocking)", e)
}
// Continue execution regardless of notification success
```

**Benefits**:
- Core operations (delete, approve, status change) always succeed
- Notification failures don't block user actions
- Errors logged for debugging
- User sees success message for main action

## Backend Behavior

### Notification Processing
1. **Validation**: Checks targetUserId exists
2. **Self-Action Skip**: Doesn't notify if actor = target user
3. **Firestore Storage**: Saves notification to user's collection
4. **FCM Push**: Sends push notification if user has FCM token
5. **Offline Support**: Notification stored even if push fails

### Notification Storage
```
Firestore Path: users/{targetUserId}/notifications/{notificationId}

Document Structure:
{
  id: "uuid",
  type: "POST_DELETED",
  actorId: "ADMIN",
  actorName: "Admin",
  targetId: "post123",
  previewText: "Your post was removed...",
  createdAt: Timestamp,
  isRead: false
}
```

## Testing Checklist

### Manual Testing

#### Post Deletion
- [ ] Admin deletes reported post
- [ ] User receives push notification
- [ ] Notification shows correct post title
- [ ] Notification includes deletion reason
- [ ] Post author's status escalates

#### Post Approval
- [ ] Admin approves AI-flagged post
- [ ] User receives push notification
- [ ] Notification shows "approved" message
- [ ] Post becomes visible in feed

#### Status Changes
- [ ] Admin changes status: NORMAL → REMINDED
  - User receives reminder notification
  
- [ ] Admin changes status: REMINDED → WARNED
  - User receives warning notification
  
- [ ] Admin changes status: WARNED → BANNED
  - User receives ban notification
  
- [ ] Admin changes status: BANNED → NORMAL
  - User receives congratulatory notification

#### AI Actions
- [ ] AI rejects post automatically
- [ ] User receives notification with "System" as actor
- [ ] Status escalates automatically
- [ ] Notification sent for status change

### Integration Testing
- [ ] Test with valid FCM token
- [ ] Test with no FCM token (should not fail)
- [ ] Test with offline user (notification stored)
- [ ] Test notification appears in app
- [ ] Test notification deep linking (if implemented in mobile app)

### Error Scenarios
- [ ] Backend API unavailable
- [ ] Invalid user ID
- [ ] Network timeout
- [ ] Invalid notification type

All should log errors but not block main operations.

## Configuration

### Actor IDs and Names
```kotlin
// System (AI) actions
const val SYSTEM_ACTOR_ID = "SYSTEM"
const val SYSTEM_ACTOR_NAME = "Forumus System"

// Admin actions
const val ADMIN_ACTOR_ID = "ADMIN"
const val ADMIN_ACTOR_NAME = "Admin"
```

### Notification Types
```kotlin
const val POST_DELETED = "POST_DELETED"
const val POST_APPROVED = "POST_APPROVED"
const val STATUS_CHANGED = "STATUS_CHANGED"
const val POST_REJECTED = "POST_REJECTED"
```

## Dependencies
No additional dependencies required - uses existing Retrofit setup.

## Permissions
Uses existing internet permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## User Experience

### Notification Display
Users will receive push notifications on their mobile devices:
- **Title**: Generated based on notification type
- **Body**: Preview text with post title or status message
- **Action**: Tap to open app (deep link if implemented)

### Notification Types User Sees

**Post Deleted**:
```
Title: Post Removed
Body: Your post "..." was removed: Community guidelines violation
```

**Post Approved**:
```
Title: Post Approved
Body: Your post "..." has been approved and is now live!
```

**Status Changed**:
```
Title: Account Status Update
Body: Your account status has been restored to normal. Welcome back!
```

## Future Enhancements
1. **Batch Notifications**: Send multiple notifications in one call
2. **Rich Notifications**: Include images, action buttons
3. **Notification Preferences**: Let users customize notification types
4. **In-App Notifications**: Show notifications within admin app
5. **Notification History**: Track sent notifications in admin panel
6. **Analytics**: Track notification delivery and open rates
7. **Retry Logic**: Automatic retry for failed notifications
8. **Rate Limiting**: Prevent notification spam

## Troubleshooting

### Notifications Not Received
1. Check backend server is running
2. Verify RetrofitClient BASE_URL is correct
3. Check user has valid FCM token in Firestore
4. Verify network connectivity
5. Check backend logs for errors

### Partial Notifications
- Some notifications sent, others not
- Check logs for specific failures
- Verify user IDs are correct
- Check Firestore user documents exist

### Backend Returns 400/500
- Validate request payload matches backend DTO
- Check backend logs for validation errors
- Verify all required fields are provided

## Related Files

### Admin App
- [PushNotificationService.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/service/PushNotificationService.kt)
- [NotificationTriggerRequest.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/model/notification/NotificationTriggerRequest.kt)
- [NotificationApiService.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/api/EmailApiService.kt)
- [RetrofitClient.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/api/RetrofitClient.kt)

### Backend
- [NotificationController.java](Forumus-server/src/main/java/com/hcmus/forumus_backend/controller/NotificationController.java)
- [NotificationService.java](Forumus-server/src/main/java/com/hcmus/forumus_backend/service/NotificationService.java)
- [FCMService.java](Forumus-server/src/main/java/com/hcmus/forumus_backend/service/FCMService.java)
- [NotificationTriggerRequest.java](Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/notification/NotificationTriggerRequest.java)
