package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

@Composable
fun ProfileScreen(nav: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar("S", AvatarSlate, size = 72)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Shashank", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("@shashank", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("PINNED", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        "Slowly rebuilding the workshop table. Say hi if you're nearby — always up for coffee and bad puns.",
                        color = TextBody, fontSize = 14.5.sp
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    StatItem("128", "friends")
                    StatItem("6", "inner circle")
                }
            }
            item {
                UnderlineTabs(listOf("All", "Thoughts", "Posts"), 0) {}
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row {
                        Text("Shashank", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("· 1d", color = TextMuted, fontSize = 11.5.sp)
                    }
                    Text("Repotted every plant on the balcony before the first frost. Overdue by a month.", color = TextBody, fontSize = 13.5.sp)
                }
            }
        }
        CircleBottomNav(nav, NavTab.PROFILE)
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextMuted, fontSize = 13.sp)
    }
}
