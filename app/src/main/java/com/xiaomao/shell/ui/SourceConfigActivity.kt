package com.xiaomao.shell.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xiaomao.shell.AppContainer
import com.xiaomao.shell.R
import com.xiaomao.shell.config.AppConfig
import com.xiaomao.shell.databinding.ActivitySourceConfigBinding
import kotlinx.coroutines.launch

class SourceConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySourceConfigBinding
    private val repository by lazy { AppContainer.repository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySourceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.action_source_config)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.textRemoteInfo.text =
            getString(R.string.message_source_remote, AppConfig.defaultRemoteSourceUrl)
        binding.editSource.setText(repository.currentSourceText())

        binding.buttonSave.setOnClickListener {
            val sourceText = binding.editSource.text?.toString().orEmpty()
            if (sourceText.isBlank()) {
                Toast.makeText(this, R.string.message_source_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            repository.saveSourceText(sourceText)
            Toast.makeText(this, R.string.message_source_saved, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }

        binding.buttonResetDefault.setOnClickListener {
            binding.editSource.setText(repository.resetSourceText())
            Toast.makeText(this, R.string.message_source_reset, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
        }

        binding.buttonSyncRemote.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val source = repository.syncDefaultRemoteSource()
                    binding.editSource.setText(source)
                    Toast.makeText(
                        this@SourceConfigActivity,
                        R.string.message_source_synced,
                        Toast.LENGTH_SHORT,
                    ).show()
                    setResult(Activity.RESULT_OK)
                } catch (error: Throwable) {
                    Toast.makeText(
                        this@SourceConfigActivity,
                        getString(R.string.message_load_failed, error.message ?: getString(R.string.message_unknown_error)),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
}
