package com.group4.notesapp

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group4.notesapp.data.AppDatabase
import com.group4.notesapp.data.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).noteDao()

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>?> = query
        .flatMapLatest { dao.searchNotes(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setQuery(text: String) {
        query.value = text
    }

    // Temporary: lets us test the list before the editor exists.
    fun addSample() {
        viewModelScope.launch {
            val colors = listOf(
                R.color.note_teal, R.color.note_salmon, R.color.note_pink,
                R.color.note_peach, R.color.note_purple
            ).map { ContextCompat.getColor(getApplication(), it) }
            dao.insert(
                Note(
                    title = "Sample note",
                    content = "Book the train to Kandy, pack the camera, and check the weather.",
                    color = colors.random()
                )
            )
        }
    }
}