# Circle: backend readiness

Started early so rules land here as they are built. Completed at the "front end complete" gate
(FRONTEND_COMPLETE.md section 9), which adds entities, operations, push, media and moderation.

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
