package com.circle.app

import android.app.Application
import android.content.Context
import com.circle.app.data.media.LocalMediaStore
import com.circle.app.data.media.MediaStore
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.session.DataStoreSessionStore
import com.circle.app.data.session.SessionStore

/**
 * The app's few long-lived objects, created once. A hand-written graph instead of a DI library:
 * there are only a handful of objects, and tests swap them by calling [install].
 */
object AppGraph {
    private var session: SessionStore? = null
    private var repo: CircleRepository? = null
    private var media: MediaStore? = null

    /** Where picked photos and videos go (the fake keeps their URIs). */
    val mediaStore: MediaStore get() = media ?: LocalMediaStore().also { media = it }

    /** The only door to data for the UI (through ViewModels). */
    val repository: CircleRepository
        get() = repo ?: FakeCircleRepository().also { repo = it }

    val sessionStore: SessionStore get() = checkNotNull(session) { "AppGraph.init was not called" }

    fun init(context: Context) {
        if (session == null) session = DataStoreSessionStore(context)
    }

    /** Tests: replace the real objects. */
    fun install(sessionStore: SessionStore? = null, repository: CircleRepository? = null) {
        sessionStore?.let { session = it }
        repository?.let { repo = it }
    }
}

class CircleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
