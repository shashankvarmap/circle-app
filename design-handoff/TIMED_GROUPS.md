# Circle: timed groups and timed chats

Read with CLAUDE.md, MASTER_PROMPT.md (privacy core, AccessPolicy), NOT_FRIENDS_YET.md and PRIVACY_RULES.md. **Reference prototype:**
`reference/prototype-timed-groups-night.dc.html` / `-day.dc.html` (open in a browser; "Screens" jumps anywhere; "View as" switches Host/Member; 1px = 1dp).
Work in the small chunks in section 10. Build, test and commit after each. Keep replies short; build with -q.

## 1. The idea, in the owner's words

A Circle **group**, or a **chat you start with friends**, can be **Permanent** or **Temporary**. A temporary one ends on a date chosen at creation (1 to 90 days),
then for **7 days it is read-only** while every member can **save a copy** on their own phone, and after that it is **deleted from the server for everyone**,
whether or not anyone saved a copy. Copies people saved stay on their phones. For things that have an end: a project, a trip, an exam, a wedding.

## 2. Decisions already made (use these; do not ask again)
- Read-only period: **7 days, fixed** in v1. Maximum for one stretch: **90 days**; the host can extend again.
- **Only the host** can extend, make permanent, or end now. In a chat with friends the host is **whoever started it**. No "ask the host" button in v1.
- **Every member** can save a copy. Saving notifies nobody and nobody can see who saved.
- A temporary **group and its group chat are one thing with one clock**; they end and are deleted together.
- A temporary **chat with friends** lives in the **Messages** tab (next to DMs), never in Groups. A group's chat lives in the **Groups** tab.
- A permanent group/chat **cannot** be converted to temporary in v1. A temporary one can become permanent (host only) until it is deleted.
- No joins, invites, posts, comments or messages once the state is Wrap-up or later.

## 3. The state machine (server is the only authority on time; the app only displays)

`Permanent` or `Temporary(endsAt)`. State is computed from the clock:
`ACTIVE` (now < endsAt - 3 days) > `ENDING_SOON` (last 3 days) > `WRAP_UP` (endsAt <= now < endsAt + 7 days, read-only) > `DELETED` (now >= endsAt + 7 days).
- `endsAt` = the **end of the chosen calendar day** in the creator's time zone, stored as UTC. Show dates in the viewer's zone.
- **Extending** moves `endsAt` forward. If it is done during Wrap-up and the new `endsAt` is in the future, the group returns to ACTIVE or ENDING_SOON and the deletion is cancelled. **Make permanent** removes the end completely.
- **End now** sets `endsAt = now` and starts Wrap-up.
- Put this in **one pure Kotlin class** `Lifetime` with an injectable `Clock`, no Android or UI dependencies, and unit-test every boundary: exactly at endsAt, exactly at 3 days before, exactly at deleteAt, extend in each state, make permanent in each state, time zones, DST changes.

## 4. AccessPolicy additions (deny by default; same pattern as the privacy core)
New rules, encoded and tested in the exhaustive table: `canPost`, `canComment`, `canSend`, `canJoin`, `canInvite`, `canRequestToJoin` are **false** when the state is WRAP_UP or DELETED.
`canExport` is true only for **members at the time**, in states ACTIVE to WRAP_UP. `canExtend`, `canMakePermanent`, `canEndNow` only for the host and only before DELETED.
Read access in WRAP_UP follows the normal membership rules. After DELETED every read returns `NotFound`. Join requests still open at the end are auto-declined.

## 5. Creating one
- **Create group** form gets a **LIFETIME** section: two option cards **Permanent** ("Stays until you delete it.") and **Temporary** ("Ends on a date you choose, then closes by itself."). For Temporary: chips **1 day, 3 days, 7 days, 14 days, 30 days, 90 days, Custom** (Custom = a stepper with + and -, clamped 1 to 90). A tinted summary card **WHAT HAPPENS** with three lines: *Today: works like any group*, *<end date>: it ends, read-only for 7 days, everyone can save a copy*, *<deletion date>: deleted for everyone*. Under it: "Everyone is told this before they join, and every member can save a copy." and a link **How timed groups work**. For the group form also show: "A temporary group has its own chat. **The group and its chat end together.**"
- **New chat** (the + in Messages, friends only) gets the same section, without the group-chat line.
- **How timed groups work** screen: the four stages as a vertical timeline (Active; Ending soon; Wrap-up: 7 days, read-only; Deleted) and four short questions (who can save a copy; what if I forget; can it be changed; can I leave early). Placeholder note about reported content (see section 9).
- **Join / accept invite** for a temporary group: a tinted card with the chip "Temporary", "This group ends on <date>.", "Until then it works like any group. Everything in it can be saved by its members. After it ends it's read-only for 7 days, then it's deleted for everyone.", "By joining you agree to this." Buttons **Join group** and **Not now**.

## 6. How it looks everywhere (use one `TimerChip` composable)
- `TimerChip(state, daysLeft)`: a pill with a small **ring that fills as time passes** and a label: "12 days", "3 days" (accent border and tint when ENDING_SOON), "Ended" (ring full, accent). Never colour alone: the label is always there.
- **Messages tab:** temporary chats with friends show the chip under the time; the ending-soon row has a faint accent tint and says "Ends in 3 days. Save a copy."; an ended row says "Ended. Read-only. Deleted in 5 days."
- **Groups tab:** a temporary group's chat has a **dashed accent outline** on its square tile and the chip. Permanent group chats look as before.
- **Explore (map and list):** a temporary group's node is a rounded square with a **dashed accent outline**, accent tint and a **small ring badge** at its top-right corner; permanent groups stay solid colour. A legend under the map: solid = Permanent group, dashed = Temporary group. The preview card kicker reads **TEMPORARY GROUP · <topic>**, with an "Ends in N days" tag and: "A temporary group. Members can save a copy before it closes. It's deleted 7 days after it ends." In the **list view**: a **TEMPORARY** tag next to the name, "Ends in N days" on the right, and a filter chip row **All | Temporary**.
- **Group screen:** the chip "Ends <date>" under the name; a **Group chat** row ("Ends with the group"); a ⋯ menu with **Save a copy**, **Extend or make permanent** (host only), **Leave**.
- **Chat header:** avatar (round for friends chats, rounded square for group chats), name, a subline "8 members · ends Thu 22 Oct" or "3 friends · ends ...", the chip, and ⋯.
- **Ending soon** (last 3 days): an accent-tinted banner "ENDING SOON / Ends in 3 days. / Save a copy to keep what's here. After that it's read-only for 7 days, then deleted." plus a **Save a copy** button; for a group chat also "The group and its chat end together."
- **Wrap-up:** a banner with a lock: "THIS CHAT HAS ENDED / Deleted on <date>. / It's read-only now. Save a copy before it goes. Anything you saved stays on your phone." Buttons: **Save a copy** (everyone) and **Extend or keep it** (host only). The composer is replaced by a locked bar "Read-only. This chat has ended."
- **Deleted:** opening a notification or link for it shows "This chat has ended and was deleted. Everything in it was removed from Circle. If you saved a copy, it's on your phone." and a button back to Messages. It disappears from every list.
- **Extend sheet (host only):** "Keep it going. Only the host can do this. Everyone is told." Buttons **+7 days, +14 days, +30 days**, an accent **Make it permanent**, and a red text button **End it now**. Members are told when it changes (Alerts: "Priya extended Project Alps. It now ends Fri 6 Nov.").

## 7. Reminders and alerts (server sends; the app shows them in Alerts and as notifications)
3 days before the end, 1 day before the end, at the end ("<name> has ended. It's read-only until <date>. Save a copy before it's deleted."), **1 day before deletion** to members who haven't saved a copy. The server does **not** track who saved; the **app remembers locally** that it saved and suppresses that last reminder. When something is extended or made permanent, tell members. All text from string resources and plurals ("1 day", "3 days"); dates via the locale formatter.

## 8. Save a copy (the export each member does for themselves)
- Opens a bottom sheet: "A ZIP file on your phone. Only you get it. Circle doesn't keep a copy of your copy, and nobody sees whether you saved one." Two options: **Everything** (shows a size estimate such as 48 MB) and **Text only** (about 0.5 MB). Description lines: for a **group**: "Posts, comments, N chat messages, N photos and videos, member names" (text only: "Posts, comments and chat messages, no photos or videos"); for a **chat with friends**: "N messages, N photos and videos, member names" (text only: "Messages and member names, no photos or videos"). Under them: "Member names only. No phone numbers."
- Then a **progress** state ("Preparing your copy", a bar and a percentage, "Keep Circle open for a moment"), then **Saved to your phone** with the file name (for example `Circle-Project-Alps-2026-10-22.zip`), where it is (Downloads) and its size, and a **Done** button.
- Implement as a **WorkManager job** (expedited / foreground with a progress notification) so it survives leaving the screen; make it **resumable and cancellable**; write to **Downloads through MediaStore** (no storage permission on Android 10+); handle a full disk, no network and the group being deleted mid-way with clear messages.
- **ZIP contents:** `manifest.json` (app version, group or chat name, kind, dates, counts, export time), a human-readable `index.html` (messages and posts in order, with time stamps and names), the raw `messages.json` (and `posts.json` for groups), and a `media/` folder for Everything. **Member display names only: no phone numbers, no handles beyond what is visible in the group.**
- **Privacy:** only a **current member** can export, and only what that member was allowed to see; **content from people the exporter blocked is omitted**; nothing about who exported is logged where others can see it. The export goes through the repository and `AccessPolicy` like everything else. Never log message or post text. Do not include the file in Android cloud backup.
- A saved copy is the member's own data on their own device from then on; Circle can't recall it.

## 9. Things that need the owner or a lawyer (build them, mark them clearly, keep them easy to change)
- **Reported content.** If a message or post in a temporary group is reported, a short, defined retention for **review** may be legally needed even after deletion. Build a hook so reported items can be held aside by moderation, and write the question in LAUNCH_CHECKLIST.md as "needs lawyer".
- The **7 days**, the **90 day maximum** and the wording "deleted for everyone" must match what the servers really do, including **backup clearing**. Add the claim to `CLAIMS.md` with an owner and the backup window as a placeholder.
- The consent line members see before joining ("Everything in it can be saved by its members") is part of the terms: list it for the lawyer.

## 10. Chunks, in order (each: build -q, tests, commit, update PROGRESS.md)
**T1: the model.** `Lifetime` + `Clock`, the state machine and its tests; the fields on group and chat models; the AccessPolicy additions and tests; fake repository rules (read-only in WRAP_UP, no joins, auto-decline, deletion at deleteAt removing the group, its chat, posts, media, notifications and cached copies from memory and from the saved database).
**T2: a Debug "time travel" control** (Settings > Demo, debug/preview builds only): advance the fake clock by 1, 3 or 7 days or jump to a state, so every stage can be tested without waiting. Seed: a temporary friends chat (12 days left), one ending soon (3 days), one ended (5 days to delete), a temporary group "Project Alps" with its chat, and two temporary public groups for Explore ("Alps Hiking Crew", "Exam Prep Circle").
**T3: creation and display.** The Lifetime section on Create group and New chat, join/accept screen, How it works, `TimerChip`, Messages vs Groups placement, chat header and banners, read-only composer, Explore map/list/filter/card changes, group screen, Alerts rows, the deleted screen, Extend sheet.
**T4: Save a copy.** The sheet, WorkManager job, ZIP writer, MediaStore save, progress and done states, tests of the ZIP contents (names only, blocked users omitted, no phone numbers), failure cases.
**T5: strings, accessibility and tests.** All text in resources with plurals; content descriptions ("Ends in 5 days", "Temporary group"); dashed outline plus a text tag so colour is never the only signal; reduce-motion respected on the ring; 48dp targets; RTL; screenshot tests of `TimerChip` states and the Explore node in both themes; UI tests for each stage using the time-travel control; update `PRIVACY_RULES.md`, `CLAIMS.md`, `BACKEND_READINESS.md` (the server job that moves a group to Wrap-up and then **hard-deletes the group and its chat in one atomic step**, idempotent, driven by server time only, no recovery) and `LAUNCH_CHECKLIST.md`.

## 11. Done when
1. Creating a temporary group and a temporary chat with friends works from both forms, with the dates shown correctly; both appear in the right tab (chat with friends in Messages, group chat in Groups).
2. Using time travel you can watch one go Active > Ending soon > Wrap-up (read-only, composer locked, no joins) > Deleted, and it vanishes from every list.
3. The host can extend, make permanent and end now; a member cannot see those buttons.
4. Save a copy produces a ZIP in Downloads with the right contents at each stage until deletion; names only; blocked users omitted.
5. Explore shows temporary groups with the dashed outline, the ring badge, the TEMPORARY tag and the filter.
6. Tests for the state machine, the access rules, deletion and the export pass; build and lint pass; commit.
