package com.example.simplenotes.fragments

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.example.simplenotes.R
import com.example.simplenotes.activities.MainActivity
import com.example.simplenotes.databinding.BottomSheetDialogBinding
import com.example.simplenotes.databinding.FragmentNoteContentBinding
import com.example.simplenotes.model.Note
import com.example.simplenotes.utils.hideKeyboard
import com.example.simplenotes.viewModel.NoteActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class NoteContentFragment : Fragment(R.layout.fragment_note_content) {
    private lateinit var navController: NavController
    private lateinit var contentBinding: FragmentNoteContentBinding
    private lateinit var result: String
    private var note: Note? = null
    private var color = -1
    private val noteActivityViewModel: NoteActivityViewModel by activityViewModels()
    private val currentDate = SimpleDateFormat.getDateInstance().format(Date())
    private val job = CoroutineScope(Dispatchers.Main)
    private val args: NoteContentFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animation = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment
            scrimColor = Color.TRANSPARENT
            duration = 300L
        }
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentBinding = FragmentNoteContentBinding.bind(view)

        ViewCompat.setTransitionName(
            contentBinding.noteContentFragmentParent,
            "recyclerView_${args.note?.id}"
        )

        navController = Navigation.findNavController(view)
        val activity = activity as MainActivity

        contentBinding.backButton.setOnClickListener {
            requireView().hideKeyboard()
            saveNoteAndGoBack()
        }

        try {
            contentBinding.edtNoteContent.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    contentBinding.bottomBar.visibility = View.VISIBLE
                    contentBinding.edtNoteContent.setStylesBar(contentBinding.stylesBar)
                } else {
                    contentBinding.bottomBar.visibility = View.GONE
                }
            }
        } catch (e: Throwable) {
            Log.d("TAG", e.stackTraceToString())
        }

        contentBinding.optionButton.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
            val bottomSheetView: View = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
            with(bottomSheetDialog) {
                setContentView(bottomSheetView)
                show()
            }
            val bottomSheetBinding = BottomSheetDialogBinding.bind(bottomSheetView)
            bottomSheetBinding.apply {
                colorPicker.apply {
                    setSelectedColor(color)
                    setOnColorSelectedListener { value ->
                        color = value
                        contentBinding.apply {
                            noteContentFragmentParent.setBackgroundColor(color)
                            toolbarNoteContent.setBackgroundColor(color)
                            bottomBar.setBackgroundColor(color)
                            activity.window.statusBarColor = color
                        }
                        bottomSheetBinding.bottomSheetParent.setCardBackgroundColor(color)
                    }
                    bottomSheetParent.setCardBackgroundColor(color)
                }
                bottomSheetView.post {
                    bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        setUpNote()

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveNoteAndGoBack()
            }
        })
    }

    private fun saveNoteAndGoBack() {
        if (contentBinding.edtTitle.text.toString().isEmpty() && contentBinding.edtNoteContent.text.toString().isEmpty()) {
            result = "빈 노트가 삭제되었습니다"
            setFragmentResult("key", bundleOf("bundleKey" to result))
            navController.navigate(NoteContentFragmentDirections.actionNoteContentFragmentToNoteFragment())
        } else {
            note = args.note
            when (note) { null -> {
                    noteActivityViewModel.saveNote(
                        Note(
                            0,
                            contentBinding.edtTitle.text.toString(),
                            contentBinding.edtNoteContent.getMD(),
                            currentDate,
                            color
                        )
                    )
                    result = "노트가 저장됨"
                    setFragmentResult("key", bundleOf("bundleKey" to result))
                    navController.navigate(NoteContentFragmentDirections.actionNoteContentFragmentToNoteFragment())
                }
                else -> {
                    updateNote()
                    navController.popBackStack()
                }
            }
        }
    }

    private fun updateNote() {
        if (note != null) {
            noteActivityViewModel.updateNote(
                Note(
                    note!!.id,
                    contentBinding.edtTitle.text.toString(),
                    contentBinding.edtNoteContent.getMD(),
                    currentDate,
                    color
                )
            )
        }
    }

    private fun setUpNote() {
        val note = args.note
        val title = contentBinding.edtTitle
        val content = contentBinding.edtNoteContent
        val lastEdited = contentBinding.lastEdited

        if (note == null) {
            lastEdited.text = getString(R.string.edited_on, SimpleDateFormat.getDateInstance().format(Date()))
        }

        if (note != null) {
            title.setText(note.title)
            content.renderMD(note.content)
            lastEdited.text = getString(R.string.edited_on, note.date)
            color = note.color
            contentBinding.apply {
                job.launch {
                    delay(10)
                    noteContentFragmentParent.setBackgroundColor(color)
                }
                toolbarNoteContent.setBackgroundColor(color)
                bottomBar.setBackgroundColor(color)
            }
            activity?.window?.statusBarColor = note.color
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (job.isActive) {
            job.cancel()
        }
    }
}