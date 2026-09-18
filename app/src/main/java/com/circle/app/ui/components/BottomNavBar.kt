package com.circle.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NavTab { HOME, MESSAGES, EXPLORE, NOTIFICATIONS, PROFILE }

/**
 * Shared bottom nav. `active` highlights the current tab. Explore sits raised
 * in the center with an accent OUTLINE (not filled) — that replaced the old
 * "+" compose button in the design. Holding the Home icon for ~450ms does a
 * clean, cumulative 360° spin and fires [onHomeIconHold] — used on HomeScreen
 * to flip between the Friends/Groups feed without ever spinning "back".
 */
@Composable
fun CircleBottomNav(
    nav: NavHostController,
    active: NavTab,
    onHomeIconHold: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Background)
            .border(width = 1.dp, color = Border),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home — tap navigates; press-and-hold spins and flips the feed (Home screen only)
        Box(
            modifier = Modifier
                .size(40.dp)
                .pointerInput(onHomeIconHold) {
                    detectTapGestures(
                        onPress = {
                            var held = true
                            val holdJob = scope.launch {
                                delay(450)
                                if (held) {
                                    onHomeIconHold?.invoke()
                                    scope.launch {
                                        rotation.animateTo(rotation.value + 360f, animationSpec = tween(650))
                                    }
                                }
                            }
                            val released = tryAwaitRelease()
                            held = false
                            if (released) holdJob.cancel()
                            if (active != NavTab.HOME) nav.navigate(Routes.HOME)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = "Home",
                tint = if (active == NavTab.HOME) AccentDefault else TextMuted,
                modifier = Modifier.rotate(rotation.value)
            )
        }

        NavIcon(Icons.Outlined.Send, "Messages", active == NavTab.MESSAGES) { nav.navigate(Routes.MESSAGES) }

        // Explore — raised center, accent-outlined
        Box(
            modifier = Modifier
                .size(46.dp)
                .offset(y = (-14).dp)
                .clip(CircleShape)
                .background(Background)
                .border(width = 2.dp, color = AccentDefault, shape = CircleShape)
                .clickable { nav.navigate(Routes.EXPLORE) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Explore, contentDescription = "Explore", tint = AccentDefault)
        }

        Box {
            NavIcon(Icons.Outlined.FavoriteBorder, "Notifications", active == NavTab.NOTIFICATIONS) {
                nav.navigate(Routes.NOTIFICATIONS)
            }
            if (active != NavTab.NOTIFICATIONS) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .background(Danger, CircleShape)
                )
            }
        }

        NavIcon(Icons.Outlined.Person, "Profile", active == NavTab.PROFILE) { nav.navigate(Routes.PROFILE) }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = label,
        tint = if (isActive) AccentDefault else TextMuted,
        modifier = Modifier.size(24.dp).clickable(onClick = onClick)
    )
}
