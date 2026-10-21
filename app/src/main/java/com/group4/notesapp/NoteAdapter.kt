package com.group4.notesapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.group4.notesapp.data.Note
import com.group4.notesapp.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("EEE, h:mm a", Locale.getDefault())

    var selectedIds: Set<Int> = emptySet()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, SELECTION)
        }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.tvNoteTitle.text = note.title.ifBlank { "Untitled" }
            binding.tvNoteContent.text = note.content
            binding.tvNoteDate.text = dateFormat.format(Date(note.updatedAt))
            binding.noteCard.setOnClickListener { onClick(note) }
            binding.noteCard.setOnLongClickListener {
                onLongClick(note)
                true
            }
            applySelection(note, animate = false)
        }

        fun applySelection(note: Note, animate: Boolean) {
            val selected = note.id in selectedIds
            val density = itemView.resources.displayMetrics.density
            val scale = if (selected) 0.965f else 1f
            val badgeAlpha = if (selected) 1f else 0f

            binding.noteCard.background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(ColorUtils.setAlphaComponent(note.color, 0x33))
                setStroke(
                    ((if (selected) 2 else 1) * density).toInt(),
                    if (selected) Color.WHITE else ColorUtils.setAlphaComponent(note.color, 0x80)
                )
            }
            binding.ivPin.visibility = if (note.pinned && !selected) View.VISIBLE else View.GONE

            if (animate) {
                binding.noteCard.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
                binding.ivBadge.animate().alpha(badgeAlpha).setDuration(200).start()
            } else {
                binding.noteCard.scaleX = scale
                binding.noteCard.scaleY = scale
                binding.ivBadge.alpha = badgeAlpha
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(SELECTION)) {
            holder.applySelection(getItem(position), animate = true)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(old: Note, new: Note) = old.id == new.id
        override fun areContentsTheSame(old: Note, new: Note) = old == new
    }

    private companion object {
        const val SELECTION = "selection"
    }
}