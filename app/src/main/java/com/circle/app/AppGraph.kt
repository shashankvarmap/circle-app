package com.circle.app

import android.app.Application
import android.content.Context
import com.circle.app.data.session.DataStoreSessionStore
import com.circle.app.data.session.SessionStore

/**
 * The app's few long-lived objects, created once. A hand-written graph instead of a DI library:
 * there are only a handful of objects, and tests swap them by calling [install].
 */
object AppGraph {
    private var session: SessionStore? = null

    val sessionStore: SessionStore get() = checkNotNull(session) { "AppGraph.init was not called" }

    fun init(context: Context) {
        if (session == null) session = DataStoreSessionStore(context)
    }

    /** Tests: replace the real objects. */
    fun install(sessionStore: SessionStore) {
        session = sessionStore
    }
}

class CircleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
