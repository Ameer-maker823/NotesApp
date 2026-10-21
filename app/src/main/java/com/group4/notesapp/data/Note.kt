package com.group4.notesapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val color: Int,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)