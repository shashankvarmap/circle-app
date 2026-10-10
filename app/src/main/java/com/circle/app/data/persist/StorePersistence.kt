package com.circle.app.data.persist

import androidx.compose.runtime.snapshotFlow
import com.circle.app.data.CircleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps the fake server's data (CircleStore) on the phone so the demo survives a restart (F6).
 * One JSON file in the app's private files directory, written atomically after changes settle.
 * With a backend this goes away: the server is the source of truth and Room becomes a cache.
 */
class StorePersistence(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Loads the saved state into the store. False (and the seed stays) when there is none or it's from an older version. */
    fun load(store: CircleStore = CircleStore): Boolean {
        if (!file.exists()) return false
        val snapshot = runCatching { json.decodeFromString(StoreSnapshot.serializer(), file.readText()) }.getOrNull()
            ?.takeIf { it.version == StoreSnapshot.VERSION } ?: return false
        return runCatching { store.restore(snapshot) }.isSuccess.also { ok -> if (!ok) store.reset() }
    }

    fun save(snapshot: StoreSnapshot) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(StoreSnapshot.serializer(), snapshot))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    fun clear() { file.delete() }

    /** Saves every change, 300ms after the last one. */
    @OptIn(FlowPreview::class)
    fun watch(store: CircleStore = CircleStore, scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
        scope.launch {
            snapshotFlow { store.capture() }
                .distinctUntilChanged()
                .drop(1) // the state just loaded
                .debounce(300)
                .collect { runCatching { save(it) } }
        }
    }
}
