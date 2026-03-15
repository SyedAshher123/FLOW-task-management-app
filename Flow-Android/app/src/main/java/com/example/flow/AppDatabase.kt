package com.ahmedprojects.flow

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        PendingProjectEntity::class,
        TaskEntity::class,
        PendingTaskEntity::class,
        PendingTaskUpdateEntity::class,
        TaskCacheEntity::class,
        TaskUpdateCacheEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun pendingProjectDao(): PendingProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun pendingTaskDao(): PendingTaskDao
    abstract fun pendingTaskUpdateDao(): PendingTaskUpdateDao
    abstract fun taskCacheDao(): TaskCacheDao
    abstract fun taskUpdateCacheDao(): TaskUpdateCacheDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flow_local.db"
                )
                    // WARNING: fallbackToDestructiveMigration will wipe all data if version changes
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
