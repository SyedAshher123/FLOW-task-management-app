package com.ahmedprojects.flow

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class sign_up_page : AppCompatActivity() {

    private lateinit var fullNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var createAccountBtn: TextView
    private lateinit var termsCheck: CheckBox
    private lateinit var profileImage: ImageView
    private lateinit var signinLink: TextView

    private var selectedImageBase64: String? = null

    private val BASE_URL = IP_String().IP
    private val SIGNUP_URL = BASE_URL + "signup.php"

    // Firebase Auth instance
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up_page)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        fullNameInput = findViewById(R.id.input_fullname)
        emailInput = findViewById(R.id.input_email)
        passwordInput = findViewById(R.id.input_password)
        confirmPasswordInput = findViewById(R.id.input_confirm_password)
        createAccountBtn = findViewById(R.id.btn_create_account)
        termsCheck = findViewById(R.id.checkbox_terms)
        profileImage = findViewById(R.id.profileImage)
        signinLink = findViewById(R.id.signin_link)

        profileImage.setOnClickListener { selectImage() }

        createAccountBtn.setOnClickListener { signupUser() }

        signinLink.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }
    }

    // ---------------------- SELECT IMAGE ------------------------

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            profileImage.setImageBitmap(bitmap)
            selectedImageBase64 = encodeToBase64(bitmap)
        }
    }

    private fun encodeToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    }

    // ---------------------- SIGNUP FUNCTION ------------------------

    private fun signupUser() {
        val fullName = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (!termsCheck.isChecked) {
            Toast.makeText(this, "You must accept the Terms and Privacy Policy", Toast.LENGTH_SHORT).show()
            return
        }

        createAccountBtn.isEnabled = false
        createAccountBtn.text = "Creating Account..."

        // ------------------- Firebase Signup -------------------
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase signup success
                    Toast.makeText(this, "Firebase Account created!", Toast.LENGTH_SHORT).show()

                    // Now continue to save data to your local PHP API
                    val jsonData = JSONObject()
                    jsonData.put("name", fullName)
                    jsonData.put("email", email)
                    jsonData.put("password", password)
                    jsonData.put("profile_photo", selectedImageBase64 ?: JSONObject.NULL)

                    val request = JsonObjectRequest(
                        Request.Method.POST,
                        SIGNUP_URL,
                        jsonData,
                        { response ->
                            if (response.getBoolean("success")) {
                                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, login::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                            }

                            createAccountBtn.isEnabled = true
                            createAccountBtn.text = "Create Account"
                        },
                        { error ->
                            Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            createAccountBtn.isEnabled = true
                            createAccountBtn.text = "Create Account"
                        }
                    )

                    Volley.newRequestQueue(this).add(request)

                } else {
                    // Firebase signup failed
                    Toast.makeText(this, "Firebase Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    createAccountBtn.isEnabled = true
                    createAccountBtn.text = "Create Account"
                }
            }
    }
}
