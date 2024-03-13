package com.ninezero.simplenotes.adapters

import androidx.recyclerview.widget.DiffUtil
import com.ninezero.simplenotes.model.Note

class DiffUtilCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    override fun getChangePayload(oldItem: Note, newItem: Note): Any? {
        return super.getChangePayload(oldItem, newItem)
    }
}