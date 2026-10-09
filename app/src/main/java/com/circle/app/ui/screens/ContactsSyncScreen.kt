package com.circle.app.ui.screens

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.circle.app.R
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.data.repo.PersonResult
import com.circle.app.data.signup.PhoneRules
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.FriendRequestButton
import com.circle.app.ui.components.PrimaryButton
import com.circle.app.ui.signup.StepButton
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Stage { PROMPT, LOADING, RESULTS, DENIED }

/**
 * Contacts matching: numbers are read and normalised on the phone, hashed (SHA-256 of E.164),
 * and only the hashes are sent. Nothing is kept on the phone or logged.
 */
class ContactsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    var matches by mutableStateOf<List<PersonResult>>(emptyList())
        private set

    fun match(context: Context, region: String, then: () -> Unit) {
        viewModelScope.launch {
            val hashes = withContext(Dispatchers.IO) { readHashes(context, region) }
            val result = repo.matchContacts(hashes)
            if (result is Outcome.Success) matches = result.value
            then()
        }
    }

    private fun readHashes(context: Context, region: String): List<String> = runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null
        )?.use { c ->
            buildList { while (c.moveToNext()) c.getString(0)?.let { PhoneRules.normalise(it, region) }?.let { add(PhoneRules.hash(it)) } }.distinct()
        }.orEmpty()
    }.getOrDefault(emptyList())
}

/** Find your friends: one screen after sign-up (and from the empty Home). Connect contacts or skip. */
@Composable
fun ContactsSyncScreen(onDone: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(Stage.PROMPT) }
    val vm = circleViewModel { ContactsViewModel(it) }
    val region = remember { java.util.Locale.getDefault().country.ifBlank { "US" } }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { stage = Stage.LOADING; vm.match(context, region) { stage = Stage.RESULTS } } else stage = Stage.DENIED
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            Text(
                stringResource(R.string.find_skip), color = colors.muted, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDone).padding(14.dp)
            )
        }
        when (stage) {
            Stage.PROMPT, Stage.DENIED -> Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Canvas(Modifier.size(120.dp)) {
                    val r = size.minDimension / 2 - 2.dp.toPx()
                    drawCircle(colors.border, r, style = Stroke(1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))))
                    drawCircle(colors.border, r * 0.62f, style = Stroke(1.2.dp.toPx()))
                    drawCircle(colors.accent, 5.dp.toPx(), center = Offset(center.x, center.y - r))
                }
                Text(stringResource(R.string.find_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = colors.text, modifier = Modifier.padding(top = 18.dp).semantics { heading() })
                Text(
                    stringResource(R.string.find_body), color = colors.body, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
                if (stage == Stage.DENIED) {
                    Text(stringResource(R.string.find_denied), color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                }
            }
            Stage.LOADING -> Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                Text(stringResource(R.string.find_matching), color = colors.body, fontSize = 13.5.sp, modifier = Modifier.padding(top = 16.dp))
            }
            Stage.RESULTS -> Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.find_found, vm.matches.size, vm.matches.size),
                    fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.text
                )
                Text(stringResource(R.string.find_results_note), color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                vm.matches.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(m.person.initials, m.person.color, size = 42)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.person.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("@${m.handle}", color = colors.muted, fontSize = 11.5.sp)
                        }
                        FriendRequestButton(m.person.id, m.relationship, addLabel = stringResource(R.string.profile_add_friend))
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp)) {
            if (stage == Stage.RESULTS) StepButton(stringResource(R.string.signup_continue), enabled = true, onClick = onDone)
            else if (stage != Stage.LOADING) StepButton(stringResource(R.string.find_connect), enabled = true) { ask.launch(Manifest.permission.READ_CONTACTS) }
        }
    }
}
