package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val updateId: String,
    val timestamp: Long = System.currentTimeMillis()
)
