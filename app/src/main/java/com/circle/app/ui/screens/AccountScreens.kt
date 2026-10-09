package com.circle.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.AppGraph
import com.circle.app.R
import com.circle.app.data.ExportEvent
import com.circle.app.data.ExportStep
import com.circle.app.data.billing.Plan
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.UnderlineTabs
import com.circle.app.ui.theme.Body
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun Tinted(content: @Composable ColumnScope.() -> Unit) {
    val colors = CircleTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.accent.copy(alpha = 0.08f))
            .border(1.dp, colors.accent.copy(alpha = 0.30f), RoundedCornerShape(18.dp)).padding(18.dp),
        content = content
    )
}

@Composable
private fun CapsLabel(text: String, accent: Boolean = false) {
    Text(
        text, color = if (accent) CircleTheme.colors.accent else CircleTheme.colors.muted, fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun Pill(text: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(999.dp))
            .background(if (destructive) colors.alertDot else colors.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (destructive) androidx.compose.ui.graphics.Color.White else colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/** "Lead. rest" with the lead in bold. */
@Composable
private fun LeadLine(lead: String, rest: String) {
    val colors = CircleTheme.colors
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.padding(top = 8.dp, end = 10.dp).size(6.dp).background(colors.accent, CircleShape))
        Text(
            buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.text)) { append(lead) }; append(" "); append(rest) },
            color = colors.body, fontSize = 14.5.sp, lineHeight = 21.sp
        )
    }
}

// ------------------------------------------------------------------ Terms, Privacy, Company

/** Terms / Privacy / Company: the short version first, then the full text (placeholders until a lawyer writes them). */
@Composable
fun LegalScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.legal_title)) { nav.popBackStack() }
        Box(Modifier.padding(start = 16.dp, top = 4.dp)) {
            UnderlineTabs(listOf(stringResource(R.string.legal_terms), stringResource(R.string.legal_privacy), stringResource(R.string.legal_company)), tab) { tab = it }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Tinted { Text(stringResource(R.string.legal_placeholder_notice), color = colors.text, fontSize = 13.sp, lineHeight = 19.sp) }
            when (tab) {
                0 -> {
                    CapsLabel(stringResource(R.string.legal_short_caps))
                    LeadLine(stringResource(R.string.terms_1_lead), stringResource(R.string.terms_1))
                    LeadLine(stringResource(R.string.terms_2_lead), stringResource(R.string.terms_2))
                    LeadLine(stringResource(R.string.terms_3_lead), stringResource(R.string.terms_3))
                    LeadLine(stringResource(R.string.terms_4_lead), stringResource(R.string.terms_4))
                    LeadLine(stringResource(R.string.terms_5_lead), stringResource(R.string.terms_5))
                    Text(stringResource(R.string.terms_full), color = colors.muted, fontSize = 13.sp)
                }
                1 -> {
                    CapsLabel(stringResource(R.string.legal_short_caps))
                    LeadLine(stringResource(R.string.privacy_1_lead), stringResource(R.string.privacy_1))
                    LeadLine(stringResource(R.string.privacy_2_lead), stringResource(R.string.privacy_2))
                    LeadLine(stringResource(R.string.privacy_3_lead), stringResource(R.string.privacy_3))
                    LeadLine(stringResource(R.string.privacy_4_lead), stringResource(R.string.privacy_4))
                    LeadLine(stringResource(R.string.privacy_5_lead), stringResource(R.string.privacy_5))
                    Text(stringResource(R.string.privacy_full), color = colors.muted, fontSize = 13.sp)
                }
                else -> {
                    CapsLabel(stringResource(R.string.company_provider_caps))
                    listOf(R.string.company_name, R.string.company_street, R.string.company_city, R.string.company_email).forEach {
                        Text(stringResource(it), color = colors.text, fontSize = 15.sp)
                    }
                    Text(stringResource(R.string.company_note), color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Membership

class MembershipViewModel : androidx.lifecycle.ViewModel() {
    private val billing = AppGraph.billing
    val prices = billing.prices
    val isMember = billing.isMember
    fun start(plan: Plan) { viewModelScope.launch { billing.start(plan) } }
}

/** Membership: two plans with the store's price strings (placeholders now), what it pays for, small print. */
@Composable
fun MembershipScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = androidx.lifecycle.viewmodel.compose.viewModel { MembershipViewModel() }
    val prices by vm.prices.collectAsStateWithLifecycle(emptyMap())
    val member by vm.isMember.collectAsStateWithLifecycle(false)
    var plan by rememberSaveable { mutableStateOf(Plan.YEARLY) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.membership_title)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Tinted {
                CapsLabel(stringResource(R.string.membership_kicker), accent = true)
                Text(stringResource(R.string.membership_headline), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, color = colors.text, modifier = Modifier.padding(top = 8.dp))
            }
            if (member) {
                Text(stringResource(R.string.membership_member), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.text)
                Text(stringResource(R.string.membership_thanks), color = colors.body, fontSize = 15.sp)
                Pill(stringResource(R.string.membership_manage)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Plan.MONTHLY, Plan.YEARLY).forEach { p ->
                        val selected = p == plan
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.surface)
                                .border(1.5.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(14.dp))
                                .selectable(selected, role = Role.RadioButton) { plan = p }.padding(14.dp)
                        ) {
                            CapsLabel(stringResource(if (p == Plan.MONTHLY) R.string.membership_monthly else R.string.membership_yearly))
                            Text(prices[p].orEmpty(), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = colors.text, modifier = Modifier.padding(top = 4.dp))
                            Text(stringResource(if (p == Plan.MONTHLY) R.string.membership_per_month else R.string.membership_per_year), color = colors.muted, fontSize = 12.5.sp)
                        }
                    }
                }
                CapsLabel(stringResource(R.string.membership_pays_caps))
                LeadLine(stringResource(R.string.membership_pays_1_lead), stringResource(R.string.membership_pays_1))
                LeadLine(stringResource(R.string.membership_pays_2_lead), stringResource(R.string.membership_pays_2))
                val price = prices[plan].orEmpty()
                Pill(stringResource(if (plan == Plan.YEARLY) R.string.membership_cta_year else R.string.membership_cta_month, price), enabled = price.isNotEmpty()) { vm.start(plan) }
                Text(stringResource(R.string.membership_small_print), color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ Your data

class YourDataViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val log = repo.exportLog().state(emptyList())
    fun request() = run({ requestDataExport() })
}

/** Your data: request a download (built automatically; nobody opens it) and the audit log; the danger zone. */
@Composable
fun YourDataScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { YourDataViewModel(it) }
    val log by vm.log.collectAsStateWithLifecycle()
    val requested = log.any { it.kind == ExportStep.REQUESTED }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.data_title)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.data_download_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.text, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.data_download_body), color = colors.body, fontSize = 14.5.sp, lineHeight = 21.sp)
            if (requested) Tinted { Text(stringResource(R.string.data_requested), color = colors.text, fontSize = 14.sp, lineHeight = 20.sp) }
            else Pill(stringResource(R.string.data_request)) { vm.request() }
            if (log.isNotEmpty()) {
                CapsLabel(stringResource(R.string.data_log_caps))
                log.forEach { Text(exportLine(it), color = colors.body, fontSize = 13.5.sp) }
            }
            Spacer(Modifier.height(12.dp))
            CapsLabel(stringResource(R.string.data_danger_caps))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, colors.alertDot.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { nav.navigate(Routes.DELETE_ACCOUNT) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.data_delete), color = colors.alertDot, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(R.string.data_delete_sub), color = colors.muted, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
private fun exportLine(e: ExportEvent): String {
    val date = e.at.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    return stringResource(
        when (e.kind) { ExportStep.REQUESTED -> R.string.data_log_requested; ExportStep.READY -> R.string.data_log_ready; ExportStep.DOWNLOADED -> R.string.data_log_downloaded },
        date
    )
}

// ------------------------------------------------------------------ Delete account

class DeleteAccountViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    fun delete(then: () -> Unit) = run({ deleteAccount() }) { if (it is Outcome.Success) then() }
}

/** Delete my account: says exactly what is removed; enabled only when DELETE is typed. */
@Composable
fun DeleteAccountScreen(nav: NavHostController, onDeleted: () -> Unit) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { DeleteAccountViewModel(it) }
    var typed by rememberSaveable { mutableStateOf("") }
    var done by rememberSaveable { mutableStateOf(false) }
    val canDelete = typed.trim().equals("DELETE", ignoreCase = true)
    Column(Modifier.fillMaxSize().background(colors.background).imePadding()) {
        if (!done) DetailHeader(stringResource(R.string.data_delete)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (done) {
                Spacer(Modifier.height(80.dp))
                Text(stringResource(R.string.delete_done_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = colors.text)
                Text(stringResource(R.string.delete_done_body), color = colors.body, fontSize = 15.sp)
                Pill(stringResource(R.string.delete_back_to_start), onClick = onDeleted)
                return@Column
            }
            Text(stringResource(R.string.delete_cant_undo), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.text)
            Text(stringResource(R.string.delete_body), color = colors.body, fontSize = 15.sp, lineHeight = 22.sp)
            Text(stringResource(R.string.delete_membership_note), color = colors.muted, fontSize = 13.5.sp)
            CapsLabel(stringResource(R.string.delete_type_caps))
            val hint = stringResource(R.string.delete_type_hint)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (typed.isEmpty()) Text(hint, color = colors.muted, fontSize = 15.sp)
                BasicTextField(
                    typed, { typed = it.take(10) }, singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 15.sp, fontFamily = Body), cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
                )
            }
            Pill(stringResource(R.string.data_delete), enabled = canDelete, destructive = true) { vm.delete { done = true } }
        }
    }
}
