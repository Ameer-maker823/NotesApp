package com.group4.notesapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.group4.notesapp.data.AppDatabase
import com.group4.notesapp.data.Note
import com.group4.notesapp.databinding.ActivityEditNoteBinding
import kotlinx.coroutines.launch

class EditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditNoteBinding
    private val dao by lazy { AppDatabase.getInstance(applicationContext).noteDao() }
    private val palette by lazy {
        listOf(
            R.color.note_teal, R.color.note_salmon, R.color.note_pink,
            R.color.note_peach, R.color.note_purple
        ).map { ContextCompat.getColor(this, it) }
    }

    private var noteId = -1
    private var color = 0
    private var original: Note? = null
    private var loaded = false
    private var saving = false

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            saveAndClose()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, backCallback)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.content.setPadding(
                bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom)
            )
            insets
        }

        noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1)
        loaded = noteId == -1
        color = savedInstanceState?.getInt(KEY_COLOR, 0)?.takeIf { it != 0 } ?: palette.random()

        buildColorRow()
        refreshColor()

        binding.btnBack.setOnClickListener { saveAndClose() }
        binding.btnDone.setOnClickListener { saveAndClose() }

        if (noteId == -1) {
            binding.etTitle.requestFocus()
            binding.etTitle.post {
                WindowCompat.getInsetsController(window, binding.etTitle)
                    .show(WindowInsetsCompat.Type.ime())
            }
        } else {
            lifecycleScope.launch {
                val note = dao.getNoteById(noteId)
                if (note != null) {
                    original = note
                    if (savedInstanceState == null) {
                        binding.etTitle.setText(note.title)
                        binding.etContent.setText(note.content)
                        color = note.color
                        refreshColor()
                    }
                }
                loaded = true
            }
        }

        if (savedInstanceState == null) {
            val rise = dp(24).toFloat()
            binding.editorCard.alpha = 0f
            binding.editorCard.translationY = rise
            binding.editorCard.animate().alpha(1f).translationY(0f).setDuration(350).start()

            binding.colorRow.alpha = 0f
            binding.colorRow.translationY = rise
            binding.colorRow.animate().alpha(1f).translationY(0f)
                .setStartDelay(120).setDuration(350).start()
        }
    }

    private fun buildColorRow() {
        val size = dp(34)
        palette.forEach { c ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = dp(8)
                    marginEnd = dp(8)
                }
                contentDescription = getString(R.string.cd_color)
                setOnClickListener {
                    color = c
                    refreshColor()
                }
            }
            binding.colorRow.addView(dot)
        }
    }

    private fun refreshColor() {
        val selected = color
        binding.editorCard.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(ColorUtils.setAlphaComponent(selected, 0x33))
            setStroke(dp(1), ColorUtils.setAlphaComponent(selected, 0x80))
        }
        for (i in 0 until binding.colorRow.childCount) {
            val dot = binding.colorRow.getChildAt(i)
            val c = palette[i]
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(c)
                if (c == selected) setStroke(dp(3), Color.WHITE)
            }
        }
    }

    private fun saveAndClose() {
        if (saving) return
        if (!loaded) {
            finish()
            return
        }
        saving = true
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val existing = original

        lifecycleScope.launch {
            when {
                title.isEmpty() && content.isEmpty() ->
                    existing?.let { dao.delete(listOf(it)) }
                existing == null ->
                    dao.insert(Note(title = title, content = content, color = color))
                existing.title != title || existing.content != content || existing.color != color ->
                    dao.update(
                        existing.copy(
                            title = title,
                            content = content,
                            color = color,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_COLOR, color)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        private const val KEY_COLOR = "color"
    }
}