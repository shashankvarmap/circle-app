# Circle: Android build brief

You are working in the existing Circle repo (Kotlin, Jetpack Compose, package `com.circle.app`, a NavHost with
`Routes`). **Do not rewrite it from scratch.** Bring it in line with the final design in this folder, phase by phase,
keeping everything that already works. The owner prefers precise, detailed work and has asked to be asked when
something is unclear, so **stop and ask with options** rather than guessing on anything product-shaped.

## 0. How to work

1. Read this whole brief, then `design-tokens.json`, then skim `reference/prototype-logic.js`.
2. Open `reference/prototype-night.dc.html` and `prototype-day.dc.html` in a browser if you can. They are clickable
   prototypes of the whole app; the numbers in them (radii, sizes, angles) are the source of truth.
3. Before changing anything, write a short gap list: what the repo does today vs. what this brief says. Show it to the owner.
4. Work in the phases in section 8. After each phase: `./gradlew build`, run the unit tests, commit with a clear
   message, and report what changed and anything you deviated on. Do not start the next phase until the build is green.
5. `kotlin/` holds four **unverified starter files** written without a compiler. Treat them as a head start: merge
   them into the existing packages, fix whatever doesn't compile, and don't keep duplicates of what already exists
   (for example the old single-palette `ui/theme/Color.kt`).
6. 1 px in the prototype = 1 dp in Compose. 1 px of text = 1 sp.

## 1. Product principles (non-negotiable)

Circle is a friends-only social app. Every decision should pass one test: does it serve real connection between real friends?

- No ads, ever. No algorithmic ranking or injected content. Chronological, always.
- Connections are mutual (request, accept). No follower or following. Friend list and counts are private to the owner.
- No public like counts. A like is a private signal to the author only. No reactions picker.
- Calm pacing: the app says when you are caught up. Nothing is built to be endless.
- Profiles are private by default: a stranger sees photo and pinned thought only.
- **Inner circle** is private. It has no visible badge and nobody is told who is in it. It only decides who can see your inner-circle notes.
- Groups default to private. A public group can be asked to join, never instantly joined. **Join requests are visible to every member**, not just the hosts.
- Events: time, public venue, capacity, visibility (friends / friends of friends / open nearby). The reply thread is **locked until you take a seat**. Leaving a seat locks you out and quietly tells the host.
- Explore is the one public layer. It is placed by **topic and how soon**, never ranked.
- Group posts stay in the group: comment and like only, no repost or quote.

## 2. Look and feel

Two themes, following the system day/night setting by default (a manual switch can come later).
Night is the "Orbit" palette. Day is the "Edition" palette. Exact values are in `design-tokens.json`.

| Token | Night | Day |
|---|---|---|
| background | `#12151A` | `#F3EDE2` |
| surface (cards) | `#171B22` | `#FAF6EE` |
| border | `#21252E` | `#D9CFBD` |
| text | `#F1F0EC` | `#1F1B16` |
| body | `#E7E5DF` | `#2B2620` |
| muted | `#7C828E` | `#6B6256` |
| accent | `#E8A87C` | `#B5482A` |
| on accent | `#181310` | `#FFF8F0` |

- Fonts: **Fraunces** for the wordmark, headings and day-mode post text. **Public Sans** for everything else (night-mode post text too). Both are OFL: bundle the files in `res/font`. The wordmark is "Circle." with the full stop.
- Cards: surface fill, 1dp border, 13dp radius, soft shadow (night `0 8 20 black@28%`, day `0 6 16 brown@10%`).
- People are **circles** with initials. Groups are **rounded squares** (corner radius about 30% of the size). Avatar colors are in the tokens; use dark text except on slate.
- Never use the muted color's dimmer cousins for readable text. Muted is the lowest allowed text color.
- Bottom bar: 64dp, five items: Home, Messages, **Explore (raised 46dp ring, 2dp accent outline, no fill, offset up 24dp)**, Alerts (red dot when unread), Profile (your avatar). Detail screens hide the bar.
- Motion: ordinary default navigation transitions. **Do not add circular reveals or circular gestures.** They were tried and rejected.

## 3. Rejected ideas: do not reintroduce

Reaction pickers (heart/laugh/cry); circle-reveal screen transitions; draw-a-circle or spin gestures; a friends wheel;
rows or arcs of small avatar circles on Home; a rolling drum or porthole feed; a one-note-at-a-time deck; inline comment
expansion on Home (comments open the Thread screen); instant group joins; a separate "Requests" tab (requests live in Alerts).
Home stays an **ordinary vertical scroll.** If the existing Home-icon press-and-hold feed flip is already in the repo,
leave it as it is.

## 4. State: one shared store

All screens read and write one in-memory store so an action on one screen shows up on the others. See `kotlin/CircleStore.kt`.

- `innerCircle` (set of person ids), `isInner`, `toggleInner`. `innerCount` is shown on Profile, Your circle and Compose.
- `friendRequests` (Riya, Marco start pending): accept sets accepted, decline removes the row. `friendCount = 128 + accepted`.
- `likes` per post id (private). `myPosts` with `audience` Friends or Inner circle.
- `joinedEvents` per event id. `goingCount = goingBase + (1 if I joined)`. Cannot join past capacity.
- `memberOfGroup` (Aachen Photo Walk starts true, others false) and `requestedGroup`.
- Nothing is persisted yet. Persistence is Phase 10 and optional.

## 5. Screens

Coordinates below are inside each orbit's own container, origin top-left. Angles are **compass angles**: 0 = straight up, clockwise positive. `kotlin/Orbit.kt` has `compassPoint`, `ringPoints` and the geometry constants.

### Home (normal scroll)
Header: date line "TUESDAY · 6 OCTOBER" (9.5sp, bold, 0.2em tracking, muted), wordmark "Circle." (Fraunces 24sp semibold). Under it small underline tabs **Friends | Groups** (13sp semibold, 1.5dp accent underline on the active one, muted otherwise).
Friends tab: a "What's on your mind?" row (opens Compose), then cards newest first. Card: 34dp avatar, name 13.5sp bold, time 12sp muted, text (night Public Sans 14sp / day Fraunces 16.5sp, line height about 1.45), actions: comment (opens Thread), repost (inert for now), heart (private like: outline when off, accent fill when on). Your own inner-circle notes carry an accent tag "INNER CIRCLE ONLY" next to the time. Photo posts show a media block under the text. The feed ends with "You're all caught up." (Fraunces 19sp) and "Nothing below this line."
Groups tab: cards with a small group tile + group name above the author line; actions are comment and like only. One line of muted text: "Group posts stay in the group. No repost, no quote."

### Thread
Back + "Thread". Root card (surface, 16dp radius, shadow, 44dp avatar, 17sp text, actions). Then first-layer comments always visible (36dp avatars). A comment that has replies shows **"View 2 replies" / "View 1 reply"** (accent, 12.5sp bold, chevron); tapping reveals those replies inline, indented 30dp, with a 2dp left border in `border`. The root's heart is the same like as Home. Reply bar at the bottom (surface, upward shadow): text field + accent send button; a sent reply appears at the end as "You · now".

### Compose
Cancel / **Post** (accent pill, 40% opacity until there is text). Large text area (17sp). Bottom row: image icon on the left, **audience pill** on the right that toggles Friends and Inner circle. Under it: "All your friends can see this." or "Only your N inner-circle friends can see this." and a link "Edit who's in your inner circle" that opens Your circle. Post adds the note to the top of Home and to Profile (tagged if inner circle) and returns to Home.

### Explore: the polar map
Header: "Explore" (Fraunces 24) with a city chip "Aachen ▾", then a search pill (not wired yet). Map container 390 x 340, centre (195, 170):
- Centre: 44dp accent circle with a pin icon, "AACHEN" label under it (9sp, 0.16em tracking).
- Rings: outer dashed r=124, inner solid r=68 (1.4dp, `border`). Four faint spokes at 0/90/180/270 from r=40 to r=146. Sector labels (8.5sp bold, 0.16em) at r=154: OUTDOORS 45°, SOCIAL 135°, MUSIC 225°, MAKING 315°.
- **Angle = topic, distance = how soon.** Events (this week) on the inner ring, groups on the outer ring. Positions are the `compassDeg` values in `Seed`. **Never rank or reorder by popularity.**
- Event node: 40dp circle, surface fill, 1.5dp accent border, day number 13sp extra-bold over month 7.5sp bold accent. Group node: 36dp rounded square (11dp radius) in the group's color, letter 15sp extra-bold in `#181310`.
- Selected node: scale 1.18 plus a double ring (3dp gap in background color, then 2dp accent).
- Caption top-left: "SQUARES = GROUPS · CIRCLES = EVENTS" (9sp, muted).
- Preview card below the map for the selected node: kicker ("EVENT · OUTDOORS" or "GROUP · GAMING", 10sp bold accent), title (Fraunces 22), meta line (when and place, or "64 members, public"), for events a capacity bar plus "4 of 10 going", then a right-aligned accent pill **View event / View group**. Footnote: "Placed by topic and how soon. Nothing here is ranked."

### Your circle (private)
Reached from **Profile**: the highlighted row "128 friends · 5 inner circle · See your circle ›" (the friend count goes up when you accept a request), and from Compose. Header: back, "Your circle" (Fraunces 21), subtitle "N friends · M inner circle", lock + "Only you" on the right. Map 390 x 350, centre (195, 170): 48dp "S" avatar; inner ring r=64 (solid), outer ring r=122 (dashed); "INNER CIRCLE" label under the centre; people are 38dp (inner) and 36dp (outer) circles. Inner people are spaced evenly from the top; outer people are spaced evenly with a half-slot offset (`outerOffsetDeg`). Tapping selects (double ring). A detail card shows avatar, name, status "In your inner circle · since 2023" or "Friend · since 2024" and one button: **Move to inner circle** (accent fill) or **Move back to friends** (outlined). Moving animates the person to their new ring (about 550ms, slight overshoot). Footnote: "They're never told, and there's no badge. It only decides who sees your inner-circle notes."
The prototype shows 12 of 128 people. The real screen needs a way to reach the rest (see section 10).

### Group
Header tile 56dp (17dp radius) in the group color, name 17sp bold, "Photography · 128 members · PUBLIC".
- **Not a member:** accent pill "Request to join", note "Your request will be visible to every member, not just the hosts.", then a dashed locked panel "Join to see what's posted here." After requesting it shows an outlined "Requested" state. Never instant.
- **Member:** tabs **Posts | People**. Posts: group cards (comment and like only). People: orbit 390 x 312, centre (195, 156): 44dp group tile in the middle; hosts ring r=58 with two hosts at 0° and 180° (accent double ring); members ring r=118 (dashed) with 12 slots starting at 15° and every 30° after. Slot order: AO, TN, RS, EK, MD, **request**, SY, NB, LF, **request**, OH, **"+117" chip**. A request is a circle with a 2dp **dashed accent** border and accent initials. Label "HOSTS" under the centre. Below: "ASKED TO JOIN · 2" and "Dashed circles are join requests. Every member can see them, not just the hosts." In the real app list the requesters under it, each with "asked 2h ago · 1 mutual friend". No approve or decline buttons for ordinary members.

### Event (the table)
Title (Fraunces 25) with the visibility tag in accent caps; when and place under it. Orbit 390 x 300, centre (195, 150): dashed seat ring r=108; a table disc r=58 (surface, border) holding the count, e.g. "6/12" (Fraunces 40sp, the "/12" smaller and muted) over "at the table". **One seat per place of capacity**, evenly spaced from the top. Seats up to 12 are 38dp, above 12 are 30dp. Seat 0 is the host (accent double ring). Filled seats are people. **Empty seats are 2dp dashed muted circles**. If I have joined, I take the next seat after the going people (accent double ring, avatar "S"). See `eventSeats()` in `kotlin/CircleModels.kt`.
Below: "Hosted by Priya Kapoor · N seats left". Not joined: accent pill **Take a seat** + lock line "The conversation opens when you take a seat." Joined: outlined "✓ You're going" + text button "Leave seat", and the conversation unlocks. In the real app the unlocked thread (replies, composer) appears under this using the Thread components. Show "Event full" instead of the button at capacity.

### Messages and chat
Tabs **Messages | Group Messages** (use one-line labels, no wrapping). Messages: one-to-one chats and any chat you start with friends. Group Messages: only chats tied to a group you've joined; group chats use a rounded-square avatar. Opening a chat shows bubbles (mine right in accent, theirs left on surface) and a text bar. The prototype fakes a reply after 1.3s. Do not ship that.

### Alerts
Tabs **Notifications | Activity**. Notifications: "FRIEND REQUESTS" first (Accept = accent pill, Decline = outlined pill; after Accept the row shows "✓ Friends" and "Now friends", after Decline it disappears), then "RECENT" (likes, comments, reposts, quotes aimed at you). Activity: an ambient feed of what friends did (not aimed at you).

### Profile
Avatar 68dp, name (Fraunces 24), handle, "Edit profile" pill. A PINNED card (the pinned thought is the bio). The **highlighted friends row** (1.5dp accent border) is the entry to Your circle; under it a lock line "Only you see this. Tap it to open your circle." Tabs All / Thoughts / Posts (the real app should filter). Then your notes, newest first, including ones posted from Compose.

## 6. Accessibility (required, not optional)

- Every node on an orbit needs a **48dp minimum touch target** even though the visible node is 36 to 40dp (pad the clickable area, e.g. `minimumInteractiveComponentSize()` or a larger transparent hit box).
- Every node gets a `contentDescription`/semantics, e.g. "Priya Kapoor, inner circle, selected" or "Board Game Night, event, 19 September, 4 of 10 going".
- A circle layout is hard for screen-reader users. **Each orbit screen (Explore, Your circle, Event seats, Group people) gets a Map | List toggle**, and the list is a plain, ordered, fully accessible list with the same actions. Default to the list when TalkBack is on.
- Respect the system "remove animations" setting: skip the ring-to-ring glide.
- Text contrast: keep text at `muted` or stronger. Day-mode `muted` on the paper background meets 4.5:1.
- Color is never the only signal: an empty seat is dashed, a request is dashed, a selected node has a ring, not just a color.

## 7. What is out of scope for now

Backend, accounts, push notifications, real photo and video upload, real group chat delivery, payment, persistence
(until Phase 10), onboarding changes (the story and contacts screens already exist), the repost/quote menu, and any
new features that are not in this brief.

## 8. Build phases

Each phase ends with a green `./gradlew build`, passing tests and a commit.

0. **Gap list.** Read the repo, compare to this brief, list differences and risks. No code changes. Get the owner's OK.
1. **Theme.** Add `CirclePalette` (night/day), bundle Fraunces and Public Sans, follow the system setting, replace hard-coded colors with palette tokens. Both themes render correctly on every existing screen.
2. **Store.** Add `CircleStore`, hold one instance, and move likes, posts, friend requests, event joins, group requests and inner circle out of per-screen `remember` state into it.
3. **Home, Thread, Compose.** Match section 5. The audience pill, the "Inner circle only" tag, the collapsed "View N replies", private likes and the caught-up ending all work and read from the store.
4. **Orbit foundation + Your circle.** Add `Orbit.kt`, an `OrbitNode` composable with the 48dp hit area, and the Your circle screen with Profile and Compose entry points and ring-to-ring animation.
5. **Explore map.** Polar map, preview card, View event / View group, Map | List toggle.
6. **Group.** Member and non-member states, request-to-join pending state, Posts | People tabs, join requests visible to all.
7. **Event table.** Seats from capacity, Take a seat / Leave seat, thread unlock, full state.
8. **Alerts and Messages.** Friend request accept/decline updating counts everywhere. Tabs on one line. Group Messages rules.
9. **Accessibility and tests.** Map | List on all orbit screens, semantics, reduce-motion. Unit tests for `compassPoint`, `ringPoints`, `outerOffsetDeg` and `eventSeats` (full table, empty, joined, over 12 seats), and for the store rules (cannot join past capacity, accept increments friend count, inner circle toggle).
10. **(Optional) Persistence.** Room or DataStore for the store. Ask the owner first.

## 9. Definition of done: the owner's click-through

1. Profile, tap the highlighted friends row: Your circle opens. Move someone between rings, go back, and "inner circle" on Profile has changed.
2. Home: tap a heart (fills accent). Tap a comment icon: Thread opens with first-layer replies visible and "View 2 replies" collapsed. Open it, send a reply.
3. Compose: write a note, choose Inner circle, Post. It is at the top of Home with the tag, and on Profile.
4. Explore: tap a dot, see the preview card, tap View event. Take a seat: you appear at the table and the count goes up. Leave seat: it goes back.
5. Explore: View group on a group you aren't in: Request to join becomes Requested, never member.
6. Alerts: Accept Riya. Friend count on Profile goes up by one.
7. Messages: open a chat and send a message. "Group Messages" stays on one line.
8. Switch the phone to day mode and back: both themes look intentional on every screen above.
9. TalkBack on: every orbit screen is usable through its list view.

## 10. Open design questions: ask the owner, don't decide

- Your circle shows 12 of 128 people. Options: a search field plus a scrolling list under the map, paging the outer ring, or "show recent 12 + search". Which?
- Explore will hold dozens of events and groups. Options: "See all" list below the map, filters by topic chip, or zoom. Which?
- What does an empty state look like (no friends yet, nothing nearby, no events)?
- Should day/night follow the system only, or also have a switch in Profile?
- Event capacity above 20 seats gets crowded on the ring. Cap the ring at 20 and show "+N more" as a chip?
