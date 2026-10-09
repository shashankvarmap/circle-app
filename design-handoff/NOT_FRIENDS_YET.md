# Circle: people who aren't friends yet

Read with BUILD_BRIEF.md, FRONTEND_COMPLETE.md and MESSAGES_AND_SETTINGS_V2.md. **Reference prototype:**
`reference/prototype-messages-settings-night.dc.html` / `-day.dc.html` (in Priya's chat, tap the second shared note;
then open Groups > Aachen Photo Walk and tap Elena).

## The rule

> **Enforcement design: see MASTER_PROMPT.md section 2.** One `AccessPolicy`, redacted result types (`ProfileView.Limited`, `NoteView.Snippet`, `ThreadResult.Denied`), exhaustive tests. This file describes the screens; that section describes how to make them impossible to get wrong.

**You can always see who someone is and ask to be their friend. You can never see their notes, threads, friends, counts or
anything private until they accept.** A request is never instant. This holds everywhere a person can appear.

## 1. One relationship, decided in one place

Every screen that shows a person or a note must be driven by the **relationship** between the viewer and that person:

`SELF`, `FRIEND`, `REQUEST_SENT`, `REQUEST_RECEIVED`, `NONE`, `BLOCKED`.

Add `relationshipTo(personId)` to the store/repository and make profile, thread, chat-member and card screens read it.
Do not scatter "is friend" checks in composables. (The backend must enforce the same rule server-side; note this in
BACKEND_READINESS.md.)

## 2. The profile of someone who isn't your friend

One Profile screen, rendered by relationship (prototype screen "Profile"):

- **Always shown:** avatar (92dp), name (Fraunces 26), handle and the **PINNED** card with their pinned thought. **Nothing else, and no mutual-friends number anywhere in this version.**
- **NONE:** accent pill **Add friend**, then a dashed locked panel: "You're not friends yet. You'll see their notes and
  friends once they accept. Until then you see only their photo and pinned thought."
- **REQUEST_SENT:** an outlined **Requested** state plus a small **Cancel request** link. Toast "Friend request sent" /
  "Request cancelled".
- **REQUEST_RECEIVED:** **Accept** and **Decline** buttons in place of Add friend.
- **FRIEND:** **Message** (accent pill), "✓ Friends", their notes visible to you, and a ⋯ menu with *Mute*, *Unfriend*, *Block*, *Report*.
- **Not shown to non-friends, ever:** their notes, friends list, counts, groups, inner circle, activity.
- **No Message button unless you are friends.** Only friends can message each other.
- **BLOCKED:** the person doesn't appear anywhere; there is no profile to open.
- The ⋯ menu on a non-friend offers *Report* and *Block* only.

## 3. Every place a person can be tapped must open this profile

Avatar **and** name are tappable (48dp minimum target) in all of these, and open the profile by relationship:

- Group **People** tab: members, hosts, and join-request circles.
- Group posts and group comment threads.
- Event table seats, and event threads.
- Search results (People).
- Alerts rows and friend-request rows.
- Comments in any thread (people who commented on a friend's note may not be your friends).
- The author line of a shared note card in chat.
- **Group chats and chats you create with several friends:** members may not be friends with each other. In the chat the
  first message of each run shows a 26dp avatar and the sender's first name; both open the profile. Priya (friend)
  opens a friend profile; Elena (not a friend) opens the not-friends profile.
- On orbit screens that also select (Your circle), tapping a **non-friend** opens their profile instead of selecting.

Direct messages stay friends-only. "New chat" lists friends only; a group's chat is created by the group, and a
multi-person chat you start contains only your friends (they may not know each other).

## 4. A shared note, when the viewer isn't friends with its author

Prototype: the second shared note in Priya's chat (Elena Voss).

- The card still shows in the chat: caption "SHARED NOTE", the author's name, and the note text **truncated to about 90
  characters with an ellipsis**. Photos in the card are a blurred placeholder. Instead of "Open the thread" it shows a small
  lock and **"Not friends yet"** (muted).
- **Tapping the card does not open the thread.** It opens a bottom sheet (22dp top radius, drag handle): a 52dp circle with a
  lock in accent at 10%, the title **"You're not friends with Elena yet"** (Fraunces 21), the line "Send a friend request to
  open this thread. You can still see who posted it.", and three actions: **Send friend request** (accent pill; sends and
  closes with the toast), **View Elena's profile** (outlined; opens the not-friends profile), and **Not now**.
- Tapping the author's **name** on the card also opens the profile.
- Swipe-to-reply and hold-for-menu still work on the card. *Copy* copies only the visible snippet.
- If the viewer **is** friends with the author, tapping the card opens the thread as normal.
- When the request is accepted, the same card starts opening the thread without any change to the message.

### Privacy rules for sharing (enforce them in the store now, on the server later)

1. **Inner-circle notes can't be shared at all** outside the inner circle, and never to a non-friend of the author.
2. **Group posts can't be shared outside their group.**
3. A shared card reveals only the truncated snippet, never photos, replies or the author's other notes.
4. **Recommended (the owner decides):** a Settings > Privacy switch, **"Let friends share my notes in messages"**, on by
   default. When off, the "Send in a message" action is hidden on that author's notes.
5. A deleted or edited note updates or removes its snippet in every chat.

This design shows an author's words to someone who isn't their friend, one at a time, only when a friend chooses to share.
That is accepted for now; the rules above limit it. Raise it with the owner if anything here feels too open.

## 5. Events and groups shared into chats

- **Event card:** opens the event if its visibility allows the viewer (friends of friends / open nearby). If not, show the
  same gate style: "This event is for the host's friends" with **Send friend request to the host** / **View profile** / **Not now**.
- **Public group card:** opens the group (with Request to join). **Private group:** opens an "Invite only" state, never the posts.

## 6. Data and tests

- The store/repository exposes `relationshipTo(id)`; a profile, a shared-note tap, a group member tap and a search result
  tap all branch on it.
- Add the person "Elena Voss" (id `EV`, not a friend) to the seed, one shared note from her in Priya's chat,
  and a message from her in the Aachen Photo Walk chat, so the owner can click through it.
- Unit tests: sending a request moves NONE to REQUEST_SENT; cancelling returns it to NONE; accepting makes FRIEND and opens
  previously gated notes; blocking removes the person from group lists and chats on the viewer's side; an inner-circle note
  cannot be shared to a non-inner-circle friend or a non-friend.

## 7. Done when

1. Tapping Elena's avatar or name in the group chat opens her not-friends profile with **Add friend**; sending it shows
   **Requested** and a toast; **Cancel request** undoes it. Priya opens a friend profile with **Message**.
2. In Priya's chat, tapping Amara's shared note opens the thread; tapping Elena's shared note opens the "not friends yet"
   sheet instead, and **Send friend request** and **View profile** both work.
3. The group People tab, event seats and search results open the same profile screens.
4. A non-friend profile never shows notes, friends or counts, and has no Message button.
5. Build, tests and lint pass; commit; report.
