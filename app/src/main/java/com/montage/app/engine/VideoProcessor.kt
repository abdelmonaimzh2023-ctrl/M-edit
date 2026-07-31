package com.montage.app.engine

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoProcessor {

    /**
     * قص فيديو بدون إعادة ترميز (يحافظ على الجودة الأصلية 100%)
     * @param context السياق
     * @param inputUri مسار الفيديو المدخل
     * @param outputFile ملف الإخراج
     * @param startMs وقت البداية بالميلي ثانية
     * @param endMs وقت النهاية بالميلي ثانية
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { fd ->
                extractor.setDataSource(fd.fileDescriptor)
            } ?: return@withContext false

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val numTracks = extractor.trackCount
            val trackIndices = mutableMapOf<Int, Int>() // old index -> new index

            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val newIndex = muxer.addTrack(format)
                    trackIndices[i] = newIndex
                }
            }

            if (trackIndices.isEmpty()) {
                muxer.release()
                extractor.release()
                return@withContext false
            }

            // قص: التقدم إلى البداية
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            muxer.start()

            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var outputStarted = false

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(extractor.sampleData!!, 0)

                if (bufferInfo.size < 0) break

                val currentSampleTime = extractor.sampleTime / 1000 // to ms
                if (currentSampleTime > endMs) break

                val trackIndex = extractor.sampleTrackIndex
                val newTrackIndex = trackIndices[trackIndex] ?: continue

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(newTrackIndex, extractor.sampleData!!, bufferInfo)
                extractor.advance()
                outputStarted = true
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return@withContext outputStarted
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
