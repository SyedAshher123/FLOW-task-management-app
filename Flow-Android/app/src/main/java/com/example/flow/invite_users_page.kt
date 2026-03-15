package com.ahmedprojects.flow

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class invite_users_page : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnInvite: RelativeLayout
    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: UserSearchAdapter

    private val userList = ArrayList<UserModel>()
    private val ip = IP_String().IP
    private var projectId = -1
    private var senderId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.invite_users_page)

        etSearch = findViewById(R.id.etSearch)
        etEmail = findViewById(R.id.etEmail)
        btnInvite = findViewById(R.id.btnInvite)
        rvResults = findViewById(R.id.rvResults)

        projectId = intent.getIntExtra("project_id", -1)
        senderId = getSharedPreferences("user_session", MODE_PRIVATE).getInt("id", -1)

        adapter = UserSearchAdapter(userList) { selectedUser ->
            etEmail.setText(selectedUser.email)
        }

        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        // Live search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.length >= 1) searchUsers(s.toString())
            }
        })

        // Send invite
        btnInvite.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendInvite(email)
        }
    }

    private fun searchUsers(query: String) {
        val url = ip + "search_users.php"
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply {
            put("search", query)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->
                if (response.getBoolean("success")) {
                    val arr = response.getJSONArray("users")
                    userList.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        userList.add(
                            UserModel(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                email = obj.getString("email")
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }
            },
            {
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun sendInvite(email: String) {
        val url = ip + "send_invite.php"
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply {
            put("project_id", projectId)
            put("sender_id", senderId)
            put("email", email)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, json,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Invite sent!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }
}
