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
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.FriendRequestButton
import com.circle.app.ui.components.PrimaryButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.access.Relationship
import com.circle.app.data.repo.CircleRepository
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Stage { PROMPT, LOADING, RESULTS }

/** Phone contacts who are already on Circle (matched by the repository; Limited information only). */
class ContactsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val matches = repo.contactMatches().state(emptyList())
}

@Composable
fun ContactsSyncScreen(onDone: () -> Unit) {
    var stage by remember { mutableStateOf(Stage.PROMPT) }
    val vm = circleViewModel { ContactsViewModel(it) }
    val matches by vm.matches.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Skip for now", color = CircleTheme.colors.muted, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onDone).padding(vertical = 12.dp)
            )
        }

        when (stage) {
            Stage.PROMPT -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(CircleTheme.colors.surface, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.People, contentDescription = null, tint = CircleTheme.colors.accent) }
                Spacer(Modifier.height(22.dp))
                Text("Find your circle", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = CircleTheme.colors.text)
                Spacer(Modifier.height(10.dp))
                Text(
                    "See which of your contacts are already here, so you're not starting from zero.",
                    color = CircleTheme.colors.body, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We only check for matches — your contacts are never stored or shared.",
                    color = CircleTheme.colors.muted, fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
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
                CircularProgressIndicator(color = CircleTheme.colors.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(16.dp))
                Text("Looking for matches…", color = CircleTheme.colors.body, fontSize = 13.5.sp)
            }

            Stage.RESULTS -> Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("${matches.size} people you know", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = CircleTheme.colors.text)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Send a friend request now — decide who goes in your Inner Circle later from their profile.",
                    color = CircleTheme.colors.muted, fontSize = 12.5.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                matches.forEach { m ->
                    val c = m.person
                    val s = m.relationship
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.initials, c.color, size = 42)
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            val subtitle = when (s) {
                                Relationship.REQUEST_SENT -> "Request sent"
                                Relationship.REQUEST_RECEIVED -> "Wants to be friends"
                                Relationship.FRIEND -> "Already in your circle"
                                else -> "In your phone contacts"
                            }
                            Text(subtitle, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                        }
                        FriendRequestButton(c.id, s, addLabel = "Add")
                    }
                }

                Spacer(Modifier.weight(1f))
                PrimaryButton("Continue", modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), onClick = onDone)
            }
        }
    }
}

