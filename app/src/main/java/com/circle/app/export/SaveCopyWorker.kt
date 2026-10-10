package com.circle.app.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.circle.app.AppGraph
import com.circle.app.BuildConfig
import com.circle.app.R
import com.circle.app.data.Clock
import com.circle.app.data.access.DenyReason
import com.circle.app.data.export.CopyWriter
import com.circle.app.data.repo.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.ZoneId

/**
 * Save a copy (TIMED_GROUPS 8) as a WorkManager job: expedited, with a progress notification, so it
 * survives leaving the screen. If the process dies, WorkManager runs it again from the start (the
 * half-written file is in the cache and is thrown away). Cancel stops it and leaves nothing behind.
 * The ZIP is written to the cache (never backed up), then published to Downloads through MediaStore:
 * no storage permission on Android 10+. Never logs message or post text.
 */
class SaveCopyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground(0)

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return fail(FAIL_OTHER)
        val withMedia = inputData.getBoolean(KEY_MEDIA, true)
        runCatching { setForeground(foreground(0)) } // optional: denied notifications or background limits
        val repo = AppGraph.repository
        val bundle = when (val r = repo.exportBundle(id, withMedia)) {
            is Outcome.Success -> r.value
            is Outcome.Error -> return fail(FAIL_OFFLINE)
            is Outcome.Denied -> return fail(if (r.reason == DenyReason.NOT_FOUND) FAIL_GONE else FAIL_OTHER)
        }
        val zone = ZoneId.systemDefault()
        val name = CopyWriter.fileName(bundle.name, Clock.now().atZone(zone).toLocalDate(), textOnly = !withMedia)
        val dir = File(applicationContext.cacheDir, "exports").apply { mkdirs() }
        val part = File(dir, "$name.part")
        val progress = MutableStateFlow(0f)
        return try {
            StatFs(dir.path).let { if (it.totalBytes > 0 && it.availableBytes < MIN_FREE_BYTES) return fail(FAIL_DISK_FULL) }
            withContext(Dispatchers.IO) {
                val reporter = launch { progress.collect { p -> setProgress(workDataOf(KEY_PROGRESS to (p * 80).toInt())) } }
                part.outputStream().buffered().use { out ->
                    CopyWriter.write(bundle, out, BuildConfig.VERSION_NAME, zone, ::openMedia) { progress.value = it }
                }
                reporter.cancel()
            }
            // Preview builds: let the progress bar be seen. A real export takes as long as it takes.
            if (BuildConfig.PREVIEW) for (p in 30..80 step 10) { setProgress(workDataOf(KEY_PROGRESS to p)); delay(180) }
            // Deleted while we were writing: nothing is saved.
            if (repo.wasDeleted(bundle.id).first()) return fail(FAIL_GONE)
            setProgress(workDataOf(KEY_PROGRESS to 90))
            val size = part.length()
            val where = withContext(Dispatchers.IO) { publish(part, name) }
            AppGraph.savedCopies.markSaved(bundle.id)
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success(workDataOf(KEY_NAME to name, KEY_SIZE to size, KEY_WHERE to where))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            fail(if (e.message.orEmpty().contains("ENOSPC") || e.message.orEmpty().contains("No space")) FAIL_DISK_FULL else FAIL_OTHER)
        } finally {
            part.delete()
        }
    }

    private fun fail(code: String) = Result.failure(workDataOf(KEY_FAIL to code))

    private fun openMedia(ref: com.circle.app.data.media.MediaRef) =
        runCatching { applicationContext.contentResolver.openInputStream(Uri.parse(ref.uri)) }.getOrNull()

    /** Copies the finished ZIP into Downloads. Returns where it went ("downloads" or "app"). */
    private fun publish(file: File, name: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("insert failed")
            try {
                (resolver.openOutputStream(uri) ?: throw IOException("no stream")).use { out -> file.inputStream().use { it.copyTo(out) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return WHERE_DOWNLOADS
        }
        // Android 8 and 9: the app's own Downloads folder (no storage permission needed).
        val target = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name)
        file.copyTo(target, overwrite = true)
        return WHERE_APP
    }

    private fun foreground(percent: Int): ForegroundInfo {
        val ctx = applicationContext
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.save_channel), NotificationManager.IMPORTANCE_LOW))
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(ctx.getString(R.string.save_preparing))
            .setContentText(ctx.getString(R.string.save_keep_open))
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ForegroundInfo(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(NOTIFICATION_ID, n)
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_MEDIA = "media"
        const val KEY_PROGRESS = "progress"
        const val KEY_NAME = "name"
        const val KEY_SIZE = "size"
        const val KEY_WHERE = "where"
        const val KEY_FAIL = "fail"
        const val FAIL_OFFLINE = "offline"
        const val FAIL_DISK_FULL = "disk_full"
        const val FAIL_GONE = "gone"
        const val FAIL_OTHER = "other"
        const val WHERE_DOWNLOADS = "downloads"
        const val WHERE_APP = "app"
        private const val CHANNEL = "save-copy"
        private const val NOTIFICATION_ID = 4107
        private const val MIN_FREE_BYTES = 5L * 1024 * 1024
    }
}

/** Starting, following and cancelling a member's Save a copy job. One job per group or chat. */
object SaveCopyJobs {
    fun uniqueName(id: String) = "save-copy-$id"

    fun start(context: Context, id: String, withMedia: Boolean) {
        val request = OneTimeWorkRequestBuilder<SaveCopyWorker>()
            .setInputData(workDataOf(SaveCopyWorker.KEY_ID to id, SaveCopyWorker.KEY_MEDIA to withMedia))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("save-copy")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName(id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, id: String) { WorkManager.getInstance(context).cancelUniqueWork(uniqueName(id)) }

    /** The latest job for [id], or null when there is none. */
    fun follow(context: Context, id: String): Flow<WorkInfo?> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(uniqueName(id)).map { it.lastOrNull() }
}
