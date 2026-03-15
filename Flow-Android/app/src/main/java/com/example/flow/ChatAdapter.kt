package com.ahmedprojects.flow.adapters

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ahmedprojects.flow.R
import com.ahmedprojects.flow.models.ChatPreview
import de.hdodenhof.circleimageview.CircleImageView
import android.graphics.BitmapFactory

class ChatAdapter(
    private val chatList: List<ChatPreview>,
    private val onItemClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.username_text)
        val lastMessageText: TextView = itemView.findViewById(R.id.last_message_text)
        val messageTimeText: TextView = itemView.findViewById(R.id.message_time_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item_layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        holder.usernameText.text = chat.username.ifEmpty { "Unknown User" }
        holder.lastMessageText.text = chat.lastMessage.ifEmpty { "No messages yet" }
        holder.messageTimeText.text = chat.lastMessageTime.ifEmpty { "" }

        if (!chat.profileImageBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(chat.profileImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        holder.itemView.setOnClickListener { onItemClick(chat) }
    }

    override fun getItemCount(): Int = chatList.size
}
