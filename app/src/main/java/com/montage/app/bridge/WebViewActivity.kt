package com.montage.app.bridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.montage.app.R

class WebViewActivity : AppCompatActivity() {

    lateinit var webView: WebView
    lateinit var bridge: WebViewBridge
    private var pendingFileType: String? = null

    var currentVideoUri: Uri? = null
    var currentSpeed: Double = 1.0
    var currentRotation: Int = 0
    var currentFlipH: Boolean = false
    var currentAspect: String = "free"
    var currentFilter: String = "original"
    var currentTransition: String = "none"
    var brightness: Float = 0f
    var contrast: Float = 0f
    var saturation: Float = 0f
    val textLayers = mutableListOf<String>()
    val imageLayers = mutableListOf<String>()
    val audioTracks = mutableListOf<String>()
    var isExporting = false
    private var lastExportedPath: String? = null

    companion object {
        private const val PICK_VIDEO = 101
        private const val PICK_IMAGE = 102
        private const val PICK_AUDIO = 103
        private const val PERMISSION_REQUEST = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webview)
        bridge = WebViewBridge(this)

        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Toast.makeText(this@WebViewActivity, "خطأ في التحميل", Toast.LENGTH_SHORT).show()
            }
        }

        webView.loadUrl("file:///android_asset/index.html")

        // طلب الصلاحيات عند البدء
        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO),
                    PERMISSION_REQUEST
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم منح الصلاحيات", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "يحتاج التطبيق للصلاحيات لاختيار الملفات", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun launchFilePicker(type: String) {
        // نطلب الصلاحيات أولاً إذا لم تُمنح
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                requestRequiredPermissions()
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestRequiredPermissions()
                return
            }
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            when(type) {
                "video" -> this.type = "video/*"
                "image" -> this.type = "image/*"
                "audio" -> this.type = "audio/*"
            }
        }
        try {
            when(type) {
                "video" -> startActivityForResult(intent, PICK_VIDEO)
                "image" -> startActivityForResult(intent, PICK_IMAGE)
                "audio" -> startActivityForResult(intent, PICK_AUDIO)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لاختيار الملفات", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadVideo(uri: Uri) {
        currentVideoUri = uri
        callJS("onFilePicked", "video", uri.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        data?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            when(requestCode) {
                PICK_VIDEO -> {
                    currentVideoUri = uri
                    callJS("onFilePicked", "video", uri.toString())
                }
                PICK_IMAGE -> callJS("onFilePicked", "image", uri.toString())
                PICK_AUDIO -> callJS("onFilePicked", "audio", uri.toString())
            }
        }
    }

    fun callJS(function: String, vararg args: Any?) {
        val argsStr = args.joinToString(",") { arg ->
            when(arg) {
                is String -> "\"${arg.replace("\"", "\\\"")}\""
                is Boolean -> if(arg) "true" else "false"
                else -> arg.toString()
            }
        }
        runOnUiThread {
            webView.evaluateJavascript("javascript:$function($argsStr)", null)
        }
    }

    fun shareLastExported() {
        lastExportedPath?.let { path ->
            val uri = Uri.parse(path)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
