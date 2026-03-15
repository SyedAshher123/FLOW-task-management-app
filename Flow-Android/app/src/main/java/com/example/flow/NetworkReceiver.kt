package com.ahmedprojects.flow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("OfflineSync", "NetworkReceiver triggered")
        if (!isOnline(context)) {
            Log.d("OfflineSync", "Device is offline, skipping sync")
            return
        }

        Log.d("OfflineSync", "Device is online, starting sync")

        val db = AppDatabase.getInstance(context)
        val pendingProjectDao = db.pendingProjectDao()
        val pendingTaskDao = db.pendingTaskDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Sync pending projects
            val pendingProjects = pendingProjectDao.getPending()
            Log.d("OfflineSync", "Pending projects: ${pendingProjects.size}")
            pendingProjects.forEach { project ->
                Log.d("OfflineSync", "Syncing project: ${project.name}")
                val success = syncPendingProject(context, project)
                Log.d("OfflineSync", "Project sync success: $success")
                if (success) pendingProjectDao.deletePending(project)
            }

            // Sync pending tasks
            val pendingTasks = pendingTaskDao.getAllTasks()
            Log.d("OfflineSync", "Pending tasks: ${pendingTasks.size}")
            pendingTasks.forEach { task ->
                Log.d("OfflineSync", "Syncing task: ${task.title}")
                val success = syncPendingTask(context, task)
                Log.d("OfflineSync", "Task sync success: $success")
                if (success) pendingTaskDao.deleteTask(task)
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d("OfflineSync", "isOnline check: $online")
        return online
    }

    private suspend fun syncPendingProject(context: Context, p: PendingProjectEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_project.php"
            Log.d("OfflineSync", "Sending project: ${p.name} to $url")

            val jsonBody = JSONObject().apply {
                put("ownerId", p.ownerId)
                put("name", p.name)
                put("description", p.description)
                put("joinCode", p.joinCode)
                if (!p.picturePath.isNullOrEmpty()) {
                    val b64 = try { File(p.picturePath).readBytes().let { Base64.encodeToString(it, Base64.NO_WRAP) } } catch(e:Exception){ "" }
                    put("picture_base64", b64)
                } else {
                    put("picture_base64", "")
                }
            }

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Project server response: $response")
                        cont.resume(response.optBoolean("success", false)) {}
                    },
                    { error ->
                        Log.e("OfflineSync", "Project sync error: ${error.message}", error)
                        cont.resume(false) {}
                    }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception syncing project: ${e.message}", e)
            false
        }
    }

    private suspend fun syncPendingTask(context: Context, t: PendingTaskEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_task.php"
            Log.d("OfflineSync", "Sending task: ${t.title} to $url")

            val jsonBody = JSONObject().apply {
                put("project_id", t.projectId)
                put("assigned_to", t.assignedTo)
                put("assigned_by", t.assignedBy)
                put("title", t.title)
                put("description", t.description)
                put("priority", t.priority)
                put("deadline", t.deadline)
            }

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Task server response: $response")
                        cont.resume(response.optBoolean("success", false)) {}
                    },
                    { error ->
                        Log.e("OfflineSync", "Task sync error: ${error.message}", error)
                        cont.resume(false) {}
                    }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception syncing task: ${e.message}", e)
            false
        }
    }

    private suspend fun syncPendingTaskUpdate(context: Context, u: PendingTaskUpdateEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "submit_update.php"
            Log.d("OfflineSync", "Sending task update for task_id: ${u.taskId} to $url")

            val jsonBody = JSONObject().apply {
                put("task_id", u.taskId)
                put("user_id", u.userId)
                put("message", u.message)
                put("image_url", u.imageBase64)
            }

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Task update server response: $response")
                        cont.resume(response.optBoolean("success", false)) {}
                    },
                    { error ->
                        Log.e("OfflineSync", "Task update sync error: ${error.message}", error)
                        cont.resume(false) {}
                    }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception syncing task update: ${e.message}", e)
            false
        }
    }
}
