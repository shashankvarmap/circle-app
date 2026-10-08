# Circle: backend readiness

Started early so rules land here as they are built. Completed at the "front end complete" gate
(FRONTEND_COMPLETE.md section 9), which adds entities, operations, push, media and moderation.

## Privacy rules the server must enforce (not just the app)

### Relationship is the single gate for people and their content

The app decides what to show from one function, `CircleStore.relationshipTo(personId)`, returning
`SELF`, `FRIEND`, `REQUEST_SENT`, `REQUEST_RECEIVED`, `NONE` or `BLOCKED`. The server must compute
the same relationship for every request and filter responses by it. A client that skips the check
must still never receive private data.

| Data | SELF / FRIEND | REQUEST_SENT / REQUEST_RECEIVED / NONE | BLOCKED |
|---|---|---|---|
| Photo, name, handle, pinned thought | yes | yes | no (the person doesn't exist for you) |
| Mutual-friend count (a number, never names) | yes | yes | no |
| Notes and their threads | yes (inner-circle notes only for the author's inner circle) | no | no |
| Friends list, counts, groups, inner circle, activity | owner only | no | no |
| Direct messages | friends only | no | no |

- Friend requests are never auto-accepted. Accepting is the recipient's action only.
- Blocking ends any friendship or pending request and hides the blocker from the blocked person
  (and vice versa) in feeds, search, group lists, chats and event seats.

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
