package com.ahmedprojects.flow

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatCacheDB(context: Context) :
    SQLiteOpenHelper(context, "chat_cache.db", null, 1) {

    companion object {
        const val TABLE = "messages"
        const val COL_ID = "msgId"
        const val COL_CHAT = "chatId"
        const val COL_SENDER = "sender"
        const val COL_TEXT = "text"
        const val COL_IMAGE = "image"
        const val COL_TYPE = "type"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_VANISH = "vanish"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_CHAT INTEGER,
                $COL_SENDER TEXT,
                $COL_TEXT TEXT,
                $COL_IMAGE TEXT,
                $COL_TYPE TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_VANISH INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, newV: Int) {}

    fun saveMessage(msgId: String, chatId: Int, sender: String, text: String?, img: String?, type: String?, timestamp: Long, vanish: Int) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ID, msgId)
            put(COL_CHAT, chatId)
            put(COL_SENDER, sender)
            put(COL_TEXT, text)
            put(COL_IMAGE, img)
            put(COL_TYPE, type)
            put(COL_TIMESTAMP, timestamp)
            put(COL_VANISH, vanish)
        }
        db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getChatMessages(chatId: Int) =
        readableDatabase.rawQuery("SELECT * FROM $TABLE WHERE $COL_CHAT=$chatId", null)
}
