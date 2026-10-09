package com.circle.app.ui.format

import androidx.compose.ui.graphics.Color
import com.circle.app.data.repo.EventSummary
import com.circle.app.data.repo.GroupView

/* Display helpers on the repository's view types, so screens read naturally. */

val EventSummary.day: String get() = eventDay(startsAt)
val EventSummary.month: String get() = eventMonth(startsAt)
val EventSummary.whenLabel: String get() = eventWhen(startsAt)
fun EventSummary.dateSpoken(): String = eventDateSpoken(startsAt)

val GroupView.id: String get() = ref.id
val GroupView.name: String get() = ref.name
val GroupView.initial: String get() = ref.initial
val GroupView.color: Color get() = ref.color
