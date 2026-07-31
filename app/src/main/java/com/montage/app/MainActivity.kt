package com.montage.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var tvLoading: TextView
    private var selectedVideoUri: Uri? = null

    companion object {
        private const val PICK_VIDEO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        loadingLayout = findViewById(R.id.loadingLayout)
        tvLoading = findViewById(R.id.tvLoading)

        // أنيميشن دخول الواجهة
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<LinearLayout>(R.id.mainLayout).startAnimation(slideUp)

        // زر اختيار الفيديو
        findViewById<LinearLayout>(R.id.btnPickVideo).setOnClickListener {
            openVideoPicker()
        }

        // زر التشغيل
        findViewById<LinearLayout>(R.id.btnPlay).setOnClickListener {
            selectedVideoUri?.let {
                videoView.start()
                showSnackbar("تم التشغيل", "#4CAF50")
            } ?: showSnackbar("الرجاء اختيار فيديو", "#FF9800")
        }

        // زر الإيقاف
        findViewById<LinearLayout>(R.id.btnPause).setOnClickListener {
            videoView.pause()
            showSnackbar("تم الإيقاف", "#2196F3")
        }

        // زر التصدير
        findViewById<LinearLayout>(R.id.btnExport4K).setOnClickListener {
            if (selectedVideoUri != null) {
                showLoading("جاري معالجة الفيديو...")
                Handler(Looper.getMainLooper()).postDelayed({
                    hideLoading()
                    showSnackbar("جاهز للتصدير 4K", "#4CAF50")
                }, 3000)
            } else {
                showSnackbar("الرجاء اختيار فيديو", "#FF9800")
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
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedVideoUri = uri
                videoView.setVideoURI(uri)
                videoView.start()
                showSnackbar("تم تحميل الفيديو", "#4CAF50")
            }
        }
    }

    private fun showLoading(message: String) {
        tvLoading.text = message
        loadingLayout.visibility = View.VISIBLE
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        loadingLayout.startAnimation(fadeIn)
    }

    private fun hideLoading() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        loadingLayout.startAnimation(fadeOut)
        loadingLayout.visibility = View.GONE
    }

    private fun showSnackbar(message: String, colorHex: String) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(android.graphics.Color.parseColor(colorHex))
        snackbar.setTextColor(android.graphics.Color.WHITE)
        snackbar.show()
    }
}
