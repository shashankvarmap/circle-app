package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.ui.components.SectionLabel
import com.circle.app.ui.theme.*

@Composable
fun CreateEventScreen(nav: NavHostController) {
    var capacity by remember { mutableStateOf(12) }
    var visibility by remember { mutableStateOf(0) } // 0 friends, 1 fof, 2 open

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = TextBody, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            Text("New event", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AccentDefault)
                    .clickable { nav.popBackStack() }.padding(horizontal = 16.dp, vertical = 7.dp)
            ) { Text("Create", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledField("TITLE", "", {}, "e.g. Board Game Night")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("DATE")
                    PickerRow(Icons.Outlined.CalendarToday, "Select date")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("TIME")
                    PickerRow(Icons.Outlined.Schedule, "Select time")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("VENUE")
                PickerRow(Icons.Outlined.LocationOn, "Search for a public venue")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("CAPACITY")
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Surface).padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max attendees", color = TextBody, fontSize = 13.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(SurfaceAlt)
                                .clickable { if (capacity > 1) capacity-- },
                            contentAlignment = Alignment.Center
                        ) { Text("−", color = TextPrimary, fontWeight = FontWeight.Bold) }
                        Text("$capacity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(SurfaceAlt)
                                .clickable { capacity++ },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = TextPrimary, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WHO CAN SEE THIS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityChip("Friends", visibility == 0) { visibility = 0 }
                    VisibilityChip("Friends of friends", visibility == 1) { visibility = 1 }
                    VisibilityChip("Open nearby", visibility == 2) { visibility = 2 }
                }
            }

            LabeledField("DESCRIPTION", "", {}, "What should people know before joining?", multiline = true)

            Text(
                "The reply thread only unlocks for people who tap Join — not a public comment section. Report and block tools are available on every event.",
                color = TextDim, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PickerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, placeholder: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Surface).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(placeholder, color = TextMuted, fontSize = 13.5.sp)
    }
}

@Composable
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) AccentDefault else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) AccentDefault else BorderStrong, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) OnAccent else TextBody, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
    }
}
