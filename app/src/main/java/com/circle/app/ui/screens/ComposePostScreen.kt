package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = CircleTheme.colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent).padding(horizontal = 18.dp, vertical = 7.dp)) {
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
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.surface).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Friends", color = CircleTheme.colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }
            }
            Text(
                "Or switch audience to Inner circle", color = CircleTheme.colors.muted, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
