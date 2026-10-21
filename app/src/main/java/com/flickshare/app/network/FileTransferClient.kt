package com.flickshare.app.network

import android.content.ContentResolver
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import com.flickshare.app.discovery.ReceiverEndpoint
import com.flickshare.app.gesture.ThrowAim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class StagedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val inlineBytes: ByteArray? = null
) {
    val isVirtualText: Boolean
        get() = inlineBytes != null
}

data class UploadProgress(
    val bytesSent: Long,
    val totalBytes: Long,
    val fraction: Float,
    val speedMbps: Float,
    val currentFileName: String
)

data class UploadResult(
    val success: Boolean,
    val filesTransferred: Int,
    val totalBytes: Long,
    val durationMs: Long,
    val serverMessage: String,
    val errorMessage: String? = null
)

/**
 * Streams one or more [StagedFile] payloads over HTTP `multipart/form-data` using a 64 KB
 * chunked [RequestBody] directly from [ContentResolver.openInputStream] without buffering
 * entire files in heap memory.
 */
class FileTransferClient {

    companion object {
        const val CHUNK_SIZE = 64 * 1024 // 64 KB streaming buffer
        private const val PROGRESS_EMIT_INTERVAL_MS = 16L // ~60fps progress updates

        /**
         * Resolves file display name, MIME type, and byte size from Android's [ContentResolver].
         */
        fun resolveStagedFile(contentResolver: ContentResolver, uri: Uri): StagedFile? {
            return try {
                var displayName: String? = null
                var sizeBytes: Long = -1L

                contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                            displayName = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }

                if (sizeBytes < 0L) {
                    try {
                        contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                            if (afd.length >= 0L) {
                                sizeBytes = afd.length
                            }
                        }
                    } catch (_: Exception) {
                        // Stream length unknown; OkHttp will use chunked transfer encoding
                    }
                }

                val fallbackName = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringAfterLast(':')
                    ?.takeIf { it.isNotBlank() }
                    ?: "shared_file_${System.currentTimeMillis()}"

                val resolvedName = sanitizeFileName(displayName ?: fallbackName)
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                StagedFile(
                    uri = uri,
                    name = resolvedName,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Wraps plain shared text (`Intent.EXTRA_TEXT`) into an in-memory [StagedFile] so links
         * and notes shared from browsers or apps can be thrown to the desktop just like files.
         */
        fun createVirtualTextFile(text: String): StagedFile {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val fileName = "shared_text_${System.currentTimeMillis()}.txt"
            return StagedFile(
                uri = Uri.parse("flickshare://text/$fileName"),
                name = fileName,
                mimeType = "text/plain; charset=utf-8",
                sizeBytes = bytes.size.toLong(),
                inlineBytes = bytes
            )
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes < 0L) return "Unknown size"
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }

        private fun sanitizeFileName(raw: String): String {
            val cleaned = raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
            return cleaned.ifEmpty { "shared_file_${System.currentTimeMillis()}" }
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS) // Unlimited write timeout for multi-GB LAN streams
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun uploadFiles(
        endpoint: ReceiverEndpoint,
        files: List<StagedFile>,
        contentResolver: ContentResolver,
        aim: ThrowAim? = null,
        onProgress: (UploadProgress) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        if (files.isEmpty()) {
            return@withContext UploadResult(
                success = false,
                filesTransferred = 0,
                totalBytes = 0L,
                durationMs = 0L,
                serverMessage = "",
                errorMessage = "No files staged to throw."
            )
        }

        val totalExpectedBytes = files.sumOf { max(0L, it.sizeBytes) }
        val startTimeMs = SystemClock.elapsedRealtime()
        var cumulativeBytesSent = 0L
        var lastEmitTimeMs = 0L

        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        for (stagedFile in files) {
            val fileBody = object : RequestBody() {
                override fun contentType(): MediaType? =
                    stagedFile.mimeType.toMediaTypeOrNull()

                override fun contentLength(): Long =
                    if (stagedFile.sizeBytes >= 0L) stagedFile.sizeBytes else -1L

                override fun writeTo(sink: BufferedSink) {
                    val inputStream = if (stagedFile.inlineBytes != null) {
                        ByteArrayInputStream(stagedFile.inlineBytes)
                    } else {
                        contentResolver.openInputStream(stagedFile.uri)
                            ?: throw IOException("Unable to open stream for ${stagedFile.name}")
                    }

                    inputStream.use { stream ->
                        val buffer = ByteArray(CHUNK_SIZE)
                        var bytesRead: Int
                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                            cumulativeBytesSent += bytesRead

                            val now = SystemClock.elapsedRealtime()
                            if (now - lastEmitTimeMs >= PROGRESS_EMIT_INTERVAL_MS) {
                                lastEmitTimeMs = now
                                val elapsedSec = max(0.001f, (now - startTimeMs) / 1000f)
                                val speedMbps = (cumulativeBytesSent / (1024f * 1024f)) / elapsedSec
                                val fraction = if (totalExpectedBytes > 0L) {
                                    (cumulativeBytesSent.toFloat() / totalExpectedBytes.toFloat())
                                        .coerceIn(0f, 1f)
                                } else {
                                    0.5f
                                }
                                onProgress(
                                    UploadProgress(
                                        bytesSent = cumulativeBytesSent,
                                        totalBytes = totalExpectedBytes,
                                        fraction = fraction,
                                        speedMbps = speedMbps,
                                        currentFileName = stagedFile.name
                                    )
                                )
                            }
                        }
                        sink.flush()
                    }
                }
            }

            multipartBuilder.addFormDataPart("files", stagedFile.name, fileBody)
        }

        val request = Request.Builder()
            .url(endpoint.uploadUrl)
            .post(multipartBuilder.build())
            .header("X-FlickShare-Client", "Android/1.0")
            .apply {
                // Lets the desktop liquid glass enter from the side the file was thrown from.
                if (aim != null) {
                    header("X-FlickShare-Direction", String.format(Locale.US, "%.1f", aim.degrees))
                    header("X-FlickShare-Direction-Source", aim.source)
                }
            }
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val durationMs = max(1L, SystemClock.elapsedRealtime() - startTimeMs)
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext UploadResult(
                        success = false,
                        filesTransferred = 0,
                        totalBytes = cumulativeBytesSent,
                        durationMs = durationMs,
                        serverMessage = responseBody,
                        errorMessage = "Server returned HTTP ${response.code}: ${response.message}"
                    )
                }

                val elapsedSec = max(0.001f, durationMs / 1000f)
                val finalSpeedMbps = (cumulativeBytesSent / (1024f * 1024f)) / elapsedSec
                onProgress(
                    UploadProgress(
                        bytesSent = cumulativeBytesSent,
                        totalBytes = max(totalExpectedBytes, cumulativeBytesSent),
                        fraction = 1.0f,
                        speedMbps = finalSpeedMbps,
                        currentFileName = files.last().name
                    )
                )

                UploadResult(
                    success = true,
                    filesTransferred = files.size,
                    totalBytes = cumulativeBytesSent,
                    durationMs = durationMs,
                    serverMessage = responseBody
                )
            }
        } catch (e: Exception) {
            val durationMs = max(1L, SystemClock.elapsedRealtime() - startTimeMs)
            UploadResult(
                success = false,
                filesTransferred = 0,
                totalBytes = cumulativeBytesSent,
                durationMs = durationMs,
                serverMessage = "",
                errorMessage = e.localizedMessage ?: "Network transfer failed"
            )
        }
    }
}
