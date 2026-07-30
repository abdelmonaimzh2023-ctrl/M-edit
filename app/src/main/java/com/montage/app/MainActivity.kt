package com.montage.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var selectedVideoUri: Uri? = null

    companion object {
        private const val PICK_VIDEO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)

        // زر اختيار الفيديو
        findViewById<Button>(R.id.btnPickVideo).setOnClickListener {
            openVideoPicker()
        }

        // زر التشغيل
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            selectedVideoUri?.let {
                videoView.start()
            } ?: Toast.makeText(this, "اختر فيديو أولاً", Toast.LENGTH_SHORT).show()
        }

        // زر الإيقاف
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            videoView.pause()
        }

        // زر التصدير (سنبرمجه لاحقاً)
        findViewById<Button>(R.id.btnExport4K).setOnClickListener {
            if (selectedVideoUri != null) {
                Toast.makeText(this, "🎉 جاري التطوير... قريباً!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "اختر فيديو أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, PICK_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // نحتفظ بالإذن للقراءة لاحقاً
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedVideoUri = uri
                videoView.setVideoURI(uri)
                videoView.start()
                Toast.makeText(this, "✅ تم تحميل الفيديو", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
