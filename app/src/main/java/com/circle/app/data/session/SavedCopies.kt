package com.circle.app.data.session

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which temporary groups and chats this phone has saved a copy of (TIMED_GROUPS 7). Kept on the phone
 * only: the server never learns who saved. The app uses it to hide the "deleted tomorrow" reminder.
 */
interface SavedCopies {
    fun saved(): Flow<Set<String>>
    fun markSaved(id: String)
}

class PrefsSavedCopies(context: Context) : SavedCopies {
    private val prefs = context.getSharedPreferences("saved-copies", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    override fun saved(): Flow<Set<String>> = state.asStateFlow()
    override fun markSaved(id: String) {
        val next = state.value + id
        prefs.edit().putStringSet(KEY, next).apply()
        state.value = next
    }
    private companion object { const val KEY = "ids" }
}

/** Before [com.circle.app.AppGraph.init] and in tests. */
class MemorySavedCopies : SavedCopies {
    private val state = MutableStateFlow<Set<String>>(emptySet())
    override fun saved(): Flow<Set<String>> = state.asStateFlow()
    override fun markSaved(id: String) { state.value = state.value + id }
}
