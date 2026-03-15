package com.ahmedprojects.flow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserSearchAdapter(
    private val list: List<UserModel>,
    private val onClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserHolder>() {

    class UserHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val email: TextView = v.findViewById(R.id.tv_email)
        val roleBadge: TextView = v.findViewById(R.id.tv_role)
        val btnEmail: View = v.findViewById(R.id.btn_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_search_item, parent, false)
        return UserHolder(v)
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val user = list[position]
        holder.name.text = user.name
        holder.email.text = user.email
        holder.roleBadge.text = "" // optional, or could use user.role if available

        holder.btnEmail.setOnClickListener {
            onClick(user)
        }
    }

    override fun getItemCount(): Int = list.size
}
