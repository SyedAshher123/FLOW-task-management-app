package com.ahmedprojects.flow

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Locale

class NotificationAdapter(
    private val notifications: MutableList<NotificationModel>,
    private val ip: String
) : RecyclerView.Adapter<NotificationAdapter.NotificationVH>() {

    class NotificationVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_name)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_notificationDescription)
        val tvTimestamp: TextView = itemView.findViewById(R.id.timestamp)

        val btnAccept: RelativeLayout = itemView.findViewById(R.id.btnAccept)
        val btnReject: RelativeLayout = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_card, parent, false)
        return NotificationVH(view)
    }

    override fun onBindViewHolder(holder: NotificationVH, position: Int) {
        val item = notifications[position]   // FIXED

        holder.tvTitle.text = "Project Invite"
        holder.tvDescription.text =
            "${item.senderName} invited you to join '${item.projectName}'"

        val formattedTime = formatTimestamp(item.timestamp)
        holder.tvTimestamp.text = "• $formattedTime"

        holder.btnAccept.setOnClickListener {
            acceptInvite(item, position, holder)
        }

        holder.btnReject.setOnClickListener {
            rejectInvite(item, position, holder)
        }
    }

    override fun getItemCount(): Int = notifications.size   // FIXED

    @SuppressLint("SimpleDateFormat")
    private fun formatTimestamp(mysqlTime: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("h:mm a", Locale.getDefault())
            val parsed = input.parse(mysqlTime)
            output.format(parsed!!)
        } catch (e: Exception) {
            mysqlTime
        }
    }

    private fun acceptInvite(item: NotificationModel, position: Int, holder: RecyclerView.ViewHolder) {
        val ctx = holder.itemView.context
        val prefs = ctx.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)
        if (currentUserId == -1) {
            Toast.makeText(ctx, "Invalid session", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = Volley.newRequestQueue(ctx)
        val url = ip + "accept_invite.php"

        val json = JSONObject().apply {
            put("invite_id", item.inviteId)
            put("project_id", item.projectId)
            put("receiver_id", currentUserId)      // <-- use logged-in user
        }

        val req = JsonObjectRequest(Request.Method.POST, url, json,
            {
                Toast.makeText(ctx, "Invite accepted", Toast.LENGTH_SHORT).show()
                notifications.removeAt(position)
                notifyItemRemoved(position)
            },
            {
                Toast.makeText(ctx, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(req)
    }


    private fun rejectInvite(item: NotificationModel, position: Int, holder: RecyclerView.ViewHolder) {
        val queue = Volley.newRequestQueue(holder.itemView.context)
        val url = ip + "reject_invite.php"

        val json = JSONObject().apply {
            put("invite_id", item.inviteId)
        }

        val req = JsonObjectRequest(
            Request.Method.POST, url, json,
            {
                Toast.makeText(holder.itemView.context, "Invite rejected", Toast.LENGTH_SHORT).show()
                notifications.removeAt(position)
                notifyItemRemoved(position)
            },
            {
                Toast.makeText(holder.itemView.context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(req)
    }
}
