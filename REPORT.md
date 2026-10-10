# Circle: build report (MASTER_PROMPT.md run)

Appended after every phase: what now works, what is still a stub, and decisions made.
The checkbox view is in PROGRESS.md.

---

## Phase 0: Baseline (2026-10-09)

**Build:** `./gradlew build` (assemble, unit tests, lint) is green at `c7a84a7`. Unit tests: `CircleStoreTest`,
`EventSeatsTest`, `OrbitTest`. No instrumented or Compose UI tests yet.

**Repo shape:** one `object CircleStore` (Compose snapshot state) in `data/`, seed in `Seed.kt` and `Chats.kt`,
26 screens/components in `ui/`, a `NavHost` in `navigation/`. **24 UI files read the store directly** (that is the
main gap for section 2). `strings.xml` holds only `app_name`: all user-visible text is hard-coded.

### Gap list: done / partial / missing against the documents

| Area (document) | Status | Notes |
|---|---|---|
| Theme night/day, bundled fonts, palette (BRIEF ph.1) | done | fbf2ee7 |
| One shared store (BRIEF ph.2) | done | feb791c; becomes the fake "server" behind the repository in Phase 2/3 |
| Your circle orbit + All friends list (BRIEF ph.4) | done | 4182af8 |
| Explore polar map, preview, Map/List, search (BRIEF ph.5) | done | fdfa5d8, e05accf |
| Quote fix, Repost/Undo, popover (SETTINGS_AND_POLISH 0-1) | done | f2886fb, 89e3078. The cause of the Quote bug is in f2886fb's message; to be copied here in Phase 4 |
| Header hides on scroll (S&P 2) | done | a1fe900 (Home, Messages, Alerts) |
| Settings, Edit profile (S&P 3) | done | dd6300b |
| About Circle / Why I built it text (S&P 4, V2 5, A&L 0.3) | **partial** | Old copy (leads with "No ads, ever", "€1"), not tinted cards. Needs the new connection-first text + tinted styling |
| Notifications as one row + own screen (V2 4) | **missing** | Toggles are still inline in Settings; no quiet hours |
| Messages list (V2 1) | **partial** | Tabs still "Group Messages"; no search, Pinned, unread tint, swipe actions, footer, empty state; "+" is a dimmed placeholder |
| Chat screen (V2 2) | **partial** | Bubbles, runs, swipe reply, hold menu, shared note + event cards done (4293c55). Missing ⋯ menu, + sheet (photo, event invite), photo messages, Jump to latest, New messages divider, Sending/Not sent, read-receipt setting |
| New chat flow (V2 3) | **missing** | |
| Not friends yet (NOT_FRIENDS_YET) | **partial** | d5e7ac4 built relationship, one profile, gate sheet, Elena. Shows a **mutual-friends number** (now forbidden). Checks are spread over the store and composables; no AccessPolicy, no redacted types |
| Privacy core (MASTER 2) | **missing** | No AccessPolicy, no ProfileView/NoteView/ThreadResult, UI reads raw store, BLOCKED is not split into BLOCKED_BY_ME / BLOCKED_ME, no purge on unfriend, no tests of the table |
| First launch (A&L 0.1) | **missing** | App always starts at the 4-slide story |
| Repository + ViewModels (F6 / MASTER 3) | **missing** | No repository, no ViewModels, times are strings ("2h"), list-index-free ids already strings |
| F1 posts and threads | **partial** | Likes, comment, repost/quote, audience pill, discard dialog, soft limit, group posting done. Missing: ⋯ menus on cards, comment Like/Reply + nested reply chip, long-press Delete/Report on comments, photo viewer, photo/video picker (30s), pull-to-refresh, "New" pill |
| F2 profiles | **partial** | Screen exists; must be driven by ProfileView; Report flow is a single confirm dialog |
| F4 groups and events | **partial** | Request-to-join, People orbit, seats, thread unlock done. Missing host Approve/Decline, leave confirm, host tools, invite friends, event date/time/topic in Create event, leave notifies host |
| F5 safety and states | **partial** | Blocked/Muted lists done. Missing report flow, Alerts tap targets + Mark all read, unread dot logic, empty/offline/error component + skeletons |
| Getting in (A&L 1-4) | **missing** | No Welcome, phone, code, profile setup, age, invite sheet, Membership, Your data, legal tabs |
| Strings in resources (A&L 6) | **missing** | Everything hard-coded |
| Scaling (A&L 5) | **partial** | OrbitMap scales to width; no clamp/list fallback, no screenshot tests |
| Persistence (F6) | **missing** | |
| BACKEND_READINESS.md | **partial** | Started (privacy section); needs the AccessPolicy table and the full gate contents |

### Plan notes
- MASTER 2.1.3 requires that nothing in `ui/` imports the store, and that this is checked by a test, **before** Phase 3.
  So the repository interface, the fake repository and the screen migration land in Phase 2; Phase 3 then finishes
  the layer (Instant timestamps, Outcome on every network-shaped call, MediaStore) instead of redoing screens.
- Compose UI tests and the later screenshot tests run on the JVM with **Robolectric** so `./gradlew build` runs them
  without an emulator (reason recorded under Decisions when added).

### Decisions
- (Phase 0) None yet.

---

## Phase 1: First-launch fix (ACCESS_AND_LAUNCH 0.1)

**Works now**
- The start screen is decided by `data/session/LaunchFlow` (pure Kotlin) from two local facts kept in DataStore
  (`SessionStore`): has a session, and onboardingSeen. Logged in: straight to Home. First launch: Welcome. Logged out after
  onboarding: Welcome with **Log in only**. The 4-slide story appears exactly once, after Create account > Find friends.
  Log in never shows it; Log out and Delete account return to Welcome.
- The system splash (core-splashscreen) stays only while the session is read; nothing else is shown before Home.
- New **Welcome** screen per the getting-in prototype (wordmark over two rings, tagline, Create account, Log in, Terms link).
  Its text is the first to live in `strings.xml`.
- Tests: `LaunchFlowTest` (the five cases + a walk-through), `FirstLaunchUiTest` (Robolectric: logged in shows Home and no
  story; first launch shows Create account; logged out shows Log in only). Verified on the Pixel 7 emulator: create account >
  find friends > story > Home; force-stop and reopen: Home; log out: Welcome without Create account; log in: Home, no story.

**Stubs:** Create account and Log in skip the phone, code, profile and age steps (Phase 9 inserts them). Sessions have no
token yet (the fake has none).

**Decisions**
- Compose BOM 2024.06 -> 2024.09.03 (Compose 1.7): lifecycle 2.8's `collectAsStateWithLifecycle` needs Compose UI 1.7's
  `LocalLifecycleOwner`; the old pair crashes at runtime.
- New libraries: DataStore preferences (required by the doc), core-splashscreen (keeps the cold start to the system splash),
  lifecycle-viewmodel-compose / runtime-compose (ViewModels, required by FRONTEND_COMPLETE 8), and for tests Robolectric +
  compose ui-test (UI tests run inside `./gradlew build` with no emulator) and coroutines-test.
- Unit tests run on the debug variant only (`enableUnitTest = false` for release): the Compose test activity exists only
  in the debug manifest.
- A small hand-written `AppGraph` instead of a DI library (a handful of objects; tests swap them).
- Case 3 taken literally: after the story was seen on a device, Welcome offers Log in only (Create account is hidden).
  Deleting the account also lands there. Flag if the owner wants Create account back after a deletion.

---

## Phase 2: Privacy core (MASTER_PROMPT section 2)

**Works now**
- **One place decides.** `data/access/AccessPolicy.kt` is pure Kotlin, deny by default, and answers every question from one
  `Relationship` (SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME) plus the note's facts
  (audience, whether the viewer is in the author's inner circle, group membership).
- **The UI never receives data it may not show.** `CircleRepository` / `FakeCircleRepository` (over the store, which is now
  plain data: the fake "server") asks the policy before returning anything and returns redacted types
  (`ProfileView.Full/Limited/NotFound`, `NoteView.Full/Snippet/Hidden`, `ThreadResult.Opened/Denied(reason)`, plus
  `PersonRef`, `ChatView`, `EventView`, `GroupView`...). `Limited` has no field for anything but photo, name, handle and
  pinned thought. Every action (comment, like, repost, quote, share, message, seat, post) returns `Outcome.Denied(reason)`
  when the rules say no.
- **Every screen** has a ViewModel over the repository; nothing in `ui/`, `navigation/` or `MainActivity` touches the store
  (`PrivacyBoundaryTest` fails the build otherwise, and also on any logging call or a mutual-friend count).
- **Re-evaluation:** observations are Compose snapshot flows, so a relationship change re-renders every open screen. Unfriend
  and block purge the client `ContentCache` (opened threads, notes, snippets by that person); an open thread turns into the
  gate. Accepting a request opens the same card and thread with nothing else changing.
- **Deep links** (`circle://app/thread/{id}`, `circle://app/person/{id}`) use the same routes and ViewModels, so a note you
  can't open shows the gate, never the thread.
- Mutual-friend numbers are gone from profiles, request rows, search, seats and group join requests.
- Alerts are built from typed seed records; two prototype rows were dropped because they broke the rules
  ("Marco and Priya are now friends" revealed someone else's friends; "Priya liked Jonah's post" revealed a like).
- Times are `Instant`s shown in the viewer's zone and locale; seed events always fall in the coming week. New ids are UUIDs.
- Tests: `AccessPolicyTableTest` (every relationship x resource x action with hand-written expectations: 224 note cells, group
  cells, profile, friend graph, DMs, shared card, sharing, events, groups, snippet length), `FakeCircleRepositoryTest`
  (section 2.4 repository cases plus all the old store behaviour), `FlowReactivityTest`, `PrivacyBoundaryTest`, and
  `PrivacyScenariosUiTest` (Robolectric, on the real screens): the six owner scenarios (scenario 4 as search, group People tab
  and event seat), each asserting the restricted text is absent.
- `PRIVACY_RULES.md` (plain-language table for the backend) and the BACKEND_READINESS privacy section rewritten around it.
- Verified on the Pixel 7 emulator: Elena's shared card is a snippet with a lock, tapping it opens the gate sheet; tapping her
  in the group chat opens the Limited profile with Add friend, no Message, no notes, no count.

**Still a stub:** the demo has no trigger for someone accepting *your* request (tests use `acceptedByThem`; a backend event does
this). Group and event screens keep their earlier design until Phase 7.

**Decisions**
- Phase 3's repository, ViewModels, Outcome type, string ids and Instant timestamps landed here: section 2.1.3 requires that
  nothing in `ui/` imports the store before Phase 3, so every screen had to move to the repository now. Phase 3 keeps
  MediaStore and the fail switch.
- Snapshot flows over the existing Compose-state store, rather than a new reactive store: every observation re-evaluates when
  anything it read changes, which is exactly "every screen re-evaluates on change", with no extra code.
- A note embedded in another card (a quote, a repost of a note you can no longer see) follows the shared-card rule: full if you
  may open it, else a snippet, else hidden. Tapping any embedded note goes to the thread route, which shows the gate if needed.
- Group posts: members can view, comment and like whatever their friendship with the author ("by membership, never by
  friendship alone"); blocks still hide the author.
- Inner-circle notes: only the author shares them, and only into chats whose members are all in the author's inner circle (a
  friend passing one on would make the server reveal who else is in it).
- The snippet card shows no photo placeholder at all: the spec's "blurred placeholder" would itself reveal that there is a
  photo, and `Snippet` has no photo field. Flag if the owner wants the placeholder back.
- "Seen" shows only while your own read receipts are on (mutual, as the settings text promises).
- A friend's `Full` profile has no counts or friends fields; your own counts come only from `circleSummary()`.
- The UI test for scenario 5 uses the sheet button's click action rather than a tap, because Robolectric can tap the sheet's
  scrim while it is still animating in (a test artefact, not an app bug).

---

## Phase 3: Repository + ViewModel layer (FRONTEND_COMPLETE 8)

**Works now** (most of this phase landed in Phase 2, see there): `CircleRepository` with Flows and suspend actions,
`FakeCircleRepository`, a ViewModel per screen, `Outcome` (Success / Denied / Error) on every action, string ids (UUIDs for
new records) and `Instant` timestamps. Added now:
- `data/media/MediaStore` with `LocalMediaStore` (keeps picker URIs; refuses videos over 30 seconds). A backend version uploads.
- Every action runs through `RepoViewModel.run`, which reports any failure app-wide (`UiMessages`); the root shows it as a short
  message ("Couldn't do that. Try again.", or the rule that refused it), so no tap fails silently.

**Still a stub:** the debug switch that makes the fake fail (Phase 11) and full-screen loading/error states (Phase 8).

**Decisions:** a hand-written `AppGraph` provides the repository, session and media store (no DI library).

---

## Phase 4: Posts and threads (F1)

**Why Quote was broken** (found and fixed in f2886fb, before this run): the Quote menu item navigated to the plain Compose
route, which took no argument, so the quoted note was dropped; Compose had no real text field and Post did nothing; a quote
was stored as an author/text string pair with no link to the original; Repost was a per-card `remember {}` flag (lost on
scroll or rotation, never created a card); and Home was hard-coded cards, so new posts could never appear. Now Quote opens
Compose with the original embedded, and both live in the store behind the repository and its rules.

**Works now**
- Thread: each comment has a private Like and Reply. Reply shows a "Replying to Priya" chip above the composer; the reply
  nests under that comment (a reply to a reply joins the same top-level comment, one level deep) and its replies open to show
  it. "View N replies" in accent per the prototype, replies indented with a left rule. Long-press your own comment: Delete
  (confirmed); someone else's: Report.
- ⋯ menu on every card. Others' notes: Send in a message (a friend picker showing only friends the note may go to; an
  inner-circle note only to your inner circle), Mute this friend, Report, Block. Your notes: Edit (Compose in edit mode),
  Change audience (making a note inner-circle removes its reposts), Delete (confirmed; its repost cards go too).
- Photo viewer: full screen, pinch to zoom, double-tap to reset, swipe between items, close; videos play. It loads the note
  through the repository, so a note you can't see can't be opened by route.
- Compose: photos and videos from the system picker (up to 10; a video over 30 seconds is refused with a clear message),
  removable previews that survive rotation, Post enabled with text or media, edit mode with "Discard your changes?".
- Home: pull to refresh and the wordmark both reveal the header, check, and say "Nothing to refresh. You're caught up." when
  nothing is new; a "New" pill appears if newer notes arrive while you are scrolled down (your place is kept); the feed ends
  with "You're all caught up." (Fraunces 19) and "Nothing below this line."
- Deep links now also work while the app is already open (found on the device: the intent reached the running activity and
  was ignored).
- Tests: `PostsAndThreadsTest` (reply nesting, comment likes, deleting only your own comments, edit/audience/delete only on
  your notes, reposts removed when a note becomes inner-circle, share candidates, refused shares create no chat, media).
  Verified on the emulator: reply chip, nested replies, ⋯ menus, deep link into a thread and into a gated thread.

**Stubs:** the fake keeps picked media as device URIs (no upload); seed photo posts show a placeholder. Report is a simple
confirm until the reason-list flow in Phase 8.

**Decisions**
- Group posts get Report / Block only in the ⋯ menu (no "Send in a message": they stay in the group).
- "Mute this friend" appears on personal notes only (their authors are your friends by the rules); group-post authors may not be.

---

## Phase 5: Profiles and the friend graph (F2)

**Works now** (most of F2 landed with the privacy core): one profile screen rendered from `ProfileView`; friends get
Message, ✓ Friends, their notes (inner-circle ones only if you're in their inner circle) and Mute / Unfriend / Block /
Report; everyone else gets the Limited profile with Add friend / Requested + Cancel request / Accept + Decline and Report /
Block. Unfriend leaves Your circle and the counts; Block hides the person everywhere. Added now:
- **Report flow** (one `ReportSheet` for people, notes and comments): a reason (spam, harassment, hate, sexual content,
  violence, self-harm, impersonation, something else), an optional note, then "Thanks. We'll look at this." Recorded in the
  store as a typed `Report` (kind, target, reason, note, time). It says only what is true ("Your report is recorded"),
  per ACCESS_AND_LAUNCH 0.2, until moderation exists.
- My Profile's PHOTOS grid shows your own photo notes (tap opens the viewer) instead of six placeholder boxes; it is
  hidden when you have none.

**Decisions**
- The report confirmation does not promise human review: there is no moderation yet (BACKEND_READINESS / LAUNCH_CHECKLIST
  will list it).

**Note:** a scripted edit briefly corrupted ProfileScreen.kt (an empty match inserted text between every character) and hung
the Kotlin compiler; the file was restored from git before the edit was redone. Nothing broken was committed.

---

## Phase 6: Messages and chat (MESSAGES_AND_SETTINGS_V2)

**Works now**
- **Messages list:** "Messages" (Fraunces 26) with the New chat button (42dp accent ring), "Search chats" filtering by chat
  or member name, tabs **Messages | Groups** (renamed everywhere), PINNED then ALL CHATS, 52dp avatars (rounded squares
  for groups), "You:" and "Tomas:" prefixes, Photo / Event invite / Shared a note previews, unread tint + heavier name +
  accent time + 10dp dot, newest first. Swipe a row left to reveal Pin / Mute / Delete (long-press gives the same menu;
  TalkBack gets them as custom actions). Footer, the Groups line, and empty states with one action. No presence or typing.
- **Chat:** ⋯ menu with Mute, Search in chat (filters the conversation), Clear chat (your side only), Block (1:1) and
  Report (opens the report flow). The "+" button opens a sheet with Photo (system picker, several) and Invite to an event
  (events you host or have a seat at). Photo messages are 210 x 140 cards that open the full-screen viewer. A "New
  messages" divider marks the first message after you last read the chat; "Jump to latest" appears when scrolled up.
- **New chat:** a searchable friend picker with checkboxes; one friend opens (or creates) the 1:1, several ask for an
  optional name and create a chat in Messages. Non-friends are refused by the repository.
- Tests: `ChatsTest` (new chats, friends only, pin/mute/clear/delete on your side only, photo and invite messages,
  new-messages marker, ordering). Verified on the emulator: list layout, swipe actions, chat menu, "+" sheet, new chat picker.

**Stubs:** "Sending…" / "Not sent · tap to retry" need a network that can fail (Phase 11 fail switch). Muting a chat is
recorded but there are no push notifications yet. Photos stay as device URIs.

**Decisions**
- The Messages footer says "Only friends can message you directly." (ACCESS_AND_LAUNCH 0.2 wording, because group chats can
  include non-friends) instead of V2's "Nobody else can message you."
- Chats sort newest first; pinned ones sit in their own section.
- A chat started with several friends and no name is titled by first names ("Priya, Jonah").
- Report from a 1:1 reports the person; from a group chat, the group.
- Found on the device and fixed: the swipe actions showed through the unread row's translucent tint.

---

## Phase 7: Groups and events (F4)

**Works now**
- **Groups:** creating a group makes you host and member, opens it, lists it in Explore when public, and gives it a linked
  chat under Messages > Groups. The ⋯ menu offers Edit group (name, description, public/private) to hosts, Invite friends and
  Leave group (confirmed) to members, and Report group to non-hosts. In the People tab, **hosts only** get Approve / Decline on
  each join request (approving moves the person onto the members list) and Remove on members (hosts can't be removed).
  Ordinary members still see the requests, never the buttons.
- **Events:** Create event has real date and time pickers (in the palette), a topic (Outdoors / Social / Music / Making) and a
  venue; it refuses a time in the past. An event within 7 days sits on the Explore map in its topic's quarter at a free angle;
  later ones are in the list. The ⋯ menu offers Edit event and Cancel event (confirmed; everyone with a seat gets a notice) to
  the host, and Invite friends, Report event, Block host to everyone else. Leaving a seat locks the thread again and records a
  quiet notice to the host. The unlocked thread is a real thread: like, reply with the "Replying to" chip, nested replies,
  delete your own, report others'.
- Invites (groups and events) go to friends only and are recorded; with a backend they land in the friends' Alerts.
- Material containers (date and time pickers, chips) now use palette colours instead of Material purple.
- Tests: `GroupsAndEventsTest` (creator is host with a chat, only hosts approve/decline, edit/remove rules, leaving, invites to
  friends only, map placement by topic and time, past times refused, edit/cancel by host only, leave notifies host, event
  thread replies/likes/delete). Verified on the emulator: create-event form, date and time pickers.

**Stubs:** invites and host notices are recorded but delivered only with a backend. Events have no time-zone field yet (times
are shown in the viewer's zone).

**Decisions**
- Group join requests and their Approve/Decline live in the People tab's "Asked to join" list (also under the map view).
- A cancelled event is removed (not shown as "cancelled") because nothing in the documents describes a cancelled state.

---

## Phase 8: Settings, safety, states (F5)

**Works now**
- **Notifications** is one Settings row ("5 of 6 on", or "Paused") that opens its own screen: the helper line, six switches
  with the prototype's descriptions, Pause all (Off / 1 hour / Until tomorrow) and quiet hours (on/off, tap the times for
  pickers). The switches decide which quiet alerts appear (likes off hides like alerts; friend requests off stops them
  lighting the dot).
- **About Circle** and **Why I built it** use the SETTINGS_AND_POLISH text in the V2 tinted cards (accent 8% fill, 30% border,
  18dp radius; accent dots, Fraunces, the accent full stop, the large opening quote, the rule and the signature). No
  gradients or motion.
- **Alerts:** "Mark all read" in the header; the bottom bar's red dot shows only while something is unread; unread rows have
  a faint tint; tapping a row opens its thread (or the person).
- **States:** one `EmptyState` (120dp ring with a dashed outer ring and accent dot, Fraunces title, muted body, one action)
  with the nine kinds and copy of ACCESS_AND_LAUNCH 3, used on Home (no friends; Groups tab), Messages, Explore, Alerts,
  Your circle (no inner circle: "Choose people" opens the list), Profile (no notes). Loading skeletons (a calm pulse, still
  when animations are removed), a live offline banner (system connectivity), and an error state with Try again on Home,
  Messages and Alerts through a `Load` wrapper that re-subscribes.
- The report flow landed in Phase 5. Membership's sub-label is now "Keeps Circle running".
- Tests: `AlertsTest` (dot and Mark all read, switches hide alerts, quiet hours stored). Verified on the emulator:
  Notifications screen, About Circle, Why I built it.

**Stubs:** no push notifications yet, so Pause and quiet hours only shape in-app alerts. The error state appears only when the
fake fails (Phase 11 adds the switch to make it fail).

**Decisions**
- "Why I built it" drops "No ads," from "No ads, no tricks to keep me there" (ACCESS_AND_LAUNCH 0.3 allows "no ads" only in About,
  Membership's small print, the privacy summary and the onboarding pricing slide). The rest of the owner's text is unchanged.
- The Home empty state's outlined "Invite a friend" comes with the invite sheet in Phase 9.
- New permission: ACCESS_NETWORK_STATE, only for the offline banner.

---

## Phase 9: Getting in (ACCESS_AND_LAUNCH 1-4)

**Works now**
- **Create account:** Welcome > Your number > Enter the code > Set up your profile > Age and agreement > Find your friends >
  the story > the **empty** Home (a new account has no friends, chats, groups or demo feed; people already on Circle can be
  found). **Log in:** Welcome > Your number > Enter the code > Home (the demo account), never the story. Every step has a back
  button; system back goes one step. Verified end to end on the emulator.
- **Your number:** the code is typed (prefilled from the SIM, then the network, then the device region), the country it
  belongs to is shown in accent ("India"; "United States / Canada and 23 more" for +1), an unknown code says so, "Choose from a
  list" opens every country, searchable by name or code. libphonenumber validates per country and the number is kept as E.164;
  spaces, dashes and a leading zero are fine. Send code is enabled only for a valid number.
- **Enter the code:** six boxes over one hidden numeric field; verifies at six digits; "Send a new code" after a 30-second
  countdown; a wrong code shakes the boxes once and says "That code isn't right."; five wrong tries lock for five minutes (any
  six digits work in the fake, so the lock logic is unit-tested in `CodeGate`).
- **Profile:** photo from the system picker, name (30), handle (lowercase letters, numbers, dots, underscores; 3-20) with the
  live messages and a repository availability check after a 400ms pause, pinned thought (140 with counter). Continue only when
  the name is set and the handle is valid and free.
- **Age:** DD / MM / YYYY, 18+ by the real calendar (18 today is fine, 18 tomorrow isn't), the refusal line, the agreement
  checkbox and "Read the short version". The date isn't stored or sent.
- **Find your friends:** the new copy; READ_CONTACTS is asked only on "Connect contacts"; numbers are normalised and SHA-256
  hashed on the phone and only hashes reach the repository; nothing is kept or logged.
- **Invite sheet** (empty Home, Settings): the link with Copy ("Link copied"), Messages / WhatsApp / Mail / More (real
  intents; More is the system share sheet), FROM YOUR CONTACTS with Invite opening your SMS app (you press send; it then shows
  "Invited ✓"), and a count of shares shown only to you. **App Link:** `https://circle.app/i/{handle}` (placeholder domain,
  `autoVerify`) opens that person's Limited profile with Add friend; `design-handoff/assetlinks.json` is the template for the site.
- **Membership:** plan cards with the price strings from a `MembershipBilling` interface (placeholder €1 / €12 from a fake
  "store"), what it pays for, the CTA following the plan, the small print, then "You're a member." and Manage in Google Play.
- **Your data:** Request a download (recorded in an audit log the user sees); DANGER ZONE > Delete my account, which needs
  DELETE typed (any case), then "Your account is deleted." and Back to the start (Welcome).
- **Terms / Privacy / Company:** three tabs, a placeholder notice, the prototype's short-version bullets, the provider block.
- **CLAIMS.md** lists every promise with what makes it true now and what the backend must keep true.
- **"No ads" (0.3):** now only in About (last point), Membership small print, the Privacy short version, and one sentence in the
  story's pricing slide (whose "If the app is free, you're the product", "Nothing about you to sell" and hard-coded "€1" were
  removed; the slide now says "Circle is paid for by the people who use it." / "A small membership keeps it running, in your own
  currency" / "No ads.").
- Tests: `SignupRulesTest` (codes to countries, E.164 validation, search, hashing, age by calendar incl. 29 Feb, handles, code
  gate) and `GettingInTest` (codes, taken handles, new account starts empty, log in, invite link to Limited profile, contact
  matching, export log).

**Stubs (need the backend or accounts):** real SMS sending and verification, SMS one-time-code autofill (needs the SMS
Retriever hash from the server), email sign-in ("coming soon" toast as the doc asks), Play Billing products, preparing and
delivering the data export, server-side contact matching, deferred App Link after install (Play Install Referrer), the real
invite domain and its assetlinks.json.

**Decisions**
- A 29 February birthday counts from 1 March in non-leap years (the later, safer reading). Confirm with the lawyer.
- New dependency: libphonenumber (the document asks for it: real country list, validation, E.164).
- New permission: READ_CONTACTS, requested only on "Connect contacts" or "Show my contacts here", after the explanation.
- Log in replaces local state with the demo account; Create account starts a fresh, empty one.

---

## Phase 10: Scaling, strings, accessibility, device pass (ACCESS_AND_LAUNCH 5-7)

**Works now**
- **Strings:** every user-visible text in `ui/` comes from `res/values/strings.xml` or `plurals.xml` (about 230 strings moved,
  counts as plurals: seats left, empty seats, members, "and N more", replies; format arguments instead of concatenation, e.g.
  "Hosted by %1$s · %2$s", "%1$d of %2$d going"). Enum display names (theme, who can send requests/invite, Explore sectors,
  event visibility) moved out of the data layer into `ui/format/EnumText.kt`. Lint `HardcodedText` is an **error**; because lint
  only checks XML, `HardcodedTextTest` fails the build on any Compose literal with words in Text / contentDescription /
  onClickLabel / description / placeholder / title / subtitle. Pseudolocales (`en-XA`, `ar-XB`) are on in debug.
- **Orbit scaling:** `orbitFit()` (pure, unit-tested): scale = width / 390, clamped to 0.8..1.25; under 0.8 the screen shows its
  list view and hides the Map | List switch; above 1.25 the orbit stops growing and is centred. Geometry **and** node visuals
  scale; the touch target stays at least 48dp. Text inside an orbit grows with the system font only up to 1.3x (so labels never
  overlap nodes); everything around it (cards, lists, headers) reflows at any size.
- **Large font fixes found by the screenshots:** Explore's legend no longer sits under the Map | List switch (it overlapped even
  at 320dp / 100%); sector labels have room for "OUTDOORS"; the "Explore" title shrinks instead of breaking mid-word
  (`FitText`); the event's visibility tag moved above the title (the title was one letter per line at 200%); the group's
  Member / Leave row shares the width; at large font the group's Map | List switch drops under the tabs; Profile's
  "128 friends · 5 inner circle" wraps so "See your circle" always shows; Compose in landscape with the keyboard up scrolls as
  one piece instead of collapsing the text field.
- **Screenshot tests:** `OrbitScreenshotTest` (Roborazzi 1.13, Robolectric native graphics) renders Explore, Your circle,
  Group people and Event table at 320/360/390/430dp x night/day x font 1.0/2.0 = 64 images in `app/src/test/screenshots`
  (`./gradlew recordRoborazziDebug` to refresh; a normal `build` renders them all and fails on a crash).
- **48dp audit:** `TouchTargetTest` walks 14 screens and checks every clickable node's touch area is at least 48x48dp
  (passes: small visuals get their touch area extended).

**Device pass** (emulator, 411dp phone, API 34; the 320/360/430dp widths are covered by the screenshot tests)

| Screen | Test | Result |
|---|---|---|
| Home, Profile, Explore | Font 1.3 and 2.0 | Pass after fixes (Profile friends row, Explore legend/title) |
| Explore, Your circle, Group people, Event table | 320-430dp, both themes, font 1.0/2.0 (screenshots) | Pass after fixes |
| Compose | Rotation with text typed | Pass (text kept); landscape + keyboard fixed (field was collapsed) |
| Compose | Process death (home, `am kill`, relaunch) | Pass (draft and screen restored) |
| Settings | Theme Night -> Day -> Night while running | Pass (applies at once) |
| Settings, Home | Back button | Pass |
| Home | Right to left (`ar-XB`) | Pass (mirrored, icons and nav order flip) |
| Home | Long text (`en-XA`) | Pass (no clipping) |
| Orbit screens | Screen reader on | Pass (list view by default; Phase 2 tests) |
| Orbit screens | Remove animations | Pass (reduce motion honoured; earlier phases) |
| All | Crash log after the pass | Clean (`AndroidRuntime:E` empty) |

Not done on a device: a real tablet or foldable (the orbit cap at 1.25 is covered by a unit test only), the Accessibility
Scanner app (not installed on the emulator), TalkBack itself (not on this emulator image; labels and the list default are
covered by the UI tests).

**Decisions**
- Text inside orbits stops growing at 1.3x the default size (the doc allows orbit text to clip; capping it keeps it legible
  without overlap). Lists and cards reflow at 200%.
- Demo-only text that stands for user content (seed posts, the Explore search sample captions, the "Aachen" city) stays as data,
  not strings; the city picker is a stub until location exists.
- New test-only dependency: Roborazzi 1.13.0 (the doc names Paparazzi or Roborazzi; Roborazzi reuses the Robolectric setup).


---

## Phase 11: Persistence (FRONTEND_COMPLETE F6)

**Works now**
- **State survives restarts.** `data/persist/StorePersistence` writes the whole fake server (`CircleStore`) as one JSON file
  (`files/circle-demo-store.json`, app-private) 300ms after changes settle (atomic temp-file rename), and loads it in
  `CircleApplication.onCreate` before any screen reads. `StoreSnapshot` is a versioned, plain serializable copy (times as epoch
  ms, enums by name); an unreadable or older file is ignored and the seed loads. Verified on the emulator: a new account and a
  like survive force-stop and relaunch.
- **Reset demo data** (Settings > Demo, confirmed): puts the seed back, purges the content cache, you stay logged in.
- **Debug fail switch** (Settings > Demo, debug builds only): every repository action returns `Outcome.Error("offline")`, and the
  Home feed, Messages list and Alerts throw, so their error state with Try again shows. Chat now shows a sent message as
  "Sending…", then "Not sent · tap to retry" if it fails; tapping retries (the Phase 6 leftover).
- Tests: `PersistenceTest` (round trip of the whole store, broken/old file keeps the seed, reset, fail switch),
  `ChatSendingUiTest` (Robolectric: Not sent, then retry delivers).

**Stubs:** with a backend this file goes away; the server is the source of truth and any local cache must hold only the
redacted views the user may see, purged on unfriend/block (BACKEND_READINESS).

**Decisions**
- One JSON file via kotlinx.serialization instead of Room: the fake server is small and in-memory, and Room would be thrown away
  when the backend arrives. New dependency: kotlinx-serialization-json 1.6.3 (+ the Kotlin serialization plugin 1.9.24).
- Robolectric tests never load or save the file (each test starts from the seed).
- The Demo section is hidden if the repository is not the fake (`DemoControls`), so it cannot ship with a backend.
