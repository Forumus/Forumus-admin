package com.hcmus.forumus_admin

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

import android.content.Context
import com.hcmus.forumus_admin.core.LocaleHelper

class ForumusAdminApplication : Application() {
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Sign in anonymously to get Firestore access
        authenticateApp()
    }
    
    private fun authenticateApp() {
        val auth = FirebaseAuth.getInstance()
        
        // Check if already signed in
        if (auth.currentUser != null) {
            Log.d("ForumusAdmin", "Already authenticated: ${auth.currentUser?.uid}")
            return
        }
        
        // Sign in anonymously
        auth.signInAnonymously()
            .addOnSuccessListener { authResult ->
                Log.d("ForumusAdmin", "Anonymous authentication successful: ${authResult.user?.uid}")
            }
            .addOnFailureListener { exception ->
                Log.e("ForumusAdmin", "Anonymous authentication failed", exception)
            }
    }
}
