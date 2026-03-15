package com.ahmedprojects.flow

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView
import org.w3c.dom.Text
import kotlin.jvm.java

class home_page : AppCompatActivity() {

    private var ip: IP_String = IP_String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)  // enable edge-to-edge
        window.statusBarColor = Color.TRANSPARENT  // make status bar transparent

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        val userName = prefs.getString("name", "Guest")

        // Just to confirm it's working, show a toast (optional)
        Toast.makeText(this, "Logged in as: $userName (ID: $userId)", Toast.LENGTH_SHORT).show()

        var projectsBtn = findViewById<LinearLayout>(R.id.Projects)
        var tasksBtn = findViewById<LinearLayout>(R.id.Tasks)
        var notificationsBtn = findViewById<LinearLayout>(R.id.Notifications)
        var profilePic = findViewById<CircleImageView>(R.id.ivProfile)
        var greetingsName = findViewById<TextView>(R.id.tvHello)
        var chatbtn = findViewById<ImageView>(R.id.floatingChat)
        val btncheckin = findViewById<RelativeLayout>(R.id.btnCheckIn)
        var homeBtn = findViewById<LinearLayout>(R.id.homeBtn)

        loadUserProfilePhoto(userId, profilePic)
        loadUserName(userId, greetingsName)
        btncheckin.setOnClickListener {
            val intent = Intent(this,Select_project::class.java)
            startActivity(intent)
        }

        projectsBtn.setOnClickListener {
            val intent = Intent(this, projects::class.java)
            startActivity(intent)
        }
        tasksBtn.setOnClickListener {
            val intent = Intent(this, tasks_page::class.java)
            startActivity(intent)
        }
        chatbtn.setOnClickListener {
            val intent = Intent(this, all_chats_page::class.java)
            startActivity(intent)
        }
        notificationsBtn.setOnClickListener {
            val intent = Intent(this, notifications_page::class.java)
            startActivity(intent)
        }

        homeBtn.setOnClickListener {
            val intent = Intent(this, home_page::class.java)
            startActivity(intent)
        }




    }

    private fun loadUserProfilePhoto(userId: Int, profilePic: CircleImageView) {
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        val url = IP_String().IP + "get_profile_pic.php"

        val jsonBody = org.json.JSONObject()
        jsonBody.put("userId", userId)

        val request = com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {

                        val base64Image = response.getString("profile_photo")

                        if (!base64Image.isNullOrEmpty()) {
                            try {
                                val decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                                profilePic.setImageBitmap(bitmap)

                            } catch (e: Exception) {
                                Toast.makeText(this, "Error decoding image", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Could not retrieve profile photo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun loadUserName(userId: Int, greetings: TextView) {
        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        val url = IP_String().IP + "get_profile_username.php"

        val jsonBody = org.json.JSONObject()
        jsonBody.put("userId", userId)

        val request = com.android.volley.toolbox.JsonObjectRequest(
            com.android.volley.Request.Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {

                        val profileName = response.getString("name")

                        if (!profileName.isNullOrEmpty()) {
                            greetings.text = "Hello, $profileName"
                        } else {
                            Toast.makeText(this, "Name is empty!", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "Could not retrieve profile name", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }


}