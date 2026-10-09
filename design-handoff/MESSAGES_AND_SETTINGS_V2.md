# Circle: Messages, chat, notification settings, About highlight

Read with BUILD_BRIEF.md, FRONTEND_COMPLETE.md and SETTINGS_AND_POLISH.md. **Reference prototype:**
`reference/prototype-messages-settings-night.dc.html` and `-day.dc.html` (open in a browser; the numbers in them are the
source of truth, 1px = 1dp). Do the items in order; build, test and commit after each.

## 1. Messages list (replaces the current plain list)

- **Header:** "Messages" in Fraunces 26sp semibold, and on the right a **New chat** button: 42dp circle, 1.5dp accent border,
  accent fill at 8%, accent "+" icon. It opens the new-chat flow (section 3).
- **Search pill** under it: "Search chats" (surface, 1dp border, 12dp radius). Filters the list as you type.
- **Tabs: "Messages" and "Groups".** Rename "Group Messages" to **Groups** everywhere (tab, empty states, strings).
  Same underline style as elsewhere, 13.5sp semibold, one line.
- **Row:** 52dp avatar (circle for people, rounded square 17dp for groups), name 15sp bold, last-message preview 13sp muted
  on one line with ellipsis, time on the right (11.5sp). **Your own last message starts with "You: ".** In group chats
  the sender's first name starts the preview ("Tomas: ..."). Photo, event and shared-note messages show "Photo",
  "Event invite", "Shared a note".
- **Unread:** the row gets a faint accent tint (accent at 8%), the name is heavier (800), the preview is full-strength
  text and semibold, the time turns accent, and a 10dp accent dot sits under the time. Opening the chat clears it.
- **Pinned:** pinned chats sit first under a small caps heading **PINNED** (10.5sp bold, muted, 0.14em tracking) with a
  small pin icon after the name. The rest sit under **ALL CHATS**.
- **Swipe a row (Compose swipe-to-reveal):** left reveals **Pin / Mute / Delete**. Long-press does the same as a menu.
- **Footer on the Messages tab:** "Only people who are your friends. Nobody else can message you."
- **Groups tab:** a muted line "Chats for the groups you've joined. They appear when you join, not when you ask."
  Only groups you are a member of. Leaving a group removes its chat from the list.
- **No presence and no typing indicators.** Circle is calm: no "online now", no "typing...".
- **Empty state:** "No chats yet." with one button "Start a chat".

## 2. The chat screen

**Top bar:** back, 40dp avatar (rounded square 12dp for groups), name (15.5sp bold) over a subline (11.5sp muted): "Friends
since 2023" for people, "128 members" for groups. Tapping the avatar/name opens that friend's profile or the group.
A ⋯ menu: *Mute*, *Search in chat*, *Clear chat*, *Block*, *Report*.

**Messages**
- Newest at the bottom; the list opens scrolled to the newest message. A small "TODAY" / "YESTERDAY" pill separates days.
- **Bubbles:** mine = accent fill with on-accent text, right-aligned; theirs = surface fill with 1dp border, left-aligned.
  Radius 18dp; in a run of messages from the same person only the **last** bubble has the small 5dp corner. Max width
  276dp, text 14.5sp / 1.4. A run of messages from one person has 3dp between bubbles and 10dp between runs.
- **Time and status** (10.5sp muted) appear under the **last** message of each run only. Your last message shows
  "now · Sent", and "Seen 09:20" **only if the other person has read receipts on** (Settings > Privacy). Show "Sending..."
  and "Not sent · tap to retry" for failures.
- **Group chats:** the sender's first name (11sp bold muted) appears above the first bubble of each run.
- **Gestures on a message (this replaces any tap-to-show-buttons idea; tapping a plain text message does nothing):**
  - **Swipe right** on any message (hold and drag) to **reply**. The bubble follows the finger up to about 72dp with a small
    reply icon revealing behind it; past about 44dp, releasing starts the reply and the bubble springs back. A short drag
    does nothing. Vertical scrolling must keep working (only treat the gesture as a swipe when it is mostly horizontal).
  - A reply shows a quote strip above the composer ("Replying to Priya", the snippet, a close button, an accent left bar),
    and the sent bubble carries a small quoted snippet at its top.
  - **Hold (long-press about 400ms)** on a message to open a small menu: **Copy**, **Delete for me**, and **Unsend**
    (only on your own messages). *Delete for me* removes it **from your chat only**; the other person still has it, and a
    small toast says "Removed from your chat only". *Unsend* removes it for both of you. Tapping elsewhere closes the menu.
- **Rich messages (no bubble, just a card). Swipe-to-reply and hold work on these too, but a plain tap opens the thing:**
  - **Photo:** 210 x 140dp rounded 16dp. Tap opens the full-screen viewer.
  - **Shared note** (from "Send in a message" on a post): card with a "SHARED NOTE" accent caption, a chevron, the author, the
    text and "Open the thread". **Tapping the card opens that post's thread directly** (the same Thread screen as from Home);
    Back returns to the chat. It respects visibility: an inner-circle note can only be shared with someone in the inner circle.
  - **Event invite:** card with a date circle, "EVENT INVITE", title, when. **Tapping it opens the event screen**; Back returns to the chat.
- Scrolling up and new messages arriving shows a "Jump to latest" pill. A "New messages" divider marks the first unread.

**Composer**
- **+** button (42dp, 1.5dp border) opens a bottom sheet (22dp top radius, drag handle) with **Photo** and **Invite to an
  event**. Photo uses the system picker (several allowed). Event invite lets you pick one of your events.
- A rounded text field (42dp high, grows to 4 lines) with the placeholder "Message".
- The **send button** is 42dp: muted/track-coloured and inert while the field is empty, accent when there is text.
- Sending adds the message at once. No fake replies.

## 3. New chat flow

"+" opens a friend picker (searchable list, checkboxes). **One friend** opens (or creates) the 1:1 chat. **Two or more**
asks for an optional chat name, then creates a chat that appears in Messages (not Groups). Groups' chats are made only by
creating or joining a Circle group.

## 4. Notifications moves out of the Settings list

- In **Settings**, the NOTIFICATIONS section is now **one row**: "Notifications", with a summary on the right or below
  ("5 of 6 on"). Tapping it opens a **Notifications screen** (like Edit profile does). Remove the inline toggles from Settings.
- **Notifications screen** (title "Notifications", Fraunces 22):
  - Helper line: "Likes are always private. These only decide which quiet alerts you get."
  - **ALERT ME ABOUT:** six rows, each with a title, a one-line description and a 48 x 28dp switch (on = accent track):
    *Likes*, *Comments and replies*, *Friend requests*, *Event updates*, *Group activity*, *Messages*.
  - **PAUSE ALL:** three pill buttons: Off / 1 hour / Until tomorrow (selected = accent fill).
  - **QUIET HOURS:** a row "22:00 to 07:00" with Off/On; tapping opens time pickers.
  - All of it writes to the settings store and is respected by the alerts the app creates.

## 5. About Circle and Why I built it: same words, subtle highlight

**Do not change the text** (it's in SETTINGS_AND_POLISH.md section 4). Only the styling changes, so people read it:
- Use **tinted cards**: fill = accent at 8% (`accent.copy(alpha = 0.08f)`), 1dp border = accent at 30%, 18dp radius.
- **About Circle:**
  - Hero card: small caps kicker "WHAT IT IS" in accent (10sp bold, 0.18em), then the headline "A small place for the people you
    actually know." in Fraunces 25sp semibold.
  - Under "HOW IT'S DIFFERENT" (muted caps) the six points as rows with a 7dp **accent dot** and the first words in
    bold, the rest in `body`; hairline dividers.
  - A second tinted card "WHY "CIRCLE"?" with the explanation; the **full stop at the end of the name is accent and bold**.
- **Why I built it:** one tinted card with a large accent opening quote mark (Fraunces 64sp, accent at 55%), the note in
  Fraunces 17.5sp / 1.55 in `text` color, a short 36 x 2dp accent rule above the signature, and "Shashank" in Fraunces
  italic 20sp in accent.
- Keep it **subtle**: no gradients, no glow, no animation. Check contrast in both themes.

## 6. Done when

1. Messages shows Pinned and All chats, unread tint and dot, "You:" prefix, Groups tab with the new name, search, "+" button.
2. Opening a chat shows the bubble rules above; swipe-right replies, hold gives Copy / Delete for me (Unsend on mine), tapping a shared note opens its thread, tapping an event invite opens the event; the + sheet adds a photo
   and an event invite; sending works and the Sent/Seen line behaves.
3. New chat creates 1:1 and multi-person chats that show in the list.
4. Settings has one Notifications row; it opens the new Notifications screen with working switches, pause and quiet hours.
5. About Circle and Why I built it have the tinted look in night and day, with the text unchanged.
6. Build, tests and lint pass; commit; report.
