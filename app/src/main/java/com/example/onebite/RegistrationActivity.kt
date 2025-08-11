package com.example.onebite

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.onebite.databinding.ActivityRegistrationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize binding
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Set status bar to transparent for immersive experience
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            handleRegistration()
        }

        binding.tvLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun handleRegistration() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Reset error states
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Validate input
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return
            }
            !isValidEmail(email) -> {
                binding.tilEmail.error = "Please enter a valid email"
                binding.etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return
            }
            !isStrongPassword(password) -> {
                binding.tilPassword.error = "Password must contain letters and numbers"
                binding.etPassword.requestFocus()
                return
            }
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Please confirm your password"
                binding.etConfirmPassword.requestFocus()
                return
            }
            password != confirmPassword -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                binding.etConfirmPassword.requestFocus()
                return
            }
        }

        // Show loading state
        showLoading(true)

        // Perform Firebase registration
        performFirebaseRegistration(email, password)
    }

    private fun performFirebaseRegistration(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    // Registration successful
                    val user = firebaseAuth.currentUser

                    // Send email verification (optional but recommended)
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Registration successful! Please check your email for verification.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Registration successful! You can now login.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Navigate to login activity
                        navigateToLoginWithSuccess()
                    }
                } else {
                    // Registration failed
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthUserCollisionException -> {
                "An account with this email already exists. Please login instead."
            }
            is FirebaseAuthWeakPasswordException -> {
                "Password is too weak. Please use a stronger password."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Invalid email format. Please enter a valid email address."
            }
            else -> {
                "Registration failed: ${exception?.message ?: "Unknown error"}"
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

        // If user already exists, suggest login
        if (exception is FirebaseAuthUserCollisionException) {
            binding.tvLogin.performClick()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Creating Account..." else "Register"

        // Disable input fields during loading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.tvLogin.isEnabled = !isLoading
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()

        // Add smooth transition
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToLoginWithSuccess() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("registration_success", true)
        intent.putExtra("user_email", binding.etEmail.text.toString().trim())
        startActivity(intent)
        finish()

        // Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isStrongPassword(password: String): Boolean {
        // Check if password contains both letters and numbers
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToLogin()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}