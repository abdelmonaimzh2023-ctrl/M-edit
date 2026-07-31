package com.montage.app.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ExportEngine {

    suspend fun export(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        fps: Int,
        bitrate: Int,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { fd ->
                extractor.setDataSource(fd.fileDescriptor)
            } ?: return@withContext false

            val videoTrackIndex = selectVideoTrack(extractor)
            if (videoTrackIndex < 0) return@withContext false

            val inputFormat = extractor.getTrackFormat(videoTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext false

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)

            val encoderFormat = MediaFormat.createVideoFormat(
                if (mime.contains("hevc")) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC,
                targetWidth,
                targetHeight
            )
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            encoderFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            val encoder = MediaCodec.createEncoderByType(
                if (mime.contains("hevc")) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
            )
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            decoder.configure(inputFormat, inputSurface, null, 0)
            decoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerStarted = false
            var videoTrackInMuxer = -1
            val bufferInfo = MediaCodec.BufferInfo()

            val totalDuration = inputFormat.getLong(MediaFormat.KEY_DURATION).toFloat()
            var lastProgress = 0

            extractor.selectTrack(videoTrackIndex)

            // حلقة المعالجة
            while (true) {
                val inputIndex = decoder.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        break
                    }
                    val sampleTime = extractor.sampleTime
                    decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                    extractor.advance()
                }

                // إخراج من المشفر
                val encoderOutputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (encoderOutputIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(encoderOutputIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        encoder.releaseOutputBuffer(encoderOutputIndex, false)
                        continue
                    }
                    if (!muxerStarted) {
                        videoTrackInMuxer = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(videoTrackInMuxer, outputBuffer, bufferInfo)
                    encoder.releaseOutputBuffer(encoderOutputIndex, false)

                    val progress = (bufferInfo.presentationTimeUs / totalDuration * 100).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                } else if (encoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrackInMuxer = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            decoder.stop()
            decoder.release()
            encoder.stop()
            encoder.release()
            muxer.stop()
            muxer.release()
            extractor.release()

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun selectVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                return i
            }
        }
        return -1
    }
}
