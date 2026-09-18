package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.ui.theme.*

/** Circular initials avatar — the same visual language used for people everywhere. */
@Composable
fun Avatar(initials: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

/** Rounded-square avatar — reserved for Groups, distinct from people's circles. */
@Composable
fun GroupTile(initial: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3).dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = (size / 2.5).sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
}

/** Small underline text tabs — the "Twitter For You / Following" style used
 *  across Home (Friends/Groups), Notifications, Explore search, and Group. */
@Composable
fun UnderlineTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        tabs.forEachIndexed { i, label ->
            val isActive = i == selected
            Column(
                modifier = Modifier.clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    color = if (isActive) Color(0xFFD8D6D0) else TextDim,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(1.5.dp)
                        .fillMaxWidth()
                        .background(if (isActive) AccentDefault else Color.Transparent)
                )
            }
        }
    }
}

/** Reply / repost / like action row shared by Thought, Post, Repost and Quote cards. */
@Composable
fun PostActionRow(liked: Boolean = false, onLike: () -> Unit = {}, onRepost: () -> Unit = {}) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Reply", tint = TextMuted, modifier = Modifier.size(18.dp))
        Icon(
            Icons.Outlined.Repeat, contentDescription = "Repost / Quote", tint = TextMuted,
            modifier = Modifier.size(18.dp).clickable(onClick = onRepost)
        )
        Icon(
            Icons.Outlined.FavoriteBorder, contentDescription = "Like",
            tint = if (liked) AccentDefault else TextMuted,
            modifier = Modifier.size(18.dp).clickable(onClick = onLike)
        )
    }
}

@Composable
fun CircleCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(13.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) { content() }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentDefault)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
