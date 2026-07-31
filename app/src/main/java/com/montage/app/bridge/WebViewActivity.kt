package com.montage.app.bridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.montage.app.R

class WebViewActivity : AppCompatActivity() {

    lateinit var webView: WebView
    lateinit var bridge: WebViewBridge

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
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                callJS("updateBridgeIndicator", true)
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    fun launchFilePicker(type: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            when(type) {
                "video" -> this.type = "video/*"
                "image" -> this.type = "image/*"
                "audio" -> this.type = "audio/*"
            }
        }
        when(type) {
            "video" -> startActivityForResult(intent, PICK_VIDEO)
            "image" -> startActivityForResult(intent, PICK_IMAGE)
            "audio" -> startActivityForResult(intent, PICK_AUDIO)
        }
    }

    fun loadVideo(uri: Uri) {
        currentVideoUri = uri
        callJS("onVideoLoaded", uri.toString(), 0, 0, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        data?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            when(requestCode) {
                PICK_VIDEO -> {
                    loadVideo(uri)
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
        webView.evaluateJavascript("javascript:$function($argsStr)", null)
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
