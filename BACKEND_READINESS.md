# Circle: backend readiness

What a backend must provide so `FakeCircleRepository` can be replaced by a real implementation of
`data/repo/CircleRepository.kt` without changing any screen. The interface is the contract: every Flow becomes a
subscription (or a fetch plus push updates), every suspend action becomes one API call returning
`Success` / `Denied(reason)` / `Error`. Completed in Phase 12 (2026-10-10).

Contents: privacy rules (first, because they gate everything) · entities · operations · notifications · media ·
moderation · data export and deletion · sessions and sign-in · what the app assumed.

## Privacy rules the server must enforce (not just the app)

**The full table is in `PRIVACY_RULES.md`. The server must implement exactly that table.** The app's checks are for the
experience; the server's checks are for security: a modified client must still never receive private data.

### One policy, redacted responses

The app decides every access question in one pure function set, `data/access/AccessPolicy.kt` (deny by default, tested cell
by cell in `AccessPolicyTableTest`), from one relationship value: `SELF`, `FRIEND`, `REQUEST_SENT`, `REQUEST_RECEIVED`, `NONE`,
`BLOCKED_BY_ME`, `BLOCKED_ME`. The repository (`data/repo/CircleRepository`) is the only data access and returns
**redacted types**, which is the shape the API should return too:

- `ProfileView`: `Full` (you, friends), `Limited` (photo, name, handle, pinned thought, and no field for anything else) or
  `NotFound` (blocked either way). The limited profile endpoint must not even load the other fields.
- `NoteView`: `Full`, `Snippet` (author name + at most 90 characters, cut on the server, `canOpen = false`) or `Hidden`.
- `ThreadResult`: `Opened(thread)` or `Denied(NOT_FRIENDS | BLOCKED | AUDIENCE | GROUP_NOT_MEMBER | EVENT_LOCKED)`.
  Opening a thread, loading comments, commenting, liking, reposting, quoting and sharing all return `Denied` for non-friends.
- There is **no mutual-friends number** anywhere, and no endpoint returns another person's friends, counts, groups or
  inner circle.
- Friend requests are never auto-accepted. Accepting is the recipient's action only. Blocking ends any friendship or pending
  request and hides each person from the other everywhere.
- On unfriend or block the client purges cached content by that person; the server must stop serving it at once.
- Note, comment and message text never go into logs, crash reports, analytics or notification payloads for people who can't
  see them.

### Sharing a note into a chat

A shared card exposes an author's words to chat members who may not be their friends. The server
must enforce, on send and on read:

1. Inner-circle notes can only be shared by their author, and only into chats whose members are all
   in the author's inner circle.
2. Group posts can only be shared into that group's own chat.
3. A recipient who can't open the note (relationship to the author is not FRIEND/SELF) receives only
   the author's public profile fields and a ~90 character snippet, never photos, replies or other
   notes. The full note and thread are returned only once they are friends.
4. Deleting or editing a note updates or removes its snippet in every chat.
5. Open question for the owner: a per-author setting "Let friends share my notes in messages"
   (recommended, on by default) that hides "Send in a message" on that author's notes.

### Events shared into chats

Friends-only events open only for the host's friends. Friends-of-friends and open-nearby events
open for any viewer who can see the card.


---

## Entities

All ids are opaque strings (the fake uses UUIDs for new records). All times are UTC instants; the app formats them in the
viewer's zone and locale. Field names follow `data/Models.kt`, `data/Chats.kt`, `data/Settings.kt`.

| Entity | Fields | Notes |
|---|---|---|
| User (account) | id, phoneE164, displayName (≤ 30 at sign-up, ≤ 40 in edit), handle (ASCII `a-z0-9._`, 3-20, unique), pinnedThought (≤ 140), avatar media id, createdAt | Birth date is **not** stored (at most an "is 18+" fact) |
| Settings | theme; privacy {findableByName, findableFromContacts, requestsFrom EVERYONE / FRIENDS_OF_FRIENDS, invitesFrom FRIENDS / FRIENDS_OF_FRIENDS, readReceipts}; notifications {likes, comments, friendRequests, events, groups, messages, pause OFF / ONE_HOUR / UNTIL_TOMORROW, quietHours, quietStart, quietEnd (minutes)} | Theme may stay local |
| Friendship | userA, userB, since | Symmetric. No endpoint returns someone else's list or count |
| FriendRequest | from, to, createdAt, status PENDING / ACCEPTED / DECLINED | A decline is silent: the sender keeps seeing "Requested" |
| InnerCircle | owner, memberIds | Private to the owner; used only to decide INNER_CIRCLE audiences |
| Block / Mute | owner, targetId | Block is mutual invisibility; mute only hides from the owner's Home |
| Note | id, authorId, createdAt, text (soft limit 500), media[], audience FRIENDS / INNER_CIRCLE, quotedNoteId?, repostOfId?, groupId? | A repost is a note with repostOfId and no text |
| Comment | id, noteId or eventId, authorId, createdAt, text, parentCommentId? | One nested level: a reply to a reply joins the top comment |
| Like | userId, noteId or commentId | Seen by the liker and the author only. No counts anywhere |
| Group | id, name, category, topic, description, color, isPublic, map position, hostIds, memberIds, joinRequests[{personId, at}] | Joining is always a request a host approves; requests visible to all members |
| Event | id, title, topic, startsAt, venue, visibility FRIENDS / FRIENDS_OF_FRIENDS / OPEN_NEARBY, hostId, description, capacity, seats | Thread unlocks for seated people and the host |
| Chat | id, memberIds, name?, groupId?, per member {pinned, muted, lastReadAt, hiddenForMe, clearedAt} | 1:1 only between friends; group chats follow group membership |
| Message | id, chatId, senderId, createdAt, kind TEXT / PHOTO / SHARED_NOTE / EVENT_INVITE, text, noteId?, eventId?, replyToId?, media[], seenAt? | "Delete for me" is per member; "Unsend" removes it for everyone |
| Alert | id, recipientId, actorId, kind, createdAt, noteId?, aboutId?, quote? | Made on the server, filtered by the recipient's switches and the privacy rules |
| Report | reporterId, kind PERSON / NOTE / COMMENT / GROUP / EVENT / MESSAGE, targetId, reason, note, createdAt, status | See Moderation |
| Invite | fromId, toPersonId, groupId?, eventId?, createdAt | Never creates a friendship or membership by itself |
| ExportRequest | userId, step REQUESTED / READY / DOWNLOADED, timestamps | Shown to the user as an audit log |
| Media | id, ownerId, kind PHOTO / VIDEO, durationMs?, signed short-lived URL | Video ≤ 30 s |

## Operations (candidate API calls)

One call per repository method. Every call is authenticated as the signed-in user and checked against the privacy rules on
the server; responses are the redacted views of `data/repo/Views.kt`. "→ Denied" marks calls that can return
`Denied(reason)`; the app shows the matching message.

- **Getting in:** `POST /auth/code {e164}` (rate-limited per number, device and IP) · `POST /auth/verify {e164, code}` → session
  (5 wrong tries lock 5 minutes) · `GET /handles/{h}/available` · `POST /accounts {name, handle, pinned, avatar}` ·
  `POST /contacts/match {sha256[]}` → Limited people (hashes deleted at once) · `GET /invite-link` · `POST /invites/shared` ·
  `GET /people/by-handle/{h}` (App Link).
- **You and settings:** `GET /me` · `PATCH /me` · `GET|PUT /me/privacy` · `GET|PUT /me/notifications` · `DELETE /me` ·
  `GET /me/export` · `POST /me/export`.
- **Your circle (owner only):** `GET /me/circle-summary` · `GET /me/friends` · `PUT|DELETE /me/inner-circle/{id}`.
- **People:** `GET /people/{id}/relationship` · `GET /people/{id}` → Full / Limited / NotFound · `GET /people/search?q=` → Limited ·
  `GET /me/requests` · `POST /people/{id}/request` → Denied · `DELETE /people/{id}/request` · `POST /requests/{id}/accept|decline` ·
  `DELETE /friends/{id}` · `POST|DELETE /blocks/{id}` · `POST|DELETE /mutes/{id}` · `GET /me/blocked` · `GET /me/muted` · `POST /reports`.
- **Notes and threads:** `GET /feed/home` · `GET /feed/groups` · `GET /me/notes` · `GET /people/{id}/notes` → Denied ·
  `GET /notes/{id}` → Full / Snippet / Hidden · `GET /notes/{id}/thread` → Opened / Denied · `POST|DELETE /notes/{id}/like` → Denied ·
  `POST /notes/{id}/comments {text, replyTo?}` → Denied · `POST|DELETE /comments/{id}/like` · `DELETE /comments/{id}` (own) ·
  `POST|DELETE /notes/{id}/repost` → Denied · `POST /notes {text, audience, quotedNoteId?, groupId?, media[]}` → Denied ·
  `PATCH /notes/{id}` (own: text or audience) · `DELETE /notes/{id}` (own) · `GET /notes/{id}/share-candidates` ·
  `POST /notes/{id}/share {friendId | chatId}` → Denied.
- **Messages:** `GET /chats` · `GET /chats/{id}` · `POST /chats {friendIds, name?}` (friends only) · `POST /chats/direct/{personId}` → Denied ·
  `POST /chats/{id}/messages {text, replyTo?, clientId}` · `POST /chats/{id}/media` · `POST /chats/{id}/event-invite` ·
  `PUT /chats/{id} {pinned, muted}` · `DELETE /chats/{id}` (for me) · `POST /chats/{id}/clear` · `DELETE /messages/{id}?scope=me|all` ·
  `POST /chats/{id}/read`.
- **Groups and events:** `GET /explore?city=` · `GET /me/groups` · `GET /groups/{id}` · `GET /groups/{id}/posts` → Denied ·
  `POST /groups/{id}/join-request` · `POST /groups/{id}/leave` · `POST /groups/{id}/requests/{person}/approve|decline` (host) ·
  `PATCH /groups/{id}` (host) · `DELETE /groups/{id}/members/{person}` (host) · `POST /groups/{id}/invites` · `POST /groups` ·
  `GET /events/{id}` · `POST|DELETE /events/{id}/seat` (capacity enforced) · `POST /events/{id}/comments` → Denied (EVENT_LOCKED) ·
  `POST /events` · `PATCH /events/{id}` (host) · `POST /events/{id}/cancel` (host; seated people told) · `POST /events/{id}/invites` ·
  `GET /me/events`.
- **Alerts:** `GET /alerts` · `GET /activity` (friends' activity on notes the viewer may see) · `GET /alerts/unread` · `POST /alerts/read-all`.

Sends (notes, comments, messages) carry a client id so "Not sent · tap to retry" never creates duplicates.

## Notifications (push)

- FCM or equivalent. The app has no push code yet; in-app alerts already follow the six switches, Pause all and quiet hours.
- **The server decides** whether to send: the recipient's switches, pause, quiet hours (in the recipient's zone) and mutes.
- **Payloads carry ids only** (type plus note, chat or person id), never note, comment or message text. The app fetches the
  content through the repository on tap, so the privacy check runs again (a link to something no longer allowed shows the gate).
- Ask for POST_NOTIFICATIONS (Android 13+) at a natural moment (first message or request), not at launch.
- Kinds: request received / accepted, comment on your note, reply to your comment, like (quiet), repost or quote of your note,
  message, join request (hosts), join approved, event changed or cancelled, someone left your event (hosts), invites.

## Media upload

- The app uses the system photo picker (no storage permission); `data/media/MediaStore` is the seam.
- Flow: `POST /media` → signed upload URL → upload → media id attached to the note, message or avatar.
- Limits: video ≤ 30 s (checked on the phone and again on the server); size caps to choose (suggest 50 MB video, 15 MB photo).
- Strip EXIF location before storing; thumbnails made on the server.
- Media URLs are signed, short-lived and issued only to viewers who pass the same check as the note or chat. An avatar is
  part of the Limited profile.

## Moderation

- Reports (kind, target, reason, optional note) are stored with a status (open, actioned, dismissed). The reporter sees
  "Thanks. We'll look at this." and is not told outcomes about other people.
- **Needs the owner:** who reviews reports, how fast (suggest within 24 hours for harassment, self-harm and sexual content),
  removal and appeal rules, and cover across time zones. EU and UK platform-safety laws may apply; the lawyer says which.
- Tools needed: see the reported item with context, remove content, suspend an account, audit trail.
- Blocking is immediate and needs no review.

## Data export and deletion

- **Export:** an automated job covering profile, notes, comments, photos, chats, friends, groups, events and settings. Nobody at
  Circle opens it. Encrypted, available in the app for 7 days or until downloaded, then deleted; each step is added to the
  audit log the user sees (`ExportEvent`).
- **Deletion:** in the app (Your data > type DELETE) **and** on a web page (Play requirement). Removes profile, notes, comments,
  likes, media, memberships and seats; the person's chat messages are removed or shown as from "Deleted account" (owner to
  decide). Friends aren't told why. Backups cleared within the stated window (placeholder 30 days: confirm). The app tells
  members to cancel their Play subscription first.

## Sessions and sign-in

- SMS codes through a provider that handles per-country sender rules (for example India's DLT registration); per-country cost
  budget; rate limits per number, device and IP.
- SMS Retriever autofill needs the app hash in the message (server side).
- Tokens in encrypted storage (Keystore), refresh tokens; Log out revokes the session.
- "Log in" must load the server's account (the fake replaces local state with the demo account).

## What the app assumed (confirm or change)

1. A declined friend request looks the same as a pending one to the sender.
2. Inner-circle notes can be shared only by the author, into chats whose members are all in the author's inner circle.
3. Group posts are visible to members whatever their friendship with the author (blocks still hide).
4. A friend's full profile shows no counts; your own counts come only from `circle-summary`.
5. Read receipts are mutual: you see "Seen" only while your own receipts are on.
6. A comment author on a friend's thread who isn't your friend opens as a Limited profile.
7. Explore's city is a stub ("Aachen" exists only in demo data); the real app needs city search or optional approximate location.
8. **Local cache:** the demo keeps the whole fake server in one app-private JSON file (`StorePersistence`, Phase 11). With a
   backend that file goes away: any local cache may hold only the redacted views this user may see and must purge an
   author's content on unfriend or block, like the in-memory `ContentCache`. Android backup is off (`allowBackup="false"`).
9. Seed events are generated relative to "now" so the demo always has a week of events; real events come from the server.
