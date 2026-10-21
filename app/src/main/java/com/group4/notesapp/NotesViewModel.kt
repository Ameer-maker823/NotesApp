package com.group4.notesapp

import android.app.Application
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

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>?> = _query
        .flatMapLatest { dao.searchNotes(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selected = MutableStateFlow<Set<Int>>(emptySet())
    val selected: StateFlow<Set<Int>> = _selected

    fun setQuery(text: String) {
        _query.value = text
    }

    fun setSearching(open: Boolean) {
        _searching.value = open
        if (!open) _query.value = ""
    }

    fun toggleSelection(id: Int) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun selectedNotes(): List<Note> =
        notes.value.orEmpty().filter { it.id in _selected.value }

    fun togglePinSelected() {
        val chosen = selectedNotes()
        if (chosen.isEmpty()) return
        val allPinned = chosen.all { it.pinned }
        viewModelScope.launch { dao.setPinned(chosen.map { it.id }, !allPinned) }
        clearSelection()
    }

    fun deleteSelected(): List<Note> {
        val chosen = selectedNotes()
        if (chosen.isNotEmpty()) viewModelScope.launch { dao.delete(chosen) }
        clearSelection()
        return chosen
    }

    fun restore(deleted: List<Note>) {
        viewModelScope.launch { dao.insertAll(deleted) }
    }
}