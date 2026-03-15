package com.ahmedprojects.flow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Task_Details : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAssignedBy: TextView
    private lateinit var tvAssignedTo: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvStatus: TextView

    private lateinit var btnRequestUpdate: Button
    private lateinit var btnMarkCompleted: Button
    private lateinit var memberUpdateSection: LinearLayout
    private lateinit var managerButtons: LinearLayout
    private lateinit var btnChooseImage: Button
    private lateinit var etUpdateMessage: EditText
    private lateinit var btnSubmitUpdate: Button
    private lateinit var imgPreview: ImageView

    private lateinit var rvUpdates: RecyclerView
    private lateinit var updatesAdapter: TaskUpdatesAdapter
    private val updatesList = mutableListOf<TaskUpdate>()

    private var selectedImageUri: Uri? = null
    private var taskId = -1
    private var userId = -1
    private var userRole = "member"

    private val IP = IP_String().IP
    private lateinit var db: AppDatabase

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imgPreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.task_details)

        db = AppDatabase.getInstance(this)
        taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)

        bindViews()
        setupRecyclerView()
        setupActions()
        loadTaskDetails()
        if (isNetworkAvailable()) {
            syncPendingUpdates()
        }
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tvTaskTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvAssignedBy = findViewById(R.id.tvAssignedBy)
        tvAssignedTo = findViewById(R.id.tvAssignedTo)
        tvDeadline = findViewById(R.id.tvDeadline)
        tvPriority = findViewById(R.id.tvPriority)
        tvStatus = findViewById(R.id.tvStatus)

        managerButtons = findViewById(R.id.managerButtons)
        btnRequestUpdate = findViewById(R.id.btnRequestUpdate)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)
        memberUpdateSection = findViewById(R.id.memberUpdateSection)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        etUpdateMessage = findViewById(R.id.etUpdateMessage)
        btnSubmitUpdate = findViewById(R.id.btnSubmitUpdate)
        imgPreview = findViewById(R.id.imgPreview)

        rvUpdates = findViewById(R.id.rvUpdates)
    }

    private fun setupRecyclerView() {
        rvUpdates.layoutManager = LinearLayoutManager(this)
        updatesAdapter = TaskUpdatesAdapter(updatesList)
        rvUpdates.adapter = updatesAdapter
    }

    private fun setupActions() {
        btnChooseImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSubmitUpdate.setOnClickListener { queueUpdate() }
        btnRequestUpdate.setOnClickListener { requestUpdate() }
        btnMarkCompleted.setOnClickListener { markTaskCompleted() }
    }

    /** Request update (manager functionality) */
    private fun requestUpdate() {
        val url = "$IP/request_update.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.optBoolean("success")) {
                    Toast.makeText(this, "Update requested successfully", Toast.LENGTH_SHORT).show()
                    loadTaskDetails()
                } else {
                    val message = response.optString("message", "Failed to request update")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.d("NETWORK_ERROR", "Request Update failed: ${error.message}")
            })
        queue.add(request)
    }

    /** Load task details from server, fetch names, save to cache */
    private fun loadTaskDetails() {
        if (!isNetworkAvailable()) {
            loadTaskDetailsFromCache()
            return
        }

        val url = "$IP/get_task_details.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (!response.getBoolean("success")) {
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    return@JsonObjectRequest
                }

                val taskObj = response.getJSONObject("task")
                val assignedById = taskObj.getInt("assigned_by")
                val assignedToId = taskObj.getInt("assigned_to")

                tvTitle.text = taskObj.getString("title")
                tvDescription.text = taskObj.getString("description")
                tvDeadline.text = taskObj.getString("deadline")
                tvPriority.text = taskObj.getString("priority").capitalize()
                tvStatus.text = taskObj.getString("status").replace("_", " ").capitalize()

                // Fetch names using old logic
                fetchUserName(assignedById) { assignedByName ->
                    tvAssignedBy.text = assignedByName
                    fetchUserName(assignedToId) { assignedToName ->
                        tvAssignedTo.text = assignedToName

                        // Save to Room cache
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.taskCacheDao().insertTask(
                                TaskCacheEntity(
                                    taskId = taskId,
                                    title = taskObj.getString("title"),
                                    description = taskObj.getString("description"),
                                    assignedBy = assignedById,
                                    assignedTo = assignedToId,
                                    assignedByName = assignedByName,
                                    assignedToName = assignedToName,
                                    deadline = taskObj.getString("deadline"),
                                    priority = taskObj.getString("priority"),
                                    status = taskObj.getString("status")
                                )
                            )
                        }
                    }
                }

                userRole = if (userId == assignedById) "manager" else "member"
                managerButtons.visibility = if (userRole == "manager") Button.VISIBLE else LinearLayout.GONE
                btnRequestUpdate.visibility = if (userRole == "manager") Button.VISIBLE else Button.GONE
                btnMarkCompleted.visibility = if (userRole == "manager") Button.VISIBLE else Button.GONE
                memberUpdateSection.visibility = if (userRole == "member") LinearLayout.VISIBLE else LinearLayout.GONE

                // Load updates
                updatesList.clear()
                val updatesArray = response.getJSONArray("updates")
                val cacheUpdates = mutableListOf<TaskUpdateCacheEntity>()
                for (i in 0 until updatesArray.length()) {
                    val u = updatesArray.getJSONObject(i)
                    val update = TaskUpdate(
                        id = u.getInt("id"),
                        userName = u.getString("user_name"),
                        message = u.optString("message", ""),
                        imageUrl = u.optString("image_url", ""),
                        createdAt = u.getString("created_at")
                    )
                    updatesList.add(update)
                    cacheUpdates.add(
                        TaskUpdateCacheEntity(
                            id = update.id,
                            taskId = taskId,
                            userName = update.userName,
                            message = update.message,
                            imageUrl = update.imageUrl,
                            createdAt = update.createdAt
                        )
                    )
                }
                updatesAdapter.notifyDataSetChanged()

                // Save updates cache
                lifecycleScope.launch(Dispatchers.IO) {
                    db.taskUpdateCacheDao().insertUpdates(cacheUpdates)
                }

            }, { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }

    /** Load task and updates from Room cache (offline) */
    private fun loadTaskDetailsFromCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            val taskCache = db.taskCacheDao().getTask(taskId)
            val updateCache = db.taskUpdateCacheDao().getUpdates(taskId)

            taskCache?.let {
                withContext(Dispatchers.Main) {
                    tvTitle.text = it.title
                    tvDescription.text = it.description
                    tvDeadline.text = it.deadline
                    tvPriority.text = it.priority.capitalize()
                    tvStatus.text = it.status.replace("_", " ").capitalize()
                    tvAssignedBy.text = it.assignedByName
                    tvAssignedTo.text = it.assignedToName

                    userRole = if (userId == it.assignedBy) "manager" else "member"
                    managerButtons.visibility = if (userRole == "manager") Button.VISIBLE else LinearLayout.GONE
                    btnRequestUpdate.visibility = if (userRole == "manager") Button.VISIBLE else Button.GONE
                    btnMarkCompleted.visibility = if (userRole == "manager") Button.VISIBLE else Button.GONE
                    memberUpdateSection.visibility = if (userRole == "member") LinearLayout.VISIBLE else LinearLayout.GONE
                }
            }

            updatesList.clear()
            updateCache.forEach { u ->
                updatesList.add(
                    TaskUpdate(
                        id = u.id,
                        userName = u.userName,
                        message = u.message,
                        imageUrl = u.imageUrl,
                        createdAt = u.createdAt
                    )
                )
            }
            withContext(Dispatchers.Main) { updatesAdapter.notifyDataSetChanged() }
        }
    }

    /** Old logic: fetch username by ID from API */
    private fun fetchUserName(userId: Int, callback: (String) -> Unit) {
        val url = "$IP/get_profile_username.php"
        val body = JSONObject().apply { put("userId", userId) }
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { res -> callback(if (res.optBoolean("success")) res.getString("name") else "Unknown") },
            { callback("Unknown") })
        Volley.newRequestQueue(this).add(req)
    }

    /** Queue update offline if no network, else send online */
    private fun queueUpdate() {
        val message = etUpdateMessage.text.toString().trim()
        if (message.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Please enter a message or select image", Toast.LENGTH_SHORT).show()
            return
        }

        var imageBase64 = ""
        selectedImageUri?.let { uri ->
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) { e.printStackTrace() }
        }

        val pendingUpdate = PendingTaskUpdateEntity(0, taskId, userId, message, imageBase64)
        lifecycleScope.launch(Dispatchers.IO) {
            db.pendingTaskUpdateDao().insertUpdate(pendingUpdate)
        }

        Toast.makeText(this, "Update queued", Toast.LENGTH_SHORT).show()
        etUpdateMessage.text.clear()
        selectedImageUri = null
        imgPreview.setImageDrawable(null)

        if (isNetworkAvailable()) {
            syncPendingUpdates()
        }
    }

    /** Send a single update online */
    private fun sendUpdateOnline(message: String, imageBase64: String) {
        val url = "$IP/submit_update.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
            put("message", message)
            put("image_url", imageBase64)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.optBoolean("success")) {
                    loadTaskDetails()
                }
            }, { error -> Log.d("NETWORK_ERROR", "Failed: ${error.message}") })
        queue.add(request)
    }

    /** Mark task as completed (manager) */
    private fun markTaskCompleted() {
        val url = "$IP/mark_task_completed.php"
        val jsonBody = JSONObject().apply { put("task_id", taskId); put("user_id", userId) }
        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.optBoolean("success")) loadTaskDetails()
            }, { error -> Log.d("NETWORK_ERROR", "Failed: ${error.message}") })
        queue.add(request)
    }

    /** Check network availability */
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    /** Sync all pending offline updates */
    private fun syncPendingUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            val pendingUpdates = db.pendingTaskUpdateDao().getAllUpdates()
            pendingUpdates.forEach { update ->
                try {
                    sendUpdateOnline(update.message, update.imageBase64)
                    db.pendingTaskUpdateDao().deleteUpdate(update)
                } catch (_: Exception) {}
            }
        }
    }

    /** Task update model */
    data class TaskUpdate(
        val id: Int,
        val userName: String,
        val message: String,
        val imageUrl: String,
        val createdAt: String
    )
}
