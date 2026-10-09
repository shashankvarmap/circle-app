package com.circle.app.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.NoteView
import com.circle.app.ui.components.rememberUriBitmap
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

class ViewerViewModel(repo: CircleRepository, noteId: String) : RepoViewModel(repo) {
    /** The note, through the same access rules as everywhere else. */
    val note = repo.note(noteId).state(null)
}

/**
 * Full-screen viewer for a note's photos and videos: pinch to zoom, double-tap to reset, swipe
 * between items, close button. Only notes you may see in full can be opened here.
 */
@Composable
fun PhotoViewerScreen(nav: NavHostController, noteId: String, index: Int) {
    val vm = circleViewModel(key = "viewer-$noteId") { ViewerViewModel(it, noteId) }
    val view by vm.note.collectAsStateWithLifecycle()
    when (val v = view) {
        is NoteView.Full -> MediaViewer(v.note.media, index) { nav.popBackStack() }
        null -> MediaViewer(null, index) { nav.popBackStack() }
        else -> MediaViewer(null, index, unavailable = true) { nav.popBackStack() }
    }
}

/**
 * The viewer itself. [media] null = loading (or [unavailable]); empty = a seed photo post with no
 * file, shown as the calm placeholder.
 */
@Composable
fun MediaViewer(media: List<MediaRef>?, index: Int, unavailable: Boolean = false, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            unavailable -> Text(stringResource(R.string.note_unavailable), color = Color.White, modifier = Modifier.align(Alignment.Center))
            media == null -> Unit
            media.isEmpty() -> ZoomBox { Icon(Icons.Outlined.Image, contentDescription = stringResource(R.string.note_photo), tint = Color.Gray, modifier = Modifier.size(64.dp)) }
            else -> {
                val pager = rememberPagerState(initialPage = index.coerceIn(0, media.lastIndex)) { media.size }
                HorizontalPager(pager, Modifier.fillMaxSize()) { page -> MediaPage(media[page]) }
                if (media.size > 1) {
                    Text(
                        "${pager.currentPage + 1} / ${media.size}", color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                    )
                }
            }
        }
        Box(
            Modifier.statusBarsPadding().padding(8.dp).size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.media_close), tint = Color.White) }
    }
}

@Composable
private fun MediaPage(m: MediaRef) {
    if (m.kind == MediaKind.VIDEO) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                    setVideoURI(Uri.parse(m.uri))
                    setOnPreparedListener { start() }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    val bitmap = rememberUriBitmap(m.uri, 2048)
    ZoomBox {
        if (bitmap != null) Image(bitmap, contentDescription = stringResource(R.string.note_photo), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}

/** Pinch to zoom and pan; double-tap resets. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ZoomBox(content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero }) }
            .transformable(state, canPan = { scale > 1f })
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
        contentAlignment = Alignment.Center
    ) { content() }
}
