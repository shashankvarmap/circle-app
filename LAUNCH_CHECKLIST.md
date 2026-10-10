# Circle: launch checklist (ACCESS_AND_LAUNCH 8 and 10)

Engineering view, written 2026-10-10 at the end of the MASTER_PROMPT run. **Not legal advice: a lawyer must confirm every
legal item.** Status: **Done** (in the app now) · **Owner** (a decision, an account or money) · **Lawyer** · **Backend** (needs the server).

## Google Play

| Item | Status | Notes |
|---|---|---|
| Target the required API level | Done (34) | Re-check Play's requirement at submission time; raise to 35 if required |
| App signing (Play App Signing, upload key) | Owner | Create the upload key; keep it out of the repo |
| R8 / minify on a checked release build | **Open** | `isMinifyEnabled = false` today. Turn on, add keep rules (kotlinx.serialization, libphonenumber metadata), run the click-through on the release build |
| Data safety form | Owner + Lawyer | Collected: phone number, name, photos/videos, messages, friends, contacts hashes (not stored). Nothing sold, no ads, no analytics |
| Privacy policy on a public URL | Lawyer + Owner | In-app Terms / Privacy / Company are placeholders saying so |
| Content rating questionnaire | Owner | User-generated content + messaging |
| Account deletion in the app | Done | Your data > Delete my account (type DELETE) |
| Account deletion on the web | Owner + Backend | Play requires a web link as well |
| Reporting and blocking | Done (app) | Report flow on people, notes, comments, groups, events, chats; Block everywhere |
| Acting on reports | Owner + Backend | Who reviews, how fast, removal and appeal rules (BACKEND_READINESS > Moderation) |
| Subscriptions (Play Billing) | Owner + Backend | `MembershipBilling` interface with a fake; create products and per-country prices in Play Console |
| Prices per country | Owner | Set the monthly and yearly price for every country in Play Console. The app shows only Play's `ProductDetails` formattedPrice, in the user's Play currency, and never formats or converts prices. The `PreviewPricing` table (+91 INR, +49 EUR, +1 USD, +44 GBP, +61 AUD, +81 JPY, labelled "Preview price") is for preview builds only and goes away with Play Billing |
| Store listing, screenshots, feature graphic | Owner | Screenshot tests can produce clean frames |
| Testing tracks: internal, then closed with real friends, then production | Owner | Closed test needs the backend for real accounts |

## Permissions

| Item | Status | Notes |
|---|---|---|
| Photo picker with no storage permission | Done | System photo picker |
| Contacts only on "Connect contacts", after the explanation | Done | READ_CONTACTS runtime request |
| Notifications (Android 13+) at a natural moment | Backend | No push yet; ask on first message/request |
| Network state (offline banner) | Done | ACCESS_NETWORK_STATE |

## Security

| Item | Status | Notes |
|---|---|---|
| No ad or analytics SDKs | Done | Dependency review each release (CLAIMS.md) |
| No logging of note, comment or message text | Done | `PrivacyBoundaryTest` fails the build on any logging call |
| HTTPS only, no cleartext | Done by default (API 28+ blocks cleartext) | Add a network security config when the API host is known |
| Tokens in encrypted storage | Backend | No tokens yet; use Keystore |
| Android backup | Done (off) | `allowBackup="false"`, so chats and tokens never go to cloud backup; revisit with explicit rules if wanted |
| Server enforces the privacy rules | Backend | PRIVACY_RULES.md is the spec; the app's checks are only for the experience |
| Crash reporting | Owner | If added: privacy-respecting, opt-out, in the privacy policy, never with user text |
| FLAG_SECURE for chats | Owner (optional) | Not set |

## Going global (section 10)

| Item | Status | Notes |
|---|---|---|
| No hard-coded country, city or currency | Done (app), Backend (city) | Country code from SIM/network/region; prices from billing; "Aachen" only in demo data. City search / optional location needs the backend |
| Phone numbers: libphonenumber, E.164, national formats | Done | `SignupRulesTest` |
| All text in resources, lint `HardcodedText` as an error | Done | Plus `HardcodedTextTest` for Compose |
| Right to left | Done | `supportsRtl`, checked with `ar-XB` |
| Locale-aware dates and times, stored as UTC | Done | Home date header now follows the device locale |
| Fonts for non-Latin scripts | Done | System fallback for other scripts; test line height with real translations |
| Names in any script, ASCII handles | Done | |
| Company info page | Lawyer + Owner | Placeholder with the fields to fill |
| Minimum age 18 worldwide | Done (app) | Calendar check on the phone; birth date not stored. Lawyer confirms; ask the owner before lowering |
| Map of privacy laws for launch countries (GDPR, UK GDPR, India DPDP, US states) | Lawyer | Lawful basis, notices, export, deletion, breach notice, transfers |
| Where the data lives (server region) | Owner + Backend | Write it in the privacy policy |
| SMS codes: provider, per-country rules (India DLT), costs, rate limits | Owner + Backend | |
| Report handling across time zones; EU DSA / UK OSA duties | Owner + Lawyer | |
| Launch in a few countries first | Owner | Play Console country list |

## Claims

| Item | Status | Notes |
|---|---|---|
| Every promise listed with what makes it true | Done | CLAIMS.md; review before every release |
| "No ads" only in the allowed places | Done | About (last point), Membership small print, Privacy short version, one line in the story |
| Export: automated, nobody opens it, 7 days | Backend | App records the request only |
| Deletion: backups cleared within 30 days | Lawyer + Backend | Placeholder window |

## Release

| Item | Status | Notes |
|---|---|---|
| Version name and code | Done (0.1 / 1) | Bump per release |
| Changelog | Owner | |
| Demo-only code out of release | **Open** | The Demo section (Reset demo data) appears only with the fake repository and the fail switch only in debug builds; the fake itself must be replaced by the backend repository before a public release |
