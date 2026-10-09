package com.circle.app.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.circle.app.AppGraph
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.MeView
import com.circle.app.data.repo.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Base for screen ViewModels: they read the repository as state and run its actions. */
abstract class RepoViewModel(protected val repo: CircleRepository) : ViewModel() {

    protected fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    /** Runs an action; [then] gets its outcome on the main thread. */
    protected fun <T> run(action: suspend CircleRepository.() -> Outcome<T>, then: (Outcome<T>) -> Unit = {}) {
        viewModelScope.launch { then(repo.action()) }
    }
}

/** A screen's ViewModel, created with the app's repository. [key] separates instances (e.g. per note id). */
@Composable
inline fun <reified VM : ViewModel> circleViewModel(key: String? = null, crossinline create: (CircleRepository) -> VM): VM =
    viewModel(key = key) { create(AppGraph.repository) }

/** Your own profile, provided at the root so avatars anywhere can show your photo. */
val LocalMe = staticCompositionLocalOf<MeView?> { null }
