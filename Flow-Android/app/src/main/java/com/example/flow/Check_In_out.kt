package com.ahmedprojects.flow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Check_In_out : AppCompatActivity() {

    private val TAG = "CheckInOut"

    private var ip = IP_String().IP
    private lateinit var tvProjectName: TextView
    private lateinit var btnCheckIn: LinearLayout
    private lateinit var btnCheckOut: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var tvTotalTimeWorked: TextView
    private lateinit var backBtn : ImageView
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView


    private var userId = -1
    private var projectId = -1
    private var projectName = ""

    // Variables to track time
    private var sessionStart: String? = null
    private var dbTotalSeconds: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateActiveAndTotalTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.check_in_out)

        // 1. Get User Session
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Initialize Views
        tvProjectName = findViewById(R.id.tvProjectName)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        backBtn = findViewById(R.id.backBtn)
        tvStatus = findViewById(R.id.tvStatus)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvTotalTimeWorked = findViewById(R.id.tvTotalTimeWorked)
        tvTime = findViewById(R.id.tvTime)
        tvDate = findViewById(R.id.tvDate)

        // 3. Get Intent Data
        projectId = intent.getIntExtra("project_id", -1)
        projectName = intent.getStringExtra("project_name") ?: ""
        tvProjectName.text = projectName

        // 4. Listeners
        btnCheckIn.setOnClickListener { startSession() }
        btnCheckOut.setOnClickListener { endSession() }
        backBtn.setOnClickListener { finish() }
        dateTimeHandler.post(dateTimeRunnable)

        // 5. Check Initial State
        getSession()
    }

    // -------------------- API Calls --------------------
    private val dateTimeHandler = Handler(Looper.getMainLooper())
    private val dateTimeRunnable = object : Runnable {
        override fun run() {

            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

            val now = Date()

            tvTime.text = timeFormat.format(now)
            tvDate.text = dateFormat.format(now)

            dateTimeHandler.postDelayed(this, 1000)
        }
    }

    private fun getSession() {
        val url = "$ip/get_session.php"

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "getSession response: $response")
                try {
                    val obj = JSONObject(response)

                    if (obj.getString("status") == "success") {
                        // Handle "session_start" being actual JSON null or "null" string
                        if (obj.isNull("session_start") || obj.getString("session_start") == "null") {
                            sessionStart = null
                        } else {
                            sessionStart = obj.getString("session_start")
                        }

                        // Get total seconds previously worked
                        dbTotalSeconds = obj.optInt("total_seconds", 0)

                        // LOGIC: If sessionStart has a value, we are Checked In.
                        if (!sessionStart.isNullOrEmpty()) {
                            showCheckOut() // Show Check Out button
                            handler.post(timeRunnable) // Start the UI timer
                        } else {
                            showCheckIn() // Show Check In button
                            updateStaticTotalTime() // Just show total time without ticking
                        }

                    } else {
                        // If status is error (e.g. "No session found"), implies fresh start
                        // Assume no record exists -> Show Check In
                        sessionStart = null
                        dbTotalSeconds = 0
                        showCheckIn()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // On parse error, default to safe state (Check In)
                    showCheckIn()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                showCheckIn()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun startSession() {
        val url = "$ip/start_session.php"

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
                        // Refresh session data to get the exact server time for session_start
                        getSession()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error -> error.printStackTrace() }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun endSession() {
        val url = "$ip/end_session.php"
        Log.d(TAG, "endSession called - URL: $url")

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "endSession response: $response")
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()

                        // 1. Reset Session Start
                        sessionStart = null

                        // 2. Update Total Seconds from the Fresh DB calculation
                        dbTotalSeconds = obj.optInt("total_seconds", dbTotalSeconds)

                        // 3. Update UI
                        handler.removeCallbacks(timeRunnable)
                        showCheckIn()
                        updateStaticTotalTime() // This will now show the correct incremented time
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Error parsing endSession response", e)
                    Toast.makeText(this, "Error ending session", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Log.e(TAG, "Network error in endSession", error)
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    // -------------------- UI Updates --------------------

    private fun showCheckIn() {
        btnCheckIn.visibility = View.VISIBLE
        btnCheckOut.visibility = View.GONE
        tvStatus.text = "Status: Checked Out"
        tvStatus.setTextColor(resources.getColor(android.R.color.darker_gray))
        tvActiveTime.visibility = View.INVISIBLE // Hide active timer
    }

    private fun showCheckOut() {
        btnCheckIn.visibility = View.GONE
        btnCheckOut.visibility = View.VISIBLE
        tvStatus.text = "Status: Checked In"
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        tvActiveTime.visibility = View.VISIBLE // Show active timer
    }

    /**
     * Called every second when user is Checked In.
     * Updates "Active Time" and "Total Worked".
     */
    private fun updateActiveAndTotalTime() {
        if (!sessionStart.isNullOrEmpty()) {
            try {
                // 1. Calculate Active Duration (Now - StartTime)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                // Ensure server time zone matches or is handled. Here assuming local/server match.
                val startTime = sdf.parse(sessionStart!!)
                val now = Date()

                // Difference in seconds
                val currentSessionSeconds = ((now.time - startTime.time) / 1000).toInt()

                // 2. Format Active Time
                tvActiveTime.text = "Current Session: " + formatSeconds(currentSessionSeconds)

                // 3. Calculate Total (Stored DB time + Current Session)
                val totalWithCurrent = dbTotalSeconds + currentSessionSeconds
                tvTotalTimeWorked.text = "Total Worked: " + formatSeconds(totalWithCurrent)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating time", e)
            }
        }
    }

    /**
     * Called when user is Checked Out.
     * Just displays the DB total time, no ticking calculation.
     */
    private fun updateStaticTotalTime() {
        tvTotalTimeWorked.text = "Total Worked: " + formatSeconds(dbTotalSeconds)
        tvActiveTime.text = "Current Session: 00h 00m 00s"
    }

    // Helper to format seconds into HH:MM:SS
    private fun formatSeconds(totalSecs: Int): String {
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return "%02dh %02dm %02ds".format(hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeRunnable)
        dateTimeHandler.removeCallbacks(dateTimeRunnable)

    }
}