package com.montage.app.bridge

import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import com.montage.app.engine.VideoClipper
import com.montage.app.engine.ExportEngine
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class WebViewBridge(private val context: Context, private val activity: WebViewActivity) {

    @JavascriptInterface
    fun openFilePicker(type: String) {
        activity.launchFilePicker(type)
    }

    @JavascriptInterface
    fun trimVideo(startMs: Long, endMs: Long) {
        activity.lifecycleScope.launch {
            val inputUri = activity.currentVideoUri ?: return@launch
            val outputFile = File(activity.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
            val success = VideoClipper.trimVideo(activity, inputUri, outputFile, startMs, endMs)
            activity.runOnUiThread {
                if (success) {
                    activity.loadVideo(Uri.fromFile(outputFile))
                    activity.callJS("onOperationComplete", "trim", "success")
                } else {
                    activity.callJS("onExportError", "فشل القص")
                }
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
        try {
            val options = JSONObject(optionsJson)
            activity.lifecycleScope.launch {
                val inputUri = activity.currentVideoUri ?: return@launch
                val outputFile = File(activity.getExternalFilesDir("exports"), "M-edit_${System.currentTimeMillis()}.mp4")
                ExportEngine.simpleExport(
                    context = activity,
                    inputUri = inputUri,
                    outputFile = outputFile
                )
                activity.runOnUiThread {
                    activity.callJS("onExportComplete", outputFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            activity.callJS("onExportError", e.message ?: "خطأ في التصدير")
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
