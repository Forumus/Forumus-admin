# Email Notification Quick Reference

## Quick Setup

### 1. Configure Backend URL
Edit `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/" // For emulator
// OR
private const val BASE_URL = "https://your-server.com/" // For production
```

### 2. Status Change Flow
```
Status Escalation (↑ severity):
NORMAL → REMINDED → WARNED → BANNED
  └─ Sends warning/reminder emails

Status De-escalation (↓ severity):
BANNED → WARNED → REMINDED → NORMAL
  └─ Sends congratulatory emails
```

## Key Files

### Created Files
```
app/src/main/java/com/hcmus/forumus_admin/data/
├── api/
│   ├── EmailApiService.kt          # Retrofit API interface
│   └── RetrofitClient.kt            # HTTP client configuration
├── model/email/
│   ├── ReportEmailRequest.kt        # Email request model
│   ├── EmailResponse.kt             # Email response model
│   └── ReportedPost.kt             # Reported post info
└── service/
    └── EmailNotificationService.kt  # Email notification logic
```

### Modified Files
```
app/build.gradle.kts                                    # Added Retrofit dependencies
app/src/main/java/.../service/UserStatusEscalationService.kt  # Added email on auto-escalation
app/src/main/java/.../ui/blacklist/BlacklistFragment.kt       # Added email on manual change
```

## Usage Examples

### Automatic Escalation (AI/Reports)
```kotlin
// In UserStatusEscalationService.kt
emailNotificationService.sendEscalationEmail(
    userEmail = user.email,
    userName = user.fullName,
    newStatus = newUserStatus,
    reportedPosts = null
)
```

### Manual Status Change (Admin)
```kotlin
// In BlacklistFragment.kt
if (isStatusIncreasing(oldStatus, newStatus)) {
    emailNotificationService.sendEscalationEmail(...)
} else {
    emailNotificationService.sendDeEscalationEmail(...)
}
```

## API Endpoint

### Backend
```
POST http://your-backend.com/api/email/send-report

Request Body:
{
  "recipientEmail": "user@example.com",
  "userName": "John Doe",
  "userStatus": "WARNED",
  "reportedPosts": []
}

Response:
{
  "success": true,
  "message": "Report email sent successfully"
}
```

## Email Types by Status

| New Status | Email Type | Tone |
|-----------|-----------|------|
| NORMAL | Congratulatory | ✅ Positive, encouraging |
| REMINDED | Reminder/Congrats | ⚠️ Gentle (escalation) or 🎉 Positive (de-escalation) |
| WARNED | Warning/Improvement | ⚠️ Serious (escalation) or 📈 Encouraging (de-escalation) |
| BANNED | Ban Notice | 🚫 Final (escalation only) |

## Testing Commands

### Start Backend Server
```bash
cd Forumus-server
./mvnw spring-boot:run
```

### Test Email API
```bash
curl -X POST http://localhost:8080/api/email/send-report \
  -H "Content-Type: application/json" \
  -d '{
    "recipientEmail": "test@example.com",
    "userName": "Test User",
    "userStatus": "WARNED",
    "reportedPosts": []
  }'
```

## Status Severity Comparison
```kotlin
NORMAL = 0    (lowest severity)
REMINDED = 1
WARNED = 2
BANNED = 3    (highest severity)

Escalation: newLevel > oldLevel
De-escalation: newLevel < oldLevel
```

## Error Handling
- ✅ Non-blocking: Email failures don't prevent status updates
- ✅ Logged: All errors logged for debugging
- ✅ Graceful: User still sees success toast for status change

## Dependencies
```kotlin
// Added to app/build.gradle.kts
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Email not sending | Check BASE_URL in RetrofitClient.kt |
| Network timeout | Increase timeout in RetrofitClient.kt |
| Backend not reachable | Use 10.0.2.2 for emulator, local IP for device |
| Invalid email | Service validates with Android Patterns.EMAIL_ADDRESS |
| Status not updating | Check Firestore permissions |

## Next Steps
1. ✅ Sync Gradle (if prompted)
2. ✅ Update BASE_URL to your backend server
3. ✅ Run backend server
4. ✅ Test status changes in admin app
5. ✅ Check logs for email delivery confirmation
