package com.group4.notesapp

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.group4.notesapp.data.Note
import com.group4.notesapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NotesViewModel by viewModels()
    private val adapter = NoteAdapter(
        onClick = { onNoteClick(it) },
        onLongClick = { onNoteLongClick(it) }
    )

    private var selectionShown: Boolean? = null
    private var searchShown: Boolean? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (viewModel.selected.value.isNotEmpty()) {
                viewModel.clearSelection()
            } else {
                viewModel.setSearching(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, backCallback)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.header.setPadding(
                bars.left + dp(16), bars.top + dp(12), bars.right + dp(16), dp(8)
            )
            binding.rvNotes.setPadding(
                bars.left + dp(14), dp(4), bars.right + dp(14), bars.bottom + dp(120)
            )
            (binding.fabAdd.layoutParams as FrameLayout.LayoutParams).apply {
                rightMargin = bars.right + dp(18)
                bottomMargin = bars.bottom + dp(18)
            }
            (binding.actionBar.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = bars.left + dp(16)
                rightMargin = bars.right + dp(16)
                bottomMargin = bars.bottom + dp(16)
            }
            binding.fabAdd.requestLayout()
            binding.actionBar.requestLayout()
            insets
        }

        binding.rvNotes.adapter = adapter
        binding.fabAdd.setOnClickListener { openEditor(-1) }
        binding.actionPin.setOnClickListener { viewModel.togglePinSelected() }
        binding.actionDelete.setOnClickListener { deleteSelected() }
        binding.actionShare.setOnClickListener { shareSelected() }

        binding.btnSearch.setOnClickListener {
            when {
                viewModel.selected.value.isNotEmpty() -> viewModel.clearSelection()
                viewModel.searching.value -> viewModel.setSearching(false)
                else -> {
                    viewModel.setSearching(true)
                    binding.etSearch.post {
                        binding.etSearch.requestFocus()
                        WindowCompat.getInsetsController(window, binding.etSearch)
                            .show(WindowInsetsCompat.Type.ime())
                    }
                }
            }
        }

        binding.etSearch.setText(viewModel.query.value)
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notes.collect { list ->
                        if (list == null) return@collect
                        adapter.submitList(list)
                        binding.tvEmpty.setText(
                            if (viewModel.query.value.isBlank()) R.string.empty_notes
                            else R.string.empty_search
                        )
                        binding.emptyState.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch { viewModel.selected.collect { renderSelection(it) } }
                launch { viewModel.searching.collect { renderHeader() } }
            }
        }
    }

    private fun onNoteClick(note: Note) {
        if (viewModel.selected.value.isNotEmpty()) {
            viewModel.toggleSelection(note.id)
        } else {
            openEditor(note.id)
        }
    }

    private fun onNoteLongClick(note: Note) {
        binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        viewModel.toggleSelection(note.id)
    }

    private fun openEditor(id: Int) {
        startActivity(
            Intent(this, EditNoteActivity::class.java).putExtra(EditNoteActivity.EXTRA_NOTE_ID, id)
        )
    }

    private fun renderSelection(ids: Set<Int>) {
        val active = ids.isNotEmpty()
        adapter.selectedIds = ids

        val chosen = viewModel.selectedNotes()
        val allPinned = chosen.isNotEmpty() && chosen.all { it.pinned }
        binding.tvActionPin.setText(if (allPinned) R.string.action_unpin else R.string.action_pin)

        if (selectionShown != active) {
            val animate = selectionShown != null
            setShown(binding.actionBar, active, animate, dp(18).toFloat())
            setShown(binding.fabAdd, !active, animate, 0f)
        }
        selectionShown = active
        renderHeader()
    }

    private fun renderHeader() {
        val ids = viewModel.selected.value
        val selecting = ids.isNotEmpty()
        val searching = viewModel.searching.value
        val showField = searching && !selecting

        binding.tvTitle.text =
            if (selecting) getString(R.string.selected_count, ids.size)
            else getString(R.string.title_notes)
        binding.tvTitle.visibility = if (showField) View.GONE else View.VISIBLE

        binding.btnSearch.setImageResource(
            if (selecting || searching) R.drawable.ic_close else R.drawable.ic_search
        )
        binding.btnSearch.contentDescription = getString(
            when {
                selecting -> R.string.cd_clear_selection
                searching -> R.string.cd_close_search
                else -> R.string.cd_search
            }
        )
        backCallback.isEnabled = selecting || searching
        setSearchField(showField, searching)
    }

    private fun setSearchField(show: Boolean, searching: Boolean) {
        if (!searching && binding.etSearch.text.isNotEmpty()) binding.etSearch.setText("")
        if (searchShown == show) return
        val animate = searchShown != null
        searchShown = show
        val field = binding.etSearch
        field.animate().cancel()

        if (show) {
            field.visibility = View.VISIBLE
            if (animate) {
                field.alpha = 0f
                field.translationX = dp(24).toFloat()
                field.animate().alpha(1f).translationX(0f).setDuration(250).start()
            } else {
                field.alpha = 1f
                field.translationX = 0f
            }
        } else {
            hideKeyboard()
            if (animate) {
                field.animate().alpha(0f).setDuration(150)
                    .withEndAction { field.visibility = View.GONE }
                    .start()
            } else {
                field.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(window, binding.etSearch)
            .hide(WindowInsetsCompat.Type.ime())
        binding.etSearch.clearFocus()
    }

    private fun setShown(view: View, shown: Boolean, animate: Boolean, offset: Float) {
        view.animate().cancel()
        if (!animate) {
            view.visibility = if (shown) View.VISIBLE else View.GONE
            view.alpha = if (shown) 1f else 0f
            view.translationY = if (shown) 0f else offset
            return
        }
        if (shown) {
            if (view.visibility != View.VISIBLE) {
                view.alpha = 0f
                view.translationY = offset
                view.visibility = View.VISIBLE
            }
            view.animate().alpha(1f).translationY(0f).setDuration(350).start()
        } else {
            view.animate().alpha(0f).translationY(offset).setDuration(250)
                .withEndAction { view.visibility = View.GONE }
                .start()
        }
    }

    private fun deleteSelected() {
        val deleted = viewModel.deleteSelected()
        if (deleted.isEmpty()) return
        val message = if (deleted.size == 1) {
            getString(R.string.note_deleted)
        } else {
            getString(R.string.notes_deleted, deleted.size)
        }
        Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(0xF01F1B2E.toInt())
            .setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            .setActionTextColor(ContextCompat.getColor(this, R.color.accent_soft))
            .setAction(R.string.action_undo) { viewModel.restore(deleted) }
            .apply { (view.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp(88) }
            .show()
    }

    private fun shareSelected() {
        val chosen = viewModel.selectedNotes()
        if (chosen.isEmpty()) return
        val text = chosen.joinToString("\n\n") { note ->
            listOf(note.title, note.content).filter { it.isNotBlank() }.joinToString("\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (chosen.size == 1 && chosen[0].title.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, chosen[0].title)
            }
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)))
        viewModel.clearSelection()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}