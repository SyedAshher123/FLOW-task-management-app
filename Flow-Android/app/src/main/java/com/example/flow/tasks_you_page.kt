package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class tasks_you_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private val tasksByYou = mutableListOf<TaskModel>()

    private var userId = -1
    private val IP = IP_String().IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.tasks_you_page)

        setupBottomNav()
        setupUserId()
        setupInsets()
        setupRecyclerView()
        setupTabs()
        setupStatusFilters()

        loadTasksOfflineThenOnline()
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.homeBtn).setOnClickListener {
            startActivity(Intent(this, home_page::class.java))
        }
        findViewById<LinearLayout>(R.id.projectsBtn).setOnClickListener {
            startActivity(Intent(this, projects::class.java))
        }
    }

    private fun setupUserId() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(mutableListOf(), onClick = { task ->
            val intent = Intent(this, Task_Details::class.java)
            intent.putExtra("task_id", task.id)
            startActivity(intent)
        })
        recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        val tabForYou: TextView = findViewById(R.id.tabForYou)
        val tabByYou: TextView = findViewById(R.id.tabByYou)

        tabForYou.setOnClickListener {
            startActivity(Intent(this, tasks_page::class.java))
            finish()
        }

        tabByYou.setTextColor(resources.getColor(R.color.blue))
        tabForYou.setTextColor(resources.getColor(R.color.gray))
    }

    private fun setupStatusFilters() {
        val all: TextView = findViewById(R.id.all)
        val pending: TextView = findViewById(R.id.pending)
        val completed: TextView = findViewById(R.id.completed)

        all.setOnClickListener {
            adapter.updateTasks(tasksByYou)
            highlightTab(all, pending, completed)
        }
        pending.setOnClickListener {
            val filtered = tasksByYou.filter { it.status.equals("pending", true) }
            adapter.updateTasks(filtered)
            highlightTab(pending, all, completed)
        }
        completed.setOnClickListener {
            val filtered = tasksByYou.filter { it.status.equals("completed", true) }
            adapter.updateTasks(filtered)
            highlightTab(completed, all, pending)
        }
    }

    private fun highlightTab(active: TextView, vararg others: TextView) {
        active.setBackgroundResource(R.drawable.oblong_blue)
        active.setTextColor(resources.getColor(R.color.white))
        others.forEach {
            it.setBackgroundResource(R.drawable.oblong_white)
            it.setTextColor(resources.getColor(R.color.blue))
        }
    }

    // 🔹 Load offline tasks first, filter by assignedBy == userId
    private fun loadTasksOfflineThenOnline() {
        val db = AppDatabase.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            val offlineTasks = db.taskDao().getTasksByUser(userId)
            val filteredOffline = offlineTasks.filter { it.assignedBy == userId }
            tasksByYou.clear()
            tasksByYou.addAll(filteredOffline.map { it.toTaskModel() })

            withContext(Dispatchers.Main) {
                adapter.updateTasks(tasksByYou)
            }

            // Fetch online tasks
            fetchTasksOnline(db)
        }
    }

    private fun fetchTasksOnline(db: AppDatabase) {
        val url = "$IP/get_tasks_by_you.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                parseTasksAndFetchNames(db, response)
            },
            { error ->
                Toast.makeText(this, "Failed to fetch tasks: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("API_FETCH_TASKS", "Failed: ${error.message}")
            }
        )
        queue.add(request)
    }

    // 🔹 Parse tasks, fetch assignedByName asynchronously, and store in DB
    private fun parseTasksAndFetchNames(db: AppDatabase, response: JSONObject) {
        if (!response.getBoolean("success")) return

        val tasksArray = response.getJSONArray("tasks")
        tasksByYou.clear()

        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)

            val task = TaskModel(
                id = obj.getInt("id"),
                title = obj.getString("title"),
                description = obj.getString("description"),
                priority = obj.getString("priority"),
                status = obj.getString("status"),
                assignedBy = obj.getInt("assigned_by"),
                assignedByName = "Loading...",
                organisationName = obj.getString("project_name"),
                updateRequested = obj.optInt("update_requested", 0) == 1,
                percentageCompleted = obj.optInt("completion", 0),
                dueDate = obj.optString("deadline", "N/A")
            )

            if (obj.getInt("assigned_by") == userId) tasksByYou.add(task)

            // Fetch assignedByName and update DB
            fetchUserName(task.assignedBy) { name ->
                task.assignedByName = name
                runOnUiThread { adapter.updateTasks(tasksByYou) }

                CoroutineScope(Dispatchers.IO).launch {
                    db.taskDao().insertTasks(listOf(task.toEntity(userId)))
                }
            }
        }

        // Initial update before names fetched
        runOnUiThread {
            adapter.updateTasks(tasksByYou)
            highlightTab(findViewById(R.id.all), findViewById(R.id.pending), findViewById(R.id.completed))
        }
    }

    // 🔹 Fetch username helper
    private fun fetchUserName(userId: Int, onResult: (String) -> Unit) {
        val url = "$IP/getUserName.php"
        val queue = Volley.newRequestQueue(this)
        val json = JSONObject().apply { put("userId", userId) }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->
                val name = if (response.getBoolean("success")) response.getString("name") else "Unknown"
                onResult(name)
            },
            { _ -> onResult("Error") }
        )
        queue.add(request)
    }
}

// 🔹 Extension helpers
fun TaskEntity.toTaskModel() = TaskModel(
    id = id,
    title = title,
    description = description,
    priority = priority,
    status = status,
    assignedBy = assignedBy,
    assignedByName = assignedByName,
    organisationName = organisationName,
    updateRequested = updateRequested,
    percentageCompleted = percentageCompleted,
    dueDate = dueDate
)

fun TaskModel.toEntity(userId: Int) = TaskEntity(
    id = id,
    title = title,
    description = description,
    priority = priority,
    status = status,
    assignedBy = assignedBy,
    assignedByName = assignedByName,
    assignedTo = userId,
    organisationName = organisationName,
    updateRequested = updateRequested,
    percentageCompleted = percentageCompleted,
    dueDate = dueDate
)
