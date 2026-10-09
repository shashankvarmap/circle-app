package com.circle.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.ui.theme.AvatarSlate
import com.circle.app.ui.vm.LocalMe
import com.circle.app.ui.theme.avatarTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Your own avatar: the photo you picked in Edit profile, or your initial on your avatar color.
 * [uri] overrides the saved photo (used by the edit form before saving).
 */
@Composable
fun MeAvatar(
    size: Int,
    modifier: Modifier = Modifier,
    uri: String? = LocalMe.current?.avatarUri,
    fill: Color = LocalMe.current?.person?.color ?: AvatarSlate,
    textColor: Color = avatarTextColor(fill),
    initialOverride: String? = null,
) {
    val initial = initialOverride ?: LocalMe.current?.person?.initials.orEmpty()
    val bitmap = rememberUriBitmap(uri, size * 3)
    Box(
        modifier = modifier.size(size.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp))
        } else {
            Text(initial, color = textColor, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
        }
    }
}

/** Decodes a content URI off the main thread, downsampled to about [targetPx]. Null while loading or on failure. */
@Composable
fun rememberUriBitmap(uri: String?, targetPx: Int): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = uri?.let { withContext(Dispatchers.IO) { decode(context, Uri.parse(it), targetPx) } }
    }
    return bitmap
}

private fun decode(context: Context, uri: Uri, targetPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
}.getOrNull()
