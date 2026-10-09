package com.circle.app.ui.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circle.app.data.session.Destination
import com.circle.app.data.session.LaunchFlow
import com.circle.app.data.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Getting in and out: every step asks [LaunchFlow] where to go next and records it in the [SessionStore]. */
class SessionViewModel(private val session: SessionStore) : ViewModel() {

    /** The start screen, decided once at launch. Null until the session has been read. */
    private val _start = MutableStateFlow<Destination?>(null)
    val start: StateFlow<Destination?> = _start

    /** Welcome shows Create account only before the story was ever seen on this device. */
    val canCreateAccount: StateFlow<Boolean> = session.state
        .map { LaunchFlow.start(it.copy(hasSession = false)) == Destination.WELCOME }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch { _start.value = LaunchFlow.start(session.state.first()) }
    }

    fun createAccount(then: (Destination) -> Unit) = viewModelScope.launch {
        session.accountCreated()
        then(LaunchFlow.afterAccountCreated())
    }

    fun logIn(then: (Destination) -> Unit) = viewModelScope.launch {
        session.loggedIn()
        then(LaunchFlow.afterLogIn())
    }

    fun findFriendsDone(then: (Destination) -> Unit) = viewModelScope.launch {
        then(LaunchFlow.afterFindFriends(session.state.first()))
    }

    fun storyDone(then: (Destination) -> Unit) = viewModelScope.launch {
        session.onboardingFinished()
        then(LaunchFlow.afterStory())
    }

    fun logOut(then: (Destination) -> Unit) = viewModelScope.launch {
        session.loggedOut()
        then(LaunchFlow.afterSignOut(session.state.first()))
    }

    fun accountDeleted(then: (Destination) -> Unit) = viewModelScope.launch {
        session.accountDeleted()
        then(LaunchFlow.afterSignOut(session.state.first()))
    }
}
