# Circle: getting in, empty states, account, and everything needed to ship

Read with BUILD_BRIEF.md, FRONTEND_COMPLETE.md and the other addenda. **Reference prototype:**
`reference/prototype-getting-in-night.dc.html` / `-day.dc.html` (open in a browser; the "Screens" button jumps anywhere;
1px = 1dp). Items below are in build order. Build, test and commit after each. **Everything marked (placeholder) is design only
and needs a real decision or a lawyer before launch; build it, but keep it easy to change.**

## 0. Two rules that apply to everything below

### 0.1 The story and sign-up appear once, not every time the app opens

Today the intro appears on every launch. Fix that first. Decide the start screen like this, in order:

1. **Has a valid session** (the user is logged in) > go **straight to Home** and show the cached feed immediately. No splash story, no Welcome, no slides.
2. **No session and `onboardingSeen == false`** (first launch ever, or just after creating an account) > Welcome > sign-up. The 4-slide "what Circle is" story shows
   **once, right after the account is created**, then never again automatically.
3. **No session and `onboardingSeen == true`** (the user logged out, or the session expired) > **Welcome with Log in** only. No story.
4. **Log in** (an existing account, including on a new phone or after reinstalling) never shows the story.
5. **Log out** and **Delete account** return to Welcome, never to the story.

Store `onboardingSeen` locally (DataStore) and, once there is a backend, on the account too, so a new device doesn't replay it. The story stays readable any time in
**Settings > About > About Circle** and **Why I built it**. A cold start must not show anything before Home except the system splash (keep it under 1 second).
Add tests for each of the five cases.

### 0.2 Say only what is true, and make it true first

Circle's promises are its identity, so every claim in the app must match what the code and the servers actually do. Use this rule when writing or changing any text:
**promise behaviour you control, describe the mechanism, and don't promise technical guarantees you can't prove.** Specifically:

| Claim | Say | Don't say | What must be true behind it |
|---|---|---|---|
| Storing data | "What we store: ... Circle can't work without it." | "We don't have your data" | Be plain that the servers hold it. |
| Who can read it | "Only the people you've shared it with. Nobody at Circle browses your notes or chats. We look at something only if it's reported or the law requires it, and we record every time." | "We can't see your messages" (unless chats are end-to-end encrypted) | Staff have no routine access; access is role-limited, logged and reviewable. |
| Contacts | "Compares scrambled versions of the numbers in your contacts. Doesn't keep them." | "Compares them on your phone" (matching happens on a server) | Hash on the device, send only hashes, delete them after matching, never log them. |
| Likes | "Likes are never shown publicly. Only the person you liked sees them." | "Likes are private" with no detail | The author sees it; nobody else does. |
| Deletion | "Permanently removed. Backups are cleared within 30 days." | "Gone instantly and forever" | State the real backup and legal-retention windows. |
| Errors | "Your notes are still there." | "Your notes are safe." | Only claim what you can verify at that moment. |
| Ads and selling | "No ads. We never sell your data." | Anything stronger than the business model | Subscription revenue, no ad SDKs, a data processing agreement with each provider. |
| Messaging | "Only friends can message you directly." | "Nobody else can message you" (group chats include non-friends) | Group and multi-person chats are the exception; say so where it matters. |

Where text and behaviour disagree, change the text or the behaviour in the same commit. Keep the claim list in `CLAIMS.md` next to the code so every promise has an owner and a test or a review date.

### 0.3 How much to talk about "no ads"

Circle is about **connection: enjoying your friends and seeing what they're up to.** "No ads" is a fact, not the headline. Keep it to a few quiet places, one line each:
the last point in **About Circle**, one sentence in the small print of **Membership**, the **privacy** summary ("What we never do"), and at most one sentence in the onboarding story's pricing slide.
**Everywhere else, remove it** (headlines, hero text, the sign-up agreement, thank-you messages, empty states, settings sub-labels). When you need a reason to be proud of something, use friends:
"See what your friends are up to", "Just your friends", "Go and see them". Search the whole app for "ads", "advertis", "product", "sell", and reword anything outside the places above.

## 1. Sign-up, log-in and profile setup

Flow for **Create account:** Welcome > Your number > Enter the code > Set up your profile > Age and agreement > Find your friends
(the existing contacts screen) > the existing 4-slide onboarding > Home. **Log in:** Welcome > Your number > Enter the code > Home.
Every step has a back button. The system back button goes back one step; from Welcome it exits.

- **Welcome:** wordmark "Circle." (Fraunces 58sp, the full stop in accent) over two faint rings, tagline in Fraunces italic 18sp, a second line "See what your friends are up to." (14sp, muted),
  accent button **Create account**, outlined **Log in**, and a line "By continuing you agree to the Terms and Privacy Policy" whose
  link opens the legal screen.
- **Your number:** title "What's your number?" (Fraunces 30sp), explainer "Type your country code and your number. We'll text you a code. We never share
  your number. We only use it to find your friends, and only if you allow it." The user **types their own country code** in a small field with a "+" in front of it
  (up to 3 digits, prefilled from the SIM or network country, then the device region: for example +91 for India, +49 for Germany, +1 for the US) and **their number** in a second field.
  Under the fields show the country the code belongs to in accent ("India", or "United States / Canada" when a code is shared) or, for a code that doesn't exist,
  "We don't know that code. Check it or choose from the list." A link **Choose from a list** opens a searchable bottom sheet of **every country** (search by name or by code,
  for example "ger" or "+44"); picking one fills the code. Use **libphonenumber** for the real list, validation and formatting (national number length differs by country), store the result as **E.164**,
  and accept numbers typed with spaces, dashes or a leading zero. **Send code** is enabled only when the code is known and the number is valid for that country. A link "Use email instead"
  (email sign-in is a later option; show a toast "coming soon" for now).
- **Enter the code:** six boxes (58dp high, 12dp radius), the active box has a 2dp accent border; one hidden text field captures input
  (digits only, max 6, `autofill` for the SMS one-time code). It verifies automatically at 6 digits. "Send a new code" is available after
  30 seconds (show the countdown). Wrong code: shake the boxes once and show "That code isn't right." After 5 wrong tries, lock for 5 minutes.
  (In the fake repository any 6 digits work.)
- **Set up your profile:** 84dp avatar with an **Add a photo / Change photo** button (system photo picker, then crop to a circle);
  **Name** (max 30), **Handle** (lowercase letters, numbers, dots, underscores; 3 to 20 characters; live message: "Letters, numbers, dots and
  underscores. 3 to 20 characters." / "Too short. Use at least 3 characters." / "@x is taken. Try another." / "@x is available" in accent),
  **Pinned thought** (optional, 140 characters with a counter). Line under it: "This and your photo are the only things people who aren't your
  friends can see." **Continue** is disabled until the name is filled and the handle is valid and free. The handle check calls the repository
  (debounce 400ms; it returns taken/free).
- **Age and agreement:** "Circle is for people 18 and over. Your date of birth is used only to check this, and isn't shown to anyone."
  Three numeric fields DD / MM / YYYY. Under 18 (by the real calendar, so someone who turns 18 tomorrow is still blocked and someone who
  turns 18 today is allowed): show "Sorry, Circle is for people 18 and over, so we can't create your account." and keep Create disabled.
  A checkbox: "I agree to the Terms and the Privacy Policy." plus a link "Read the short
  version". **Create account** is enabled only with a valid age and the checkbox ticked. **(Recommended for a worldwide launch: 18. See section 10 for why. Confirm with a lawyer.)** Store only the fact that the user is old enough plus the birth year if the backend needs it; never show it.
- **Find your friends:** ring illustration, "See who's already here", "Circle can find friends by comparing scrambled versions of the phone
  numbers in your contacts. It doesn't keep them and never shows them to anyone. You can skip this and add friends one by one." Implementation: hash the numbers on the device, send only the hashes over HTTPS, match, return the matches, **delete the hashes immediately**, never log them, and say so in the privacy policy. **Connect contacts** (request the
  READ_CONTACTS permission with this rationale first) and **Skip for now**. Both go on to onboarding then Home.
- After sign-up a brand-new user lands on the **empty Home** (section 3), not on a demo feed.
- **Log out** (Settings) clears the session and returns to Welcome.

## 2. Inviting friends (the only way Circle grows)

- Entry points: the empty Home ("Invite a friend"), Settings, Your circle, and the "Find friends" screens.
- **Invite sheet** (bottom sheet, 22dp top radius): title "Invite a friend", "They'll see your name and photo, and can ask to be your friend.
  Nobody is added without saying yes." A tinted card with the invite link, e.g. `circle.app/i/shashank`, and a **Copy** button (toast "Link copied").
  Buttons Messages / WhatsApp / Mail / **More** open the Android share sheet with a short text and the link. **FROM YOUR CONTACTS:** a few contacts
  with an **Invite** button that becomes "Invited ✓" and sends an SMS intent (never send automatically; the user presses send in their own app).
- The link is an **Android App Link** (verified domain, `assetlinks.json`). Opening it with the app installed shows the inviter's not-friends
  profile with **Add friend**; without the app it goes to the Play Store listing and then, after install and sign-up, to the same profile.
  **An invite never makes anyone a friend.** (Placeholder domain: use the real one when chosen.)
- Show how many invites were sent only to the sender, privately.

## 3. Empty, offline and error states (one component, nine kinds)

One reusable `EmptyState(illustration, title, body, action?)`: a 120dp ring illustration (dashed outer ring, an accent dot at the top, a glyph
in the middle), title in Fraunces 24sp semibold, body 14sp muted (max width about 270dp), optional accent pill button. Copy:

| Where | Title | Body | Action |
|---|---|---|---|
| Home (no friends) | Your circle is empty | Circle works with friends. Add your first one, or invite someone. | Find friends (+ outlined "Invite a friend") |
| Messages | No chats yet | Start one with a friend. Only friends can message you. | Start a chat |
| Explore (nothing near) | Nothing nearby yet | Be the first. Make an event or start a group near you. | Create an event |
| Alerts | You're all caught up | Quiet alerts show up here. Likes stay private. | none |
| Your circle | No inner circle yet | Pick the few people you'd tell first. They're never told. | Choose people |
| Groups | No groups yet | Join one from Explore, or start your own. | Explore groups |
| Profile (no notes) | Nothing here yet | Your notes will live here. | Write a note |
| Offline | You're offline | Showing what we have. We'll catch up when you're back. | Try again |
| Error | Couldn't load this | Something went wrong on our side. Your notes are still there. | Try again |

Use them everywhere a list can be empty. The offline state is a slim banner on screens that still have cached content, and the full
state only when there is nothing to show. Add loading skeletons (a calm shimmer, off with "remove animations") on every list.

## 4. Membership, your data, legal

- **Membership** (Settings > Account; the row's sub-label is "Keeps Circle running"): a tinted card with the kicker "MEMBERSHIP" and the headline "Circle is for you and your friends. A small
  membership keeps it that way.", two selectable plans (Monthly / Yearly, "just a twelfth of the yearly price each month"; **show the price string Google Play returns, in the user's currency; €1 and €12 are
  placeholders**), "WHAT IT PAYS FOR" with two points (**Servers and storage:** "So your friends' notes and photos are always there when you open Circle." **Safety and support:** "Reports, blocking and a
  real person to write to."), an accent button "Start membership, <price> a year" (text follows the plan), small print "Circle runs on memberships, so there are no ads. Billed through Google Play in your own
  currency. Cancel any time in the Play Store." After starting: "You're a member." / "Thank you. Circle is yours and your friends'." and "Manage in Google Play". **(placeholder: use Google Play Billing; the real prices,
  any free trial, tax wording and the consumer-rights notice must be confirmed.)** Behind an interface so it can be switched on later.
- **Your data** (Settings > Account): **Download my data**: "A copy of what you've put into Circle: your profile, notes, photos, chats and friends. We prepare it
  automatically. Nobody at Circle opens it." Button **Request a download**, then "Requested. When it's ready we'll tell you, and you download it here in the app. It's
  only for you, and it expires after 7 days." **Design of the export (this is what makes it trustworthy):**
  - The export is built by an automated job from data the servers already hold. Be honest that the servers hold it; what you promise is that **no person opens the export**, and you enforce that.
  - **Never put the data in an email.** The email and the in-app notification only say "Your download is ready." It is downloaded **inside the app**, after the user is
    re-verified (biometric or a fresh code).
  - The file is **encrypted**, available only to that account, **single use**, and **deleted after the first download or after 7 days**, whichever comes first.
  - Every request, preparation and download is written to an **audit log** the user can see ("Requested 9 Oct, ready 9 Oct, downloaded 9 Oct").
  - If the account is deleted or the password/number changes during the 7 days, the file is deleted.
  - **(placeholder: the timings. Confirm the legal maximum with a lawyer.)**
  Under DANGER ZONE: **Delete my account** opens a screen: "This can't be undone. It permanently removes your profile, your notes and photos, your chats, and your place
  in groups and events. Backups are cleared within 30 days. Your friends aren't told why.", "If you have a membership, cancel it in Google Play first.", a field "Type DELETE to confirm"
  and a red button enabled only when the text matches (case-insensitive). After it: "Your account is deleted. Thank you for being here." and a button back to Welcome. **(placeholder: use the real backup window.)**
- **Terms, privacy and company info** (Settings > About): three tabs (Terms, Privacy, Company), each opening with "THE SHORT VERSION" bullets then the full text (placeholder text now;
  load the real documents from a URL later so they can change without an app update). An accent-tinted notice at the top says these are placeholders. The
  **Company** tab has who runs Circle (name, address, email). Some countries, including Germany where the provider is based, require specific details here; the lawyer decides the exact content. Use exactly the short bullets in the prototype for now (the privacy ones are: What we store; Who can read it; What we never do; Companies that help us; Your rights). **A lawyer must write the real versions.**

## 5. Scaling: the circle screens must fit every phone

The orbit screens (Explore map, Your circle, Group people, Event table) were designed on a 390dp-wide canvas. They must scale to the real width.

- Wrap each orbit in `BoxWithConstraints`. Compute `scale = min(maxWidth / 390.dp, maxHeightForMap / designHeight)` and **multiply every
  geometry constant** (radii, node sizes, label offsets, centre) by it. Keep geometry in `Orbit.kt`, taking `scale` as a parameter. Never hard-code dp in the composables.
- Clamp: scale between 0.8 and 1.25. Below 0.8 (very small phones) switch to the **list view**; above 1.25 (tablets, unfolded foldables) cap the orbit width and centre it.
- Node **visual** size scales, but the **touch target never drops below 48dp**. Sector labels never overlap a node: if they would at a small scale, drop the labels.
- Text sizes use `sp` and must survive the system font size at **200%**: nothing may clip or overlap. At large font sizes the orbit text (day number,
  letters) is allowed to clip inside its node, but the preview card and every list must reflow. If the orbit becomes unusable at the largest font size, switch to list view.
- Landscape: orbit on the left, details on the right. Foldables: respond to window size classes, not device names.
- Add **screenshot (Paparazzi or Roborazzi) tests** for each orbit at 320, 360, 390 and 430dp widths in both themes and at font scale 1.0 and 2.0.

## 6. All text in string resources

- **No hard-coded user-visible text anywhere.** Every string lives in `res/values/strings.xml` (`stringResource(R.string.x)`), including content descriptions,
  toasts, dialog text, notification text, and the About copy. Turn on lint `HardcodedText` as an **error**.
- Name strings by screen and role: `signup_phone_title`, `empty_messages_body`, `settings_notifications_row`. Use **plurals** (`plurals.xml`) for counts
  ("1 seat left / 5 seats left", "View 1 reply / View 2 replies") and format arguments (`%1$s`) instead of concatenating, so word order can change.
- Dates, times and numbers use the locale formatters (`DateTimeFormatter`, `NumberFormat`), never string building. Currency is formatted from the price the store returns.
- The long texts (About Circle, Why I built it, short legal bullets) are strings too, tagged `translatable` as appropriate; the owner's note keeps its signature.
- Make layouts tolerate text about **40% longer** (German). Test with **pseudolocalization** (`en-XA`) and add `values-de` later with no layout changes.
- Add a CI/lint check that fails on new hard-coded text.

## 7. Device pass (do this before calling the front end finished)

Test every screen in **night and day** on: a small phone (about 320dp wide), a normal phone (about 390dp), a large phone (about 430dp), a tablet or foldable.
For each: font scale **1.0, 1.3 and 2.0**; **rotation** (state must survive); **TalkBack** (every orbit screen via its list view, every button labelled,
sensible focus order, 48dp targets); **remove animations** on; **dark theme switch while running**; **back button** behaviour on every screen; keyboard covering inputs
(IME insets); a slow or missing network (offline banner, error states); **process death** (state restored). Fix what breaks. Report a table: screen, test, result.
Also run Android Studio's accessibility scanner and lint, and fix the findings.

## 8. Launch basics checklist (engineering side; **not legal advice, a lawyer must confirm**)

- **Google Play:** target the current required API level; app signing; R8/ProGuard with a checked release build; a **Data safety** form that matches what the app
  really collects (phone number, name, photos, messages, friends; nothing sold or shared for ads); a **privacy policy URL** on a public page; content rating questionnaire.
- **Accounts:** because users can create an account, Play requires **in-app account deletion plus a web link** for it. The Your data screen covers the in-app part; plan the web page.
- **User-generated content:** Play requires reporting and blocking in-app (built in F5) and a way to act on reports. Write down who reviews reports and how fast (feeds BACKEND_READINESS.md).
- **Permissions:** ask only when needed, with a rationale first. Use the **system photo picker** (no broad media permission), runtime **contacts** permission only when the user taps Connect contacts,
  **notification** permission (Android 13+) at a natural moment, not at launch.
- **No ad or analytics SDKs.** If crash reporting is added, pick a privacy-respecting one, make it opt-out-able and describe it in the privacy policy.
- **Security:** HTTPS only (no cleartext), certificate pinning optional, tokens in encrypted storage (Keystore), `allowBackup` thought through (exclude chats and tokens), `FLAG_SECURE` optional for chats.
- **Worldwide (a lawyer confirms; see section 10):** the privacy and consumer rules of every country where the app is offered, a company-info page, the minimum age, and the subscription wording.
- **Release:** version name and code, a changelog, an internal testing track first, then closed testing with a few real friends, then production.

## 9. Order of work and "done"

1. Sign-up and log-in screens with validation and the fake repository. 2. Empty, offline and error component and skeletons everywhere. 3. Invite sheet and App Link plumbing (stub the domain).
4. Membership, Your data, legal screens. 5. Strings in resources and the lint rule. 6. Scaling of the orbit screens plus screenshot tests. 7. The device pass and fixes. 8. The launch checklist as a written report with items done, items needing the owner, and items needing a lawyer.

**Done when:** a brand-new install can create an account (all validation as above), land on the empty Home, invite a friend, find the empty and error states, open Membership / Your data / Terms, delete the account and
return to Welcome; no hard-coded text remains; the orbit screens look right at 320 to 430dp and at 200% font; the device-pass table is clean; build, tests and lint pass; nothing is tied to one country (section 10); and you have written **LAUNCH_CHECKLIST.md**. Then stop and report.

## 10. Going global, not just Germany

Circle is meant for everyone. Build it so that **nothing is tied to one country**, and treat each of these as a requirement, not a nice-to-have.

**In the app**
- **No hard-coded country, city or currency.** Phone country code from the device (above). Explore's city is chosen by the user (search a city, or allow approximate location, which is optional); the header chip shows it, and "Aachen" exists only in seed and demo data. Empty-state copy says "near you".
- **Prices come from Google Play** in each user's currency; never format prices yourself. Set the per-country prices in Play Console.
- **Phone numbers:** use libphonenumber, store as E.164, accept national formats, handle numbers with leading zeros.
- **Languages and layout:** English first, but all text in resources (section 6), **right-to-left ready** (`supportsRtl="true"`, start/end instead of left/right; check the orbit screens mirror sensibly or stay symmetric), no text baked into images, locale-aware dates, numbers and time zones (store times as UTC, show them local; an event's time is shown in the viewer's zone with the event's zone noted).
- **Fonts:** Fraunces and Public Sans cover Latin scripts. For other scripts (Devanagari, Arabic, CJK) fall back to the system font and test line height.
- **Names and handles:** allow names in any script; handles stay ASCII.
- **Company info page** (not "imprint"): who runs Circle and how to reach them. Required details depend on where the provider is established (Germany if you are based there), not on where the users are.

**Minimum age: 18 worldwide for launch (recommended).** Rules for children differ by country and are heavy: India's data-protection rules require verifiable parental consent for anyone under 18, with the main obligations starting 14 May 2027; the EU, UK and US each have their own (and different) ages and duties. Setting 18 everywhere avoids building a parental-consent system now. It can be lowered later with proper work and legal advice. Ask the owner before changing it.

**Behind the app (feeds BACKEND_READINESS.md and LAUNCH_CHECKLIST.md)**
- A written **map of privacy laws** for the first launch countries (for example the GDPR in the EU and UK, India's DPDP Act, US state laws), what each needs (lawful basis or consent, notices, export and deletion, breach notification, limits on transfers between countries), and who owns each item. A lawyer reviews it.
- **Where the data lives:** pick the server region deliberately, document it, and write it in the privacy policy; check the rules for moving data between regions.
- **SMS sign-in codes cost money and have local rules** (India, for example, has its own sender and template registration for text messages). Use a provider that handles those rules, budget per-country costs, and add rate limits so nobody can run up the bill.
- **Safety at world scale:** reports and blocking exist (F5), but someone must act on reports in every time zone. Write down who reviews reports, how fast, and what the rules for removals and appeals are. Some regions (EU, UK) have platform-safety laws that apply to services with users there, even for small services; the lawyer says which apply to a small launch.
- **Launch in stages:** start with a small list of countries in Play Console, learn, then widen. Do not tick all countries on day one.
