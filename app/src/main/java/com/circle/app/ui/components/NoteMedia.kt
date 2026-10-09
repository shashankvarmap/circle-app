package com.circle.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.ui.theme.CircleTheme

/**
 * A note's photos and videos. Seed photo posts have no files and show a calm placeholder; picked
 * media shows thumbnails (up to three, then "+N"). Tapping opens the full-screen viewer.
 */
@Composable
fun NoteMedia(media: List<MediaRef>, modifier: Modifier = Modifier, height: Int = 180, onOpen: ((Int) -> Unit)?) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(11.dp)
    val open = stringResource(R.string.media_open)
    if (media.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(height.dp).clip(shape).background(colors.track)
                .then(if (onOpen != null) Modifier.clickable(role = Role.Image, onClickLabel = open) { onOpen(0) } else Modifier),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Image, contentDescription = stringResource(R.string.note_photo), tint = colors.muted) }
        return
    }
    Row(modifier.fillMaxWidth().height(height.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        media.take(3).forEachIndexed { i, m ->
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(shape).background(colors.track)
                    .then(if (onOpen != null) Modifier.clickable(role = Role.Image, onClickLabel = open) { onOpen(i) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                MediaThumb(m)
                if (i == 2 && media.size > 3) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        Text("+${media.size - 3}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/** A photo's thumbnail, or a video's frame with a play mark. */
@Composable
fun MediaThumb(m: MediaRef, modifier: Modifier = Modifier) {
    val bitmap = rememberUriBitmap(m.uri, 480)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Outlined.Image, contentDescription = null, tint = CircleTheme.colors.muted)
        if (m.kind == MediaKind.VIDEO) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.media_video), tint = Color.White)
            }
        }
    }
}
