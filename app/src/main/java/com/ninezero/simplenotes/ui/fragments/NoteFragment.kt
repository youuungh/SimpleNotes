package com.ninezero.simplenotes.ui.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ninezero.simplenotes.R
import com.ninezero.simplenotes.ui.activities.MainActivity
import com.ninezero.simplenotes.adapters.NotesAdapter
import com.ninezero.simplenotes.databinding.FragmentNoteBinding
import com.ninezero.simplenotes.utils.SwipeToDelete
import com.ninezero.simplenotes.utils.doOnApplyWindowInsets
import com.ninezero.simplenotes.utils.hideKeyboard
import com.ninezero.simplenotes.viewModel.NotesViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class NoteFragment : Fragment(R.layout.fragment_note) {

    private val notesViewModel: NotesViewModel by activityViewModels()

    private lateinit var navController: NavController
    private lateinit var noteBinding: FragmentNoteBinding
    private lateinit var notesAdapter: NotesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        exitTransition = MaterialElevationScale(false).addTarget(view)
        enterTransition = MaterialElevationScale(true).addTarget(view)
        reenterTransition = MaterialElevationScale(true).addTarget(view)
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        noteBinding = FragmentNoteBinding.bind(view)
        val activity = activity as MainActivity

        notesAdapter = NotesAdapter()

        requireView().hideKeyboard()

        notesViewModel.saveImagePath(null)

        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            activity.window.statusBarColor = Color.WHITE
        }

        setFragmentResultListener("key") { _, bundle ->
            when (val result = bundle.getString("bundleKey")) {
                "노트가 저장됨", "빈 노트가 삭제됨" -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        Snackbar.make(view, result, Snackbar.LENGTH_SHORT).apply {
                            animationMode = Snackbar.ANIMATION_MODE_FADE
                            anchorView = noteBinding.addFab
                        }.show()
                        noteBinding.rvNote.isVisible = false
                        delay(300)
                        displayRecyclerView()
                        noteBinding.rvNote.isVisible = true
                    }
                }
            }
        }

        noteBinding.addFabLayout.doOnApplyWindowInsets { insetView, windowInsets, _, initialMargins ->
            insetView.updatePadding(
                bottom = initialMargins.bottom + windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            )
        }

        setUpSearch()
        displayRecyclerView()
        swipeToDelete(noteBinding.rvNote)
        onFabClick()
    }

    private fun setUpSearch() {
        noteBinding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                noteBinding.noDataText.isVisible = false
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (s.toString().isNotEmpty()) {
                    noteBinding.clearText.visibility = View.VISIBLE
                    val text = s.toString()
                    val query = "%$text%"
                    if (query.isNotEmpty()) {
                        notesViewModel.searchNote(query).observe(viewLifecycleOwner) {
                            notesAdapter.submitList(it)
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
            clearText()
            it.isVisible = false
            noteBinding.noDataText.isVisible = false
        }
    }


    private fun displayRecyclerView() {
        @SuppressLint("SwitchIntDef")
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> setUpRecyclerView(2)
            Configuration.ORIENTATION_LANDSCAPE -> setUpRecyclerView(3)
        }
    }

    private fun setUpRecyclerView(spanCount: Int) {
        with(noteBinding.rvNote) {
            layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
            adapter = notesAdapter
            notesAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            setHasFixedSize(true)

            postponeEnterTransition(300L, TimeUnit.MILLISECONDS)
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }

            setOnScrollChangeListener { _, scrollX, scrollY, _, oldScrollY ->
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
        observerDataChanges()
    }

    private fun observerDataChanges() {
        notesViewModel.getAllNotes().observe(viewLifecycleOwner) { list ->
            noteBinding.noDataText.isVisible = list.isEmpty()
            noteBinding.rvNote.isNestedScrollingEnabled = list.isNotEmpty()
            val params = (noteBinding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as? AppBarLayout.Behavior
            if (list.isEmpty()) {
                params?.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                    override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                        return false
                    }
                })
            } else {
                params?.setDragCallback(null)
            }
            notesAdapter.submitList(list)
        }
    }

    private fun onFabClick() {
        noteBinding.addFab.setOnClickListener {
            //noteBinding.appBarLayout.visibility = View.INVISIBLE
            navController.navigate(NoteFragmentDirections.actionNoteFragmentToNoteContentFragment())
        }
    }

    private fun swipeToDelete(recyclerView: RecyclerView) {
        val swipeToDeleteCallback = object : SwipeToDelete() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = notesAdapter.currentList[position]
                var actionButtonTapped = false
                notesViewModel.deleteNote(note)
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
                        when (!actionButtonTapped) {
                            (note?.imagePath?.isNotEmpty()) -> {
                                val toDelete = File(note.imagePath)
                                if (toDelete.exists()) {
                                    toDelete.delete()
                                }
                            }
                            else -> {}
                        }
                        super.onDismissed(transientBottomBar, event)
                    }

                    override fun onShown(transientBottomBar: Snackbar?) {
                        transientBottomBar?.setAction("실행취소") {
                            notesViewModel.saveNote(note)
                            noteBinding.noDataText.isVisible = false
                            actionButtonTapped = true
                        }
                        super.onShown(transientBottomBar)
                    }
                }).apply {
                    animationMode = Snackbar.ANIMATION_MODE_FADE
                    setAnchorView(R.id.addFab)
                }
                snackBar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.md_blue_grey_200))
                snackBar.show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun clearText() {
        noteBinding.search.apply {
            text.clear()
            hideKeyboard()
            clearFocus()
            observerDataChanges()
        }
    }
}