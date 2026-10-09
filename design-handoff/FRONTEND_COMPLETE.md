# Circle: addendum, "front end complete"

This addendum **overrides BUILD_BRIEF.md section 7 (out of scope) and the phase order in section 8.** Everything else in
the brief still holds (principles, look and feel, screen specs, accessibility, rejected ideas).

The owner's goal: **the whole front end works like a real social app.** Every button does what a user expects, against
the in-memory store, so that the next step can be the backend. "Looks right" is not enough.

## 1. New order

1. Phase 1 (theme and fonts): done.
2. Phase 2: store (merge into the existing `object CircleStore`; keep its comments and create flows).
3. **Phase 4: Your circle** (Profile entry, Compose link, ring-to-ring move).
4. **Phase 5: Explore map** (search pill opens the existing search; "+" for Create group/event; Map | List toggle).
5. **Phase F1: posts and threads** (section 3).
6. **Phase F2: profiles and the friend graph** (section 4).
7. **Phase F3: messages** (section 5).
8. **Phase F4: groups and events** (section 6).
9. **Phase F5: settings, safety, states** (section 7).
10. **Phase F6: persistence and the repository layer** (section 8).
11. Phases 3, 6, 7, 8, 9 of the original brief: fold into the stages above wherever they overlap, and finish whatever is left.
12. **The gate** (section 9). Then stop and tell the owner.

After each stage: `./gradlew build`, tests, commit, and report what now works and what is still a stub.
Ask the owner when a product decision is unclear. Don't invent features that aren't listed here or in the brief.

## 2. Rules for "works"

- A button either performs its action or is visibly disabled with a reason. No dead taps, no toasts that pretend.
- State lives in the store and survives rotation and (after F6) app restarts.
- Every list has an empty state (one calm line + one action) and the screens that will load from a server have a loading
  skeleton and a "couldn't load, try again" state. These are stubbed for now but must exist.
- Respect the principles: no public counts, likes are private, inner-circle notes cannot be reposted, group posts have no
  repost or quote, requests are never instant.
- Use the system back button and keep the keyboard from covering inputs (IME insets).

## 3. Phase F1: posts and threads

**Post card (Home, Profile, Thread)**
- **Like (heart):** toggles, private. Filled accent when on. No count anywhere. (Replies to your own posts show a "liked your post" row in Alerts for the author.)
- **Comment:** opens the Thread screen for that post.
- **Repost:** opens a small menu **Repost / Quote**.
  - *Repost:* adds a "You reposted" card at the top of your Home and Profile; the menu now shows **Undo repost**. Disabled, with the line "Inner circle notes can't be reposted", on inner-circle notes. Not offered on group posts.
  - *Quote:* opens Compose with the original shown as a small embedded card under the text box. Posting creates a quote card. Same restrictions as repost.
- **⋯ menu** on every card: on others' posts: *Send in a message* (pick a friend, sends the post into that chat), *Mute this friend*, *Report*, *Block*. On your own posts: *Edit*, *Change audience*, *Delete* (with a confirmation).
- **Tap name or avatar:** opens that friend's profile (section 4). **Tap a photo:** full-screen viewer with pinch-to-zoom and a close button.
- Your own notes show the accent "INNER CIRCLE ONLY" tag when that was the audience.

**Thread**
- Root card has actions (like, repost menu, ⋯).
- Each comment has **Like** and **Reply**. Reply puts a chip "Replying to Priya" above the composer; the sent reply nests under that comment. "View N replies" counts nested replies and works at one nested level.
- Long-press your own comment: *Delete*. Others': *Report*.
- Sending adds the reply immediately; empty input disables Send.

**Compose**
- Add photo or video from the system picker (several photos allowed; **video max 30 seconds**: reject longer with a clear message). Show a removable preview.
- Audience pill: Friends / Inner circle (note text updates). Soft 500-character limit with a counter that appears near the end.
- Cancel with text or media shows "Discard this note?" Post disabled when empty.
- Posting from a group screen posts **to that group** (not to Home) and the audience pill is replaced by the group name.

**Home**
- Pull to refresh: if nothing new, show "Nothing to refresh. You're caught up." Tapping the wordmark scrolls to top and refreshes (keep).
- New notes from the store appear at the top without losing scroll position (show a small "New" pill to jump up).

## 4. Phase F2: profiles and the friend graph

**Other people's profiles** (new screen)
- *Friend:* avatar, name, handle, pinned thought, their notes visible to you (inner-circle notes only if you are in their inner circle; for the prototype treat all seed friends as inner), buttons **Message** and a **Friends ✓** pill. ⋯ menu: *Mute*, *Unfriend* (confirm), *Block*, *Report*.
- *Not a friend (reached from search or Explore):* **photo and pinned thought only**, plus **Add friend**. Nothing else is shown, no counts, no notes.
- *Pending:* the button reads **Requested**, tapping it offers *Cancel request*.

**Friend requests**
- Search → Add friend → Requested → (cancel) or accepted. Accepting an incoming request keeps the row as "✓ Friends · Now friends".
- Unfriend removes the person from Your circle and from the friend count. Block hides them everywhere and removes the friendship.

**My profile**
- *Edit profile:* change display name, handle, avatar photo (picker), and **pinned thought** (the pinned thought is the bio). Saves to the store.
- Tabs All / Thoughts / Posts actually filter. Reposts and quotes appear under All.
- The friends row opens Your circle. **All friends** list (searchable) is under the map in Your circle.
- Friend count and inner count are always derived from the store.

**Your circle** (Phase 4)
- Behaves exactly as specified. Plus: search field filters the All friends list; tapping a person in the list selects them on the map (or moves them if they're beyond the 12 shown).

## 5. Phase F3: messages

- **+ (new chat):** pick one friend (1:1) or several (a chat you start with friends; ask for an optional name). It appears in Messages (not Group Messages).
- Message **Send** works; timestamps; unread dot on rows; opening a chat clears it. Sending an image from the picker works.
- Long-press a message: *Copy*, *Delete for me*. Messages from a blocked person are hidden.
- **Group Messages:** only chats tied to a group you are a member of; they appear when you join a group (not when you request).
- Remove the prototype's fake auto-reply. Incoming messages exist only as seed data.
- Profile's **Message** button opens (or creates) the 1:1 chat.

## 6. Phase F4: groups and events

**Groups**
- Create group form submits for real: the creator becomes host and member; the group appears in Explore and in "Your groups"; a linked chat appears in Group Messages.
- Request to join: pending state, visible to all members. **Hosts** (only) see **Approve / Decline** on each request in the People tab; approving makes the person a member and moves them onto the member ring.
- Member menu: *Leave group* (confirm). Host menu: *Edit group* (name, description, public/private), *Remove member*.
- Private groups don't appear in Explore; they are reached by invite or from your own list.
- Posting to a group (Compose from the group screen) shows in the group's Posts tab. Group posts: comment and like only.
- **Invite friends:** a friend picker sends an invite row into Alerts for that friend (seed-only on this side).

**Events**
- Create event form submits for real: host is the creator; it appears on the Explore map at its topic angle on the inner ring (if within 7 days) or in the "See all" list.
- Take a seat / Leave seat as specified. Cannot take a seat when full ("Event full"). Leaving locks the thread and records a quiet notification to the host (shown in the host's Alerts as "Amara left Board Game Night").
- Host menu: *Edit event*, *Cancel event* (confirm; attendees get a notification). Everyone: *Invite friends*, *Report event*, *Block host*.
- The unlocked thread is a real thread (replies, like, reply, composer) using the Thread components.

**Explore**
- Map | List toggle, topic filter chips in the list, "See all". Search pill opens the existing search (People / Events / Groups / Posts) with working Add friend, Join and Request buttons. The "+" in the header opens Create group / Create event.

## 7. Phase F5: settings, safety, states

- **Settings** screen (from Profile): theme System / Night / Day, notification toggles (likes, comments, requests, events, groups), **Blocked users** (list with Unblock), **Muted friends** (list with Unmute), About, Log out (returns to onboarding), **Delete account** (confirmation that explains what is deleted).
- **Report flow** (posts, comments, people, groups, events): reason list → optional note → "Thanks. We'll look at this." Record the report in the store.
- **Block:** confirm, then the person disappears from feeds, search, groups lists, chats and event seats on your side.
- **Alerts:** tapping a row opens what it refers to (thread, profile, event). "Mark all read" clears the red dot on the bottom bar. The dot shows only when something is unread.
- **Empty and error states** on every list (see rules). Offline banner stub at the top: "You're offline. Showing what we have."
- **Onboarding and contacts sync** stay as they are; make sure "Skip for now" on contacts leads to Home.

## 8. Phase F6: persistence and the repository layer (what makes the backend a swap, not a rewrite)

- Introduce **`CircleRepository`**, an interface that exposes everything the UI needs (feed, threads, profiles, friends, requests, chats, groups, events, notifications, settings) as `Flow`s and suspend functions. One `FakeCircleRepository` backed by the store. **No composable may touch the store or any data class's mutable state directly; screens talk to a ViewModel, the ViewModel talks to the repository.**
- Persist the store locally (Room, or DataStore for small things) so state survives app restarts. Provide a "Reset demo data" action in Settings.
- Every repository call that would be a network call returns a `Result`-like type (Success / Error) and the UI handles the error case, even though the fake never fails (add a debug switch that makes it fail so the error UI can be seen).
- IDs are strings (UUID-style) everywhere, not list indexes. Timestamps are `Instant`s, not strings like "2h".
- Media is referenced by URI and stored through a small `MediaStore` abstraction.

## 9. The gate: when this stage is done

Tell the owner "front end complete" only when all of this is true:

1. The owner's click-through in the brief (section 9) passes, in night and day.
2. Every button listed in sections 3 to 7 works or is disabled with a reason. Give the owner a checklist table (screen, button, result, status).
3. `./gradlew build`, unit tests and lint pass. Unit tests cover the store rules and orbit math (brief section 9, phase 9) plus: repost rules (no inner-circle, no group), block removes content, capacity, request visibility, Undo repost.
4. Screens use ViewModels and the repository; nothing in `ui/` imports the store.
5. TalkBack: every orbit screen works through its list view.
6. State survives rotation and a full app restart.
7. You have written **`BACKEND_READINESS.md`**: (a) every entity and its fields, (b) the list of operations the repository performs, each as a candidate API call, (c) which privacy rules must be enforced **on the server, not just the app** (inner-circle visibility, mutual friendship, blocks, group/event visibility, locked threads, private likes), (d) what needs push notifications, (e) what needs media upload, (f) what the report flow needs from moderation, (g) anything the app assumed that a backend will have to decide.

Then stop and wait. Do not start the backend.
