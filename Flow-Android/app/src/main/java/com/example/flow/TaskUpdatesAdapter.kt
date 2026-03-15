package com.ahmedprojects.flow

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskUpdatesAdapter(private val updates: List<Task_Details.TaskUpdate>) :
    RecyclerView.Adapter<TaskUpdatesAdapter.UpdateViewHolder>() {

    class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val ivImage: ImageView = itemView.findViewById(R.id.ivUpdateImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_task_update, parent, false)
        return UpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        val update = updates[position]
        holder.tvUserName.text = update.userName
        holder.tvMessage.text = update.message
        holder.tvTime.text = update.createdAt

        if (update.imageUrl.isNotEmpty()) {
            holder.ivImage.visibility = View.VISIBLE
            try {
                // Decode Base64 string to byte array
                val imageBytes = Base64.decode(update.imageUrl, Base64.DEFAULT)
                // Convert to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivImage.visibility = View.GONE
                e.printStackTrace()
            }
        } else {
            holder.ivImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = updates.size
}
