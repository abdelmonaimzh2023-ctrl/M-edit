package com.montage.app.engine

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

object VideoClipper {
    suspend fun trimVideo(context: Context, inputUri: Uri, outputFile: File, startMs: Long, endMs: Long): Boolean {
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { fd ->
                extractor.setDataSource(fd.fileDescriptor)
            } ?: return false

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.contains("video") == true ||
                    format.getString(MediaFormat.KEY_MIME)?.contains("audio") == true) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                }
            }
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val sampleTimeMs = extractor.sampleTime / 1000
                if (sampleTimeMs > endMs) break

                val newTrack = trackMap[extractor.sampleTrackIndex] ?: continue
                info.presentationTimeUs = extractor.sampleTime - (startMs * 1000)
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(newTrack, buffer, info)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
