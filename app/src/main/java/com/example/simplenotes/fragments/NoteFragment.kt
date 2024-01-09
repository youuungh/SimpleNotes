package com.example.simplenotes.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.simplenotes.R
import com.example.simplenotes.activities.MainActivity
import com.example.simplenotes.adapters.RvNotesAdapter
import com.example.simplenotes.databinding.FragmentNoteBinding
import com.example.simplenotes.utils.SwipeToDelete
import com.example.simplenotes.utils.hideKeyboard
import com.example.simplenotes.viewModel.NoteActivityViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NoteFragment : Fragment(R.layout.fragment_note) {
    private val noteActivityViewModel: NoteActivityViewModel by activityViewModels()
    private lateinit var noteBinding: FragmentNoteBinding
    private lateinit var rvAdapter: RvNotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialElevationScale(false).apply { duration = 350 }
        enterTransition = MaterialElevationScale(true).apply { duration = 350 }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteBinding = FragmentNoteBinding.bind(view)
        val activity = activity as MainActivity
        val navController = Navigation.findNavController(view)

        requireView().hideKeyboard()

        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            activity.window.statusBarColor = Color.WHITE
        }

        setFragmentResultListener("key") { _, bundle ->
            when (val result = bundle.getString("bundleKey")) {
                "저장되지 않음", "빈 노트가 삭제되었습니다" -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        Snackbar.make(view, result, Snackbar.LENGTH_SHORT).apply {
                            animationMode = Snackbar.ANIMATION_MODE_FADE
                            setAnchorView(R.id.addFab)
                        }.show()
                        noteBinding.rvNote.isVisible = false
                        delay(300)
                        recyclerViewDisplay()
                        noteBinding.rvNote.isVisible = true
                    }
                }
            }
        }

        recyclerViewDisplay()
        swipeToDelete(noteBinding.rvNote)

        noteBinding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                noteBinding.noData.isVisible = false
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (s.toString().isNotEmpty()) {
                    noteBinding.clearText.visibility = View.VISIBLE
                    val text = s.toString()
                    val query = "%$text%"
                    if (query.isNotEmpty()) {
                        noteActivityViewModel.searchNote(query).observe(viewLifecycleOwner) {
                            rvAdapter.submitList(it)
                        }
                    } else {
                        observerDataChanges()
                    }
                } else {
                    observerDataChanges()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isEmpty()) {
                    noteBinding.clearText.visibility = View.GONE
                }
            }
        })

        noteBinding.search.setOnEditorActionListener { v, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus()
                requireView().hideKeyboard()
            }
            return@setOnEditorActionListener true
        }

        noteBinding.clearText.setOnClickListener {
            clearTextFunction()
            it.isVisible = false
            noteBinding.noData.isVisible = false
        }

        noteBinding.addFab.setOnClickListener {
            noteBinding.appBarLayout.visibility = View.INVISIBLE
            navController.navigate(NoteFragmentDirections.actionNoteFragmentToNoteContentFragment())
        }

        noteBinding.rvNote.setOnScrollChangeListener { _, scrollX, scrollY, _, oldScrollY ->
            when {
                scrollY > oldScrollY -> {
                    noteBinding.addFab.isVisible = false

                }
                scrollX == scrollY -> {
                    noteBinding.addFab.isVisible = true

                }
                else -> {
                    noteBinding.addFab.isVisible = true
                }
            }
        }
    }


    private fun recyclerViewDisplay() {
        @SuppressLint("SwitchIntDef")
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> setUpRecyclerView(2)
            Configuration.ORIENTATION_LANDSCAPE -> setUpRecyclerView(3)
        }
    }

    private fun setUpRecyclerView(spanCount: Int) {
        noteBinding.rvNote.apply {
            layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            rvAdapter = RvNotesAdapter()
            rvAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = rvAdapter
            postponeEnterTransition(300L, TimeUnit.MILLISECONDS)
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }
        observerDataChanges()
    }

    private fun observerDataChanges() {
        noteActivityViewModel.getAllNotes().observe(viewLifecycleOwner) { list ->
            noteBinding.noData.isVisible = list.isEmpty()
            rvAdapter.submitList(list)
        }
    }

    private fun swipeToDelete(recyclerView: RecyclerView) {
        val swipeToDeleteCallback = object : SwipeToDelete() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = rvAdapter.currentList[position]
                noteActivityViewModel.deleteNote(note)
                noteBinding.search.apply {
                    hideKeyboard()
                    clearFocus()
                }
                if (noteBinding.search.text.toString().isEmpty()) {
                    observerDataChanges()
                }
                val snackBar = Snackbar.make(
                    requireView(), "노트가 삭제됨", Snackbar.LENGTH_LONG
                ).addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                    }

                    override fun onShown(transientBottomBar: Snackbar?) {
                        transientBottomBar?.setAction("취소") {
                            noteActivityViewModel.saveNote(note)
                            noteBinding.noData.isVisible = false
                        }
                        super.onShown(transientBottomBar)
                    }
                }).apply {
                    animationMode = Snackbar.ANIMATION_MODE_FADE
                    setAnchorView(R.id.addFab)
                }
                snackBar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                snackBar.show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun clearTextFunction() {
        noteBinding.search.apply {
            text.clear()
            hideKeyboard()
            clearFocus()
            observerDataChanges()
        }
    }
}