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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.theme.*

@Composable
fun CreateGroupScreen(nav: NavHostController) {
    var isPrivate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = TextBody, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            Text("New group", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AccentDefault)
                    .clickable { nav.popBackStack() }.padding(horizontal = 16.dp, vertical = 7.dp)
            ) { Text("Create", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AvatarOrange),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Image, contentDescription = "Add group icon", tint = OnAccent) }
            }
            LabeledField("GROUP NAME", name, { name = it }, "e.g. Aachen Photo Walk")
            LabeledField("CATEGORY", category, { category = it }, "e.g. Photography, Fitness & Sports")
            LabeledField("DESCRIPTION", description, { description = it }, "What's this group about?", multiline = true)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.circle.app.ui.components.SectionLabel("VISIBILITY")
                VisibilityOption(
                    icon = Icons.Outlined.Lock, title = "Private", subtitle = "Only visible to members. Default for every group.",
                    selected = isPrivate
                ) { isPrivate = true }
                VisibilityOption(
                    icon = Icons.Outlined.Public, title = "Public", subtitle = "Listed in Explore. Join requests are visible to every member.",
                    selected = !isPrivate
                ) { isPrivate = false }
            }

            Text(
                "You can switch this later from the group's settings. Every group also gets a linked chat, in Messages → Group Messages, once it's created.",
                color = TextDim, fontSize = 11.sp
            )
        }
    }
}

@Composable
fun LabeledField(label: String, value: String, onChange: (String) -> Unit, placeholder: String, multiline: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.circle.app.ui.components.SectionLabel(label)
        OutlinedTextField(
            value = value, onValueChange = onChange,
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().let { if (multiline) it.height(96.dp) else it },
            shape = RoundedCornerShape(11.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                focusedIndicatorColor = AccentDefault, unfocusedIndicatorColor = Border,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            )
        )
    }
}

@Composable
fun VisibilityOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, if (selected) AccentDefault else Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextBody, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(subtitle, color = TextMuted, fontSize = 11.5.sp)
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) AccentDefault else BorderStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) androidx.compose.foundation.layout.Box(modifier = Modifier.size(9.dp).background(AccentDefault, CircleShape))
        }
    }
}
