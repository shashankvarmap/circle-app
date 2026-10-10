package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.ui.components.PrimaryButton
import com.circle.app.ui.theme.*

private data class Bullet(val text: String)

/**
 * 4-slide first-launch story. Deliberately has NO skip — see chat history:
 * the founder wants this mandatory. Slide 1 is the pricing/quote slide,
 * slide 2 the "once upon a time" doom-scroll story, slide 3 the mission.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var slide by remember { mutableStateOf(0) }
    val isLast = slide == 3

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Spacer(Modifier.height(44.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (slide) {
                0 -> SlideIntro()
                1 -> SlidePricing()
                2 -> SlideStory()
                3 -> SlideMission()
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 36.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (i == slide) 20.dp else 6.dp)
                            .background(if (i == slide) CircleTheme.colors.accent else CircleTheme.colors.border, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            if (isLast) {
                PrimaryButton(androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_get_started), modifier = Modifier.fillMaxWidth(), onClick = onDone)
            } else {
                PrimaryButton(androidx.compose.ui.res.stringResource(com.circle.app.R.string.newchat_next), modifier = Modifier.fillMaxWidth()) { slide++ }
            }
        }
    }
}

@Composable
private fun SlideIntro() {
    Box(
        modifier = Modifier.size(64.dp).background(CircleTheme.colors.accent, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Circle, contentDescription = null, tint = CircleTheme.colors.onAccent) }
    Spacer(Modifier.height(22.dp))
    com.circle.app.ui.components.Wordmark(fontSize = 26.sp)
    Spacer(Modifier.height(10.dp))
    Text(
        androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_a_place_for_the_people),
        color = CircleTheme.colors.body, fontSize = 14.5.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
    )
}

@Composable
private fun SlidePricing() {
    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_text), fontFamily = Display, fontSize = 40.sp, color = CircleTheme.colors.accent)
    Text(
        androidx.compose.ui.res.stringResource(com.circle.app.R.string.story_pricing_quote),
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
        fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, color = CircleTheme.colors.text, lineHeight = 28.sp
    )
    Spacer(Modifier.height(18.dp))
    // ACCESS_AND_LAUNCH 0.3: one sentence about ads here at most; the price comes from Google Play, never hard-coded.
    BulletList(
        listOf(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.story_pricing_1),
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.story_pricing_2),
        )
    )
}

@Composable
private fun SlideStory() {
    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_once_upon_a_time), color = CircleTheme.colors.muted, fontStyle = FontStyle.Italic, fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    Text(
        androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_social_media_used_to_be),
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
        textAlign = TextAlign.Center, color = CircleTheme.colors.text, lineHeight = 28.sp
    )
    Spacer(Modifier.height(16.dp))
    BulletList(
        listOf(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_it_was_just_about_keeping),
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_then_it_became_infinite_scroll)
        )
    )
}

@Composable
private fun SlideMission() {
    Box(
        modifier = Modifier.size(64.dp).background(CircleTheme.colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Circle, contentDescription = null, tint = CircleTheme.colors.accent) }
    Spacer(Modifier.height(18.dp))
    Text(
        androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_circle_is_for_you_and),
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
        textAlign = TextAlign.Center, color = CircleTheme.colors.text
    )
    Spacer(Modifier.height(14.dp))
    BulletList(
        listOf(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_no_infinite_scroll_we_tell),
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_want_more_create_or_join),
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_have_fun_outside_then_tell)
        )
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(
        modifier = Modifier.width(270.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.padding(top = 7.dp).size(5.dp).background(CircleTheme.colors.accent, CircleShape))
                Text(line, color = CircleTheme.colors.body, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
