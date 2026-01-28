package com.hcmus.forumus_admin.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupListeners()
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        if (auth.currentUser != null) {
            checkAdminRole(auth.currentUser!!.uid)
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            // Basic validation
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            binding.tilEmail.error = null

            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            }
            binding.tilPassword.error = null

            // Show loading
            showLoading(true)

            // Firebase Login
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    checkAdminRole(authResult.user?.uid)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.cbRemember.setOnClickListener {
            binding.cbRemember.isChecked = !binding.cbRemember.isChecked
        }
        
        binding.tvForgotPassword.setOnClickListener {
             val email = binding.etEmail.text.toString().trim()
             if (email.isNotEmpty()) {
                 auth.sendPasswordResetEmail(email)
                     .addOnSuccessListener {
                         Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                     }
                     .addOnFailureListener {
                         Toast.makeText(this, "Failed to send reset email: ${it.message}", Toast.LENGTH_SHORT).show()
                     }
             } else {
                 binding.tilEmail.error = "Enter email to reset password"
             }
        }
    }

    private fun checkAdminRole(userId: String?) {
        if (userId == null) {
            showLoading(false)
            return
        }

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    if (role == "ADMIN") {
                        showLoading(false)
                        navigateToDashboard()
                    } else {
                        // Not an admin
                        auth.signOut()
                        showLoading(false)
                        Toast.makeText(this, "Access Denied: You do not have administrator privileges.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    auth.signOut()
                    showLoading(false)
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                auth.signOut()
                showLoading(false)
                Toast.makeText(this, "Failed to verify role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnLogin.text = ""
            binding.progressBarLogin.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
        } else {
            binding.btnLogin.text = "Login"
            binding.progressBarLogin.visibility = View.GONE
            binding.btnLogin.isEnabled = true
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't go back to login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
