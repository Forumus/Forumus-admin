# Status Escalation System - Implementation Summary

## Overview
Automatic user status escalation when posts are deleted via AI moderation rejection or admin deletion of reported posts.

## Status Levels (Uppercase)
```
NORMAL → REMINDED → WARNED → BANNED
```

## How It Works

### Trigger Points
1. **AI Moderation Rejection**: When admin rejects a post in AI Moderation screen
2. **Reported Post Deletion**: When admin deletes a reported post

### What Happens
1. System gets the user's current status from Firebase
2. Calculates the next status level
3. Updates the user's `status` field in Firebase `users` collection
4. Shows notification to admin about the status change

### Example Flow
- User has status: `NORMAL`
- Their post is deleted
- Status automatically changes to: `REMINDED`
- If another post is deleted → status becomes: `WARNED`
- If another post is deleted → status becomes: `BANNED`
- Once `BANNED`, status stays `BANNED`

## Files Modified

### Core Logic
- **StatusHistory.kt**: Contains `UserStatusLevel` enum and `StatusEscalationResult`
- **UserStatusEscalationService.kt**: Handles escalation logic
- **UserRepository.kt**: Updates status in Firebase (uses uppercase values)

### Integration Points
- **AiModerationRepository.kt**: Calls escalation when post is rejected
- **ReportedPostsFragment.kt**: Calls escalation when reported post is deleted
- **AssistantViewModel.kt**: Handles UI notifications for escalation

## Database Structure

### Users Collection
```
users/
  {userId}/
    status: "NORMAL" | "REMINDED" | "WARNED" | "BANNED"
    email: string
    fullName: string
    role: string
    // other fields...
```

**Note**: Status values are stored in UPPERCASE in Firebase.

## Testing

1. Run the app
2. Go to AI Moderation screen → Reject a post
3. Or go to Reported Posts screen → Delete a post
4. Check Firebase Console → `users` collection → Author's `status` field should have changed
5. UI will show a toast message about the status change

## Key Features
✅ No separate `status_history` collection (simplified)
✅ Status stored directly in user document
✅ Uppercase status values: "NORMAL", "REMINDED", "WARNED", "BANNED"
✅ Automatic escalation on post deletion
✅ Users cannot escalate beyond BANNED
✅ Admin receives notification of status changes
