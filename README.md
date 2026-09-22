# Mehfil · Android

Operations app for Indian wedding-service vendors — decorators, caterers, photographers, light-and-sound crews. The calendar is the spine, outstanding money is always one tap away, and double-bookings surface before they become a problem.

Built in **Kotlin + Jetpack Compose (Material 3)**, from the Figma design
[Mehfil](https://www.figma.com/design/HiKVpYsO9qLnUAWF1iZrUY/Untitled?node-id=3-2): 24 screens, 18 components, light and dark, on the Airbnb-derived skin. Accounts and data live in **Firebase** (Google, Sign in with Apple, Cloud Firestore) — the rules and data model are in the sibling repo `mehfil-backend`; the iOS app (SwiftUI, Liquid Glass) is `mehfil-ios`.

<p>
  <img src="docs/screenshots/home.png" width="180" alt="Home">
  <img src="docs/screenshots/events.png" width="180" alt="Events">
  <img src="docs/screenshots/event-detail.png" width="180" alt="Event detail">
  <img src="docs/screenshots/change-request.png" width="180" alt="Change request">
</p>

## Start here

The code is a translation of a documented design process, not a sketch that grew. Before reading a single
screen, read **[`NOTES.md`](https://github.com/Sanu5/mehfil-design/blob/main/NOTES.md)** in the design
repository: six phases of decisions, every derivation from the source design system, the rulings applied in
review, and the full skin migration — with the measurements that forced each change. `tokens.css` there is the
single source of truth that both apps' token files are generated against.

Commercial terms, the transfer checklist and an honest account of the repository's history are in
[`mehfil-backend/HANDOVER.md`](https://github.com/Sanu5/mehfil-backend/blob/main/HANDOVER.md).

## What it does

| Flow | Screens |
|---|---|
| **A · Season view** | Home with the conflict banner, outstanding receivables and the event that needs attention · Calendar month with event dots and conflict dates (swipe to change month) · Day timeline where overlapping events sit side by side · Season summary against target |
| **B · Events** | Segmented list grouped by month · Event hub with summary tiles, collapsible Payments, Crew, Inventory and Runsheet · Two-step create with an inline conflict check the moment a date is picked · Runsheet with completion toggles and reorder · Change request that shows consequences (chairs, tables, helpers, transport) and the revised quote |
| **C · Money** | Receivables grouped Overdue → Due this week → Upcoming · Per-event payment schedule with collection progress · Record payment on a large numeric keypad (never the alphanumeric keyboard) · Reminders with real composed copy in three tones and the history of what was sent before · Expenses with the margin and a loss state |
| **D · Team & inventory** | Crew by role with availability for a chosen date · Assign crew where booked members stay tappable and open a comparison of both events · Attendance with a single-tap three-state control · Inventory with commitment bars and over-committed items first · Item detail with a 30-day commitment strip |
| **E · Clients & system** | Client list with lifetime value and repeat markers · The client-facing shared event page (cream, gradient mesh, always light, share sheet) · Enquiry intake that saves, edits, closes and converts to an event · Package templates · Profile with business details, payment details, notifications, appearance · Account with sample data, erase, sign out and account deletion |
| **Sign-in & data** | Welcome with Google (Credential Manager), email and password (verified by email), and Sign in with Apple (behind `APPLE_SIGN_IN`) · One-screen business onboarding with an optional sample season · Every record (events, clients, milestones, crew, attendance, inventory, runsheet, reminders, expenses, enquiries, packages, change requests) is a Firestore document under `vendors/{uid}`, cached offline and streamed live |

Every amount uses Indian digit grouping (`₹23,47,500`), every screen that modifies data writes through the store and shows a save state, and the event draft survives leaving the create flow. Overdue milestones, crew conflicts, payment states and season totals are **derived on the device from stored data**, never stored, so they can't go stale.

<p>
  <img src="docs/screenshots/shared-page.png" width="180" alt="Shared event page">
  <img src="docs/screenshots/record-payment.png" width="180" alt="Record payment">
  <img src="docs/screenshots/reminder.png" width="180" alt="Send reminder">
  <img src="docs/screenshots/home-dark.png" width="180" alt="Home, dark">
</p>

## Run it

Requires Android Studio 2025.3+ (Gradle 9.4, AGP 9.2, Compose BOM 2026.04, min SDK 26, target 36). Open the project folder in Android Studio and run on a phone or emulator, or from the terminal:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Without a Firebase config** the app runs in *device-only* mode: the welcome screen offers **Continue on this device**, data is stored as JSON in app storage, and the whole product can be used and reviewed. Nothing else changes.

**With Firebase** — drop `google-services.json` (git-ignored) into `app/`. Gradle applies the google-services plugin only when the file exists, `BuildConfig.HAS_FIREBASE_CONFIG` flips, and the welcome screen shows Google and Apple sign-in. Setup steps (SHA fingerprints, the Apple Services ID for the web flow), Firestore rules and the data model are in [`mehfil-backend`](https://github.com/Sanu5/mehfil-backend).

## Configuration

`model/FeatureConfig.kt` is the one place where capabilities are switched. Every sign-in method and every
product module (inventory, runsheet, expenses, enquiries, packages, change requests, the client page,
reminders, dark mode) has a flag with a documented default, so a narrower first version ships without deleting
code. They are build-time decisions on purpose: there is no in-app switchboard, so a shipped app cannot have
parts of itself turned on and off from the outside, and the whole configuration surface is one readable file.

Sign-in: **Google** (Credential Manager), **email + password** with a mandatory verification email, password
reset and change-password, and **Apple** behind `Feature.APPLE_SIGN_IN` once the Services ID is configured.
Without a Firebase config the app runs in device-only mode.

## Ship it

- Package `com.mehfil.app`; version in `app/build.gradle.kts` (`versionCode` / `versionName`).
- Release builds are minified and resource-shrunk (`proguard-rules.pro` keeps the Firebase and Credential Manager surface). Signing reads an untracked `keystore.properties` at the project root:
  ```
  storeFile=mehfil-release.jks
  storePassword=…
  keyAlias=mehfil
  keyPassword=…
  ```
  then `./gradlew bundleRelease` produces `app/build/outputs/bundle/release/app-release.aab` for Play Console. Add the Play App Signing SHA-256 to the Firebase Android app afterwards or Google sign-in will fail on the store build.
- Data safety: collects email, name and user-generated content; encrypted in transit; deletion available in-app (Profile → Account → Delete account). `allowBackup` is off.
- Privacy policy and terms: [`PRIVACY.md`](PRIVACY.md), [`TERMS.md`](TERMS.md) — link them in the Play listing.

## Layout

```
app/src/main/java/com/mehfil/app/
  MainActivity.kt        edge-to-edge, appearance, session routing (welcome → onboarding → app)
  ui/MehfilApp.kt        NavigationBar on tab roots, single NavHost, routes, toast overlay
  model/
    Models.kt            records (Event, Client, Milestone, CrewMember, …, VendorProfile)
    Mapping.kt           record ⇄ Firestore/JSON document, WriteOp
    AuthService.kt       Google via Credential Manager, Apple via Firebase OAuth, sign out, delete, device-only
    Repository.kt        Repository · LocalRepository (JSON on device) · FirestoreRepository (listeners, batches)
    AppStore.kt          ViewModel: computed views, every mutation, derived conflicts/overdue/season
    SampleSeason.kt      the demo season, date-shifted so it always starts next Saturday
    Format.kt            ₹ grouping, dates, ids
  ui/theme/Theme.kt      light/dark token sets, Inter type roles, MaterialTheme mapping
  ui/components/         Components · Layouts
  ui/screens/            AuthScreens · HomeScreens · EventScreens · MoneyScreens · TeamScreens · ProfileScreens · EditSheets
app/src/main/res/font/   Inter Light/Regular/Medium (SIL Open Font License, see OFL-Inter.txt)
```

## Design notes worth knowing

- "Today" is live (`Cal.today`). The sample season is shifted by whole weeks so its busy Saturday is always the coming one — the conflict banner, overdue money and "Needs attention" read correctly on any date.
- Nothing derived is stored: overdue is computed from due dates, crew conflicts from bookings, payment state from milestones. Firestore rules (`mehfil-backend/firestore.rules`) allow only the owner to touch `vendors/{uid}`.
- Tokens come straight from the design's `tokens.css`: white ground and surface with structure carried by hairlines and 14pt clipping, ink type at 400–700, Rausch `#ff385c` as the single voltage (filled CTAs, the FAB, the selected tab, save/assign, the hero bar, event 1 — never links or data-viz), one shadow tier reserved for things that float, and a dark set derived by mixing any hue below 4.5:1 toward canvas until it passes.
- Pushed screens drop the bottom navigation bar, as the design's tab-bar policy asks.
- Decline on the change request is tertiary, not destructive; destructive confirmations name the exact thing being cancelled.
- The client page (E2) has no dark variant on purpose — it is the client's page, opened from a WhatsApp link.

## Screens

<details>
<summary>All 26 screenshots</summary>
<p>
  <img src="docs/screenshots/home.png" width="160">
  <img src="docs/screenshots/calendar.png" width="160">
  <img src="docs/screenshots/timeline.png" width="160">
  <img src="docs/screenshots/season.png" width="160">
  <img src="docs/screenshots/events.png" width="160">
  <img src="docs/screenshots/event-detail.png" width="160">
  <img src="docs/screenshots/create-step-1.png" width="160">
  <img src="docs/screenshots/create-step-2.png" width="160">
  <img src="docs/screenshots/runsheet.png" width="160">
  <img src="docs/screenshots/change-request.png" width="160">
  <img src="docs/screenshots/receivables.png" width="160">
  <img src="docs/screenshots/payment-schedule.png" width="160">
  <img src="docs/screenshots/record-payment.png" width="160">
  <img src="docs/screenshots/reminder.png" width="160">
  <img src="docs/screenshots/team.png" width="160">
  <img src="docs/screenshots/assign-crew.png" width="160">
  <img src="docs/screenshots/crew-comparison.png" width="160">
  <img src="docs/screenshots/attendance.png" width="160">
  <img src="docs/screenshots/inventory.png" width="160">
  <img src="docs/screenshots/item-detail.png" width="160">
  <img src="docs/screenshots/clients.png" width="160">
  <img src="docs/screenshots/shared-page.png" width="160">
  <img src="docs/screenshots/enquiry.png" width="160">
  <img src="docs/screenshots/profile.png" width="160">
  <img src="docs/screenshots/home-dark.png" width="160">
  <img src="docs/screenshots/event-detail-dark.png" width="160">
</p>
</details>

## Licence

Proprietary — all rights reserved. See [`LICENSE`](LICENSE). Publication of this repository has never granted
a licence to use the code; rights transfer only by written agreement.
