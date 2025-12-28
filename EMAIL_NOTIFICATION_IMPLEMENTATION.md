# Email Notification Feature Implementation

## Overview
This feature automatically sends email notifications to users whenever their account status changes in the admin app. The emails are context-aware and adjust their tone based on whether the status is improving (de-escalation) or worsening (escalation).

## Status Levels (Severity Order)
1. **NORMAL** - No violations, good standing
2. **REMINDED** - Minor violations, gentle reminder sent
3. **WARNED** - Serious violations, warning issued
4. **BANNED** - Account suspended, cannot access the platform

## Email Notification Types

### 1. Escalation Emails (Status Worsening)
Sent when user status increases in severity:
- **NORMAL → REMINDED**: Gentle reminder about community guidelines
- **REMINDED → WARNED**: Warning about serious violations
- **WARNED → BANNED**: Account banned notification

**Tone**: Firm, informative, and preventive. Warns about consequences.

### 2. De-escalation Emails (Status Improving)
Sent when user status decreases in severity:
- **BANNED → WARNED**: Congratulations on behavior improvement
- **WARNED → REMINDED**: Positive reinforcement
- **REMINDED → NORMAL**: Congratulations on returning to good standing

**Tone**: Congratulatory, encouraging, and positive. Reinforces good behavior.

## Implementation Components

### 1. Data Models (`data/model/email/`)
- **ReportEmailRequest.kt**: Request payload for email API
- **EmailResponse.kt**: Response from email API
- **ReportedPost.kt**: Information about posts that triggered status change

### 2. API Layer (`data/api/`)
- **EmailApiService.kt**: Retrofit interface for email endpoints
- **RetrofitClient.kt**: Singleton HTTP client with base URL configuration

### 3. Service Layer (`data/service/`)
- **EmailNotificationService.kt**: Core service for sending status change emails
  - `sendStatusChangeEmail()`: Generic method for any status change
  - `sendEscalationEmail()`: Specifically for status worsening
  - `sendDeEscalationEmail()`: Specifically for status improvement
  - `compareStatusSeverity()`: Determines if status is improving or worsening

### 4. Integration Points

#### UserStatusEscalationService
Automatically sends email when AI moderation or admin reports escalate user status:
```kotlin
// After successful status update in Firestore
emailNotificationService.sendEscalationEmail(
    userEmail = user.email,
    userName = user.fullName,
    newStatus = newUserStatus,
    reportedPosts = null // Can include specific posts
)
```

#### BlacklistFragment
Sends email when admin manually changes user status:
```kotlin
// Determines escalation vs de-escalation
if (isStatusIncreasing(oldStatus, newStatus)) {
    emailNotificationService.sendEscalationEmail(...)
} else {
    emailNotificationService.sendDeEscalationEmail(...)
}
```

## Backend Integration

### Backend API Endpoint
```
POST /api/email/send-report
```

### Request Payload
```json
{
  "recipientEmail": "user@example.com",
  "userName": "John Doe",
  "userStatus": "WARNED",
  "reportedPosts": [
    {
      "postId": "123",
      "title": "Post Title",
      "reason": "Violation reason"
    }
  ]
}
```

### Response
```json
{
  "success": true,
  "message": "Report email sent successfully"
}
```

## Configuration

### Backend URL
Update the base URL in `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/" // Android emulator
// OR
private const val BASE_URL = "https://your-backend-url.com/" // Production
```

### Testing with Emulator
- Use `http://10.0.2.2:8080/` to connect to localhost on host machine
- Ensure backend server is running on port 8080

### Testing with Physical Device
- Use `http://YOUR_LOCAL_IP:8080/` (e.g., `http://192.168.1.100:8080/`)
- Ensure both device and server are on same network

## Error Handling

### Non-Blocking Email Failures
Email notifications are intentionally non-blocking:
- If email fails, the status update still succeeds
- Errors are logged but don't prevent core functionality
- Users are notified via Toast about status change success

```kotlin
try {
    val emailResult = emailNotificationService.sendEscalationEmail(...)
    if (emailResult.isSuccess) {
        Log.d(TAG, "Email sent successfully")
    } else {
        Log.w(TAG, "Failed to send email (non-blocking)")
    }
} catch (e: Exception) {
    Log.w(TAG, "Error sending email (non-blocking)", e)
    // Continue execution - don't block status update
}
```

## Testing Checklist

### Manual Testing
- [ ] **Escalation**: NORMAL → REMINDED
  - Verify email sent with reminder tone
  - Check email contains appropriate warning message
  
- [ ] **Escalation**: REMINDED → WARNED
  - Verify email sent with warning tone
  - Check email emphasizes seriousness
  
- [ ] **Escalation**: WARNED → BANNED
  - Verify email sent with ban notification
  - Check email states account is suspended
  
- [ ] **De-escalation**: BANNED → WARNED
  - Verify email sent with congratulatory tone
  - Check email encourages continued improvement
  
- [ ] **De-escalation**: WARNED → REMINDED
  - Verify email sent with positive reinforcement
  
- [ ] **De-escalation**: REMINDED → NORMAL
  - Verify email sent congratulating return to good standing

### Integration Testing
- [ ] Test from UserStatusEscalationService (AI moderation trigger)
- [ ] Test from BlacklistFragment (manual admin action)
- [ ] Test email validation (invalid emails rejected)
- [ ] Test network failure scenarios (non-blocking)
- [ ] Test backend API connection

## Dependencies Added
```kotlin
// Retrofit for REST API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

## Permissions Required
Already included in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Future Enhancements
1. **Include Specific Posts**: Pass actual reported posts to email
2. **Email Templates**: Customize email content per status type
3. **Retry Logic**: Add exponential backoff for failed emails
4. **Email Queue**: Queue emails for offline scenarios
5. **Push Notifications**: Add FCM notifications alongside emails
6. **Analytics**: Track email delivery rates and user responses
7. **Localization**: Support multiple languages in emails

## Troubleshooting

### Email Not Sending
1. Check backend server is running
2. Verify BASE_URL in RetrofitClient.kt
3. Check logs for error messages
4. Verify user has valid email address
5. Test backend endpoint directly with Postman

### Status Update Success but No Email
- This is expected behavior (non-blocking)
- Check logs for warnings about email failures
- Verify backend email service is configured correctly

### Network Timeout
- Increase timeout values in RetrofitClient.kt
- Check network connectivity
- Verify firewall rules allow connections

## Related Files
- Backend: `Forumus-server/src/main/java/com/hcmus/forumus_backend/controller/EmailController.java`
- Backend: `Forumus-server/src/main/java/com/hcmus/forumus_backend/service/EmailService.java`
- Backend DTO: `Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/email/ReportEmailRequest.java`
