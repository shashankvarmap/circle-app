package com.circle.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

/**
 * One in-memory source of truth for the whole app, so an action on one screen shows up on the others.
 * UNVERIFIED STARTER. Hold a single instance (e.g. in a ViewModel or an application-scoped object).
 * No persistence yet; see BUILD_BRIEF.md "Out of scope".
 */
enum class RequestStatus { PENDING, ACCEPTED, DECLINED }
enum class Audience { FRIENDS, INNER_CIRCLE }
data class Post(val id: String, val authorId: String, val text: String, val whenText: String, val audience: Audience)

class CircleStore {
    val innerCircle = mutableStateListOf<String>().apply { addAll(Seed.initialInnerCircle) }
    val friendRequests = mutableStateMapOf("RS" to RequestStatus.PENDING, "MD" to RequestStatus.PENDING)
    val likes = mutableStateMapOf<String, Boolean>()                 // postId -> liked (private, never shown to others)
    val myPosts = mutableStateListOf<Post>()
    val joinedEvents = mutableStateMapOf<String, Boolean>()          // eventId -> I have a seat
    val memberOfGroup = mutableStateMapOf<String, Boolean>().apply { Seed.groups.forEach { put(it.id, it.memberInitially) } }
    val requestedGroup = mutableStateMapOf<String, Boolean>()        // groupId -> request pending

    val friendCount: Int get() = 128 + friendRequests.values.count { it == RequestStatus.ACCEPTED }
    val innerCount: Int get() = innerCircle.size

    fun isInner(id: String) = innerCircle.contains(id)
    fun toggleInner(id: String) { if (!innerCircle.remove(id)) innerCircle.add(id) }

    fun acceptRequest(id: String) { friendRequests[id] = RequestStatus.ACCEPTED }
    fun declineRequest(id: String) { friendRequests[id] = RequestStatus.DECLINED }

    fun toggleLike(postId: String) { likes[postId] = !(likes[postId] ?: false) }

    fun addPost(text: String, audience: Audience) {
        val t = text.trim(); if (t.isEmpty()) return
        myPosts.add(0, Post("me-${myPosts.size}", "ME", t, "now", audience))
    }

    fun goingCount(e: EventItem) = e.goingBase + if (joinedEvents[e.id] == true) 1 else 0
    fun joinEvent(e: EventItem) { if (goingCount(e) < e.capacity) joinedEvents[e.id] = true }
    fun leaveEvent(e: EventItem) { joinedEvents[e.id] = false }   // the host is quietly notified (later, with a backend)

    /** Public group: you ask, and every member can see that you asked. Never instant. */
    fun requestToJoin(groupId: String) { if (memberOfGroup[groupId] != true) requestedGroup[groupId] = true }
}
