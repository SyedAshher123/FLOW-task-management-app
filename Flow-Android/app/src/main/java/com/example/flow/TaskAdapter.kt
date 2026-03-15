package com.ahmedprojects.flow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class TaskAdapter(
    private var taskList: MutableList<TaskModel>,
    private val onClick: (TaskModel) -> Unit, // Click listener passed from Activity
    private val IP: String = IP_String().IP
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvUpdateRequested: TextView = itemView.findViewById(R.id.tvUpdateRequested)
        val tvTeamName: TextView = itemView.findViewById(R.id.TeamName)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentCompleted)
        val tvOrganisationName: TextView = itemView.findViewById(R.id.OrganisationName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tasks_page_card, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.tvAvatar.text = task.title.first().uppercase()
        holder.tvTitle.text = task.title
        holder.tvPriority.text = task.priority
        holder.tvStatus.text = task.status
        holder.tvOrganisationName.text = task.organisationName
        holder.tvDescription.text = task.description
        holder.tvPercentage.text = "${task.percentageCompleted}% Completed"
        holder.tvDueDate.text = "Due: ${task.dueDate}"
        holder.tvUpdateRequested.visibility =
            if (task.updateRequested) View.VISIBLE else View.GONE

        // Fetch username for assigned_by
        //fetchUserName(task.assignedBy, holder)
        holder.tvTeamName.text = "Assigned By: ${task.assignedByName}"


        // Priority color
        when (task.priority.lowercase()) {
            "high" -> holder.tvPriority.setTextColor(holder.itemView.context.getColor(R.color.red))
            "medium" -> holder.tvPriority.setTextColor(holder.itemView.context.getColor(R.color.orange))
            "low" -> holder.tvPriority.setTextColor(holder.itemView.context.getColor(R.color.green))
        }

        // Status color
        when (task.status.lowercase()) {
            "in progress" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.blue))
            "pending" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.gray))
            "completed" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.green))
        }

        // 🔥 Handle click to open Task_Details
        holder.itemView.setOnClickListener {
            onClick(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTasks(newTasks: List<TaskModel>) {
        taskList.clear()
        taskList.addAll(newTasks)
        notifyDataSetChanged()
    }

    // Fetch username asynchronously
    private fun fetchUserName(userId: Int, holder: TaskViewHolder) {
        val url = "$IP/getUserName.php"
        val queue = Volley.newRequestQueue(holder.itemView.context)

        val json = JSONObject()
        json.put("userId", userId)

        val request = JsonObjectRequest(
            Request.Method.POST, url, json,
            { response ->
                if (response.getBoolean("success")) {
                    val name = response.getString("name")
                    holder.tvTeamName.text = "Assigned By: $name"
                } else {
                    holder.tvTeamName.text = "Assigned By: Unknown"
                }
            },
            {
                holder.tvTeamName.text = "Assigned By: Error"
            }
        )
        queue.add(request)
    }
}
