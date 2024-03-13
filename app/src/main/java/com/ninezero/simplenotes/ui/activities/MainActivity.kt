package com.ninezero.simplenotes.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.ninezero.simplenotes.databinding.ActivityMainBinding
import com.ninezero.simplenotes.db.NoteDatabase
import com.ninezero.simplenotes.repository.NoteRepository
import com.ninezero.simplenotes.utils.doOnApplyWindowInsets
import com.ninezero.simplenotes.utils.shortToast
import com.ninezero.simplenotes.viewModel.NotesViewModel
import com.ninezero.simplenotes.viewModel.NotesViewModelFactory

class MainActivity : AppCompatActivity() {
    lateinit var notesViewModel: NotesViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
            val noteRepository = NoteRepository(NoteDatabase(this))
            val notesViewModelFactory = NotesViewModelFactory(noteRepository)
            notesViewModel = ViewModelProvider(this,
                notesViewModelFactory)[NotesViewModel::class.java]
        } catch (e: Exception) {
            shortToast("오류 발생")
        }

        binding.parentLayout.doOnApplyWindowInsets { insetView, windowInsets, initialPadding, _ ->
            insetView.updatePadding(
                top = initialPadding.top + windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
            )
        }
    }
}