package com.example.simplenotes.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplenotes.model.Note
import com.example.simplenotes.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteActivityViewModel(private val repository: NoteRepository): ViewModel() {

    fun saveNote(newNote: Note) = viewModelScope.launch(Dispatchers.IO) { repository.addNote(newNote) }

    fun updateNote(existingNote: Note) = viewModelScope.launch(Dispatchers.IO) { repository.updateNote(existingNote) }

    fun deleteNote(existingNote: Note) = viewModelScope.launch(Dispatchers.IO) { repository.deleteNote(existingNote) }

    fun searchNote(query: String): LiveData<List<Note>> {
        return repository.searchNote(query)
    }

    private var imagePath: String? = null

    fun saveImagePath(path: String?) { imagePath = path }

    fun setImagePath(): String? {
        if (imagePath != null)
            return imagePath
        return null
    }

    fun getAllNotes(): LiveData<List<Note>> = repository.getNote()

    override fun onCleared() {
        imagePath = null
        super.onCleared()
    }
}