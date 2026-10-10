package com.circle.app

import android.app.Application
import android.content.Context
import android.os.Build
import com.circle.app.data.persist.StorePersistence
import com.circle.app.data.repo.DemoControls
import java.io.File
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
    private var billingImpl: com.circle.app.data.billing.MembershipBilling? = null
    private var saved: com.circle.app.data.session.SavedCopies? = null

    /** Which temporary groups and chats this phone saved a copy of (local only; the server never knows). */
    val savedCopies: com.circle.app.data.session.SavedCopies
        get() = saved ?: com.circle.app.data.session.MemorySavedCopies().also { saved = it }

    /** Membership billing: a fake with preview prices for the sign-up country until Play Billing is set up. */
    val billing: com.circle.app.data.billing.MembershipBilling
        get() = billingImpl ?: com.circle.app.data.billing.FakeBilling(
            session?.countryCode ?: kotlinx.coroutines.flow.flowOf(null)
        ).also { billingImpl = it }

    /** Where picked photos and videos go (the fake keeps their URIs). */
    val mediaStore: MediaStore get() = media ?: LocalMediaStore().also { media = it }

    /** The only door to data for the UI (through ViewModels). */
    val repository: CircleRepository
        get() = repo ?: FakeCircleRepository(autoAccept = BuildConfig.PREVIEW).also { repo = it }

    val sessionStore: SessionStore get() = checkNotNull(session) { "AppGraph.init was not called" }

    /** Demo-only controls (reset, fail switch); null when the repository is a real backend. */
    val demo: DemoControls? get() = repository as? DemoControls

    fun init(context: Context) {
        if (session != null) return
        session = DataStoreSessionStore(context)
        saved = com.circle.app.data.session.PrefsSavedCopies(context)
        // The fake server's data survives restarts (F6). Robolectric tests start from the seed every time.
        if (Build.FINGERPRINT != "robolectric") {
            StorePersistence(File(context.filesDir, "circle-demo-store.json")).apply { load(); watch() }
        }
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
