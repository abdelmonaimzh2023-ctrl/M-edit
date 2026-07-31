package com.montage.app.bridge

import android.net.Uri
import android.webkit.JavascriptInterface
import com.montage.app.engine.VideoClipper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class WebViewBridge(private val activity: WebViewActivity) {

    private val scope = CoroutineScope(Dispatchers.Main)

    @JavascriptInterface
    fun openFilePicker(type: String) {
        activity.launchFilePicker(type)
    }

    @JavascriptInterface
    fun trimVideo(startMs: Long, endMs: Long) {
        scope.launch {
            val inputUri = activity.currentVideoUri ?: return@launch
            val outputFile = File(activity.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
            val success = withContext(Dispatchers.IO) {
                VideoClipper.trimVideo(activity, inputUri, outputFile, startMs, endMs)
            }
            if (success) {
                activity.loadVideo(Uri.fromFile(outputFile))
                activity.callJS("onOperationComplete", "trim", "success")
            } else {
                activity.callJS("onExportError", "فشل القص")
            }
        }
    }

    @JavascriptInterface
    fun changeSpeed(factor: Double) {
        activity.currentSpeed = factor
        activity.callJS("onOperationComplete", "speed", "applied")
    }

    @JavascriptInterface
    fun rotateVideo(degrees: Int) {
        activity.currentRotation = (activity.currentRotation + degrees) % 360
        activity.callJS("onOperationComplete", "rotate", "applied")
    }

    @JavascriptInterface
    fun flipVideo(direction: String) {
        activity.currentFlipH = !activity.currentFlipH
        activity.callJS("onOperationComplete", "flip", "applied")
    }

    @JavascriptInterface
    fun cropVideo(aspect: String) {
        activity.currentAspect = aspect
        activity.callJS("onOperationComplete", "crop", "applied")
    }

    @JavascriptInterface
    fun setFilter(name: String) {
        activity.currentFilter = name
        activity.callJS("onOperationComplete", "filter", "applied")
    }

    @JavascriptInterface
    fun adjustColor(paramsJson: String) {
        try {
            val json = JSONObject(paramsJson)
            activity.brightness = json.optDouble("brightness", 0.0).toFloat()
            activity.contrast = json.optDouble("contrast", 0.0).toFloat()
            activity.saturation = json.optDouble("saturation", 0.0).toFloat()
            activity.callJS("onOperationComplete", "color", "applied")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun addTextLayer(paramsJson: String) {
        activity.textLayers.add(paramsJson)
        activity.callJS("onOperationComplete", "textLayer", "added")
    }

    @JavascriptInterface
    fun addImageOverlay(paramsJson: String) {
        activity.imageLayers.add(paramsJson)
        activity.callJS("onOperationComplete", "imageOverlay", "added")
    }

    @JavascriptInterface
    fun addAudioTrack(paramsJson: String) {
        activity.audioTracks.add(paramsJson)
        activity.callJS("onOperationComplete", "audioTrack", "added")
    }

    @JavascriptInterface
    fun removeLayer(id: String) {
        activity.textLayers.removeAll { it.contains("\"$id\"") }
        activity.imageLayers.removeAll { it.contains("\"$id\"") }
        activity.callJS("onOperationComplete", "removeLayer", "removed")
    }

    @JavascriptInterface
    fun setTransition(name: String) {
        activity.currentTransition = name
        activity.callJS("onOperationComplete", "transition", "applied")
    }

    @JavascriptInterface
    fun undo() {
        activity.callJS("onOperationComplete", "undo", "applied")
    }

    @JavascriptInterface
    fun redo() {
        activity.callJS("onOperationComplete", "redo", "applied")
    }

    @JavascriptInterface
    fun exportVideo(optionsJson: String) {
        scope.launch {
            try {
                val inputUri = activity.currentVideoUri ?: return@launch
                val outputFile = File(activity.getExternalFilesDir("exports"), "M-edit_${System.currentTimeMillis()}.mp4")
                val success = withContext(Dispatchers.IO) {
                    VideoClipper.trimVideo(activity, inputUri, outputFile, 0, Long.MAX_VALUE)
                }
                if (success) {
                    activity.callJS("onExportComplete", outputFile.absolutePath)
                } else {
                    activity.callJS("onExportError", "فشل التصدير")
                }
            } catch (e: Exception) {
                activity.callJS("onExportError", e.message ?: "خطأ غير متوقع")
            }
        }
    }

    @JavascriptInterface
    fun cancelExport() {
        activity.isExporting = false
    }

    @JavascriptInterface
    fun shareExport() {
        activity.shareLastExported()
    }

    @JavascriptInterface
    fun goBack() {
        activity.finish()
    }
}
