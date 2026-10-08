package com.circle.app.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import com.circle.app.ui.a11y.rememberReduceMotion

/**
 * State for a header that moves away as content scrolls down and comes back straight away on
 * any scroll up ("enter always"). [offset] runs from 0 (shown) to -height (hidden). The offset is read only in
 * layout and draw, so scrolling never recomposes the screen.
 */
@Stable
class CollapsingHeaderState(initialOffset: Float = 0f) {
    var heightPx by mutableFloatStateOf(0f)
        internal set
    var offset by mutableFloatStateOf(initialOffset)
        private set

    /** 0 = fully shown, 1 = fully hidden. */
    val collapsedFraction: Float get() = if (heightPx > 0f) (-offset / heightPx).coerceIn(0f, 1f) else 0f

    internal var reduceMotion = false
    internal var scope: CoroutineScope? = null
    /** The header only moves when the content can actually scroll (a short list leaves it alone). */
    internal var canScroll: () -> Boolean = { true }
    private var revealJob: Job? = null

    internal fun setOffsetClamped(value: Float) {
        offset = value.coerceIn(-heightPx, 0f)
    }

    /** Slides the header fully back into view (e.g. after tapping the wordmark). */
    suspend fun reveal() = animateTo(0f)

    /** If the user let go part-way, snap to fully shown or fully hidden. */
    internal suspend fun settle() {
        val f = collapsedFraction
        if (f > 0f && f < 1f) animateTo(if (f < 0.5f) 0f else -heightPx)
    }

    private suspend fun animateTo(target: Float) {
        if (reduceMotion) { setOffsetClamped(target); return }
        animate(offset, target, animationSpec = tween(180)) { v, _ -> setOffsetClamped(v) }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (heightPx == 0f || available.y == 0f || (!canScroll() && offset == 0f)) return Offset.Zero
            if (available.y > 0f) {
                // Scrolling up by any amount brings the header straight back; the list scrolls as usual.
                if (offset < 0f && revealJob?.isActive != true) revealJob = scope?.launch { reveal() }
                return Offset.Zero
            }
            // Scrolling down: the header follows the finger out, then the list scrolls.
            revealJob?.cancel()
            val before = offset
            setOffsetClamped(offset + available.y)
            return Offset(0f, offset - before)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    companion object {
        val Saver: Saver<CollapsingHeaderState, Float> = Saver(save = { it.offset }, restore = { CollapsingHeaderState(it) })
    }
}

@Composable
fun rememberCollapsingHeaderState(canScroll: () -> Boolean = { true }): CollapsingHeaderState {
    val state = rememberSaveable(saver = CollapsingHeaderState.Saver) { CollapsingHeaderState() }
    state.reduceMotion = rememberReduceMotion()
    state.scope = rememberCoroutineScope()
    state.canScroll = canScroll
    return state
}

/**
 * Lays out [header] above [content]. As the content scrolls down the header slides up and fades
 * out (tied to the scroll distance) and the content moves up into the freed space.
 */
@Composable
fun CollapsingHeaderLayout(
    state: CollapsingHeaderState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Layout(
        contents = listOf(
            {
                androidx.compose.foundation.layout.Box(
                    Modifier.graphicsLayer { alpha = 1f - state.collapsedFraction }
                ) { header() }
            },
            content
        ),
        modifier = modifier.nestedScroll(state.nestedScrollConnection)
    ) { (headerMeasurables, contentMeasurables), constraints ->
        val loose = constraints.copy(minHeight = 0)
        val headerPlaceables = headerMeasurables.map { it.measure(loose) }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        state.heightPx = headerHeight.toFloat()
        if (state.offset < -headerHeight) state.setOffsetClamped(state.offset)

        val shown = (headerHeight + state.offset).toInt().coerceIn(0, headerHeight)
        val contentHeight = (constraints.maxHeight - shown).coerceAtLeast(0)
        val contentPlaceables = contentMeasurables.map {
            it.measure(Constraints(constraints.minWidth, constraints.maxWidth, contentHeight, contentHeight))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceables.forEach { it.placeRelative(0, shown - headerHeight) }
            contentPlaceables.forEach { it.placeRelative(0, shown) }
        }
    }
}
