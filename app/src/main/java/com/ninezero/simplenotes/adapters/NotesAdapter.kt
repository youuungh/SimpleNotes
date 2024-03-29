package com.ninezero.simplenotes.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ninezero.simplenotes.R
import com.ninezero.simplenotes.databinding.NoteItemLayoutBinding
import com.ninezero.simplenotes.ui.fragments.NoteFragmentDirections
import com.ninezero.simplenotes.model.Note
import com.ninezero.simplenotes.utils.hideKeyboard
import com.ninezero.simplenotes.utils.loadHiRezThumbnail
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import org.commonmark.node.SoftLineBreak
import java.io.File

class NotesAdapter: androidx.recyclerview.widget.ListAdapter<Note, NotesAdapter.NotesViewHolder>(DiffUtilCallback()) {

    inner class NotesViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val contentBinding = NoteItemLayoutBinding.bind(itemView)
        val title: MaterialTextView = contentBinding.noteItemTitle
        val content: TextView = contentBinding.noteContentItemTitle
        val date: MaterialTextView = contentBinding.noteDate
        val image: ImageView = contentBinding.itemNoteImage
        val parent: MaterialCardView = contentBinding.noteItemLayoutParent
        val markWon = Markwon.builder(itemView.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(itemView.context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    super.configureVisitor(builder)
                    builder.on(SoftLineBreak::class.java){ visitor, _ -> visitor.forceNewLine() }
                }
            }).build()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        return NotesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_item_layout, parent, false))
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        getItem(position).let { note ->
            holder.apply {
                itemView.transitionName = "recyclerView_${note.id}"
                title.text = note.title
                markWon.setMarkdown(content, note.content)
                date.text = note.date
                if (note.imagePath != null) {
                    image.visibility = View.VISIBLE
                    val uri = Uri.fromFile(File(note.imagePath))
                    if (File(note.imagePath).exists())
                        itemView.context.loadHiRezThumbnail(uri, image)
                } else {
                    Glide.with(itemView).clear(image)
                    image.isVisible = false
                }

                parent.setCardBackgroundColor(note.color)

                itemView.setOnClickListener {
                    val action = NoteFragmentDirections.actionNoteFragmentToNoteContentFragment().setNote(note)
                    val extras = FragmentNavigatorExtras(itemView to "recyclerView_${note.id}")
                    it.hideKeyboard()
                    Navigation.findNavController(it).navigate(action, extras)
                }
            }
        }
    }
}