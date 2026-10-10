package com.circle.app.ui.vm

import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome

/**
 * Small actions shared by cards and buttons that appear on many screens (like, repost, friend
 * requests, join requests). Each goes through the repository, which applies the access rules;
 * [onResult] lets the caller show a toast for the outcome.
 */
class ActionsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    fun toggleLike(noteId: String) = run({ toggleLike(noteId) })
    fun repost(noteId: String) = run({ repost(noteId) })
    fun undoRepost(noteId: String) = run({ undoRepost(noteId) })

    /** Your relationship to someone, live (only yours: never who they are friends with). */
    fun relationship(personId: String) = repo.relationshipTo(personId)
    fun sendFriendRequest(personId: String, onResult: (Outcome<Unit>) -> Unit = {}) = run({ sendFriendRequest(personId) }, onResult)
    fun cancelFriendRequest(personId: String, onResult: (Outcome<Unit>) -> Unit = {}) = run({ cancelFriendRequest(personId) }, onResult)
    fun acceptRequest(personId: String, onResult: (Outcome<Unit>) -> Unit = {}) = run({ acceptRequest(personId) }, onResult)
    fun declineRequest(personId: String, onResult: (Outcome<Unit>) -> Unit = {}) = run({ declineRequest(personId) }, onResult)

    fun requestToJoin(groupId: String) = run({ requestToJoin(groupId) })
}
