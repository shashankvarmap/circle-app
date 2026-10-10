package com.circle.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.data.InvitesFrom
import com.circle.app.data.RequestsFrom
import com.circle.app.data.ThemeMode
import com.circle.app.ui.orbit.Sector

/** Display names for the enums the data layer keeps language-free. */
@Composable
fun ThemeMode.text(): String = stringResource(
    when (this) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.NIGHT -> R.string.theme_night; ThemeMode.DAY -> R.string.theme_day }
)

@Composable
fun RequestsFrom.text(): String = stringResource(
    when (this) { RequestsFrom.EVERYONE -> R.string.from_everyone; RequestsFrom.FRIENDS_OF_FRIENDS -> R.string.visibility_fof }
)

@Composable
fun InvitesFrom.text(): String = stringResource(
    when (this) { InvitesFrom.FRIENDS -> R.string.visibility_friends; InvitesFrom.FRIENDS_OF_FRIENDS -> R.string.visibility_fof }
)

/** "Outdoors", "Social"...; orbit labels upper-case it. */
@Composable
fun Sector.text(): String = stringResource(
    when (this) {
        Sector.OUTDOORS -> R.string.sector_outdoors; Sector.SOCIAL -> R.string.sector_social
        Sector.MUSIC -> R.string.sector_music; Sector.MAKING -> R.string.sector_making
    }
)

/** Stage names for temporary groups and chats (TIMED_GROUPS 3). */
@Composable
fun com.circle.app.data.lifetime.LifeState.text(): String = stringResource(
    when (this) {
        com.circle.app.data.lifetime.LifeState.PERMANENT -> R.string.life_permanent
        com.circle.app.data.lifetime.LifeState.ACTIVE -> R.string.life_active
        com.circle.app.data.lifetime.LifeState.ENDING_SOON -> R.string.life_ending_soon
        com.circle.app.data.lifetime.LifeState.WRAP_UP -> R.string.life_wrap_up
        com.circle.app.data.lifetime.LifeState.DELETED -> R.string.life_deleted
    }
)
