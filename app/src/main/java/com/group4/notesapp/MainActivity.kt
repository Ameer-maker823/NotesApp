package com.group4.notesapp

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.group4.notesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}