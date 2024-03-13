package com.example.simplenotes.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.example.simplenotes.R
import com.example.simplenotes.ui.activities.MainActivity
import com.example.simplenotes.databinding.BottomSheetDialogBinding
import com.example.simplenotes.databinding.FragmentNoteContentBinding
import com.example.simplenotes.model.Note
import com.example.simplenotes.utils.asyncImageLoader
import com.example.simplenotes.utils.getImageUriWithAuthority
import com.example.simplenotes.utils.getPhotoFile
import com.example.simplenotes.utils.hideKeyboard
import com.example.simplenotes.utils.shortToast
import com.example.simplenotes.viewModel.NotesViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class NoteContentFragment : Fragment(R.layout.fragment_note_content) {

    private val notesViewModel: NotesViewModel by activityViewModels()
    private val args: NoteContentFragmentArgs by navArgs()

    private lateinit var navController: NavController
    private lateinit var contentBinding: FragmentNoteContentBinding
    private lateinit var result: String
    private lateinit var photoFile: File
    private var note: Note? = null
    private var color = -1
    private val currentDate = SimpleDateFormat.getDateInstance().format(Date())
    private val job = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animation = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment
            duration = 300L
            scrimColor = Color.TRANSPARENT
        }
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
        addSharedElementListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        contentBinding = FragmentNoteContentBinding.bind(view)

        ViewCompat.setTransitionName(
            contentBinding.noteContentFragmentParent, "recyclerView_${args.note?.id}"
        )

        registerForContextMenu(contentBinding.noteImage)

        contentBinding.backButton.setOnClickListener {
            requireView().hideKeyboard()
            saveNoteAndGoBack()
        }

        setUpNote()
        setUpClickListeners()
        handleEditTextFocus()
        handleBackPress()
    }

    private fun addSharedElementListener() {
        (sharedElementEnterTransition as Transition).addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    super.onTransitionStart(transition)
                    if (args.note?.imagePath != null) {
                        contentBinding.noteImage.isVisible = true
                        val uri = Uri.fromFile(File(args.note?.imagePath!!))
                        job.launch {
                            requireContext().asyncImageLoader(uri, contentBinding.noteImage, this)
                        }
                    } else contentBinding.noteImage.isVisible = false
                }
            }
        )
    }

    private fun setUpClickListeners() {
        contentBinding.optionButton.setOnClickListener {
            showBottomSheetDialog()
        }

        contentBinding.deleteImageButton.setOnClickListener { deleteImage() }
    }

    private fun handleEditTextFocus() {
        contentBinding.edtNoteContent.setOnFocusChangeListener { _, hasFocus ->
            contentBinding.bottomBar.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }
    }

    private fun saveNoteAndGoBack() {
        if (contentBinding.edtTitle.text.toString().isEmpty() && contentBinding.edtNoteContent.text.toString().isEmpty()) {
            result = "빈 노트가 삭제됨"
            setFragmentResult("key", bundleOf("bundleKey" to result))
            navController.navigate(NoteContentFragmentDirections.actionNoteContentFragmentToNoteFragment())
        } else {
            note = args.note
            when (note) {
                null -> {
                    saveNewNote()
                    setFragmentResult("key", bundleOf("bundleKey" to result))
                    navController.popBackStack()
                }
                else -> {
                    updateNote()
                    navController.popBackStack()
                }
            }
        }
    }

    private fun saveNewNote() {
        notesViewModel.saveNote(
            Note(
                0,
                contentBinding.edtTitle.text.toString(),
                contentBinding.edtNoteContent.getMD(),
                currentDate,
                color,
                notesViewModel.setImagePath()
            )
        )
        result = "노트가 저장됨"
    }

    private fun updateNote() {
        note?.let {
            notesViewModel.updateNote(
                Note(
                    note!!.id,
                    contentBinding.edtTitle.text.toString(),
                    contentBinding.edtNoteContent.getMD(),
                    currentDate,
                    color,
                    notesViewModel.setImagePath()
                )
            )
        }
    }

    private fun setUpNote() {
        val note = args.note
        val title = contentBinding.edtTitle
        val content = contentBinding.edtNoteContent
        val lastEdited = contentBinding.lastEdited
        val savedImage = notesViewModel.setImagePath()

        if (note == null) {
            lastEdited.text = getString(R.string.edited_on, SimpleDateFormat.getDateInstance().format(Date()))
            setImage(notesViewModel.setImagePath())
        }

        if (note != null) {
            title.setText(note.title)
            content.renderMD(note.content)
            lastEdited.text = getString(R.string.edited_on, note.date)
            color = note.color

            if (savedImage != null) {
                setImage(savedImage)
            } else
                notesViewModel.saveImagePath(note.imagePath)

            contentBinding.apply {
                job.launch {
                    delay(10)
                    noteContentFragmentParent.setBackgroundColor(color)
                    noteImage.isVisible = true
                    note.imagePath?.let { deleteImageButton.isVisible = true }
                }
                toolbarNoteContent.setBackgroundColor(color)
                bottomBar.setBackgroundColor(color)
            }
            activity?.window?.statusBarColor = note.color
        }
    }

    private fun setImage(filePath: String?) {
        if (filePath != null) {
            val uri = Uri.fromFile(File(filePath))
            contentBinding.noteImage.isVisible = true
            contentBinding.deleteImageButton.isVisible = true
            try {
                job.launch {
                    requireContext().asyncImageLoader(uri, contentBinding.noteImage, this)
                }
            } catch (e: Exception) {
                context?.shortToast(e.message)
                contentBinding.noteImage.isVisible = false
                contentBinding.deleteImageButton.isVisible = false
            }
        } else {
            contentBinding.noteImage.isVisible = false
            contentBinding.deleteImageButton.isVisible = false
        }
    }

    private fun deleteImage() {
        note?.imagePath?.let {
            val toDelete = File(note?.imagePath!!)
            if (toDelete.exists()) toDelete.delete()
        }
        notesViewModel.setImagePath()?.let {
            val toDelete = File(notesViewModel.setImagePath()!!)
            if (toDelete.exists()) toDelete.delete()
            notesViewModel.saveImagePath(null)
        }

        contentBinding.noteImage.isVisible = false
        contentBinding.deleteImageButton.isVisible = false
        updateNote()
        context?.shortToast("이미지 삭제됨")
    }

    @SuppressLint("QueryPermissionsNeeded")
    @Suppress("DEPRECATION")
    private fun takePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { captureIntent ->
            photoFile = getPhotoFile(requireActivity())
            val fileProvider = FileProvider.getUriForFile(requireContext(), getString(R.string.fileAuthority), photoFile)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            captureIntent.resolveActivity(activity?.packageManager!!.also {
                startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE)
            })
        }
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val bottomSheetView: View = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
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
                        requireActivity().window.statusBarColor = color
                    }
                }
            }

            bottomSheetView.post {
                bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            camera.setOnClickListener {
                val permission = ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    val permissionArray = arrayOf(Manifest.permission.CAMERA)
                    ActivityCompat.requestPermissions(requireActivity(), permissionArray, REQUEST_IMAGE_CAPTURE)
                    ActivityCompat.OnRequestPermissionsResultCallback { requestCode, permissions, grantResults ->
                        when (requestCode) {
                            REQUEST_IMAGE_CAPTURE -> {
                                if (permissions[0] == Manifest.permission.CAMERA && grantResults.isNotEmpty()) {
                                    Log.d("tag", "this function is called")
                                    takePictureIntent()
                                }
                            }
                        }
                    }
                }
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    takePictureIntent()
                    bottomSheetDialog.dismiss()
                }
            }

            @Suppress("DEPRECATION")
            selectImage.setOnClickListener {
//                    Intent(Intent.ACTION_GET_CONTENT).also { chooseIntent ->
//                        chooseIntent.type = "image/*"
//                        chooseIntent.resolveActivity(activity.packageManager!!.also {
//                            startActivityForResult(chooseIntent, SELECT_IMAGE_FROM_STORAGE)
//                        })
//                    }
                Intent(MediaStore.ACTION_PICK_IMAGES).also { chooseIntent ->
                    chooseIntent.type = "image/*"
                    chooseIntent.resolveActivity(requireActivity().packageManager!!.also {
                        startActivityForResult(chooseIntent, SELECT_IMAGE_FROM_STORAGE)
                    })
                }
                bottomSheetDialog.dismiss()
            }
        }

        with(bottomSheetDialog) {
            setContentView(bottomSheetView)
            show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            notesViewModel.saveImagePath(photoFile.absolutePath)
            setImage(photoFile.absolutePath)
        } else if (requestCode == SELECT_IMAGE_FROM_STORAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            Log.d("TAG", uri.toString())
            uri?.let {
                val selectedImagePath = getImageUriWithAuthority(requireContext(), uri, requireActivity())
                notesViewModel.saveImagePath(selectedImagePath)
                setImage(selectedImagePath)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveNoteAndGoBack()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (job.isActive) {
            job.cancel()
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val SELECT_IMAGE_FROM_STORAGE = 101
    }
}