package com.example.simplenotes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.simplenotes.R
import com.example.simplenotes.databinding.ActivityMainBinding
import com.example.simplenotes.db.NoteDatabase
import com.example.simplenotes.repository.NoteRepository
import com.example.simplenotes.utils.shortToast
import com.example.simplenotes.viewModel.NoteActivityViewModel
import com.example.simplenotes.viewModel.NoteActivityViewModelFactory

class MainActivity : AppCompatActivity() {
    lateinit var noteActivityViewModel: NoteActivityViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
            val noteRepository = NoteRepository(NoteDatabase(this))
            val noteActivityViewModelFactory = NoteActivityViewModelFactory(noteRepository)
            noteActivityViewModel = ViewModelProvider(this,
                noteActivityViewModelFactory)[NoteActivityViewModel::class.java]
        } catch (e: Exception) {
            shortToast("오류 발생")
        }
    }
}