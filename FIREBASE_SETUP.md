# Firebase Setup Instructions

## Step 1: Download google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your **Forumus** project
3. Click on **Project Settings** (gear icon)
4. Scroll down to **Your apps** section
5. If you haven't added an Android app yet:
   - Click **Add app** and select Android
   - Enter package name: `com.anhkhoa.forumus_admin`
   - Download the `google-services.json` file
6. If the app already exists:
   - Find your app in the list
   - Click the **google-services.json** download button

## Step 2: Add google-services.json to your project

1. Place the downloaded `google-services.json` file in:
   ```
   app/google-services.json
   ```
2. **Important**: Replace the `google-services.json.template` file or place it alongside it

## Step 3: Sync Gradle

1. Click **Sync Now** in Android Studio when prompted
2. Or manually: **File > Sync Project with Gradle Files**

## What's Already Configured

✅ Firebase BOM (Bill of Materials) - version 33.6.0
✅ Firebase Firestore - for database operations
✅ Firebase Authentication - for user authentication
✅ Firebase Storage - for file storage
✅ Firebase Analytics - for app analytics
✅ Google Services plugin - for Firebase integration

## Firebase Services Ready to Use

- **Firestore Database**: Access your `otp_requests`, `otps`, and `users` collections
- **Authentication**: Manage user authentication
- **Storage**: Store and retrieve files
- **Analytics**: Track app usage and events

## Security Note

⚠️ **Never commit `google-services.json` to version control!**

Add this to your `.gitignore`:
```
app/google-services.json
```

## Verify Installation

After syncing Gradle, you should see:
- No build errors
- Firebase dependencies resolved
- Ready to initialize Firebase in your Kotlin code

## Next Steps

Once `google-services.json` is in place, you can:
1. Initialize Firebase in your Application class
2. Access Firestore database
3. Set up authentication
4. Configure Firebase Storage
