package com.circle.app.ui.signup

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.data.Clock
import com.circle.app.data.signup.PhoneRules
import com.circle.app.ui.components.MeAvatar
import com.circle.app.ui.screens.SearchField
import com.circle.app.ui.theme.Body
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Back arrow, then a big Fraunces title (30sp) and an explainer. Every step has a back button. */
@Composable
private fun StepScaffold(title: String, explainer: String?, onBack: () -> Unit, bottom: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background).systemBarsPadding().imePadding()) {
        Box(Modifier.padding(start = 4.dp, top = 6.dp).size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.text)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 35.sp,
                color = colors.text, modifier = Modifier.semantics { heading() }
            )
            if (explainer != null) Text(explainer, color = colors.muted, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(22.dp))
            content()
        }
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp), content = bottom)
    }
}

@Composable
fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(999.dp))
            .background(colors.accent).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
private fun InputBox(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, keyboard: KeyboardType = KeyboardType.Text, prefix: String? = null) {
    val colors = CircleTheme.colors
    Row(
        modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) Text(prefix, color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = colors.muted, fontSize = 15.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 15.sp, fontWeight = if (prefix != null) FontWeight.Bold else FontWeight.Normal, fontFamily = Body),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
    }
}

/** The SIM's country, then the network's, then the device region (nothing is tied to one country). */
private fun deviceRegion(context: Context): String? {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    return tm?.simCountryIso?.takeIf { it.isNotBlank() } ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }
        ?: java.util.Locale.getDefault().country.takeIf { it.isNotBlank() }
}

/** Your number: a typed country code (prefilled), the number, the country it belongs to, and a searchable list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneStep(vm: SignupViewModel, onBack: () -> Unit, onCodeSent: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.prefillCountry(deviceRegion(context)) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    val names = vm.countryNames
    val valid = vm.e164 != null
    val soon = stringResource(R.string.signup_email_soon)

    if (listOpen) {
        var q by rememberSaveable { mutableStateOf("") }
        val found = remember(q) { PhoneRules.search(q) }
        ModalBottomSheet(onDismissRequest = { listOpen = false }, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.signup_choose_country), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = colors.text)
                SearchField(q, { q = it }, placeholder = stringResource(R.string.signup_country_search), modifier = Modifier.padding(vertical = 12.dp))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                    items(found, key = { it.region }) { c ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(10.dp))
                                .clickable { vm.changeCountryCode(c.code.toString()); listOpen = false }.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.name, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text("+${c.code}", color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    StepScaffold(
        stringResource(R.string.signup_phone_title), stringResource(R.string.signup_phone_explainer), onBack,
        bottom = { StepButton(stringResource(R.string.signup_send_code), enabled = valid) { vm.sendCode(onCodeSent) } }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InputBox(vm.countryCode, vm::changeCountryCode, stringResource(R.string.signup_country_code), Modifier.width(92.dp), KeyboardType.Number, prefix = "+")
            InputBox(vm.national, vm::changeNational, stringResource(R.string.signup_your_number), Modifier.weight(1f), KeyboardType.Phone)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    vm.countryCode.isEmpty() -> stringResource(R.string.signup_type_code)
                    names.isEmpty() -> stringResource(R.string.signup_unknown_code)
                    else -> PhoneRules.countryLine(names) { n -> context.resources.getQuantityString(R.plurals.signup_and_more, n, n) }
                },
                color = when { vm.countryCode.isEmpty() -> colors.muted; names.isEmpty() -> colors.alertDot; else -> colors.accent },
                fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.signup_choose_list), color = colors.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { listOpen = true }.padding(10.dp)
            )
        }
        if (vm.national.isNotBlank() && names.isNotEmpty() && !valid) {
            Text(stringResource(R.string.signup_number_invalid), color = colors.muted, fontSize = 12.sp)
        }
        Text(
            stringResource(R.string.signup_use_email), color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp))
                .clickable { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() }.padding(vertical = 10.dp)
        )
    }
}

/** Enter the code: six boxes over one hidden field; verifies at 6 digits; resend after 30 seconds; shake on a wrong code. */
@Composable
fun CodeStep(vm: SignupViewModel, onBack: () -> Unit, onVerified: () -> Unit) {
    val colors = CircleTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    var now by remember { mutableStateOf(Clock.now()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Clock.now() } }
    val gate = vm.gate
    val locked = gate?.isLocked(now) == true
    val shake = remember { Animatable(0f) }
    LaunchedEffect(vm.shakes) {
        if (vm.shakes > 0) shake.animateTo(0f, keyframes { durationMillis = 300; -14f at 50; 14f at 100; -9f at 150; 9f at 200; -4f at 250 })
    }

    StepScaffold(
        stringResource(R.string.signup_code_title), stringResource(R.string.signup_code_explainer, vm.e164.orEmpty()), onBack,
        bottom = {}
    ) {
        Box {
            BasicTextField(
                vm.code, { vm.setCode(it, onVerified) },
                enabled = !locked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus)
            )
            Row(
                Modifier.fillMaxWidth().offset { IntOffset(shake.value.roundToInt(), 0) }.clickable { focus.requestFocus() }
                    .semantics { contentDescription = "Code, ${vm.code.length} of 6 digits" },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(6) { i ->
                    val active = i == vm.code.length
                    Box(
                        Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                            .border(if (active) 2.dp else 1.dp, if (active) colors.accent else colors.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(vm.code.getOrNull(i)?.toString().orEmpty(), color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        when {
            locked -> Text(stringResource(R.string.signup_code_locked), color = colors.alertDot, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            vm.codeError -> Text(stringResource(R.string.signup_code_wrong), color = colors.alertDot, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        }
        val canResend = gate?.canResend(now) == true
        Text(
            if (canResend || gate == null) stringResource(R.string.signup_resend)
            else stringResource(R.string.signup_resend_in, gate.secondsUntilResend(now)),
            color = if (canResend) colors.accent else colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = canResend) { vm.resend() }.padding(vertical = 10.dp)
        )
    }
}

/** Set up your profile: photo, name (30), handle (live check), pinned thought (140). */
@Composable
fun ProfileStep(vm: SignupViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.setAvatar(uri.toString())
        }
    }
    StepScaffold(
        stringResource(R.string.signup_profile_title), null, onBack,
        bottom = { StepButton(stringResource(R.string.signup_continue), enabled = vm.canContinueProfile, onClick = onContinue) }
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            MeAvatar(84, uri = vm.avatarUri, initialOverride = vm.name.trim().firstOrNull()?.uppercase())
            Text(
                stringResource(if (vm.avatarUri == null) R.string.signup_add_photo else R.string.signup_change_photo),
                color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(12.dp)
            )
        }
        Label(stringResource(R.string.signup_name_caps))
        InputBox(vm.name, vm::changeName, stringResource(R.string.signup_name_hint), Modifier.fillMaxWidth())
        Label(stringResource(R.string.signup_handle_caps))
        InputBox(vm.handle, vm::changeHandle, stringResource(R.string.signup_handle_hint), Modifier.fillMaxWidth(), prefix = "@")
        val (msg, color) = when (vm.handleState) {
            HandleState.EMPTY, HandleState.BAD -> stringResource(R.string.signup_handle_rules) to colors.muted
            HandleState.TOO_SHORT -> stringResource(R.string.signup_handle_short) to colors.alertDot
            HandleState.CHECKING -> stringResource(R.string.signup_handle_checking) to colors.muted
            HandleState.TAKEN -> stringResource(R.string.signup_handle_taken, vm.handle) to colors.alertDot
            HandleState.FREE -> stringResource(R.string.signup_handle_free, vm.handle) to colors.accent
        }
        Text(msg, color = color, fontSize = 12.5.sp, modifier = Modifier.padding(top = 6.dp))
        Label(stringResource(R.string.signup_pinned_caps))
        InputBox(vm.pinned, vm::changePinned, stringResource(R.string.signup_pinned_hint), Modifier.fillMaxWidth())
        Row(Modifier.padding(top = 6.dp)) {
            Text(stringResource(R.string.signup_pinned_note), color = colors.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${vm.pinned.length} / 140", color = colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = CircleTheme.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
}

/** Age and agreement: 18 and over, by the real calendar; the date of birth isn't kept or shown. */
@Composable
fun AgeStep(vm: SignupViewModel, onBack: () -> Unit, onLegal: () -> Unit, onCreated: () -> Unit) {
    val colors = CircleTheme.colors
    StepScaffold(
        stringResource(R.string.signup_age_title), stringResource(R.string.signup_age_explainer), onBack,
        bottom = { StepButton(stringResource(R.string.welcome_create_account), enabled = vm.canCreate) { vm.createAccount(onCreated) } }
    ) {
        Label(stringResource(R.string.signup_dob_caps))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InputBox(vm.dd, vm::changeDd, stringResource(R.string.signup_dd), Modifier.weight(1f), KeyboardType.Number)
            InputBox(vm.mm, vm::changeMm, stringResource(R.string.signup_mm), Modifier.weight(1f), KeyboardType.Number)
            InputBox(vm.yyyy, vm::changeYyyy, stringResource(R.string.signup_yyyy), Modifier.weight(1.4f), KeyboardType.Number)
        }
        if (vm.isAdult == false) {
            Text(stringResource(R.string.signup_under_18), color = colors.alertDot, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Row(
            Modifier.padding(top = 22.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Checkbox) { vm.changeAgreed(!vm.agreed) }.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (vm.agreed) colors.accent else Color.Transparent)
                    .border(1.5.dp, if (vm.agreed) colors.accent else colors.muted, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { if (vm.agreed) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.signup_agree), color = colors.text, fontSize = 14.sp)
        }
        Text(
            stringResource(R.string.signup_short_version), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.padding(start = 34.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onLegal).padding(vertical = 10.dp)
        )
    }
}
