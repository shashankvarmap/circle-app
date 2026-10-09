package com.circle.app.ui.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.circle.app.data.Clock
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.data.signup.AgeRule
import com.circle.app.data.signup.CodeGate
import com.circle.app.data.signup.HandleProblem
import com.circle.app.data.signup.PhoneRules
import com.circle.app.data.signup.SignupHandle
import com.circle.app.ui.vm.RepoViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Where the handle check stands. */
enum class HandleState { EMPTY, TOO_SHORT, BAD, CHECKING, TAKEN, FREE }

/**
 * The sign-up and log-in steps (ACCESS_AND_LAUNCH 1). One instance for the whole flow, scoped to
 * its navigation graph; text fields survive rotation and process death through [SavedStateHandle].
 */
class SignupViewModel(repo: CircleRepository, private val saved: SavedStateHandle, val creating: Boolean) : RepoViewModel(repo) {

    private fun <T> saved(key: String, initial: T) = saved.get<T>(key) ?: initial

    var countryCode by mutableStateOf(saved("cc", ""))
        private set
    var national by mutableStateOf(saved("nat", ""))
        private set
    var code by mutableStateOf("")
        private set
    var name by mutableStateOf(saved("name", ""))
        private set
    var handle by mutableStateOf(saved("handle", ""))
        private set
    var pinned by mutableStateOf(saved("pinned", ""))
        private set
    var avatarUri by mutableStateOf(saved<String?>("avatar", null))
        private set
    var dd by mutableStateOf(saved("dd", ""))
        private set
    var mm by mutableStateOf(saved("mm", ""))
        private set
    var yyyy by mutableStateOf(saved("yyyy", ""))
        private set
    var agreed by mutableStateOf(saved("agreed", false))
        private set
    var handleState by mutableStateOf(HandleState.EMPTY)
        private set
    var gate by mutableStateOf<CodeGate?>(null)
        private set
    var codeError by mutableStateOf(false)
        private set
    /** Bumped on each wrong code so the boxes can shake once. */
    var shakes by mutableStateOf(0)
        private set

    fun prefillCountry(region: String?) {
        if (countryCode.isEmpty()) changeCountryCode(PhoneRules.codeForRegion(region))
    }

    fun changeCountryCode(v: String) { countryCode = v.filter(Char::isDigit).take(3); saved["cc"] = countryCode }
    fun changeNational(v: String) { national = v.filter { it.isDigit() || it == ' ' || it == '-' }.take(18); saved["nat"] = national }

    val countryNames: List<String> get() = PhoneRules.countriesFor(countryCode)
    val e164: String? get() = PhoneRules.toE164(countryCode, national)

    fun sendCode(then: () -> Unit) {
        val number = e164 ?: return
        run({ requestCode(number) }) {
            if (it is Outcome.Success) { gate = CodeGate(Clock.now()); code = ""; codeError = false; then() }
        }
    }

    fun resend() {
        val g = gate ?: return
        val number = e164 ?: return
        if (!g.canResend(Clock.now())) return
        run({ requestCode(number) }) { if (it is Outcome.Success) gate = g.resent(Clock.now()) }
    }

    /** Digits only, max 6; verifies itself at 6. [onVerified] runs once the code is accepted. */
    fun setCode(v: String, onVerified: () -> Unit) {
        code = v.filter(Char::isDigit).take(6)
        codeError = false
        if (code.length == 6) verify(onVerified)
    }

    private fun verify(onVerified: () -> Unit) {
        val number = e164 ?: return
        val g = gate ?: CodeGate(Clock.now())
        if (g.isLocked(Clock.now())) return
        viewModelScope.launch {
            when (repo.verifyCode(number, code)) {
                is Outcome.Success -> onVerified()
                else -> { gate = g.wrong(Clock.now()); codeError = true; shakes++; code = "" }
            }
        }
    }

    fun changeName(v: String) { name = v.take(30); saved["name"] = name }
    fun changePinned(v: String) { pinned = v.take(140); saved["pinned"] = pinned }
    fun setAvatar(uri: String?) { avatarUri = uri; saved["avatar"] = uri }

    private var handleJob: Job? = null

    /** Live handle check: shape at once, availability from the repository after a 400ms pause. */
    fun changeHandle(v: String) {
        handle = SignupHandle.clean(v); saved["handle"] = handle
        handleJob?.cancel()
        handleState = when (SignupHandle.problem(handle)) {
            HandleProblem.EMPTY -> HandleState.EMPTY
            HandleProblem.TOO_SHORT -> HandleState.TOO_SHORT
            HandleProblem.TOO_LONG, HandleProblem.CHARACTERS -> HandleState.BAD
            null -> HandleState.CHECKING
        }
        if (handleState != HandleState.CHECKING) return
        val checking = handle
        handleJob = viewModelScope.launch {
            delay(400)
            handleState = if (repo.isHandleFree(checking)) HandleState.FREE else HandleState.TAKEN
        }
    }

    val canContinueProfile: Boolean get() = name.isNotBlank() && handleState == HandleState.FREE

    fun changeDd(v: String) { dd = v.filter(Char::isDigit).take(2); saved["dd"] = dd }
    fun changeMm(v: String) { mm = v.filter(Char::isDigit).take(2); saved["mm"] = mm }
    fun changeYyyy(v: String) { yyyy = v.filter(Char::isDigit).take(4); saved["yyyy"] = yyyy }
    fun changeAgreed(v: Boolean) { agreed = v; saved["agreed"] = v }

    /** Null while the date is incomplete or not a real date. */
    val birth: LocalDate? get() = AgeRule.date(dd, mm, yyyy)
    val isAdult: Boolean? get() = birth?.let { AgeRule.isAdult(it, Clock.now().atZone(ZoneId.systemDefault()).toLocalDate()) }
    val canCreate: Boolean get() = isAdult == true && agreed

    /** Only the fact of being old enough leaves this screen; the date of birth is not stored. */
    fun createAccount(then: () -> Unit) {
        if (!canCreate) return
        run({ createAccount(name, handle, pinned, avatarUri) }) { if (it is Outcome.Success) then() }
    }

    fun logIn(then: () -> Unit) {
        val number = e164 ?: return
        run({ logIn(number) }) { if (it is Outcome.Success) then() }
    }
}
