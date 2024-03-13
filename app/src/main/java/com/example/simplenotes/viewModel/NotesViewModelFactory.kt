package com.example.simplenotes.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.simplenotes.repository.NoteRepository

@Suppress("UNCHECKED_CAST")
class NotesViewModelFactory(private val repository: NoteRepository): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotesViewModel(repository) as T
    }

}