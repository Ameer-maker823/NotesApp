package com.group4.notesapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(notes: List<Note>)

    @Query("UPDATE notes SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinned(ids: List<Int>, pinned: Boolean)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query(
        "SELECT * FROM notes " +
                "WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' " +
                "ORDER BY pinned DESC, updatedAt DESC"
    )
    fun searchNotes(query: String): Flow<List<Note>>
}