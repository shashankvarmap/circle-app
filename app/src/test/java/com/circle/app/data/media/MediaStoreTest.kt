package com.circle.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreTest {
    private val store = LocalMediaStore()

    @Test fun `photos are kept by uri`() {
        val ref = store.add("content://photo/1", MediaKind.PHOTO).getOrThrow()
        assertEquals("content://photo/1", ref.uri)
        assertEquals(MediaKind.PHOTO, ref.kind)
    }

    @Test fun `videos up to 30 seconds are accepted, longer ones refused`() {
        assertTrue(store.add("content://v/1", MediaKind.VIDEO, 30_000).isSuccess)
        val refused = store.add("content://v/2", MediaKind.VIDEO, 30_001)
        assertTrue(refused.exceptionOrNull() is VideoTooLong)
    }
}
