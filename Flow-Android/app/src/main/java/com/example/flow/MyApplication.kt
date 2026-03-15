package com.ahmedprojects.flow

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(ConnectivityManager::class.java)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger offline sync when internet is available
                Log.e("OfflineSync", "Internet is available")
                triggerSync()
            }
        })
    }
    fun triggerSync() {
        CoroutineScope(Dispatchers.IO).launch {
            // Wait a bit for network to stabilize
            delay(1500)

            val db = AppDatabase.getInstance(this@MyApplication)
            val pendingProjectDao = db.pendingProjectDao()
            val pendingTaskDao = db.pendingTaskDao()
            val pendingUpdateDao = db.pendingTaskUpdateDao()

            // Sync Projects
            val pendingProjects = pendingProjectDao.getPending()
            Log.d("OfflineSync", "Pending projects: ${pendingProjects.size}")
            pendingProjects.forEach { project ->
                val success = retrySync { NetworkSyncHelper.syncPendingProject(this@MyApplication, project) }
                if (success) {
                    pendingProjectDao.deletePending(project)
                    Log.d("OfflineSync", "Project synced successfully: ${project.name}")
                } else {
                    Log.d("OfflineSync", "Project sync failed: ${project.name}")
                }
            }

            // Sync Tasks
            val pendingTasks = pendingTaskDao.getAllTasks()
            Log.d("OfflineSync", "Pending tasks: ${pendingTasks.size}")
            pendingTasks.forEach { task ->
                val success = retrySync { NetworkSyncHelper.syncPendingTask(this@MyApplication, task) }
                if (success) {
                    pendingTaskDao.deleteTask(task)
                    Log.d("OfflineSync", "Task synced successfully: ${task.title}")
                } else {
                    Log.d("OfflineSync", "Task sync failed: ${task.title}")
                }
            }

            // Sync Updates
            val pendingUpdates = pendingUpdateDao.getAllUpdates()
            Log.d("OfflineSync", "Pending updates: ${pendingUpdates.size}")
            pendingUpdates.forEach { update ->
                val success = retrySync { NetworkSyncHelper.syncPendingTaskUpdate(this@MyApplication, update) }
                if (success) {
                    pendingUpdateDao.deleteUpdate(update)
                    Log.d("OfflineSync", "Update synced successfully: ${update.id}")
                } else {
                    Log.d("OfflineSync", "Update sync failed: ${update.id}")
                }
            }
        }
    }
    suspend fun retrySync(
        retries: Int = 3,
        delayMillis: Long = 1000,
        block: suspend () -> Boolean
    ): Boolean {
        repeat(retries) { attempt ->
            val success = try {
                block()
            } catch (e: Exception) {
                Log.e("OfflineSync", "Exception during sync: ${e.localizedMessage}")
                false
            }
            if (success) return true
            Log.d("OfflineSync", "Sync attempt ${attempt + 1} failed, retrying...")
            delay(delayMillis)
        }
        return false
    }



}
