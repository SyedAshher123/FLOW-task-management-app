package com.ahmedprojects.flow

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val userList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val usernameText: TextView = itemView.findViewById(R.id.userNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.usernameText.text = user.name

        val base64Image = user.profilePhoto
        if (!base64Image.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }
}
