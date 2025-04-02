package com.orion.templete.data.local.entity

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.orion.templete.util.ListTypeConverter

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val _id: String,
    val name: String,
    val profilePicture: String,
    val dob: String,
    val gender: String,
    val language: String,
    val countryIso2: String,
    val follow:Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
