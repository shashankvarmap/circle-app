package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display

/** Fraunces title with an optional action on the right, and underline tabs below (Messages, Alerts). */
@Composable
fun TabbedScreenHeader(
    title: String,
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                letterSpacing = (-0.48).sp, color = colors.text,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            action?.invoke()
        }
        Box(Modifier.padding(start = 16.dp, bottom = 4.dp)) {
            UnderlineTabs(tabs, selected, onSelect = onSelect)
        }
    }
}
