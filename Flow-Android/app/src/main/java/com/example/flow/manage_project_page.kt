package com.ahmedprojects.flow

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class manage_project_page : AppCompatActivity() {

    private lateinit var tvProjectTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvOwner: TextView
    private lateinit var tvJoinCode: TextView
    private lateinit var btnInviteUsers: RelativeLayout
    private lateinit var rvTasks: RecyclerView
    private lateinit var btnMembers: RelativeLayout
    private lateinit var fabCreateTask: FloatingActionButton
    private lateinit var projectPfp: CircleImageView

    private var ip = IP_String().IP
    private var isManagerOrOwner = false   // NEW flag set in checkUserRole()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_project_page)

        // Views
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvOwner = findViewById(R.id.tvOwner)
        tvJoinCode = findViewById(R.id.tvJoinCode)
        btnInviteUsers = findViewById(R.id.btnInviteUsers)
        rvTasks = findViewById(R.id.rvTasks)
        btnMembers = findViewById(R.id.btnMembers)
        fabCreateTask = findViewById(R.id.fabCreateTask)
        projectPfp = findViewById(R.id.projectPfp)

        rvTasks.layoutManager = LinearLayoutManager(this)
        fabCreateTask.hide()

        val projectId = intent.getIntExtra("project_id", -1)
        if (projectId == -1) {
            Toast.makeText(this, "Invalid Project!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnInviteUsers.setOnClickListener {
            val intent = Intent(this, invite_users_page::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
        }

        // Load project details
        loadProjectDetails(projectId)

        // Determine role (this will call fetchActiveTasks after role is known)
        checkUserRole(projectId)

        btnMembers.setOnClickListener {
            val intent = Intent(this, project_members_page::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
        }

        fabCreateTask.setOnClickListener {
            val intent = Intent(this, create_task::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
        }
    }

    private fun loadProjectDetails(projectId: Int) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)

        val url = ip + "get_project_details.php"
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply { put("projectId", projectId) }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->
                if (response.getBoolean("success")) {
                    val name = response.getString("name")
                    val description = response.getString("description")
                    val ownerName = response.getString("owner_name")
                    val joinCode = response.getString("join_code")
                    val ownerId = response.getInt("owner_id")

                    tvProjectTitle.text = name
                    tvDescription.text = description
                    tvOwner.text = ownerName
                    tvJoinCode.text = if (currentUserId == ownerId) joinCode else "******"

                    // load project picture if present
                    if (response.has("picture_base64") && !response.isNull("picture_base64")) {
                        val base64String = response.getString("picture_base64")
                        if (base64String.isNotEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                projectPfp.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                projectPfp.setImageResource(R.drawable.placeholder_pfp)
                            }
                        } else {
                            projectPfp.setImageResource(R.drawable.placeholder_pfp)
                        }
                    } else {
                        projectPfp.setImageResource(R.drawable.placeholder_pfp)
                    }
                } else {
                    Toast.makeText(this, "Failed to load project", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network error: ${it.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    /** CHECK USER ROLE AND THEN LOAD APPROPRIATE TASKS */
    private fun checkUserRole(projectId: Int) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)

        val url = ip + "get_project_members.php"
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply { put("project_id", projectId) }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->
                if (!response.getBoolean("success")) {
                    // still attempt to fetch tasks as 'member' if api failed
                    isManagerOrOwner = false
                    fetchActiveTasks(projectId)
                    return@JsonObjectRequest
                }

                val members = response.getJSONArray("members")

                for (i in 0 until members.length()) {
                    val member = members.getJSONObject(i)

                    if (member.getInt("user_id") == currentUserId) {
                        val role = member.getString("role")
                        if (role == "owner" || role == "manager") {
                            fabCreateTask.show()
                            isManagerOrOwner = true
                        }
                        break
                    }
                }

                // After knowing role -> fetch tasks
                fetchActiveTasks(projectId)

            },
            {
                Toast.makeText(this, "Failed to verify role", Toast.LENGTH_SHORT).show()
                // fallback: still fetch tasks as non-manager
                isManagerOrOwner = false
                fetchActiveTasks(projectId)
            })

        queue.add(request)
    }

    /** FETCH TASKS BASED ON ROLE */
    /** FETCH ACTIVE TASKS OF THIS PROJECT */
    private fun fetchActiveTasks(projectId: Int) {

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)
        val url = ip + "get_project_active_tasks.php"
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply {
            put("project_id", projectId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->

                if (!response.getBoolean("success")) {
                    Toast.makeText(this, "No active tasks found", Toast.LENGTH_SHORT).show()
                    return@JsonObjectRequest
                }

                val tasksArray = response.getJSONArray("tasks")
                val taskList = mutableListOf<TaskModel>()

                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)

                    val assignedTo = obj.getInt("assigned_to")
                    val taskOwnerId = obj.getInt("assigned_by") // Optional, can use owner check

                    // Show only tasks for current user if not owner/manager
                    // Assume currentUser is owner/manager if currentUserId == projectOwnerId
                    val projectOwnerId = obj.optInt("project_owner_id", -1)
                    val isOwnerOrManager = currentUserId == projectOwnerId

                    if (!isOwnerOrManager && assignedTo != currentUserId) {
                        continue
                    }

                    val task = TaskModel(
                        id = obj.getInt("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        priority = obj.getString("priority"),
                        status = obj.getString("status"),
                        assignedBy = obj.getInt("assigned_by"),
                        organisationName = obj.getString("project_name"),
                        updateRequested = obj.optInt("update_requested", 0) == 1,
                        dueDate = obj.optString("deadline", ""),
                        percentageCompleted = obj.optInt("completion", 0),
                        assignedByName = "" // will fetch if needed
                    )

                    taskList.add(task)
                }

                // Set adapter with click listener
                rvTasks.adapter = TaskAdapter(taskList, { task ->
                    val intent = Intent(this, Task_Details::class.java)
                    intent.putExtra("task_id", task.id)
                    startActivity(intent)
                }, IP_String().IP)


            },
            {
                Toast.makeText(this, "Failed to fetch tasks", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

}
