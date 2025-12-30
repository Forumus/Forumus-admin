# Push Notification Quick Reference

## Quick Overview

Push notifications are automatically sent to users when:
- ✅ Admin/AI **deletes** a post → User notified
- ✅ Admin/AI **approves** a post → User notified
- ✅ Admin/System **changes** user status → User notified

## Usage Examples

### Send Post Deleted Notification
```kotlin
pushNotificationService.sendPostDeletedNotification(
    postId = "post123",
    postAuthorId = "user456",
    postTitle = "My Post Title",
    reason = "Community guidelines violation",
    isAiDeleted = false // false = admin, true = AI
)
```

### Send Post Approved Notification
```kotlin
pushNotificationService.sendPostApprovedNotification(
    postId = "post123",
    postAuthorId = "user456",
    postTitle = "My Post Title",
    isAiApproved = false // false = admin, true = AI
)
```

### Send Status Changed Notification
```kotlin
pushNotificationService.sendStatusChangedNotification(
    userId = "user456",
    oldStatus = "WARNED",
    newStatus = "NORMAL"
)
```

## Notification Types

| Type | Trigger | Actor | Message |
|------|---------|-------|---------|
| `POST_DELETED` | Admin/AI deletes post | Admin/System | "Your post was removed: [reason]" |
| `POST_APPROVED` | Admin/AI approves post | Admin/System | "Your post has been approved!" |
| `STATUS_CHANGED` | Status changes | Admin/System | Status-specific message |
| `POST_REJECTED` | Admin/AI rejects post | Admin/System | "Your post was rejected: [reason]" |

## Status Messages

| Status | Message |
|--------|---------|
| NORMAL | "Your account status has been restored to normal. Welcome back!" |
| REMINDED | "You've received a reminder about community guidelines." |
| WARNED | "Your account has received a warning. Please review our guidelines." |
| BANNED | "Your account has been suspended due to policy violations." |

## Where Notifications Are Sent

### 1. ReportedPostsFragment
- **Action**: Admin deletes reported post
- **Notification**: POST_DELETED
- **Trigger**: After successful deletion

### 2. AssistantViewModel (AI Assistant)
- **Action**: Admin approves AI-flagged post
- **Notification**: POST_APPROVED
- **Trigger**: After successful approval

### 3. BlacklistFragment
- **Action**: Admin changes user status
- **Notification**: STATUS_CHANGED
- **Trigger**: After successful status update

### 4. UserStatusEscalationService
- **Action**: AI/System auto-escalates status
- **Notification**: STATUS_CHANGED
- **Trigger**: After automatic escalation

## API Endpoint

```
POST http://your-backend.com/api/notifications

Request Body:
{
  "type": "POST_DELETED",
  "actorId": "ADMIN",
  "actorName": "Admin",
  "targetId": "post123",
  "targetUserId": "user456",
  "previewText": "Your post was removed..."
}

Response:
"Notification triggered successfully"
```

## Error Handling

All notifications are **non-blocking**:
```kotlin
try {
    pushNotificationService.sendPostDeletedNotification(...)
} catch (e: Exception) {
    Log.w("TAG", "Failed to send notification (non-blocking)", e)
}
// Main action continues regardless
```

## Testing Commands

### Test Backend Notification API
```bash
curl -X POST http://localhost:3000/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "POST_DELETED",
    "actorId": "ADMIN",
    "actorName": "Admin",
    "targetId": "test123",
    "targetUserId": "user456",
    "previewText": "Test notification"
  }'
```

## Key Files

| File | Purpose |
|------|---------|
| [PushNotificationService.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/service/PushNotificationService.kt) | Core notification logic |
| [NotificationTriggerRequest.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/model/notification/NotificationTriggerRequest.kt) | Request model |
| [NotificationApiService.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/api/EmailApiService.kt) | Retrofit API |
| [RetrofitClient.kt](Forumus-admin/app/src/main/java/com/hcmus/forumus_admin/data/api/RetrofitClient.kt) | HTTP client |

## Actor Types

| Actor | When Used |
|-------|-----------|
| `ADMIN` | Manual admin actions |
| `SYSTEM` | Automated AI/system actions |

## Integration Pattern

```kotlin
// 1. Import service
private val pushNotificationService = PushNotificationService.getInstance()

// 2. After successful action
result.onSuccess {
    // Send notification (non-blocking)
    try {
        pushNotificationService.sendPostDeletedNotification(...)
    } catch (e: Exception) {
        Log.w(TAG, "Notification failed (non-blocking)", e)
    }
    
    // Continue with UI updates
    Toast.makeText(context, "Action completed", Toast.LENGTH_SHORT).show()
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No notifications | Check backend running, BASE_URL correct |
| Partial notifications | Check logs for specific failures |
| 400/500 errors | Validate request payload, check backend logs |
| FCM token missing | User needs to login on mobile app |

## Backend Requirements

- ✅ Backend must be running on configured port
- ✅ `/api/notifications` endpoint available
- ✅ User must have FCM token in Firestore
- ✅ Network connectivity required

## Mobile App Integration

For notifications to work end-to-end:
1. ✅ Admin app sends notification via API
2. ✅ Backend stores notification in Firestore
3. ✅ Backend sends FCM push to user's device
4. ⚠️ Mobile app must handle FCM notifications (implemented separately)

## Next Steps

1. Ensure backend server is running
2. Test notification API endpoint
3. Verify notifications appear in Firestore
4. Test on physical device with FCM token
5. Verify mobile app displays notifications
