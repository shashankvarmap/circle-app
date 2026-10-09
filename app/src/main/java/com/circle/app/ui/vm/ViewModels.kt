package com.circle.app.ui.vm

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.circle.app.AppGraph
import com.circle.app.R
import com.circle.app.data.access.DenyReason
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.MeView
import com.circle.app.data.repo.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Failed actions, app-wide. The root shows each one as a short message, so no tap fails silently. */
object UiMessages {
    private val flow = MutableSharedFlow<Outcome<*>>(extraBufferCapacity = 8)
    val failures: SharedFlow<Outcome<*>> get() = flow
    fun report(outcome: Outcome<*>) {
        if (outcome !is Outcome.Success) flow.tryEmit(outcome)
    }
}

/** Base for screen ViewModels: they read the repository as state and run its actions. */
abstract class RepoViewModel(protected val repo: CircleRepository) : ViewModel() {

    protected fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    /** Runs an action; a failure is reported app-wide, and [then] gets the outcome on the main thread. */
    protected fun <T> run(action: suspend CircleRepository.() -> Outcome<T>, then: (Outcome<T>) -> Unit = {}) {
        viewModelScope.launch {
            val outcome = repo.action()
            UiMessages.report(outcome)
            then(outcome)
        }
    }
}

/** A screen's ViewModel, created with the app's repository. [key] separates instances (e.g. per note id). */
@Composable
inline fun <reified VM : ViewModel> circleViewModel(key: String? = null, crossinline create: (CircleRepository) -> VM): VM =
    viewModel(key = key) { create(AppGraph.repository) }

/** Your own profile, provided at the root so avatars anywhere can show your photo. */
val LocalMe = staticCompositionLocalOf<MeView?> { null }

/** Shows failed actions as a short toast: what the rules said, or a calm "try again". */
@Composable
fun FailureMessages() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UiMessages.failures.collect { outcome ->
            val text = when (outcome) {
                is Outcome.Denied -> context.getString(
                    when (outcome.reason) {
                        DenyReason.NOT_FRIENDS -> R.string.deny_not_friends
                        DenyReason.AUDIENCE -> R.string.deny_audience
                        DenyReason.GROUP_NOT_MEMBER -> R.string.deny_group_not_member
                        DenyReason.EVENT_LOCKED -> R.string.deny_event_locked
                        DenyReason.INNER_CIRCLE -> R.string.deny_inner_circle_share
                        DenyReason.GROUP_POST -> R.string.deny_group_post
                        DenyReason.BLOCKED, DenyReason.NOT_FOUND -> R.string.deny_not_available
                    }
                )
                is Outcome.Error -> if (outcome.message == "full") context.getString(R.string.error_event_full) else context.getString(R.string.error_try_again)
                is Outcome.Success -> return@collect
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
