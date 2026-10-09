package com.circle.app.data.media

import com.circle.app.data.newId

enum class MediaKind { PHOTO, VIDEO }

/** A photo or video attached to a note or message, referenced by URI (FRONTEND_COMPLETE 8). */
data class MediaRef(val id: String, val uri: String, val kind: MediaKind, val durationMs: Long? = null)

/**
 * Where media lives. The fake keeps the picker's content URIs; a backend implementation uploads
 * them and returns server URLs. Screens never handle files directly.
 */
interface MediaStore {
    /** Adds a picked item; refuses videos longer than [MAX_VIDEO_MS]. */
    fun add(uri: String, kind: MediaKind, durationMs: Long? = null): Result<MediaRef>

    companion object {
        const val MAX_VIDEO_MS = 30_000L
    }
}

class VideoTooLong(val durationMs: Long) : Exception("video longer than 30 seconds")

class LocalMediaStore : MediaStore {
    override fun add(uri: String, kind: MediaKind, durationMs: Long?): Result<MediaRef> {
        if (kind == MediaKind.VIDEO && (durationMs ?: 0L) > MediaStore.MAX_VIDEO_MS) return Result.failure(VideoTooLong(durationMs!!))
        return Result.success(MediaRef(newId("media"), uri, kind, durationMs))
    }
}
