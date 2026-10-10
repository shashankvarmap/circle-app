package com.circle.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.data.access.DenyReason

/** The plain-language line for a refusal, shown where an action is disabled. */
@Composable
fun denyText(reason: DenyReason): String = stringResource(
    when (reason) {
        DenyReason.NOT_FRIENDS -> R.string.deny_not_friends
        DenyReason.BLOCKED, DenyReason.NOT_FOUND -> R.string.deny_not_available
        DenyReason.AUDIENCE -> R.string.deny_audience
        DenyReason.GROUP_NOT_MEMBER -> R.string.deny_group_not_member
        DenyReason.EVENT_LOCKED -> R.string.deny_event_locked
        DenyReason.INNER_CIRCLE -> R.string.deny_inner_circle
        DenyReason.GROUP_POST -> R.string.deny_group_post
        DenyReason.ENDED -> R.string.deny_ended
        DenyReason.NOT_HOST -> R.string.deny_not_host
        DenyReason.NOT_TEMPORARY -> R.string.deny_not_available
    }
)
