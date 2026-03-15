package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
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

class tasks_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    private val tasksForYou = mutableListOf<TaskModel>()
    private var userId = -1
    private val IP = IP_String().IP

    private lateinit var homeBtn: LinearLayout
    private lateinit var projectsBtn: LinearLayout
    private lateinit var tasksBtn: LinearLayout
    private lateinit var notificationsBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.tasks_page)

        setupBottomNav()
        setupUserId()
        setupInsets()
        setupRecyclerView()
        setupTabs()
        setupStatusFilters()

        loadTasksOfflineThenOnline()

        homeBtn = findViewById(R.id.homeBtn)
        projectsBtn = findViewById(R.id.projectsBtn)
        tasksBtn = findViewById(R.id.tasksBtn)
        notificationsBtn = findViewById(R.id.notificationsBtn)


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
            adapter.updateTasks(tasksForYou)
            tabForYou.setTextColor(resources.getColor(R.color.blue))
            tabByYou.setTextColor(resources.getColor(R.color.gray))
            highlightTab(findViewById(R.id.all), findViewById(R.id.pending), findViewById(R.id.completed))
        }

        tabByYou.setOnClickListener {
            startActivity(Intent(this, tasks_you_page::class.java))
        }
    }

    private fun setupStatusFilters() {
        val all: TextView = findViewById(R.id.all)
        val pending: TextView = findViewById(R.id.pending)
        val completed: TextView = findViewById(R.id.completed)

        all.setOnClickListener {
            adapter.updateTasks(tasksForYou)
            highlightTab(all, pending, completed)
        }
        pending.setOnClickListener {
            val filtered = tasksForYou.filter { it.status.equals("pending", true) }
            adapter.updateTasks(filtered)
            highlightTab(pending, all, completed)
        }
        completed.setOnClickListener {
            val filtered = tasksForYou.filter { it.status.equals("completed", true) }
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

    // 🔹 Load offline tasks first, then online
    private fun loadTasksOfflineThenOnline() {
        val db = AppDatabase.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            // Load offline tasks assigned TO user
            val offlineTasks = db.taskDao().getTasksForUser(userId)
            val filteredOffline = offlineTasks.filter { it.assignedBy != userId }
            tasksForYou.clear()
            tasksForYou.addAll(filteredOffline.map { it.toModel() })

            withContext(Dispatchers.Main) {
                adapter.updateTasks(tasksForYou)
            }

            // Fetch online tasks
            fetchTasksOnline(db)
        }
    }

    private fun fetchTasksOnline(db: AppDatabase) {
        val url = "$IP/get_user_tasks.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                parseTasksAndFetchNames(db, response)
            },
            { error ->
                Toast.makeText(this, "Failed to fetch tasks: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    // 🔹 Parse tasks, fetch assignedByName asynchronously, save to DB
    private fun parseTasksAndFetchNames(db: AppDatabase, response: JSONObject) {
        if (!response.getBoolean("success")) return

        val tasksArray = response.getJSONArray("tasks")
        tasksForYou.clear()

        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            val assignedTo = obj.getInt("assigned_to")
            if (assignedTo != userId) continue // filter offline & online properly

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
            tasksForYou.add(task)

            // Fetch assignedByName
            fetchUserName(task.assignedBy) { name ->
                task.assignedByName = name
                runOnUiThread { adapter.updateTasks(tasksForYou) }

                // Save to local DB
                CoroutineScope(Dispatchers.IO).launch {
                    db.taskDao().insertTasks(listOf(task.toEntity(userId)))
                }
            }
        }

        // Initial update
        runOnUiThread {
            adapter.updateTasks(tasksForYou)
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
fun TaskEntity.toModel() = TaskModel(
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

