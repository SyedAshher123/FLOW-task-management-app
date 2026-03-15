package com.ahmedprojects.flow

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class project_members_page : AppCompatActivity() {

    private lateinit var adapter: ProjectMembersAdapter

    private var ip = IP_String()
    private val membersList = ArrayList<ProjectMembers>() // Correct data class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.project_members_page)

        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.membersRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        val projectId = intent.getIntExtra("project_id", -1)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)

        if (projectId == -1) {
            Toast.makeText(this, "Invalid Project ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchProjectMembers(projectId, currentUserId)
    }

    private fun fetchProjectMembers(projectId: Int, currentUserId: Int) {

        val url = ip.IP + "get_project_members.php"

        val jsonBody = JSONObject().apply {
            put("project_id", projectId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    if (!response.getBoolean("success")) {
                        Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show()
                        return@JsonObjectRequest
                    }

                    val members = response.getJSONArray("members")

                    membersList.clear()

                    for (i in 0 until members.length()) {
                        val item = members.getJSONObject(i)

                        val userId = item.getInt("user_id")

                        // Skip current logged-in user
                        if (userId == currentUserId) continue

                        membersList.add(
                            ProjectMembers(
                                userId = userId,
                                name = item.getString("name"),
                                email = item.getString("email"),
                                role = item.getString("role")
                            )
                        )
                    }

                    adapter = ProjectMembersAdapter(membersList,projectId)
                    findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.membersRecycler).adapter = adapter

                } catch (e: JSONException) {
                    Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(this).add(request)
    }
}
