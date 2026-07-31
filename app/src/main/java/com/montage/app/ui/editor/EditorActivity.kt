package com.montage.app.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.slider.RangeSlider
import com.montage.app.data.db.AppDatabase
import com.montage.app.databinding.ActivityEditorBinding
import com.montage.app.ui.export.ExportSettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var player: ExoPlayer? = null
    private var videoUri: Uri? = null
    private var projectId: Long = -1
    private var videoDuration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getLongExtra("project_id", -1)
        setupPlayer()

        binding.btnPlay.setOnClickListener {
            player?.play()
        }

        binding.btnExport.setOnClickListener {
            val intent = Intent(this, ExportSettingsActivity::class.java)
            intent.putExtra("project_id", projectId)
            intent.putExtra("video_uri", videoUri.toString())
            startActivity(intent)
        }
    }

    private fun setupPlayer() {
        lifecycleScope.launch {
            val project = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@EditorActivity).projectDao().getProjectById(projectId)
            } ?: return@launch

            videoUri = Uri.parse(project.videoUri)

            player = ExoPlayer.Builder(this@EditorActivity).build().also { exoPlayer ->
                binding.playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(videoUri!!)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
