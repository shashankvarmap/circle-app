# Circle: claims register (ACCESS_AND_LAUNCH 0.2)

Every promise the app makes, where it appears, what must be true behind it, and how that is checked. Rule: promise
behaviour we control, describe the mechanism, don't promise technical guarantees we can't prove. When text and
behaviour disagree, change one of them in the same commit.

| Claim (as worded in the app) | Where | What makes it true today | What a backend must keep true | Check |
|---|---|---|---|---|
| "Only you see this." (friend and inner counts) | Profile friends row, Your circle | Only the owner's `circleSummary()`; a friend's `ProfileView.Full` has no counts field | No endpoint returns another person's friends or counts | `AccessPolicyTableTest` friend graph row; `FakeCircleRepositoryTest` |
| "They're never told, and there's no badge." (inner circle) | Your circle | No inner-circle field on anyone else's view | Inner-circle membership is never returned to other users | Policy table; review |
| "Likes are never shown publicly. Only the person you liked sees them." / "Likes are always private." | Thread/Home like labels, Notifications screen, Alerts empty state | No counts anywhere; likes stored per viewer; alerts only to the author | Like lists readable by the author only | Repository tests; review before launch |
| "You're not friends yet… Until then you see only their photo and pinned thought." | Limited profile | `ProfileView.Limited` has only photo, name, handle, pinned thought | The limited endpoint loads only those fields | `PrivacyScenariosUiTest`, `AccessPolicyTableTest` |
| "Send a friend request to open this thread. You can still see who posted it." | Gate sheet | `ThreadResult.Denied(NOT_FRIENDS)`; snippet ≤ 90 characters | Server never returns the thread or full text to non-friends | `PrivacyScenariosUiTest` 2, 3 |
| "Only friends can message you directly." | Messages footer, empty state | `AccessPolicy.directMessage` (friends only); group chats are the stated exception | Server refuses DMs between non-friends | Policy table; `ChatsTest` |
| "A request is never instant: they say yes first." / "Nobody is added without saying yes." | Find friends, invite sheet | Requests stay REQUEST_SENT until the other person accepts; invites never befriend | Accept is the recipient's action only | `FakeCircleRepositoryTest` |
| "Your report is recorded. You can also block them at any time." | Report flow | Reports stored with reason and note | Someone reviews reports (owner to decide who and how fast) | Owner + lawyer before launch |
| "Circle can find friends by comparing scrambled versions of the phone numbers in your contacts. It doesn't keep them and never shows them to anyone." | Find your friends | Numbers are normalised and SHA-256 hashed on the phone; only hashes go to `matchContacts`; nothing stored or logged | Match hashes, return matches, delete hashes at once, never log them; say so in the privacy policy | `SignupRulesTest` (hash); code review of `ContactsViewModel`; server audit |
| "We never share your number. We only use it to find your friends, and only if you allow it." | Your number | Number used for sign-in; contacts matching only after Connect contacts | Same; SMS provider bound by a processing agreement | Lawyer + provider DPA |
| "Your date of birth is used only to check this, and isn't shown to anyone." | Age step | The date is checked on the device and not sent or stored | Store at most "is 18+" (and birth year only if needed) | Code review of `SignupViewModel.createAccount` |
| "We prepare it automatically. Nobody at Circle opens it." | Your data | Fake records the request only | Automated export job; no human access; encrypted, single use, deleted after first download or 7 days; audit log | Backend design review (BACKEND_READINESS) |
| "Permanently removes… Backups are cleared within 30 days." | Delete account | Fake resets the demo | Real deletion and a real backup window (placeholder 30 days: confirm) | Lawyer + backend |
| "No ads." / "Circle runs on memberships, so there are no ads." / "sell your data, show ads, or build an advertising profile" | About (last point), Membership small print, Privacy short version, story pricing slide (one sentence) | No ad or analytics SDKs in the app | Subscription revenue only; a processing agreement with each provider | Dependency review each release |
| "Couldn't load this… Your notes are still there." | Error state | Shown only when loading fails; nothing is deleted by a failed load | Same | Review |
| "Nothing to refresh. You're caught up." / "You're all caught up." | Home | The feed is live; the end of the list is the end of your friends' notes | No injected or ranked content | Review |
| "Placed by topic and how soon. Nothing here is ranked." | Explore | Map angle = topic, ring = within 7 days; lists sorted by time or name | No popularity ranking | `GroupsAndEventsTest`; review |
| "Billed through Google Play in your own currency." | Membership | Prices are strings from the billing interface (placeholders now) | Prices set per country in Play Console | Before launch |

Owner of this file: the app owner. Review it whenever copy or behaviour changes, and before every release.
