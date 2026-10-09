package com.circle.app.data.repo

/**
 * The client's memory of other people's content it has shown (opened threads, notes, snippets),
 * keyed by author. In the real app this is the local cache of server responses; in the fake it
 * records what was handed to the UI. On unfriend or block, everything by that person is purged
 * (MASTER_PROMPT 2.3) and the observing screens re-evaluate to the gate.
 */
class ContentCache {
    private val byAuthor = mutableMapOf<String, MutableSet<String>>()

    @Synchronized fun remember(authorId: String, key: String) {
        byAuthor.getOrPut(authorId) { mutableSetOf() }.add(key)
    }

    @Synchronized fun forget(authorId: String, key: String) {
        byAuthor[authorId]?.remove(key)
    }

    @Synchronized fun purgeAuthor(authorId: String) {
        byAuthor.remove(authorId)
    }

    @Synchronized fun holdsAnythingBy(authorId: String): Boolean = byAuthor[authorId].orEmpty().isNotEmpty()

    @Synchronized fun keysFor(authorId: String): Set<String> = byAuthor[authorId].orEmpty().toSet()

    @Synchronized fun clear() = byAuthor.clear()
}
