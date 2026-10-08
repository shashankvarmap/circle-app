package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.FriendRequestButton
import com.circle.app.ui.components.PrimaryButton
import com.circle.app.data.CircleStore
import com.circle.app.data.FriendStatus
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Stage { PROMPT, LOADING, RESULTS }

private data class Contact(val name: String, val initials: String, val color: androidx.compose.ui.graphics.Color)

private val contacts = listOf(
    Contact("Priya Kapoor", "PK", AvatarSage),
    Contact("Jonah Lee", "JL", AvatarBlue),
    Contact("Amara Osei", "AO", AvatarOrange)
)

@Composable
fun ContactsSyncScreen(nav: NavHostController) {
    var stage by remember { mutableStateOf(Stage.PROMPT) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Skip for now", color = TextMuted, fontSize = 13.sp,
                modifier = Modifier.clickable { nav.navigate(Routes.HOME) }
            )
        }

        when (stage) {
            Stage.PROMPT -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(Surface, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.People, contentDescription = null, tint = AccentDefault) }
                Spacer(Modifier.height(22.dp))
                Text("Find your circle", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Text(
                    "See which of your contacts are already here, so you're not starting from zero.",
                    color = TextBody, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We only check for matches — your contacts are never stored or shared.",
                    color = TextDim, fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
                )
                Spacer(Modifier.height(22.dp))
                PrimaryButton("Connect contacts", modifier = Modifier.fillMaxWidth()) {
                    stage = Stage.LOADING
                    scope.launch { delay(900); stage = Stage.RESULTS }
                }
            }

            Stage.LOADING -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = AccentDefault, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(16.dp))
                Text("Looking for matches…", color = TextBody, fontSize = 13.5.sp)
            }

            Stage.RESULTS -> Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("3 people you know", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Send a friend request now — decide who goes in your Inner Circle later from their profile.",
                    color = TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                contacts.forEach { c ->
                    val s = CircleStore.statusOf(c.name)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.initials, c.color, size = 42)
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            val subtitle = when (s) {
                                FriendStatus.NONE -> "In your phone contacts"
                                FriendStatus.REQUESTED -> "Request sent"
                                FriendStatus.ACCEPTED -> "${c.name.substringBefore(" ")} accepted your request"
                            }
                            Text(subtitle, color = TextMuted, fontSize = 11.5.sp)
                        }
                        FriendRequestButton(c.name, addLabel = "Add")
                    }
                }

                Spacer(Modifier.weight(1f))
                PrimaryButton("Continue", modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                    nav.navigate(Routes.HOME)
                }
            }
        }
    }
}

