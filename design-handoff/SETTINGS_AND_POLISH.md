# Circle: settings, about, quote fix, and two pieces of polish

Read this together with BUILD_BRIEF.md and FRONTEND_COMPLETE.md. It adds to them. Do the items in this order, building and
committing after each, then carry on with the Phase F stages.

## 0. Quote is broken: find out why and fix it (do this first)

The owner reports that tapping **Quote** doesn't work properly. Reproduce it, find the real cause, tell the owner what it
was, then fix it. Quote and Repost are part of Phase F1 in FRONTEND_COMPLETE.md section 3, so build them completely now:

- Quote opens Compose with the original note shown as a small embedded card under the text box (removable). Posting
  creates a quote card at the top of Home and Profile: your text, then the embedded original.
- Repost adds a "You reposted" card at the top of Home and Profile. The menu then shows **Undo repost**.
- Neither is offered on inner-circle notes (show "Inner circle notes can't be reposted") or on group posts.
- Tapping the embedded original opens its thread.
- Both survive rotation. Both update the store, not local `remember` state.

## 1. The repost menu animation

Today the Repost/Quote box appears abruptly and looks flat. Replace it with a proper popover:

- It is **anchored to the repost icon** and opens **from that point**: scale from 0.85 to 1 with a fade, using a spring
  (damping ratio about 0.7, medium stiffness, roughly 220ms). Closing is a quick fade and shrink (about 120ms).
- Card: surface fill, 14dp radius, 1dp border, soft shadow (same as other cards), 8dp padding. Two rows, each at least
  48dp high: icon + label, the label in 14sp. The rows enter 30ms apart so they feel like they cascade in.
- While the menu is open the repost icon turns accent and rotates 180 degrees. When it closes it returns.
- A very light scrim (about 8% of the text color) over the rest of the screen. Tapping the scrim closes the menu.
- A short haptic tick when it opens. Skip all of the motion when "remove animations" is on (just show it).
- It opens on **tap or long-press** of the icon. Rows show **Repost / Quote**, or **Undo repost** if already reposted,
  or a disabled row with the reason (see section 0).

## 2. Header that hides when you scroll

On **Home**, the header (date line, "Circle." wordmark, Friends | Groups tabs) must move away as the feed scrolls
and come back when you scroll up:

- Scrolling **down**: the header slides up and fades out, tied directly to the scroll distance (a collapse fraction
  from 0 to 1). The content moves up to use the space.
- Scrolling **up** by any amount: it comes back straight away (enter-always behaviour), sliding down and fading in.
- If you let go part-way, it snaps to fully shown or fully hidden with a short animation.
- Tapping the wordmark scrolls to the top and reveals the header (keep that behaviour).
- The **bottom bar stays**. Use a `NestedScrollConnection` (or `enterAlwaysScrollBehavior`) rather than a listener that
  recomposes on every pixel. Apply the same behaviour to **Messages** and **Alerts**, which have the same kind of header.
  Explore, Profile and detail screens don't need it.

## 3. Settings

**Where:** on Profile, replace the "Edit profile" pill with a **gear icon button** in the same spot (top right of the
header). It opens **Settings**. "Edit profile" moves inside Settings. There is no other Edit profile button.

**Screen:** a clean list of rows grouped under small caps headings (`muted`, 0.14em tracking), rows at least 56dp high with
a chevron, toggles where the choice is on/off, a segmented control where there are three or fewer options. Use Fraunces for
the screen title only.

**ACCOUNT**
- Edit profile: opens an edit screen with display name, handle, avatar photo (system picker) and **pinned thought** (the pinned
  thought is the bio). Save writes to the store and returns.
- Your circle: shortcut to the Your circle screen.
- Blocked users: list with Unblock. Muted friends: list with Unmute.

**PRIVACY**
- "Let people find me by name or username": toggle, on by default. People who find you see photo and pinned thought only.
- "Let friends find me from their phone contacts": toggle, on by default.
- "Who can send me friend requests": Everyone / Friends of friends.
- "Who can invite me to events and groups": Friends / Friends of friends.
- "Read receipts in messages": toggle, on.
- A short fixed box titled **Always private** (not switches, just statements): "Your friends list and counts. Your likes.
  Who is in your inner circle."
- "Download my data" (placeholder that says it will be added with the backend).

**NOTIFICATIONS**
- Toggles: Likes, Comments and replies, Friend requests, Event updates, Group activity, Messages.
- "Pause all": Off / 1 hour / Until tomorrow.

**APPEARANCE**
- Theme: System / Night / Day (segmented). Applies immediately. Follows the system by default.

**ABOUT**
- About Circle (section 4 below)
- Why I built it (section 4 below)
- Membership: sub-label "Keeps Circle running" (placeholder row; the real payment is later)
- Privacy policy, Terms (placeholder screens), Open-source licenses (Fraunces and Public Sans are SIL OFL)
- Version number

**At the bottom:** Log out (returns to onboarding) and, in a muted warning style, Delete account (confirmation that says
exactly what is removed).

All settings write to the store and persist (Phase F6). Every row must do something or be clearly marked "coming with the
backend".

## 4. About copy (use this text; the owner may edit it later)

### About Circle  (screen title: "About Circle")

**A small place for the people you actually know.**

How it's different (in this order; connection first, ads last and short):

- **Just your friends.** You ask, they accept. No followers, and no strangers in your feed.
- **See what they're up to.** Newest first, in the order it happened. Nothing is ranked or pushed at you.
- **It ends.** Circle tells you when you're caught up, so you can close it and go and see them.
- **Quiet likes.** Only the person you liked sees it. Nobody is performing for a score.
- **Made for real life.** Events and groups take you out of the house and back to real people.
- **No ads.** Circle runs on a small membership, and your data is never sold.

**Why "Circle"?** It's your circle of friends. And a circle closes: when you've seen everyone's day, the circle is closed,
and you're done. (That's why the name ends with a full stop.)

### Why I built it  (screen title: "Why I built it")

I built Circle because the apps I used to love for keeping up with friends turned into endless scrolling. I wanted a
place where I open the app, see what my friends are up to, and close it again. No ads, no tricks to keep me there.

If you want a bigger circle, make an event, go outside and meet someone. Then come back and tell your friends about it.

Thank you for being here.

Shashank

*(Draft in the owner's voice, written from what he told the designer about the app. He should edit it before release.)*

## 5. Done when

- Quote opens Compose with the embedded original and posts a quote card. Repost and Undo repost work. Neither is offered
  on inner-circle notes or group posts.
- The repost menu opens from the icon with the spring animation and closes smoothly.
- The Home header hides on scroll down and returns on scroll up; Messages and Alerts do the same.
- Profile has a gear; Settings opens with every group above; Edit profile works from inside it; the theme switch works
  live; About Circle and Why I built it show the text above.
- Build, tests and lint pass; commit with a clear message; report what changed.
