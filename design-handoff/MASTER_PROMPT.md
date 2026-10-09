# Circle: master build prompt (read this first, then follow it to the end)

You are building the Circle Android app (Kotlin, Jetpack Compose, package `com.circle.app`) in this repo with the owner, Shashank. He wants as much
as possible done in one long, autonomous run, and he cares about two things above all: **(1) the privacy rule in section 2 must be 100% correct,
simple and enforced in one place, and (2) the app must really work, not just look right.**

## 0. How to work

**Read first, in this order, completely:** `design-handoff/BUILD_BRIEF.md`, `FRONTEND_COMPLETE.md`, `SETTINGS_AND_POLISH.md`, `MESSAGES_AND_SETTINGS_V2.md`,
`NOT_FRIENDS_YET.md`, `ACCESS_AND_LAUNCH.md`, `design-tokens.json`, `kotlin/*.kt`, and skim `reference/` (clickable prototypes in HTML; 1px = 1dp).
**If two documents disagree, the later one wins, and this file beats all of them:** this file > ACCESS_AND_LAUNCH > NOT_FRIENDS_YET > MESSAGES_AND_SETTINGS_V2 >
SETTINGS_AND_POLISH > FRONTEND_COMPLETE > BUILD_BRIEF. Items already built in the repo are kept unless a document says to change them.

**Rules of engagement**
1. Work **autonomously through every phase in section 3 in order.** Do not stop to ask permission between phases. Stop and ask the owner only for: a product decision no document
   covers; anything destructive (rewriting git history, deleting data you didn't create); or a build you cannot fix after three honest attempts (then explain, and move on to the next independent item if there is one).
2. **Create `PROGRESS.md` now.** It lists every phase and sub-item as a checkbox with status. Update it after every phase and commit it. **If your context is reset or compacted, the first thing you do is read `PROGRESS.md`
   and continue from the first unchecked item.** Also keep `REPORT.md`: after each phase append what now works, what is still a stub, and any decision you made.
3. After **every** phase: `./gradlew build`, run the unit tests, run lint, fix everything, then commit with a clear message ("Phase 4: Group and Event screens"). The app must **always** build and launch. Never leave the repo broken at a commit.
4. Make decisions yourself when they are small and reversible, and **record each one in `REPORT.md`** under "Decisions". Do not invent features that are not in the documents.
5. Never reintroduce anything on the rejected list in BUILD_BRIEF section 3. Never add circular transitions or gestures. Keep Home an ordinary scroll.
6. Think hard (extended thinking) before and during: the privacy core (section 2), the repository/ViewModel layer, the orbit scaling and anything touching navigation or state. Use subagents only for independent read-only audits (for example a scan for hard-coded strings or for direct store access from UI). Do not run parallel agents that edit the same files.
7. Prefer small, tested, boring code. Pure Kotlin for rules and geometry, so they are unit-testable. No new heavy libraries without a reason written in `REPORT.md`.
8. Before you finish, give the owner the fresh-install commands (section 6) and the click-through list (section 5).

## 1. Product principles (never break)
Friends-only, mutual friendship (request, accept). No ads, no algorithmic ranking, newest first. Likes are private and only the author sees them. Friend list and counts are private to the owner. Inner circle is invisible (no badge, nobody told).
Calm: the feed ends and says so. Requests (friend, group) are never instant. Strangers see only photo and pinned thought. The app is about **connection with friends**; "no ads" is mentioned only where ACCESS_AND_LAUNCH section 0.3 allows.

## 2. THE PRIVACY CORE: build this first, and build it so a screen bug cannot leak

**The rule, in one sentence:** *Someone who is not your friend can see your photo, name, handle and pinned thought, and nothing else: not your notes, not your threads, not your comments, not your friends, not your counts.*
Everything below follows from it. There is **no mutual-friends number anywhere** in this version (remove it from the not-friends profile and from friend-request rows). Keep it simple.

### 2.1 One place decides, and the UI never receives data it may not show
1. Create **`AccessPolicy`**: a pure Kotlin object with no Android or UI dependencies. **Deny by default.** It answers every question about access from one `relationship` value:
   `SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME`.
2. The repository (and the store behind it) is the **only** code that touches real data, and it **always asks `AccessPolicy` before returning anything**. It returns **redacted types**, not full objects with fields hidden by the UI:
   - `ProfileView` is a sealed type: `Full(...)` (SELF/FRIEND) or `Limited(photo, name, handle, pinnedThought)` (REQUEST_SENT, REQUEST_RECEIVED, NONE) or `NotFound` (blocked either way). **`Limited` has no field for notes, friends, counts, groups, inner circle, or anything else**, so nothing else can be shown by mistake.
   - `NoteView`: `Full(note)` for friends (respecting audience: Friends, or Inner circle only if the viewer is in the author's inner circle), `Snippet(authorName, text up to 90 characters, canOpen = false)` for a shared note when the viewer is not the author's friend, `Hidden`.
   - `ThreadResult`: `Opened(thread)` or `Denied(reason = NOT_FRIENDS | BLOCKED | AUDIENCE | GROUP_NOT_MEMBER | EVENT_LOCKED)`. **Opening a thread, loading comments, posting a comment, liking, reposting, quoting, and sharing all go through the repository and return `Denied` for non-friends.** The Thread screen cannot be shown without an `Opened` result.
3. Compose screens may only branch on these results. **No composable and no ViewModel may read raw notes, comments or friend data from the store**, and nothing in `ui/` may import the store. Add an automated test or a lint-style check that fails the build if `ui/` imports `CircleStore`.
4. **Deep links, notifications, back stack, recents, and saved state all go through the same path.** If a route arrives with a note id the viewer can't open, the screen shows the "not friends yet" gate. There is no route that skips the repository.

### 2.2 The exact rules (the table is the spec; encode it, and test every cell)

| Resource | SELF | FRIEND | REQUEST_SENT / REQUEST_RECEIVED / NONE | BLOCKED (either way) |
|---|---|---|---|---|
| Profile | Full | Full | **Limited** (photo, name, handle, pinned thought) | NotFound |
| Author's notes | yes | yes, by audience | **never** | never |
| Open a thread / read comments | yes | yes, if viewer can see the note | **never** (gate sheet) | never |
| Comment, like, repost, quote | yes | yes, if viewer can see the note | **never** | never |
| Their friends list, counts, groups, inner circle | own only | **never** | **never** | never |
| Direct message | n/a | yes | **never** (no Message button) | never |
| Shared note card in a chat | open | open | **Snippet only**, lock + "Not friends yet", tap opens the gate sheet | hidden |
| Group member / event seat / search result / comment author tapped | own | friend profile | **Limited profile** + Add friend | NotFound |
| Group posts, event threads | by membership / seat, never by friendship alone | same | same: **never** unless member / seated | never |

Also: only **friends of the author** can comment on or like a note; inner-circle notes cannot be shared or reposted at all outside the inner circle; group posts cannot be shared outside their group.
**Shared card:** it shows the author's name and a snippet of at most 90 characters, **no photos (blurred placeholder), no replies, nothing else**. The only actions are *Send friend request*, *View profile (Limited)* and *Not now*.

### 2.3 Behaviour that must hold
- **Relationship changes while a screen is open** (unfriend, block, request accepted): every screen re-evaluates on change and on resume. On unfriend or block, **purge cached notes, threads and snippets for that person** from memory and from any local database, and any open thread closes to the gate.
- When a request is accepted, gated cards and threads open without any other change.
- **Never log note text, comment text or message text.** Never put them in crash reports, analytics, or notification payloads for people who aren't allowed to see them.
- The Limited profile is fetched with **only** the limited fields. The full profile must not be loaded into memory and then hidden.
- Direct messages: only friends. Group and multi-person chats may include non-friends; there, members see each other's names and avatars, tapping opens the Limited profile, and no DM is possible until they are friends.
- Search shows only Limited info. Handles are discoverable per the Privacy settings.
- A blocked person is invisible everywhere on the viewer's side, and cannot find or see the blocker.

### 2.4 Tests (required, and they gate this phase)
- **Exhaustive table-driven unit test** over every relationship x every resource x every action in the table above. A cell that is not explicitly allowed must be denied.
- Repository tests: `getProfile` returns `Limited` for NONE and never a `Full`; `openThread` returns `Denied(NOT_FRIENDS)`; `addComment`/`like`/`repost` return `Denied`; a shared note returns `Snippet` with `canOpen=false` and text <= 90 characters; unfriend purges cached content.
- Compose UI tests for the owner's scenarios, each asserting that no restricted text appears on screen:
  1. In a group chat, tap a non-friend's avatar: Limited profile with **Add friend**, no notes, no Message button.
  2. In a chat, tap a shared note whose author is not my friend: the gate sheet appears, the thread does not, and typing a comment is impossible.
  3. Open a thread route directly (deep link) for a note I can't access: the gate, not the thread.
  4. Search result and group People tab and event seat tap: Limited profile.
  5. Accept the request: the same card now opens the thread.
  6. Unfriend while the thread is open: it closes to the gate and cached content is gone.
- Write the same table as `PRIVACY_RULES.md` (plain language) so the backend must implement the **same** rules on the server. The app's checks are for experience; **the server's checks are for security** (add this to BACKEND_READINESS.md).
Do not move to the next phase until section 2 is complete and its tests pass.

## 3. Phases, in order (each one: build, test, lint, commit, update PROGRESS.md and REPORT.md)

0. **Baseline.** `./gradlew build`; `git status`; read all documents; write the gap list into `REPORT.md`; create `PROGRESS.md`. Do not wait for approval.
1. **First-launch fix** (ACCESS_AND_LAUNCH 0.1): the story shows only once, after account creation; logged-in users go straight to Home. The five tests.
2. **Privacy core** (section 2 above), including `AccessPolicy`, redacted types, repository entry points, tests, `PRIVACY_RULES.md`. Add the `relationshipTo(id)` API.
3. **Repository + ViewModel layer** (FRONTEND_COMPLETE section 8): `CircleRepository` interface, `FakeCircleRepository`, ViewModels, no UI access to the store; `Result` types; IDs are strings and times are `Instant`. Migrate existing screens as you touch them in the phases below.
4. **Posts and threads (F1):** private likes, comment, the Repost/Quote popover (spring from the icon, scrim, haptic tick, remove-animations aware), quote and repost with undo, the rules (no inner-circle, no group), the ⋯ menus, reply-to-reply with "View N replies", the photo viewer, Compose (photo/video max 30s, audience, discard dialog, soft limit), the Home header that hides on scroll (Home, Messages, Alerts), pull to refresh. Find out why Quote was broken and say so in `REPORT.md`.
5. **Profiles and the friend graph (F2):** one profile screen driven by `ProfileView`, Add friend / Requested / Cancel / Accept / Decline / Unfriend / Block / Mute / Report, Edit profile inside Settings, tabs that filter, "All friends" searchable list in Your circle.
6. **Messages and chat** (MESSAGES_AND_SETTINGS_V2): the new list (Pinned, unread tint, "You:" prefix, tabs "Messages | Groups", search, "+" new chat), the chat screen (swipe-right to reply, hold for Copy / Delete for me / Unsend on mine, shared-note card opens the thread **only if the viewer may**, event card opens the event, photo, composer, sheet, read receipts per setting), new-chat flow (friends only), group-chat member avatars and names opening profiles.
7. **Groups and events (F4):** create flows that really create, request-to-join pending and visible to all members, hosts approve/decline, the People orbit, event seats, "Take a seat"/"Leave seat", unlocked thread, invite friends, host tools, report/block.
8. **Settings, safety, states (F5):** Settings (Edit profile inside it; Notifications as ONE row opening its own screen with switches, pause and quiet hours; Privacy; Appearance; About), the tinted About Circle and Why I built it screens with the **new connection-first text**, Report flow, Blocked and Muted lists, Alerts (tap opens target, mark all read), empty/offline/error states with skeletons.
9. **Getting in** (ACCESS_AND_LAUNCH 1 to 4): welcome, **typed country code + number with a searchable country list (libphonenumber, E.164)**, code, profile setup, **age 18+**, agreement, find friends (hashed matching copy), invite sheet and link plumbing (stub the domain), Membership (Play Billing interface, placeholder prices from the store), Your data (download/delete with the export design), Terms/Privacy/**Company** pages, `CLAIMS.md`. **Apply section 0.3: reduce "no ads" to the allowed places.**
10. **Scaling, strings, accessibility, device pass** (ACCESS_AND_LAUNCH 5 to 7): `BoxWithConstraints` scaling for every orbit with screenshot tests, all text in resources with `HardcodedText` lint as an error, 48dp targets, Map | List toggles, TalkBack and reduce-motion, a device-pass table. Nothing may be tied to one country (section 10).
11. **Persistence** (FRONTEND_COMPLETE F6): Room/DataStore so state survives restarts; "Reset demo data" in Settings; a debug switch that makes the fake repository fail so the error UI can be seen.
12. **Final pass:** run every item in section 5 yourself, in night and day; remove dead buttons; re-read the Rejected list; write `BACKEND_READINESS.md` (entities, operations as candidate API calls, **the privacy rules that the server must enforce**, notifications, media upload, moderation, data export and deletion, anything the app assumed) and `LAUNCH_CHECKLIST.md` (ACCESS_AND_LAUNCH 8 and 10: done / needs owner / needs a lawyer). Then stop.

## 4. Quality bar
- Every button does something real or is visibly disabled with a reason. No fake auto-replies, no toasts that pretend.
- Both themes correct on every screen; fonts bundled; night = Orbit palette, day = Edition palette (design-tokens.json).
- State survives rotation and process death. Back button behaves. Keyboard never covers inputs.
- Text only from resources. Copy follows ACCESS_AND_LAUNCH 0.2 and 0.3. Claims in `CLAIMS.md` match behaviour.
- Tests for rules and geometry; screenshot tests for orbits at 320/360/390/430dp and font scale 1.0/2.0.

## 5. The owner's click-through (you run it yourself at the end and report pass/fail for each)
1. Fresh install > Welcome > type +91 and a number > any 6-digit code > profile > age (18+) > find friends > empty Home > invite sheet.
2. Log out > Welcome. Reopen the app logged in: **straight to Home, no story.**
3. Profile > highlighted friends row > Your circle: move someone between rings; the count changes on Profile and Compose.
4. Home: heart (private), comment > Thread (first-layer replies visible, "View N replies" collapsed), reply, Repost popover and Quote.
5. Compose: write, choose Inner circle, Post: it appears tagged on Home and Profile.
6. Explore map > dot > View event > Take a seat; View group on a non-member group > Request to join > Requested (never instant).
7. Alerts: accept a request; friend count rises.
8. Messages: swipe right to reply, hold for Copy/Delete for me, tap a shared note from a friend (opens), tap one from a non-friend (**gate, no thread, no comment**), tap a non-friend in a group chat (**Limited profile, Add friend**).
9. Settings: gear on Profile; Notifications is one row; theme switch works live; About Circle and Why I built it are tinted.
10. Membership, Your data (request download; delete by typing DELETE), Terms/Privacy/Company.
11. Night and day on all of the above; TalkBack on an orbit screen via its list view.

## 6. When you finish
Print, for the owner: the clean-reinstall commands: `adb devices`; `adb uninstall com.circle.app`; `./gradlew clean installDebug`; `adb shell monkey -p com.circle.app -c android.intent.category.LAUNCHER 1`;
`adb logcat -d -s AndroidRuntime:E`. Then a short summary: what is done, what is a stub, what needs the owner (decisions, a lawyer, accounts), and the exact order of the next steps toward the Play closed test.
