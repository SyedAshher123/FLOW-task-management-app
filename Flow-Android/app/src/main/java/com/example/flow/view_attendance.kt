package com.ahmedprojects.flow

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class view_attendance : AppCompatActivity() {

    private val TAG = "ViewAttendance"

    // Views
    private lateinit var tvEmployeeName: TextView
    private lateinit var tvTotalHours: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var backBtn: ImageView

    // Data
    private var userId = -1
    private var projectId = -1
    private var memberName = ""
    private var ip = IP_String().IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_attendance)

        // --- Get Views ---
        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvTotalHours = findViewById(R.id.tvTotalHours)
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus) // Ensure you add this ID to XML
        backBtn = findViewById(R.id.backBtn)

        // --- Receive values from intent ---
        userId = intent.getIntExtra("user_id", -1)
        projectId = intent.getIntExtra("project_id", -1)
        memberName = intent.getStringExtra("name") ?: "Team Member"

        // --- Set UI ---
        tvEmployeeName.text = memberName

        backBtn.setOnClickListener { finish() }

        if (userId == -1 || projectId == -1) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Fetch Data ---
        fetchAttendanceData()
    }

    private fun fetchAttendanceData() {
        // We reuse the same API because it returns total_seconds + session_start
                                                                                                                                                                                                                    val url = "$ip/get_session.php"

        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "Response: $response")
                try {
                    val json = JSONObject(response)

                    if (json.getString("status") == "success") {

                        // 1. Get stored total seconds
                        var totalSeconds = json.optInt("total_seconds", 0)

                        // 2. Check if currently active (checked in)
                        val sessionStart = if (json.isNull("session_start") || json.getString("session_start") == "null") null else json.getString("session_start")

                        if (sessionStart != null) {
                            // User is currently working
                            tvCurrentStatus.text = "Status: Currently Active"
                            tvCurrentStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))

                            // Optional: Add current session duration to the total displayed
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val startTime = sdf.parse(sessionStart)
                                val now = Date()
                                val currentDuration = ((now.time - startTime!!.time) / 1000).toInt()
                                totalSeconds += currentDuration
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            // User is checked out
                            tvCurrentStatus.text = "Status: Offline"
                            tvCurrentStatus.setTextColor(resources.getColor(android.R.color.darker_gray))
                        }

                        // 3. Display formatted time
                        tvTotalHours.text = formatTime(totalSeconds)

                    } else {
                        // "No session found" usually means 0 hours
                        tvTotalHours.text = "00h 00m 00s"
                        tvCurrentStatus.text = "Status: No Activity"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }
        queue.add(request)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02dh %02dm %02ds".format(h, m, s)
    }
}