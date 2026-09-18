# Circle — Android starter project

A real Jetpack Compose scaffold of the Circle app, matching the screens and
interactions worked out in the design mockups: Onboarding → Contacts Sync →
Home (Friends/Groups feed switcher, press-and-hold-to-rotate on the Home
icon, working refresh) → Explore (browse + People/Events/Groups/Posts
search) → Notifications (Notifications/Activity split) → Messages
(Messages/Group Messages split) → Profile → Group & Event detail (each with
a "PREVIEW AS" demo toggle) → Create Group / Create Event.

## Opening it

1. Android Studio → **File → Open** → select the `CircleApp` folder.
2. Let Gradle sync. **This project's `gradle-wrapper.jar` binary was not
   included** (this environment has no network access to
   `services.gradle.org` to fetch it) — Android Studio will offer to
   regenerate it automatically on first sync. If it doesn't, run
   **File → Sync Project with Gradle Files**, or right-click `build.gradle.kts`
   → *Reload*.
3. Android Studio will very likely prompt you to upgrade the Android Gradle
   Plugin / Kotlin / Gradle versions on first open — accept those. The
   versions pinned here (AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7, Compose BOM
   2024.06.00) were current as of when this was generated; your installed
   Studio may suggest newer ones, which is fine to accept.
4. Run on an emulator or device — `minSdk 26`.

## What's real vs. simplified

This is a genuine, navigable Compose app — not a static mockup. Tapping
around actually moves between screens via Navigation Compose, and the
signature interactions are implemented for real:
- Home's Friends/Groups tabs and the press-and-hold-to-rotate Home icon
- The tap-to-refresh "checking → caught up" sequence, with a real
  scroll-to-top
- Explore's browse vs. search mode, with the People/Events/Groups/Posts tabs
- Notifications vs. Activity, Messages vs. Group Messages
- The Group/Event "PREVIEW AS" toggle (member vs. not-joined / joined vs.
  not-joined) — that toggle itself is a demo convenience, not a real app
  feature; it exists so you can see both states without a second account

Simplified vs. the HTML mockups, to keep this a reasonable starting point
rather than a multi-week build:
- **Fonts**: using system serif/default instead of actual Fraunces/Public
  Sans. To add the real fonts: download the `.ttf` files, drop them in
  `app/src/main/res/font/`, and point `Display`/`Body` in
  `ui/theme/Type.kt` at them via `FontFamily(Font(R.font.fraunces_semibold, ...))`.
- **Reaction long-press / repost-quote menu**: the HTML mockups had a
  press-and-hold reaction picker (heart/laugh/cry) and a repost-vs-quote
  popup menu on post cards. Those aren't wired up here yet — the icons are
  present but inert. Worth adding once the core navigation feels right.
- No backend, no persistence, no real accounts — everything is hardcoded
  sample data, same as the HTML mockups.
- No launcher icon assets beyond a minimal generated vector — swap
  `ic_launcher_foreground.xml` / `ic_launcher_background.xml` for real art
  whenever you have it.

## A note on how this was built

I generated this in a sandboxed environment with no Android SDK and no
network access to Google's Maven repository, so **I could not actually run
a Gradle build to verify it compiles clean**. I've written it carefully and
reused consistent patterns throughout, but there's a real chance Android
Studio's first sync surfaces a handful of small errors (an unresolved
import, a parameter name that shifted between library versions, etc.) —
normal for a project's first open, and usually one-click fixes.

This is exactly where your Claude Code session (the one in PowerShell) is
the better tool going forward: it can actually run `./gradlew build`, see
real compiler output, and iterate against it directly — much faster than
me guessing blind. I'd suggest opening this folder there next and asking
it to get a clean build, then keep building features with it from here.

## Project layout

```
app/src/main/java/com/circle/app/
  MainActivity.kt
  navigation/CircleNavGraph.kt       — all routes, one NavHost
  ui/theme/                          — Color.kt, Type.kt, Theme.kt
  ui/components/
    BottomNavBar.kt                  — shared 5-icon nav + press-hold logic
    CommonComponents.kt              — Avatar, GroupTile, UnderlineTabs, cards, buttons
  ui/screens/                        — one file per screen, matches the mockup names
```
