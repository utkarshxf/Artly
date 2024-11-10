package com.orion.templete.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flashcall.me.data.local.dao.UserDao
import com.orion.templete.data.local.entity.UserEntity
import com.orion.templete.util.ListTypeConverter

@Database(
    entities = [
        UserEntity::class
        ],
    version = 1
)
@TypeConverters(ListTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    companion object {
        const val DATABASE_NAME = "flashcall_database"
    }
}