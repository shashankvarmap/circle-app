package com.circle.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * The local session: whether someone is logged in on this device and whether the story was shown.
 * With a backend, `onboardingSeen` is also stored on the account so a new phone doesn't replay it.
 */
interface SessionStore {
    val state: Flow<LaunchState>

    /** A new account was created: logged in, story not yet seen. */
    suspend fun accountCreated()

    /** An existing account logged in. The story is never shown after a log-in. */
    suspend fun loggedIn()

    suspend fun onboardingFinished()

    /** Clears the session; the device remembers the story was seen. */
    suspend fun loggedOut()

    /** Same as log out on this device; the account itself is removed by the repository. */
    suspend fun accountDeleted()

    /** The country calling code chosen at sign-up or log-in (digits, e.g. "91"). Preview prices use it. */
    val countryCode: Flow<String?>
    suspend fun setCountryCode(code: String)
}

class InMemorySessionStore(initial: LaunchState = LaunchState(hasSession = false, onboardingSeen = false)) : SessionStore {
    private val flow = MutableStateFlow(initial)
    override val state: Flow<LaunchState> = flow
    val current: LaunchState get() = flow.value

    override suspend fun accountCreated() { flow.value = flow.value.copy(hasSession = true) }
    override suspend fun loggedIn() { flow.value = LaunchState(hasSession = true, onboardingSeen = true) }
    override suspend fun onboardingFinished() { flow.value = flow.value.copy(onboardingSeen = true) }
    override suspend fun loggedOut() { flow.value = flow.value.copy(hasSession = false) }
    override suspend fun accountDeleted() { flow.value = flow.value.copy(hasSession = false) }

    private val code = MutableStateFlow<String?>(null)
    override val countryCode: Flow<String?> = code
    override suspend fun setCountryCode(code: String) { this.code.value = code }
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** DataStore-backed session. Holds no tokens yet (the fake has none); tokens go to encrypted storage later. */
class DataStoreSessionStore(context: Context) : SessionStore {
    private val store = context.applicationContext.sessionDataStore
    private val hasSession = booleanPreferencesKey("has_session")
    private val onboardingSeen = booleanPreferencesKey("onboarding_seen")

    override val state: Flow<LaunchState> = store.data.map {
        LaunchState(hasSession = it[hasSession] ?: false, onboardingSeen = it[onboardingSeen] ?: false)
    }

    override suspend fun accountCreated() { store.edit { it[hasSession] = true } }
    override suspend fun loggedIn() { store.edit { it[hasSession] = true; it[onboardingSeen] = true } }
    override suspend fun onboardingFinished() { store.edit { it[onboardingSeen] = true } }
    override suspend fun loggedOut() { store.edit { it[hasSession] = false } }
    override suspend fun accountDeleted() { store.edit { it[hasSession] = false } }

    private val callingCode = androidx.datastore.preferences.core.stringPreferencesKey("country_code")
    override val countryCode: Flow<String?> = store.data.map { it[callingCode] }
    override suspend fun setCountryCode(code: String) { store.edit { it[callingCode] = code } }
}
