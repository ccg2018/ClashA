package com.github.cgg.clasha.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.utils.Key

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-11
 * @describe
 */
@Database(entities = [ProfileConfig::class], version = 3)
abstract class PrivateDatabase : RoomDatabase() {
    companion object {
        private val instance by lazy {
            Room.databaseBuilder(app, PrivateDatabase::class.java, Key.DB_PRIVATE_CONFIG)
                .addMigrations(Migration3)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        }

        val profileConfigDao get() = instance.profileConfigDao()
    }

    abstract fun profileConfigDao(): ProfileConfig.Dao

    object Migration3 : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `ProfileConfig` ADD COLUMN `time` INTEGER NOT NULL DEFAULT 0")
        }

    }
}