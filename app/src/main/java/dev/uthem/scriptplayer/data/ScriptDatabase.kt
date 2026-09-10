package dev.uthem.scriptplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScriptEntity::class], version = 1, exportSchema = true)
abstract class ScriptDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        fun open(context: Context): ScriptDatabase =
            Room.databaseBuilder(context, ScriptDatabase::class.java, "scripts.db").build()
    }
}
