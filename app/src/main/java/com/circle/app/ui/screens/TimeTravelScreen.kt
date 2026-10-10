package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.Clock
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.DemoControls
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.format.clockTime
import com.circle.app.ui.format.shortDate
import com.circle.app.ui.format.text
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToLong

class TimeTravelViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    private val demo = repo as? DemoControls
    val shift = (demo?.timeShift() ?: kotlinx.coroutines.flow.flowOf(Duration.ZERO)).state(Duration.ZERO)
    val items = (demo?.timedItems() ?: kotlinx.coroutines.flow.flowOf(emptyList())).state(emptyList())
    fun travel(days: Long) = run({ demo?.travel(Duration.ofDays(days)); Outcome.Success(Unit) })
    fun backToToday() = run({ demo?.backToToday(); Outcome.Success(Unit) })
    fun jump(id: String, stage: LifeState) = run({ demo?.jumpTo(id, stage); Outcome.Success(Unit) })
}

/**
 * Settings > Demo > Time travel (TIMED_GROUPS T2, preview builds): moves the fake server's clock so
 * every stage of a temporary group or chat can be tried without waiting. Everything follows live.
 */
@Composable
fun TimeTravelScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { TimeTravelViewModel(it) }
    val shift by vm.shift.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val now = Clock.now()

    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.time_travel)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.time_travel_intro), color = colors.muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 14.dp))
            Text(
                stringResource(R.string.time_travel_now, shortDate(now), clockTime(now)),
                color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)
            )
            val days = (shift.toMinutes() / (24.0 * 60)).roundToLong()
            Text(
                when {
                    shift.isZero -> stringResource(R.string.time_travel_real_time)
                    days == 0L -> stringResource(R.string.time_travel_close)
                    days > 0 -> pluralStringResource(R.plurals.time_travel_ahead, days.toInt(), days.toInt())
                    else -> pluralStringResource(R.plurals.time_travel_behind, abs(days).toInt(), abs(days).toInt())
                },
                color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp)
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1L, 3L, 7L).forEach { d ->
                    Pill(pluralStringResource(R.plurals.time_travel_plus, d.toInt(), d.toInt()), accent = true) { vm.travel(d) }
                }
            }
            if (!shift.isZero) Box(Modifier.padding(top = 8.dp)) { Pill(stringResource(R.string.time_travel_back), accent = false) { vm.backToToday() } }

            Text(
                stringResource(R.string.time_travel_jump).uppercase(), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.54.sp,
                modifier = Modifier.padding(top = 26.dp, bottom = 6.dp).semantics { heading() }
            )
            if (items.isEmpty()) Text(stringResource(R.string.time_travel_none), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            items.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(item.name, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.dot_pair, stringResource(if (item.isGroup) R.string.time_travel_kind_group else R.string.time_travel_kind_chat), item.state.text()),
                        color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(LifeState.ACTIVE, LifeState.ENDING_SOON, LifeState.WRAP_UP, LifeState.DELETED).forEach { stage ->
                            Pill(stage.text(), accent = stage == item.state) { vm.jump(item.id, stage) }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            Text(stringResource(R.string.time_travel_note), color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 14.dp, bottom = 24.dp))
        }
    }
}

@Composable
private fun Pill(label: String, accent: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.heightIn(min = 48.dp).clip(shape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.clip(shape)
                .background(if (accent) colors.accent else colors.background)
                .border(1.5.dp, if (accent) colors.accent else colors.border, shape)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) { Text(label, color = if (accent) colors.onAccent else colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}
