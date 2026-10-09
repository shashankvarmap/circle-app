# Circle: privacy rules (the server must implement exactly these)

**The rule in one sentence:** someone who is not your friend can see your photo, name, handle and pinned thought, and
nothing else: not your notes, not your threads, not your comments, not your friends, not your counts.

The app enforces this in one place (`data/access/AccessPolicy.kt`, tested cell by cell in `AccessPolicyTableTest`) and
only ever receives redacted results from its repository. **The app's checks are for the experience. The server's checks
are for security.** A modified client must still never receive anything this table denies, so the server applies the same
table to every response and every action, and the app is just a second, friendlier copy of it.

## 1. One relationship decides everything

For every request the server first computes how the viewer relates to the person concerned (the author of a note, the
owner of a profile, the host of an event):

| Relationship | Meaning |
|---|---|
| SELF | it's you |
| FRIEND | you both said yes |
| REQUEST_SENT | you asked; they haven't answered |
| REQUEST_RECEIVED | they asked; you haven't answered |
| NONE | none of the above |
| BLOCKED_BY_ME | you blocked them |
| BLOCKED_ME | they blocked you |

**Deny by default:** anything not listed as allowed below is refused.

## 2. The table

| What | You (SELF) | A friend | Requested either way, or nobody (NONE) | Blocked, either way |
|---|---|---|---|---|
| Profile | everything | full profile | **limited**: photo, name, handle, pinned thought only | doesn't exist |
| Their notes | yes | yes, by audience (see 3) | **never** | never |
| Open a thread, read its comments | yes | yes, if you can see the note | **never** (the app shows the "not friends yet" gate) | never |
| Comment, like, repost, quote | yes | yes, if you can see the note (no repost or quote of inner-circle notes) | **never** | never |
| Their friends list, friend count, inner circle, groups, activity | yours only | **never** | **never** | never |
| Direct message | n/a | yes | **never** (no Message button) | never |
| A note shared into a chat | open | open | **snippet only**: author name and at most 90 characters, a lock, "Not friends yet"; no photos, no replies, no other notes | hidden |
| A person tapped anywhere (group member, event seat, search result, comment author, chat member) | your profile | their full profile | **limited profile** with Add friend | not found |
| Group posts and their comments | members only | members only | members only | never |
| An event's thread | people with a seat (and the host) | same | same | never |

Friendship never opens a group's posts or an event's thread on its own: only membership or a seat does.

## 3. Notes in detail

- **Audience.** A note is for *Friends* or for the author's *Inner circle*. An inner-circle note is visible only to friends
  who are in the author's inner circle. The viewer is never told who else is in it.
- **Only friends of the author** (or group members, for group posts) can comment on or like a note.
- **Inner-circle notes** can't be reposted or quoted at all, and can be shared into a chat only by their author and only when
  every recipient is in the author's inner circle.
- **Group posts** can't be reposted or quoted, and can be shared only into that group's own chat.
- **Likes are private:** only the author learns who liked their note. No counts anywhere.
- **A shared note** shows its full text only to people who could open it anyway; everyone else gets the snippet (at most 90
  characters, cut on the server, never the full text "hidden" by the client). Deleting or editing a note updates or removes its
  snippet everywhere.
- **Alerts** carry a comment's words only to someone allowed to read that thread. Activity never says who is friends with whom
  or who liked what.

## 4. People and blocks

- **Friend requests are never instant.** Only the recipient's Accept makes a friendship. An invite link never makes a friend.
- **Search and contacts** return only the limited fields (photo, name, handle) and the request state.
- **Blocking** ends any friendship or pending request, and the two people disappear from each other everywhere: feeds,
  search, group lists, chats, event seats, comments. A seat held by someone you blocked shows only as "taken".
- **Direct messages** are between friends only. Group chats and chats someone starts with several friends may contain
  people who aren't friends with each other: they see each other's name and photo, tapping opens the limited profile, and no
  direct message is possible until they are friends.

## 5. Changes while someone is looking

When a relationship changes (unfriend, block, request accepted), every response changes with it:
- after unfriend or block, the client must purge everything cached by that person (notes, threads, snippets) and any open
  thread closes to the gate; the server must stop returning them immediately (no grace period, no stale cache);
- after a request is accepted, gated cards and threads open with no other change.

## 6. Never in logs

Note text, comment text and message text are never written to logs, crash reports, analytics or notification payloads for
people who aren't allowed to see them. (The app has no logging at all; `PrivacyBoundaryTest` fails the build if any appears.)
