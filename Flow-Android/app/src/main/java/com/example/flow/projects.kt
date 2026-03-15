package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class projects : AppCompatActivity() {

    private var ip: IP_String = IP_String()
    private lateinit var etJoinCode: EditText
    private lateinit var joinBtn: LinearLayout
    private lateinit var createBtn: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProjectAdapter
    private val projectList = ArrayList<Project>()

    // Bottom nav buttons
    private lateinit var homeBtn: LinearLayout
    private lateinit var projectsBtn: LinearLayout
    private lateinit var tasksBtn: LinearLayout
    private lateinit var notificationsBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.projects)

        // --- Initialize views ---
        etJoinCode = findViewById(R.id.etJoinCode)
        joinBtn = findViewById(R.id.joinBtn)
        createBtn = findViewById(R.id.createBtn)
        recyclerView = findViewById(R.id.orgRecyclerView)

        homeBtn = findViewById(R.id.homeBtn)
        projectsBtn = findViewById(R.id.projectsBtn)
        tasksBtn = findViewById(R.id.tasksBtn)
        notificationsBtn = findViewById(R.id.notificationsBtn)

        // OR, if you prefer IDs, add IDs to each LinearLayout in XML:
        // android:id="@+id/homeBtn"
        // android:id="@+id/projectsBtn"
        // etc.
        // then simply use findViewById(R.id.homeBtn)



        // --- Load user's projects ---
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid user session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadUserProjects(userId)

        adapter = ProjectAdapter(projectList,
            onProjectClick = { clickedProject ->
                val intent = Intent(this, manage_project_page::class.java)
                intent.putExtra("project_id", clickedProject.id)
                startActivity(intent)
            },
            onDeleteClick = { project ->
                deleteProject(userId, project.id)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter



        // --- Join project functionality ---
        joinBtn.setOnClickListener {
            val joinCode = etJoinCode.text.toString().trim()
            if (joinCode.isEmpty()) {
                Toast.makeText(this, "Enter a project code", Toast.LENGTH_SHORT).show()
            } else {
                joinProject(userId, joinCode)
            }
        }

        // --- Create project functionality ---
        createBtn.setOnClickListener {
            val intent = Intent(this, create_project_page::class.java)
            startActivity(intent)
        }

        // --- Bottom navigation functionality ---
        homeBtn.setOnClickListener {
            val intent = Intent(this, home_page::class.java)
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

        notificationsBtn.setOnClickListener {
            val intent = Intent(this, notifications_page::class.java)
            startActivity(intent)
        }

    }


    private fun loadUserProjects(userId: Int) {
        val db = AppDatabase.getInstance(this)
        val projectDao = db.projectDao()

        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "get_user_projects.php"

        val jsonBody = JSONObject().apply {
            put("userId", userId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val projectsJson = response.getJSONArray("projects")
                        val list = ArrayList<Project>()

                        val roomList = ArrayList<ProjectEntity>()

                        for (i in 0 until projectsJson.length()) {
                            val obj = projectsJson.getJSONObject(i)

                            val pictureBase64 = if (obj.has("picture_base64") && !obj.isNull("picture_base64")) {
                                obj.getString("picture_base64")
                            } else null

                            val p = Project(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                description = obj.getString("description"),
                                role = obj.getString("role"),
                                membersCount = obj.getInt("membersCount"),
                                pictureUrl = pictureBase64
                            )

                            list.add(p)

                            // Optional: save to Room if you want offline caching (without picture)
                            roomList.add(
                                ProjectEntity(
                                    id = p.id,
                                    name = p.name,
                                    description = p.description,
                                    ownerId = -1,
                                    joinCode = "",
                                    role = p.role,
                                    membersCount = p.membersCount
                                )
                            )
                        }

                        projectList.clear()
                        projectList.addAll(list)
                        adapter.notifyDataSetChanged()

                        // save to local DB
                        lifecycleScope.launch {
                            projectDao.clearProjects()
                            projectDao.insertProjects(roomList)
                        }
                    }
                } catch (_: Exception) {
                    Toast.makeText(this, "Failed to parse projects", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val cached = projectDao.getAllProjects()
                    if (cached.isNotEmpty()) {
                        val mapped = cached.map {
                            Project(
                                id = it.id,
                                name = it.name,
                                description = it.description,
                                role = it.role,
                                membersCount = it.membersCount,
                                pictureUrl = null // Room cache does not store pictureBase64
                            )
                        }
                        launch(Dispatchers.Main) {
                            projectList.clear()
                            projectList.addAll(mapped)
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@projects, "Offline and no cached projects", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        queue.add(request)
    }


    private fun joinProject(userId: Int, joinCode: String) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "join_project.php"

        val jsonBody = JSONObject().apply {
            put("userId", userId)
            put("joinCode", joinCode)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(this, "Joined project!", Toast.LENGTH_SHORT).show()
                        loadUserProjects(userId) // refresh list
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("JOIN_PROJECT_ERROR", error.toString())
            })
        queue.add(request)
    }

    private fun deleteProject(userId: Int, projectId: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "delete_project.php"

        val jsonBody = JSONObject().apply {
            put("userId", userId) // verify owner on server
            put("projectId", projectId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Project deleted!", Toast.LENGTH_SHORT).show()
                    loadUserProjects(userId)
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

}
