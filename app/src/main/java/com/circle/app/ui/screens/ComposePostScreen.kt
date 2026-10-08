package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import com.circle.app.data.Audience
import com.circle.app.data.CircleStore
import com.circle.app.navigation.Routes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.theme.*

/** Unified composer — a Thought and a Post are the same object; media is optional. */
@Composable
fun ComposePostScreen(nav: NavHostController) {
    var audience by rememberSaveable { mutableStateOf(Audience.FRIENDS) }
    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = CircleTheme.colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            // Disabled until the composer has text (the text field arrives with the posts stage).
            Box(
                modifier = Modifier.alpha(0.4f).clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent)
                    .semantics { disabled() }.padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text("Post", color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Row {
                Avatar("S", AvatarSlate, size = 38)
                Spacer(Modifier.width(12.dp))
                Text("What's on your mind?", color = CircleTheme.colors.muted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Image, contentDescription = "Add photo or video", tint = CircleTheme.colors.accent)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CircleTheme.colors.surface)
                        .border(1.5.dp, CircleTheme.colors.accent, RoundedCornerShape(999.dp))
                        .clickable(onClickLabel = "Change audience") {
                            audience = if (audience == Audience.FRIENDS) Audience.INNER_CIRCLE else Audience.FRIENDS
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (audience == Audience.FRIENDS) "Friends" else "Inner circle",
                        color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                if (audience == Audience.FRIENDS) "All your friends can see this."
                else "Only your ${CircleStore.innerCount} inner-circle friends can see this.",
                color = CircleTheme.colors.muted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            Text(
                "Edit who's in your inner circle",
                color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { nav.navigate(Routes.YOUR_CIRCLE) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
