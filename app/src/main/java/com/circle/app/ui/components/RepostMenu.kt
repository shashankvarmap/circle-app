package com.circle.app.ui.components

import com.circle.app.ui.a11y.label
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.cardShadow
import kotlinx.coroutines.delay

/** One row in the repost popover. A null [onClick] is a disabled row that explains why. */
/** The menu's three labels, resolved from resources by the caller. */
data class RepostLabels(val repost: String, val undo: String, val quote: String)

data class RepostMenuItem(val label: String, val icon: ImageVector, val onClick: (() -> Unit)?)

private val RowHeight = 48.dp
private val CardPadding = 8.dp
private val Gap = 6.dp
private val Margin = 8.dp

/**
 * The repost icon and its popover. Opens on tap or long-press, from the icon: the card
 * springs from 0.85 to 1 with a fade, rows cascade in 30ms apart, a light scrim covers the
 * rest of the screen (tap it to close), the icon turns accent and rotates 180°. Closing is a
 * quick fade and shrink. With "remove animations" on, it just appears.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepostButton(reposted: Boolean, items: List<RepostMenuItem>, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val reduceMotion = rememberReduceMotion()
    val view = LocalView.current
    var anchor by remember { mutableStateOf(Rect.Zero) }
    val visible = remember { MutableTransitionState(false) }
    val open = visible.targetState
    val showPopup = visible.currentState || visible.targetState

    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "repostRotation"
    )

    fun openMenu() {
        if (open) return
        visible.targetState = true
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .onGloballyPositioned { anchor = it.boundsInRoot() } // the popup shares the content root's origin (below the status bar)
            .combinedClickable(onClick = ::openMenu, onLongClick = ::openMenu, role = Role.Button)
            .then(Modifier.label(if (reposted) androidx.compose.ui.res.stringResource(com.circle.app.R.string.repost_menu_reposted) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.repost_menu_label))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Repeat, contentDescription = null,
            tint = if (open || reposted) colors.accent else colors.muted,
            modifier = Modifier.size(18.dp).rotate(rotation)
        )
    }

    if (showPopup) {
        RepostPopover(anchor, items, visible, reduceMotion, onDismiss = { visible.targetState = false })
    }
}

@Composable
private fun RepostPopover(
    anchor: Rect,
    items: List<RepostMenuItem>,
    visible: MutableTransitionState<Boolean>,
    reduceMotion: Boolean,
    onDismiss: () -> Unit
) {
    val colors = CircleTheme.colors
    val density = LocalDensity.current
    // A fixed card width lets the popover grow from the icon's exact position.
    val cardWidth: Dp = if (items.any { it.onClick == null }) 290.dp else 200.dp
    val cardHeight: Dp = RowHeight * items.size + CardPadding * 2

    Popup(
        popupPositionProvider = FullWindow,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val windowW = maxWidth
            val windowH = maxHeight
            val (iconX, iconTop, iconBottom) = with(density) {
                Triple(anchor.center.x.toDp(), anchor.top.toDp(), anchor.bottom.toDp())
            }
            val left = (iconX - 28.dp).coerceIn(Margin, (windowW - cardWidth - Margin).coerceAtLeast(Margin))
            val below = iconBottom + Gap + cardHeight <= windowH - Margin
            val top = if (below) iconBottom + Gap else (iconTop - Gap - cardHeight).coerceAtLeast(Margin)
            val origin = TransformOrigin(
                pivotFractionX = ((iconX - left) / cardWidth).coerceIn(0f, 1f),
                pivotFractionY = if (below) 0f else 1f
            )

            // Scrim: about 8% of the text color. Tapping it closes the menu.
            AnimatedVisibility(
                visibleState = visible,
                enter = if (reduceMotion) EnterTransition.None else fadeIn(tween(160)),
                exit = if (reduceMotion) ExitTransition.None else fadeOut(tween(120))
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .background(colors.text.copy(alpha = 0.08f))
                        .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                )
            }

            AnimatedVisibility(
                visibleState = visible,
                modifier = Modifier.offset(x = left, y = top),
                enter = if (reduceMotion) EnterTransition.None else
                    scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), initialScale = 0.85f, transformOrigin = origin) +
                        fadeIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)),
                exit = if (reduceMotion) ExitTransition.None else
                    scaleOut(tween(120), targetScale = 0.85f, transformOrigin = origin) + fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .cardShadow(colors, 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(CardPadding)
                ) {
                    items.forEachIndexed { i, item ->
                        MenuRow(item, index = i, animate = !reduceMotion, onDone = onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: RepostMenuItem, index: Int, animate: Boolean, onDone: () -> Unit) {
    val colors = CircleTheme.colors
    // Rows cascade in 30ms apart.
    var shown by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { if (animate) { delay(30L * index); shown = true } }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, if (animate) tween(140) else snap(), label = "rowAlpha")
    val shift by animateFloatAsState(if (shown) 0f else 6f, if (animate) spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium) else snap(), label = "rowShift")

    val enabled = item.onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .offset(y = shift.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (enabled) Modifier.clickable(role = Role.Button) { item.onClick?.invoke(); onDone() }
                else Modifier.semantics { disabled() }
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = if (enabled) colors.text else colors.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(item.label, color = if (enabled) colors.text else colors.muted, fontSize = 14.sp)
    }
}

/** Places the popup over the whole window so the scrim can cover everything. */
private object FullWindow : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
        IntOffset.Zero
}

/** The standard repost menu for a note. */
fun repostMenuItems(
    blockReason: String?,
    reposted: Boolean,
    onRepost: () -> Unit,
    onUndo: () -> Unit,
    onQuote: () -> Unit,
    labels: RepostLabels
): List<RepostMenuItem> = when {
    blockReason != null -> listOf(RepostMenuItem(blockReason, Icons.Outlined.Lock, null))
    reposted -> listOf(RepostMenuItem(labels.undo, Icons.AutoMirrored.Outlined.Undo, onUndo))
    else -> listOf(
        RepostMenuItem(labels.repost, Icons.Outlined.Repeat, onRepost),
        RepostMenuItem(labels.quote, Icons.Outlined.FormatQuote, onQuote)
    )
}
