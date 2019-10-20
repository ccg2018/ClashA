package com.github.cgg.clasha.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.utils.Key

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-05-30
 * @describe
 */
@Database(entities = [LogMessage::class], version = 3)
@TypeConverters(DateConverters::class)
abstract class LogsDatabase : RoomDatabase() {

    companion object {
        private val instance by lazy {
            Room.databaseBuilder(app, LogsDatabase::class.java, Key.DB_LOGS)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        }
        val logMessageDao get() = instance.logMessageDao()
    }

    abstract fun logMessageDao(): LogMessage.Dao

}
