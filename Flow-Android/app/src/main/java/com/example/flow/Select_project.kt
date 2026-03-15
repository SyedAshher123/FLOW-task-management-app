package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class Select_project : AppCompatActivity() {

    private var ip = IP_String()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProjectAdapter
    private val projectList = ArrayList<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_project)

        recyclerView = findViewById(R.id.orgRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProjectAdapter(
            projectList,
            onProjectClick = { selectedProject ->
                // When user selects a project → go to attendance page
                val intent = Intent(this, Check_In_out::class.java)
                intent.putExtra("project_id", selectedProject.id)
                intent.putExtra("project_name", selectedProject.name)
                startActivity(intent)
            },
            onDeleteClick = { /* no-op */ } // just pass empty lambda since delete is not needed here
        )

        recyclerView.adapter = adapter

        // Load projects for this user
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserProjects(userId)
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
                            //Toast.makeText(this, "Offline and no cached projects", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        queue.add(request)
    }
}
