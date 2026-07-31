package com.montage.app.ui.editor

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.montage.app.R

class EditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val projectId = intent.getLongExtra("project_id", -1)
        // لاحقاً نحمّل المشروع من قاعدة البيانات ونشغل الفيديو

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
