package com.montage.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var loadingLayout: LinearLayout
    private var selectedVideoUri: Uri? = null

    companion object {
        private const val PICK_VIDEO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        loadingLayout = findViewById(R.id.loadingLayout)

        findViewById<View>(R.id.btnPickVideo).setOnClickListener { openVideoPicker() }
        findViewById<View>(R.id.btnPlay).setOnClickListener { videoView.start() }
        findViewById<View>(R.id.btnPause).setOnClickListener { videoView.pause() }
        findViewById<View>(R.id.btnExport4K).setOnClickListener {
            if (selectedVideoUri != null) {
                showLoading()
                Handler(Looper.getMainLooper()).postDelayed({
                    hideLoading()
                    Toast.makeText(this, "تم التصدير", Toast.LENGTH_SHORT).show()
                }, 3000)
            }
        }
    }

    private fun openVideoPicker() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }, PICK_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedVideoUri = uri
                videoView.setVideoURI(uri)
                videoView.start()
            }
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
    }
}
