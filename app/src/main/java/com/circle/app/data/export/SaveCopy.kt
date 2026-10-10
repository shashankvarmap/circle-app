package com.circle.app.data.export

import com.circle.app.data.media.MediaRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/*
 * Save a copy (TIMED_GROUPS 8): what a member may take home from a temporary group or chat, and the
 * ZIP it becomes. Pure Kotlin: the repository fills an [ExportBundle] through AccessPolicy (members
 * only, blocked people's content left out, display names only), and [CopyWriter] turns it into
 * manifest.json, index.html, messages.json, posts.json and media/. Nothing here logs content.
 */

enum class ExportKind { GROUP, CHAT }

/** What the sheet shows before saving: counts and size estimates. */
data class ExportPlan(
    val id: String,
    val name: String,
    val kind: ExportKind,
    val messageCount: Int,
    val mediaCount: Int,
    val postCount: Int,
    val everythingBytes: Long,
    val textBytes: Long,
)

@Serializable
data class ExportMessage(val sender: String, val at: String, val kind: String, val text: String, val media: List<String> = emptyList())

@Serializable
data class ExportComment(val author: String, val at: String, val text: String, val replies: List<ExportComment> = emptyList())

@Serializable
data class ExportPost(val author: String, val at: String, val text: String, val comments: List<ExportComment>, val media: List<String> = emptyList())

/** Everything one member may save. Names are display names only: no handles, no phone numbers. */
data class ExportBundle(
    val id: String,
    val name: String,
    val kind: ExportKind,
    val endsAt: Instant?,
    val deleteAt: Instant?,
    val exportedAt: Instant,
    val members: List<String>,
    val messages: List<ExportMessage>,
    val posts: List<ExportPost>,
    /** Photos and videos to include (Everything only), each with the name it gets under media/. */
    val media: List<Pair<String, MediaRef>>,
)

@Serializable
private data class Manifest(
    val app: String, val appVersion: String, val name: String, val kind: String,
    val endsAt: String?, val deleteAt: String?, val exportedAt: String,
    val members: Int, val messages: Int, val posts: Int, val mediaFiles: Int, val mediaMissing: Int,
    val note: String,
)

/** Rough sizes for the sheet: about 0.75 MB a photo or video, and the text itself. */
object SizeEstimate {
    const val PER_MEDIA_BYTES = 750_000L
    fun text(messages: Int, posts: Int, chars: Long): Long = 24_000L + chars * 3 + (messages + posts) * 220L
    fun everything(textBytes: Long, media: Int): Long = textBytes + media * PER_MEDIA_BYTES
}

object CopyWriter {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /** "Circle-Project-Alps-2026-10-22.zip"; "-text" before the date for Text only. */
    fun fileName(name: String, day: LocalDate, textOnly: Boolean): String {
        val safe = name.trim().replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').ifEmpty { "Copy" }.take(60)
        return "Circle-$safe${if (textOnly) "-text" else ""}-${day.format(DateTimeFormatter.ISO_LOCAL_DATE)}.zip"
    }

    /**
     * Writes the ZIP. [openMedia] reads a photo or video (null when it can't be read: counted as
     * missing, never fatal). [progress] goes from 0 to 1. Throws [java.io.IOException] (a full disk).
     */
    fun write(
        bundle: ExportBundle, out: OutputStream, appVersion: String, zone: ZoneId,
        openMedia: (MediaRef) -> InputStream? = { null }, progress: (Float) -> Unit = {},
    ) {
        val steps = 4 + bundle.media.size
        var done = 0
        fun step() { done++; progress(done.toFloat() / steps) }
        var missing = 0
        ZipOutputStream(out).use { zip ->
            fun text(path: String, body: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(body.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            text("messages.json", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ExportMessage.serializer()), bundle.messages)); step()
            if (bundle.kind == ExportKind.GROUP) text("posts.json", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ExportPost.serializer()), bundle.posts))
            step()
            text("index.html", html(bundle, zone)); step()
            bundle.media.forEach { (name, ref) ->
                val input = runCatching { openMedia(ref) }.getOrNull()
                if (input == null) missing++ else input.use { zip.putNextEntry(ZipEntry("media/$name")); it.copyTo(zip); zip.closeEntry() }
                step()
            }
            val manifest = Manifest(
                app = "Circle", appVersion = appVersion, name = bundle.name, kind = bundle.kind.name.lowercase(),
                endsAt = bundle.endsAt?.toString(), deleteAt = bundle.deleteAt?.toString(), exportedAt = bundle.exportedAt.toString(),
                members = bundle.members.size, messages = bundle.messages.size, posts = bundle.posts.size,
                mediaFiles = bundle.media.size - missing, mediaMissing = missing,
                note = "Your own copy. Member display names only; no phone numbers. Content from people you blocked is left out.",
            )
            text("manifest.json", json.encodeToString(Manifest.serializer(), manifest)); step()
        }
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun when_(iso: String, zone: ZoneId): String = runCatching {
        Instant.parse(iso).atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm"))
    }.getOrDefault(iso)

    /** A readable page: messages and posts in order, with times and names. */
    fun html(b: ExportBundle, zone: ZoneId): String = buildString {
        append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\">")
        append("<title>").append(esc(b.name)).append("</title><style>body{font-family:sans-serif;max-width:680px;margin:24px auto;padding:0 16px;color:#222}")
        append(".t{color:#888;font-size:12px}.m{margin:10px 0}.p{border:1px solid #ddd;border-radius:10px;padding:10px;margin:12px 0}.c{margin-left:16px}</style></head><body>")
        append("<h1>").append(esc(b.name)).append("</h1><p class=\"t\">Saved ").append(esc(when_(b.exportedAt.toString(), zone)))
        append(" · ").append(b.members.size).append(" members: ").append(esc(b.members.joinToString(", "))).append("</p>")
        if (b.kind == ExportKind.GROUP) {
            append("<h2>Posts</h2>")
            b.posts.forEach { p ->
                append("<div class=\"p\"><b>").append(esc(p.author)).append("</b> <span class=\"t\">").append(esc(when_(p.at, zone))).append("</span><div>")
                append(esc(p.text)).append("</div>")
                p.media.forEach { append("<div class=\"t\">media/").append(esc(it)).append("</div>") }
                p.comments.forEach { c -> comment(c, zone) }
                append("</div>")
            }
        }
        append("<h2>").append(if (b.kind == ExportKind.GROUP) "Group chat" else "Messages").append("</h2>")
        b.messages.forEach { m ->
            append("<div class=\"m\"><b>").append(esc(m.sender)).append("</b> <span class=\"t\">").append(esc(when_(m.at, zone))).append("</span><div>")
            append(esc(m.text.ifEmpty { "[" + m.kind.lowercase() + "]" })).append("</div>")
            m.media.forEach { append("<div class=\"t\">media/").append(esc(it)).append("</div>") }
            append("</div>")
        }
        append("</body></html>")
    }

    private fun StringBuilder.comment(c: ExportComment, zone: ZoneId) {
        append("<div class=\"c\"><b>").append(esc(c.author)).append("</b> <span class=\"t\">").append(esc(when_(c.at, zone))).append("</span> ")
        append(esc(c.text))
        c.replies.forEach { comment(it, zone) }
        append("</div>")
    }
}
