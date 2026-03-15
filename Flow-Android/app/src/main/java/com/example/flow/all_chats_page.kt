package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ahmedprojects.flow.adapters.ChatAdapter
import com.ahmedprojects.flow.models.ChatPreview
import org.json.JSONArray
import org.json.JSONObject

class all_chats_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var chatList = mutableListOf<ChatPreview>()

    private lateinit var backButton: ImageView
    private lateinit var newChatButton: ImageView
    private lateinit var usernamePreview: TextView

    private val ipString = IP_String().IP  // Use IP from IP_String.kt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.all_chats_page)

        recyclerView = findViewById(R.id.chat_list_recycler)
        backButton = findViewById(R.id.back_button)
        newChatButton = findViewById(R.id.new_chat_button)
        usernamePreview = findViewById(R.id.username_text)

        setupRecyclerView()

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1).toString()  // Flow stores id as int

        if (currentUserId == "-1") {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchCurrentUsername(currentUserId)
        fetchChats(currentUserId)

        backButton.setOnClickListener {
            startActivity(Intent(this, home_page::class.java))
            finish()
        }

        newChatButton.setOnClickListener {
            startActivity(Intent(this, add_chats_page::class.java))
            finish()
        }

        /*findViewById<LinearLayout>(R.id.search_bar).setOnClickListener {
            startActivity(Intent(this, SearchPage::class.java))
        }*/
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(chatList) { selectedChat ->
            val intent = Intent(this, chat_page::class.java)
            intent.putExtra("receiverId", selectedChat.userId)
            intent.putExtra("receiverUsername", selectedChat.username)
            intent.putExtra("receiverProfileBase64", selectedChat.profileImageBase64)
            startActivity(intent)
        }
        recyclerView.adapter = chatAdapter
    }

    private fun fetchCurrentUsername(currentUserId: String) {
        val url = "${ipString}fetch_current_user.php?user_id=$currentUserId"
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val username = response.getJSONObject("user").getString("name")
                    usernamePreview.text = username
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching username: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        requestQueue.add(jsonObjectRequest)
    }

    private fun fetchChats(currentUserId: String) {
        val url = "${ipString}fetch_chats.php"
        val body = JSONObject().apply { put("current_user_id", currentUserId) }

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                chatList.clear()
                if (response.getString("status") == "success") {
                    val chatsArray: JSONArray = response.getJSONArray("chats")
                    for (i in 0 until chatsArray.length()) {
                        val chatObj = chatsArray.getJSONObject(i)
                        val chat = ChatPreview(
                            userId = chatObj.getString("user_id"),
                            username = chatObj.getString("username"),
                            displayName = chatObj.getString("display_name"),
                            profileImageBase64 = chatObj.getString("profile_picture_url"),
                            lastMessage = chatObj.optString("last_message", "Tap to start chatting"),
                            lastMessageTime = chatObj.optString("last_message_time", "")
                        )
                        chatList.add(chat)
                    }
                    chatAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "No chats found", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching chats: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        requestQueue.add(jsonObjectRequest)
    }
}
