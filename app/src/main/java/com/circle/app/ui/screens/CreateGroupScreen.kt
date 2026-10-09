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
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.theme.*

class CreateViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    fun createGroup(name: String, category: String, description: String, isPublic: Boolean, then: (Outcome<String>) -> Unit) =
        run({ createGroup(name, category, description, isPublic) }, then)
    fun createEvent(
        title: String, visibility: com.circle.app.data.access.EventVisibility, description: String, capacity: Int,
        startsAt: java.time.Instant, topic: String, venue: String, then: (Outcome<String>) -> Unit,
    ) = run({ createEvent(title, visibility, description, capacity, startsAt, topic, venue) }, then)
}

@Composable
fun CreateGroupScreen(nav: NavHostController) {
    val vm = circleViewModel { CreateViewModel(it) }
    var isPrivate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.cancel), color = CircleTheme.colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_new_group), color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val canCreate = name.isNotBlank()
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (canCreate) CircleTheme.colors.accent else CircleTheme.colors.track)
                    .clickable(enabled = canCreate) {
                        vm.createGroup(name, category, description, isPublic = !isPrivate) { if (it is Outcome.Success) nav.navigate(com.circle.app.navigation.Routes.groupDetail(it.value)) { popUpTo(com.circle.app.navigation.Routes.CREATE_GROUP) { inclusive = true } } }
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.newchat_create), color = if (canCreate) CircleTheme.colors.onAccent else CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AvatarOrange),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Image, contentDescription = androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_add_group_icon), tint = avatarTextColor(AvatarOrange)) }
            }
            LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_group_name), name, { name = it }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_e_g_aachen_photo_walk))
            LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_category), category, { category = it }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_e_g_photography_fitness_sports))
            LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.createevent_description), description, { description = it }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_what_s_this_group_about), multiline = true)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.circle.app.ui.components.SectionLabel(androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_visibility))
                VisibilityOption(
                    icon = Icons.Outlined.Lock, title = androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_private), subtitle = androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_only_visible_to_members_default),
                    selected = isPrivate
                ) { isPrivate = true }
                VisibilityOption(
                    icon = Icons.Outlined.Public, title = androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_public), subtitle = androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_listed_in_explore_join_requests),
                    selected = !isPrivate
                ) { isPrivate = false }
            }

            Text(
                androidx.compose.ui.res.stringResource(com.circle.app.R.string.creategroup_you_can_switch_this_later),
                color = CircleTheme.colors.muted, fontSize = 11.sp
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
            placeholder = { Text(placeholder, color = CircleTheme.colors.muted, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().let { if (multiline) it.height(96.dp) else it },
            shape = RoundedCornerShape(11.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CircleTheme.colors.surface, unfocusedContainerColor = CircleTheme.colors.surface,
                focusedIndicatorColor = CircleTheme.colors.accent, unfocusedIndicatorColor = CircleTheme.colors.border,
                focusedTextColor = CircleTheme.colors.text, unfocusedTextColor = CircleTheme.colors.text
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
            .background(CircleTheme.colors.surface)
            .border(1.dp, if (selected) CircleTheme.colors.accent else CircleTheme.colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CircleTheme.colors.body, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(subtitle, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (selected) CircleTheme.colors.accent else CircleTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) androidx.compose.foundation.layout.Box(modifier = Modifier.size(9.dp).background(CircleTheme.colors.accent, CircleShape))
        }
    }
}
