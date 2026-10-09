package com.circle.app.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** A screen-reader label. The text is resolved by the caller (from string resources), outside the semantics lambda. */
fun Modifier.label(text: String): Modifier = semantics { contentDescription = text }
