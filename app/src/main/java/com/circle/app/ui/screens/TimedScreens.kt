package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import androidx.compose.ui.draw.drawBehind
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.LifetimePreview
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.ChipShell
import com.circle.app.ui.components.GroupTile
import com.circle.app.ui.components.LifetimeRing
import com.circle.app.ui.components.lastDay
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

/*
 * Creating and joining temporary groups and chats (TIMED_GROUPS 5). Layout, text and sizes from
 * prototype-timed-groups-night/day: the LIFETIME section, How timed groups work, and the join card.
 */

/** The choice in a create form: permanent, or temporary for [days] (1 to 90). */
class LifetimeChoice(temporary: Boolean = false, days: Int = 7, custom: Boolean = false) {
    var temporary by mutableStateOf(temporary)
    var days by mutableIntStateOf(days)
    var custom by mutableStateOf(custom)
    /** What to pass to the repository: null for permanent. */
    val lifetimeDays: Int? get() = if (temporary) days else null
}

@Composable
fun rememberLifetimeChoice(): LifetimeChoice = rememberSaveable(
    saver = androidx.compose.runtime.saveable.listSaver(
        save = { listOf(it.temporary, it.days, it.custom) },
        restore = { LifetimeChoice(it[0] as Boolean, it[1] as Int, it[2] as Boolean) }
    )
) { LifetimeChoice() }

private val DayChoices = listOf(1, 3, 7, 14, 30, 90)

@Composable
private fun CapsLabel(text: String, top: Int) {
    Text(
        text, color = CircleTheme.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.32.sp,
        modifier = Modifier.padding(top = top.dp, bottom = 8.dp).semantics { heading() }
    )
}

/**
 * HOW LONG DOES IT LAST?: Permanent | Temporary cards; for Temporary the day chips, Custom stepper,
 * the WHAT HAPPENS card with real dates from the server's clock, and the How it works link.
 */
@Composable
fun LifetimeSection(choice: LifetimeChoice, isGroup: Boolean, preview: (Int) -> LifetimePreview, onHow: () -> Unit) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxWidth()) {
        CapsLabel(stringResource(R.string.lifetime_heading), top = 20)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            LifeCard(stringResource(R.string.lifetime_permanent), stringResource(R.string.lifetime_permanent_sub), !choice.temporary, Modifier.weight(1f).fillMaxHeight()) { choice.temporary = false }
            LifeCard(stringResource(R.string.lifetime_temporary), stringResource(R.string.lifetime_temporary_sub), choice.temporary, Modifier.weight(1f).fillMaxHeight()) { choice.temporary = true }
        }
        if (!choice.temporary) {
            Text(stringResource(R.string.lifetime_permanent_note), color = colors.muted, fontSize = 12.5.sp, lineHeight = 18.75.sp, modifier = Modifier.padding(top = 12.dp))
            return@Column
        }
        CapsLabel(stringResource(R.string.lifetime_ends_in), top = 18)
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DayChoices.forEach { d ->
                DayChip(pluralStringResource(R.plurals.timer_days, d, d), !choice.custom && choice.days == d) { choice.days = d; choice.custom = false }
            }
            DayChip(stringResource(R.string.lifetime_custom), choice.custom) { choice.custom = true }
        }
        if (choice.custom) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", stringResource(R.string.lifetime_fewer)) { choice.days = (choice.days - 1).coerceAtLeast(1) }
                Row(Modifier.widthIn(min = 110.dp).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    Text(choice.days.toString(), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, color = colors.text)
                    Text(pluralStringResource(R.plurals.lifetime_days_word, choice.days), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
                }
                StepButton("+", stringResource(R.string.lifetime_more)) { choice.days = (choice.days + 1).coerceAtMost(90) }
            }
            Text(stringResource(R.string.lifetime_range), color = colors.muted, fontSize = 11.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        val p = preview(choice.days)
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.padding(top = 16.dp).fillMaxWidth().clip(shape).background(colors.accent.copy(alpha = 0.08f))
                .border(1.dp, colors.accent.copy(alpha = 0.3f), shape).padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(stringResource(R.string.lifetime_what_happens), color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            HappensLine(stringResource(R.string.lifetime_today), stringResource(if (isGroup) R.string.lifetime_today_group else R.string.lifetime_today_chat), top = 8)
            HappensLine(lastDay(p.endsAt), stringResource(R.string.lifetime_end_line), top = 6)
            HappensLine(lastDay(p.deleteAt), stringResource(R.string.lifetime_delete_line), top = 6)
        }
        Text(stringResource(R.string.lifetime_told), color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 10.dp))
        if (isGroup) {
            val s = RoundedCornerShape(12.dp)
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(s).border(1.dp, colors.border, s).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, null, tint = colors.accent, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.text)) { append(stringResource(R.string.lifetime_group_chat)); append(" ") }
                        withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.Bold)) { append(stringResource(R.string.timed_together)) }
                    },
                    fontSize = 12.5.sp, lineHeight = 18.75.sp
                )
            }
        }
        Text(
            stringResource(R.string.lifetime_how_link), color = colors.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onHow)
                .padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun LifeCard(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.surface)
            .border(1.5.dp, if (selected) colors.accent else colors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = colors.muted, fontSize = 12.sp, lineHeight = 16.8.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.heightIn(min = 48.dp).clip(shape).clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (selected) colors.onAccent else colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(shape).background(if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.5.dp, if (selected) colors.accent else colors.border, shape).padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun StepButton(symbol: String, label: String, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, colors.border, CircleShape), contentAlignment = Alignment.Center) {
            Text(symbol, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HappensLine(lead: String, text: String, top: Int) {
    val colors = CircleTheme.colors
    Row(Modifier.padding(top = top.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(lead, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 19.6.sp, modifier = Modifier.widthIn(min = 70.dp))
        Text(text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.6.sp)
    }
}

// ---------------------------------------------------------------- How timed groups work

@Composable
fun HowTimedScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.how_title)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.how_headline), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.36).sp, color = colors.text)
            Text(stringResource(R.string.how_intro), color = colors.muted, fontSize = 14.sp, lineHeight = 21.7.sp, modifier = Modifier.padding(top = 8.dp))
            val stages = listOf(
                R.string.how_stage1 to R.string.how_stage1_body, R.string.how_stage2 to R.string.how_stage2_body,
                R.string.how_stage3 to R.string.how_stage3_body, R.string.how_stage4 to R.string.how_stage4_body,
            )
            Box(Modifier.padding(top = 22.dp)) {
                // The timeline's line, behind the numbered dots.
                Column(Modifier.drawBehind {
                    val x = 12.dp.toPx()
                    drawLine(colors.border, androidx.compose.ui.geometry.Offset(x, 14.dp.toPx()), androidx.compose.ui.geometry.Offset(x, size.height - 14.dp.toPx() - 18.dp.toPx()), strokeWidth = 2.dp.toPx())
                }) {
                    stages.forEachIndexed { i, (title, body) ->
                        Row(Modifier.padding(bottom = 18.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val last = i == stages.lastIndex
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(if (last) colors.background else colors.accent).border(2.dp, colors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text((i + 1).toString(), color = if (last) colors.accent else colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
                            Column {
                                Text(stringResource(title), color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(body), color = colors.body, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.how_good_to_know), color = colors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.47.sp,
                modifier = Modifier.padding(start = 2.dp, top = 22.dp, bottom = 4.dp).semantics { heading() }
            )
            listOf(R.string.how_q1 to R.string.how_a1, R.string.how_q2 to R.string.how_a2, R.string.how_q3 to R.string.how_a3, R.string.how_q4 to R.string.how_a4).forEach { (q, a) ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 12.dp).semantics(mergeDescendants = true) {}) {
                    Text(stringResource(q), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(a), color = colors.body, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            // Placeholder: reported-content retention and exact deletion timings need a lawyer (TIMED_GROUPS 9).
            val s = RoundedCornerShape(12.dp)
            Text(
                stringResource(R.string.how_placeholder), color = colors.body, fontSize = 11.5.sp, lineHeight = 17.25.sp,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth().clip(s).background(colors.accent.copy(alpha = 0.08f))
                    .border(1.dp, colors.accent.copy(alpha = 0.3f), s).padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- Join a temporary group

class JoinTimedViewModel(repo: CircleRepository, private val groupId: String) : RepoViewModel(repo) {
    val group = repo.group(groupId).state(null)
    fun request(then: (Outcome<Unit>) -> Unit) = run({ requestToJoin(groupId) }, then)
}

/**
 * Before joining a temporary group: what joining means. Requests to join are never instant (a host
 * approves them), so the button asks to join; the consent line is part of the terms (TIMED_GROUPS 9).
 */
@Composable
fun JoinTimedGroupScreen(nav: NavHostController, groupId: String) {
    val colors = CircleTheme.colors
    val vm = circleViewModel(key = "join-$groupId") { JoinTimedViewModel(it, groupId) }
    val group by vm.group.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sent = stringResource(R.string.group_request_sent_note)
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader("") { nav.popBackStack() }
        val g = group ?: return@Column
        val life = g.lifetime
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GroupTile(g.ref.initial, g.ref.color, size = 60)
                Column {
                    Text(g.ref.name, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = colors.text, modifier = Modifier.semantics { heading() })
                    Text(
                        stringResource(R.string.dot_pair, stringResource(if (g.isPublic) R.string.join_public_group else R.string.join_private_group), pluralStringResource(R.plurals.members_count, g.memberCount, g.memberCount)),
                        color = colors.muted, fontSize = 12.5.sp
                    )
                }
            }
            if (life != null) {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(colors.accent.copy(alpha = 0.08f))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), shape).padding(16.dp)
                ) {
                    ChipShell(loud = true, text = stringResource(R.string.timer_temporary)) { LifetimeRing(life.elapsed) }
                    Text(
                        stringResource(R.string.join_ends_on, lastDay(life.endsAt)), fontFamily = Display, fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp, lineHeight = 25.sp, color = colors.text, modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(stringResource(R.string.join_body), color = colors.body, fontSize = 13.5.sp, lineHeight = 20.9.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Text(stringResource(R.string.join_agree), color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val canAsk = !g.isMember && !g.joinRequested && g.isOpen
            Box(
                Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (canAsk) colors.accent else colors.track)
                    .clickable(enabled = canAsk, role = Role.Button) {
                        vm.request { if (it is Outcome.Success) { android.widget.Toast.makeText(context, sent, android.widget.Toast.LENGTH_SHORT).show(); nav.popBackStack() } }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(if (g.joinRequested) R.string.profile_requested else R.string.common_request_to_join),
                    color = if (canAsk) colors.onAccent else colors.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).clickable(role = Role.Button) { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.join_not_now), color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

