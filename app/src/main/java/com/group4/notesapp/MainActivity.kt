package com.group4.notesapp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.group4.notesapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NotesViewModel by viewModels()
    private val adapter = NoteAdapter(onClick = {}, onLongClick = {})

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.fabAdd.setOnClickListener { viewModel.addSample() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect { list ->
                    if (list == null) return@collect
                    adapter.submitList(list)
                    binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}