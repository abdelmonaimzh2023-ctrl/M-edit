package com.montage.app.engine

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class ClipSegment(
    val uri: Uri,
    val startMs: Long,
    val endMs: Long
)

object VideoClipper {

    suspend fun mergeClips(
        context: Context,
        clips: List<ClipSegment>,
        outputFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) return@withContext false

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        val trackMap = mutableMapOf<Int, Int>() // oldTrackIndex -> newTrackIndex
        var baseTimeUs = 0L

        try {
            for ((index, clip) in clips.withIndex()) {
                val extractor = MediaExtractor()
                context.contentResolver.openFileDescriptor(clip.uri, "r")?.use { fd ->
                    extractor.setDataSource(fd.fileDescriptor)
                } ?: continue

                // اختيار المسارات
                val tracks = mutableListOf<Int>()
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        tracks.add(i)
                    }
                }

                extractor.seekTo(clip.startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                if (!muxerStarted) {
                    for (trackIndex in tracks) {
                        val format = extractor.getTrackFormat(trackIndex)
                        val newIndex = muxer.addTrack(format)
                        trackMap[trackIndex] = newIndex
                    }
                    muxer.start()
                    muxerStarted = true
                }

                val buffer = ByteBuffer.allocate(512 * 1024)
                val bufferInfo = android.media.MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size < 0) break

                    val currentTimeMs = extractor.sampleTime / 1000
                    if (currentTimeMs > clip.endMs) break

                    val trackIndex = extractor.sampleTrackIndex
                    val newTrackIndex = trackMap[trackIndex] ?: continue

                    bufferInfo.presentationTimeUs = extractor.sampleTime - (clip.startMs * 1000) + baseTimeUs
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(newTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                // تحديث الوقت الأساسي للدمج
                if (index < clips.size - 1) {
                    val lastSampleTime = if (extractor.sampleTime > 0) extractor.sampleTime else 0
                    baseTimeUs += lastSampleTime - (clip.startMs * 1000)
                }

                extractor.release()
                onProgress(((index + 1).toFloat() / clips.size * 100).toInt())
            }

            muxer.stop()
            muxer.release()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            muxer.release()
            return@withContext false
        }
    }
}
