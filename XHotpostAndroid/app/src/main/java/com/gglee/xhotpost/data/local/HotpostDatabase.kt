package com.gglee.xhotpost.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HotTopicEntity::class, DraftEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HotpostDatabase : RoomDatabase() {
    abstract fun hotpostDao(): HotpostDao

    companion object {
        fun build(context: Context): HotpostDatabase =
            Room.databaseBuilder(context, HotpostDatabase::class.java, "x_hotpost.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
