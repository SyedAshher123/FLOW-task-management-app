package com.ahmedprojects.flow

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

object NetworkSyncHelper {

    suspend fun syncPendingProject(context: Context, p: PendingProjectEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_project.php"

            val jsonBody = JSONObject().apply {
                put("ownerId", p.ownerId)
                put("name", p.name)
                put("description", p.description)
                put("joinCode", p.joinCode)
            }

            Log.d("OfflineSync", "Syncing Project: $jsonBody")

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Project Server Response: $response")
                        val success = response.optBoolean("success", false)
                        if (!success) Log.d("OfflineSync", "Project sync failed on server side")
                        cont.resume(success) {}
                    },
                    { error ->
                        Log.e("OfflineSync", "Project Volley Error: ${error.message}", error)
                        cont.resume(false) {}
                    }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception during project sync: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun syncPendingTask(context: Context, t: PendingTaskEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_task.php"

            val jsonBody = JSONObject().apply {
                put("project_id", t.projectId)
                put("assigned_to", t.assignedTo)
                put("assigned_by", t.assignedBy)
                put("title", t.title)
                put("description", t.description)
                put("priority", t.priority)
                put("deadline", t.deadline)
            }

            Log.d("OfflineSync", "Syncing Task: $jsonBody")

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Task Server Response: $response")
                        val success = response.optBoolean("success", false)
                        if (!success) Log.d("OfflineSync", "Task sync failed on server side")
                        cont.resume(success) {}
                    },
                    { error ->
                        Log.e("OfflineSync", "Task Volley Error: ${error.message}", error)
                        cont.resume(false) {}
                    }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception during task sync: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun syncPendingTaskUpdate(context: Context, u: PendingTaskUpdateEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "submit_update.php"

            val jsonBody = JSONObject().apply {
                put("task_id", u.taskId)
                put("user_id", u.userId)
                put("message", u.message)
                if (!u.imageBase64.isNullOrEmpty()) put("image_url", u.imageBase64)
            }

            Log.d("OfflineSync", "Syncing Task Update: $jsonBody")

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response ->
                        Log.d("OfflineSync", "Task Update Server Response: $response")
                        val success = response.optBoolean("success", false)
                        if (!success) Log.d("OfflineSync", "Task update sync failed on server side")
                        cont.resumeWith(Result.success(success))
                    },
                    { error ->
                        Log.e("OfflineSync", "Task Update Volley Error: ${error.message}", error)
                        cont.resumeWith(Result.success(false))
                    }
                ).apply {
                    retryPolicy = DefaultRetryPolicy(
                        15000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                }

                cont.invokeOnCancellation { request.cancel() }
                queue.add(request)
            }
        } catch (e: Exception) {
            Log.e("OfflineSync", "Exception during task update sync: ${e.localizedMessage}", e)
            false
        }
    }

}
