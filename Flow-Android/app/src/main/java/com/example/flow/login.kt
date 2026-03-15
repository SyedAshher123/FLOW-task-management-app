package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging


class login : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: TextView
    private lateinit var signupLink: TextView

    private val BASE_URL = IP_String().IP   // <-- USING YOUR GLOBAL CLASS
    private val LOGIN_URL = BASE_URL + "login.php"

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        firebaseAuth = FirebaseAuth.getInstance() // Initialize Firebase Auth

        emailInput = findViewById(R.id.input_email)
        passwordInput = findViewById(R.id.input_password)
        loginBtn = findViewById(R.id.btn_login)
        signupLink = findViewById(R.id.signup_link)

        loginBtn.setOnClickListener {
            loginUser()
        }



        signupLink.setOnClickListener {
            startActivity(Intent(this, sign_up_page::class.java))
        }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        loginBtn.isEnabled = false
        loginBtn.text = "Logging in..."

        // ------------------- Firebase Login -------------------
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Firebase Login Successful", Toast.LENGTH_SHORT).show()

                    // ------------------- Continue Local Login -------------------
                    val queue = Volley.newRequestQueue(this)

                    val jsonBody = JSONObject()
                    jsonBody.put("email", email)
                    jsonBody.put("password", password)

                    val request = JsonObjectRequest(
                        Request.Method.POST,
                        LOGIN_URL,
                        jsonBody,
                        { response ->
                            try {
                                if (response.getBoolean("success")) {
                                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                                    val user = response.getJSONObject("user")
                                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                                    prefs.edit().apply {
                                        putInt("id", user.getInt("id"))
                                        putString("name", user.getString("name"))
                                        putString("email", user.getString("email"))
                                        apply()
                                    }
                                    val userId = user.getInt("id")
                                    saveFcmToken(userId)
                                    startActivity(Intent(this, home_page::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                                Log.d("PAKROMUJHE", "Invalid server response: $e")
                            }

                            loginBtn.isEnabled = true
                            loginBtn.text = "Login"
                        },
                        { error ->
                            Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                            Log.d("PAKROMUJHE", "Network error: ${error.message}")
                            loginBtn.isEnabled = true
                            loginBtn.text = "Login"
                        }
                    )

                    queue.add(request)

                } else {
                    Toast.makeText(this, "Firebase Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    loginBtn.isEnabled = true
                    loginBtn.text = "Login"
                }
            }
    }

    private fun saveFcmToken(userId: Int) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val url = IP_String().IP + "save_fcm_token.php"
            val queue = Volley.newRequestQueue(this)

            val json = JSONObject()
            json.put("user_id", userId)
            json.put("fcm_token", token)

            val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                json,
                { Log.d("FCM", "Token saved") },
                { Log.e("FCM", "Failed to save token") }
            )

            queue.add(request)
        }
    }
}
