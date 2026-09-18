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
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.PrimaryButton
import com.circle.app.ui.theme.*

private data class Bullet(val text: String)

/**
 * 4-slide first-launch story. Deliberately has NO skip — see chat history:
 * the founder wants this mandatory. Slide 1 is the pricing/quote slide,
 * slide 2 the "once upon a time" doom-scroll story, slide 3 the mission.
 */
@Composable
fun OnboardingScreen(nav: NavHostController) {
    var slide by remember { mutableStateOf(0) }
    val isLast = slide == 3

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
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
                            .background(if (i == slide) AccentDefault else BorderStrong, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            if (isLast) {
                PrimaryButton("Get started", modifier = Modifier.fillMaxWidth()) {
                    nav.navigate(Routes.CONTACTS_SYNC)
                }
            } else {
                PrimaryButton("Next", modifier = Modifier.fillMaxWidth()) { slide++ }
            }
        }
    }
}

@Composable
private fun SlideIntro() {
    Box(
        modifier = Modifier.size(64.dp).background(AccentDefault, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Circle, contentDescription = null, tint = OnAccent) }
    Spacer(Modifier.height(22.dp))
    Text("Circle.", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = TextPrimary)
    Spacer(Modifier.height(10.dp))
    Text(
        "A place for the people you actually know — not everyone you've ever met.",
        color = TextBody, fontSize = 14.5.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
    )
}

@Composable
private fun SlidePricing() {
    Text("\u201C", fontFamily = Display, fontSize = 40.sp, color = AccentDefault)
    Text(
        "If the app is free, you're the product.",
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
        fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, color = TextPrimary, lineHeight = 28.sp
    )
    Spacer(Modifier.height(18.dp))
    Text("So this one isn't free.", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
    Spacer(Modifier.height(10.dp))
    BulletList(
        listOf(
            "About €1 a month — just enough to cover the servers",
            "No ads, ever",
            "Nothing about you to sell to anyone"
        )
    )
}

@Composable
private fun SlideStory() {
    Text("Once upon a time,", color = TextMuted, fontStyle = FontStyle.Italic, fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    Text(
        "social media used to be simple.",
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
        textAlign = TextAlign.Center, color = TextPrimary, lineHeight = 28.sp
    )
    Spacer(Modifier.height(16.dp))
    BulletList(
        listOf(
            "It was just about keeping up with friends",
            "Then it became infinite scroll, and hours disappeared"
        )
    )
}

@Composable
private fun SlideMission() {
    Box(
        modifier = Modifier.size(64.dp).background(Surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Circle, contentDescription = null, tint = AccentDefault) }
    Spacer(Modifier.height(18.dp))
    Text(
        "Circle is for you and your friends.",
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
        textAlign = TextAlign.Center, color = TextPrimary
    )
    Spacer(Modifier.height(14.dp))
    BulletList(
        listOf(
            "No infinite scroll — we tell you when you're caught up",
            "Want more? Create or join an event or a group",
            "Have fun outside, then tell everyone about it here"
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
                Box(modifier = Modifier.padding(top = 7.dp).size(5.dp).background(AccentDefault, CircleShape))
                Text(line, color = TextBody, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
