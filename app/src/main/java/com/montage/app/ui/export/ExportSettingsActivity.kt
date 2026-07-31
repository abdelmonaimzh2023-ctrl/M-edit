package com.montage.app.ui.export

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.montage.app.R

class ExportSettingsActivity : AppCompatActivity() {
    private var resolution = "1080p"
    private var fps = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_settings)

        val projectId = intent.getLongExtra("project_id", -1)
        val videoUri = intent.getStringExtra("video_uri") ?: return

        findViewById<MaterialButton>(R.id.btn1080p).setOnClickListener { resolution = "1080p" }
        findViewById<MaterialButton>(R.id.btn2k).setOnClickListener { resolution = "2k" }
        findViewById<MaterialButton>(R.id.btn4k).setOnClickListener { resolution = "4k" }
        findViewById<MaterialButton>(R.id.btn30fps).setOnClickListener { fps = 30 }
        findViewById<MaterialButton>(R.id.btn60fps).setOnClickListener { fps = 60 }

        findViewById<MaterialButton>(R.id.btnStartExport).setOnClickListener {
            // بدء نشاط التصدير الفعلي
            val intent = Intent(this, ExportActivity::class.java)
            intent.putExtra("project_id", projectId)
            intent.putExtra("video_uri", videoUri)
            intent.putExtra("resolution", resolution)
            intent.putExtra("fps", fps)
            startActivity(intent)
        }
    }
}
