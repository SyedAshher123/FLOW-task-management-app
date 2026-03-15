package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class notifications_page : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val ip = IP_String().IP  // your base API URL



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications_page)

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvNotifications = findViewById(R.id.recyclerViewNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(notificationList, ip)
        rvNotifications.adapter = adapter

        fetchUserInvites()
        var tasksBtn = findViewById<LinearLayout>(R.id.tasksBtn)
        var notificationsBtn = findViewById<LinearLayout>(R.id.notificationsBtn)
        var homeBtn = findViewById<LinearLayout>(R.id.homeBtn)
        var projectBtn = findViewById<LinearLayout>(R.id.projectsBtn)

        projectBtn.setOnClickListener {
            val intent = Intent(this, projects::class.java)
            startActivity(intent)
        }
        tasksBtn.setOnClickListener {
            val intent = Intent(this, tasks_page::class.java)
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

    private fun fetchUserInvites() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)
        if (currentUserId == -1) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "$ip/get_user_invites.php"
        val queue = Volley.newRequestQueue(this)

        val jsonBody = JSONObject().apply {
            put("receiver_id", currentUserId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val invitesJson = response.getJSONArray("invites")
                        notificationList.clear()

                        for (i in 0 until invitesJson.length()) {
                            val obj = invitesJson.getJSONObject(i)
                            val notification = NotificationModel(
                                inviteId   = obj.getInt("invite_id"),
                                projectId  = obj.getInt("project_id"),
                                senderId   = obj.getInt("sender_id"),
                                receiverId = obj.getInt("receiver_id"),
                                senderName = obj.getString("sender_name"),
                                projectName= obj.getString("project_name"),
                                timestamp  = obj.getString("timestamp")
                            )
                            notificationList.add(notification)
                        }

                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this, "No notifications", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to parse notifications", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("NOTIFICATIONS_ERROR", error.toString())
            }
        )

        queue.add(request)
    }
}
