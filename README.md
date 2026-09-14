# Mehfil · Android

Operations app for Indian wedding-service vendors — decorators, caterers, photographers, light-and-sound crews. The calendar is the spine, outstanding money is always one tap away, and double-bookings surface before they become a problem.

Built in **Kotlin + Jetpack Compose (Material 3)**, from the Figma design
[Mehfil](https://www.figma.com/design/HiKVpYsO9qLnUAWF1iZrUY/Untitled?node-id=3-2): 24 screens, 18 components, light and dark. The iOS app (SwiftUI, Liquid Glass) lives in the sibling repo `mehfil-ios`.

<p>
  <img src="docs/screenshots/home.png" width="180" alt="Home">
  <img src="docs/screenshots/events.png" width="180" alt="Events">
  <img src="docs/screenshots/event-detail.png" width="180" alt="Event detail">
  <img src="docs/screenshots/change-request.png" width="180" alt="Change request">
</p>

## What it does

| Flow | Screens |
|---|---|
| **A · Season view** | Home with the conflict banner, outstanding receivables and the event that needs attention · Calendar month with event dots and conflict dates (swipe to change month) · Day timeline where overlapping events sit side by side · Season summary against target |
| **B · Events** | Segmented list grouped by month · Event hub with summary tiles, collapsible Payments, Crew, Inventory and Runsheet · Two-step create with an inline conflict check the moment a date is picked · Runsheet with completion toggles and reorder · Change request that shows consequences (chairs, tables, helpers, transport) and the revised quote |
| **C · Money** | Receivables grouped Overdue → Due this week → Upcoming · Per-event payment schedule with collection progress · Record payment on a large numeric keypad (never the alphanumeric keyboard) · Reminders with real composed copy in three tones and the history of what was sent before · Expenses with the margin and a loss state |
| **D · Team & inventory** | Crew by role with availability for a chosen date · Assign crew where booked members stay tappable and open a comparison of both events · Attendance with a single-tap three-state control · Inventory with commitment bars and over-committed items first · Item detail with a 30-day commitment strip |
| **E · Clients & system** | Client list with lifetime value and repeat markers · The client-facing shared event page (cream, gradient mesh, always light) · Enquiry intake with the same conflict check · Profile with appearance and settings |

Every amount uses Indian digit grouping (`₹23,47,500`), every string is real content, and every screen that modifies data shows a save state. The event draft survives leaving the create flow.

<p>
  <img src="docs/screenshots/shared-page.png" width="180" alt="Shared event page">
  <img src="docs/screenshots/record-payment.png" width="180" alt="Record payment">
  <img src="docs/screenshots/reminder.png" width="180" alt="Send reminder">
  <img src="docs/screenshots/home-dark.png" width="180" alt="Home, dark">
</p>

## Run it

Requires Android Studio 2025.3+ (Gradle 9.4, AGP 9.2, Compose BOM 2026.04, min SDK 26). Open the project folder in Android Studio and run on a phone or emulator, or from the terminal:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No backend, no network: the app runs on an in-memory content model and stores only the appearance setting and the event draft in `SharedPreferences`.

## Layout

```
app/src/main/java/com/mehfil/app/
  MainActivity.kt        edge-to-edge, appearance
  ui/MehfilApp.kt        NavigationBar on tab roots, single NavHost, routes, toast overlay
  model/                 Models · Seed (content model) · AppStore (ViewModel) · Format (₹ grouping, dates)
  ui/theme/Theme.kt      light/dark token sets, Inter type roles, MaterialTheme mapping
  ui/components/         Components (buttons, chips, inputs, keypad, rows, banners, stepper, segmented, empty, toast)
                         Layouts (screen scaffold, cards, calendar grid, timeline, date strip, sheet content)
  ui/screens/            HomeScreens · EventScreens · MoneyScreens · TeamScreens · ProfileScreens
app/src/main/res/font/   Inter Light/Regular/Medium (SIL Open Font License, see OFL-Inter.txt)
```

## Design notes worth knowing

- "Today" is pinned to **Thu 12 Nov 2026** (`Cal.today`) so the season, conflicts and receivables read exactly as designed. Switch it to `LocalDateTime.now()` when real data arrives.
- Tokens come straight from the design's `tokens.css`: ground `#F6F9FC`, surface white, ink `#0D253D`, indigo `#533AFD`, ruby only for danger, and a dark set on ink/brand-navy with the accent lifted to subdued indigo for contrast. Tinted cards composite over the surface colour so elevation shadows never show through.
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
