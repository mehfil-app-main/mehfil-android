package com.mehfil.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.*
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.Routes
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.CardShape
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class EventSegment(val label: String) { UPCOMING("Upcoming"), ENQUIRY("Enquiry"), COMPLETED("Completed") }

/** B1 · Event list — segmented, grouped under month headers, floating create action. */
@Composable
fun EventListScreen(store: AppStore, nav: Nav) {
    var segment by rememberSaveable { mutableStateOf(EventSegment.UPCOMING) }
    var searching by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    fun matches(e: Event) = query.isBlank() || e.name.contains(query, true) || e.venue.short.contains(query, true) || store.clientFor(e)?.name?.contains(query, true) == true
    val upcoming = store.upcomingEvents.filter(::matches)
    val completed = store.completedEvents.filter(::matches)
    val list = if (segment == EventSegment.COMPLETED) completed else upcoming
    val groups = list.groupBy { Fmt.monthYear(it.day) }
    Box(Modifier.fillMaxSize()) {
        Screen(
            title = "Events", subtitle = if (upcoming.isEmpty()) "Nothing booked yet" else "${upcoming.size} upcoming · ${Fmt.inr(upcoming.sumOf { it.quoted })} booked",
            isRoot = true, spacing = Space.lg, bottomPadding = 96.dp,
            headerTrailing = { CircleAction(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, { searching = !searching; if (!searching) query = "" }) },
        ) {
            if (searching) InputField("Search", query, { query = it }, placeholder = "Event, client or venue", trailing = Icons.Outlined.Search)
            SegmentedControl(EventSegment.entries, segment, { it.label }, { segment = it })
            when (segment) {
                EventSegment.ENQUIRY -> EnquiryList(store, nav)
                else -> if (groups.isEmpty()) {
                    EmptyState(
                        if (segment == EventSegment.UPCOMING) "No events yet" else "Nothing completed yet",
                        if (segment == EventSegment.UPCOMING) "Bookings you create will appear here with their date, venue and balance." else "Events move here once you mark them completed.",
                        Modifier.padding(top = 120.dp), actionTitle = if (segment == EventSegment.UPCOMING) "Create event" else null, actionIcon = Icons.Outlined.Add, primary = true,
                    ) { nav.push(Routes.create) }
                } else groups.forEach { (name, evs) ->
                    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        SectionHeading(name, trailing = "${evs.size} event${if (evs.size == 1) "" else "s"} · ${Fmt.inr(evs.sumOf { it.quoted })}")
                        evs.forEach { e -> EventCard(e, store.clientFor(e), store.paymentState(e), onClick = { nav.push(Routes.event(e.id)) }, conflicted = store.isConflicted(e)) }
                    }
                }
            }
        }
        if (segment != EventSegment.COMPLETED) FloatingAction(if (segment == EventSegment.ENQUIRY) "New enquiry" else "New event", Icons.Outlined.Add) { nav.push(if (segment == EventSegment.ENQUIRY) Routes.enquiry() else Routes.create) }
    }
}

@Composable
private fun EnquiryList(store: AppStore, nav: Nav) {
    val open = store.openEnquiries
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        if (open.isEmpty()) EmptyState("No open enquiries", "Log an enquiry the moment someone asks, and check their date against the calendar.", Modifier.padding(top = 120.dp), "New enquiry", Icons.Outlined.Add, true) { nav.push(Routes.enquiry()) }
        else {
            SectionHeading("Open", trailing = "${open.size} enquir${if (open.size == 1) "y" else "ies"}")
            RowGroup {
                open.forEachIndexed { i, q ->
                    if (i > 0) Hairline()
                    ListRow(q.name, subtitle = listOfNotNull(q.source, q.date?.let { Fmt.weekdayDayMonth(it) }).joinToString(" · "), onClick = { nav.push(Routes.enquiry(q.id)) }, leading = { IconTile(Icons.Outlined.MailOutline) }) {
                        Text(q.budget, style = Type.caption, color = M.textMute, maxLines = 1); Chevron()
                    }
                }
            }
        }
    }
}

/** B2 · Event detail — the hub for one event. Densest screen in the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(store: AppStore, nav: Nav, eventId: String) {
    val m = M
    val e = store.event(eventId) ?: return
    val client = store.clientFor(e)
    val balance = store.balanceDue(e.id); val overdue = store.overdueAmount(e.id)
    val conflicted = store.isConflicted(e)
    val pending = store.pendingChangeRequest(e.id)
    var paymentsOpen by remember { mutableStateOf(false) }
    var record by remember { mutableStateOf<Milestone?>(null) }
    var cancelConfirm by remember { mutableStateOf(false) }
    var holds by remember { mutableStateOf(false) }
    var change by remember { mutableStateOf(false) }
    var editClient by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    Screen(
        title = e.name, heroHeader = true, onBack = nav::back, spacing = Space.lg,
        actions = {
            CircleAction(Icons.Outlined.IosShare, { nav.push(Routes.shared(e.id)) }, contentDescription = "Share")
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Log change request") }, { menu = false; change = true })
                    DropdownMenuItem({ Text("Expenses and margin") }, { menu = false; nav.push(Routes.expenses(e.id)) })
                    DropdownMenuItem({ Text("Set inventory holds") }, { menu = false; holds = true })
                    if (e.status == EventStatus.UPCOMING) DropdownMenuItem({ Text("Mark completed") }, { menu = false; store.markCompleted(e.id) })
                    HorizontalDivider()
                    if (e.status != EventStatus.CANCELLED) DropdownMenuItem({ Text("Cancel event", color = m.danger) }, { menu = false; cancelConfirm = true })
                }
            }
        },
    ) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.width(Dim.edge).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(m.event(e.hue)))
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(e.name, style = Type.displayMd, color = m.text)
                Text("${Fmt.weekdayLong(e.day)} · ${Fmt.timeRange(e.start, e.end)}", style = Type.bodyMd, color = m.textSecondary)
                Text(listOfNotNull(e.venue.short, e.venue.hall).joinToString(" · "), style = Type.caption, color = m.textMute)
                Row(Modifier.padding(top = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    if (e.status == EventStatus.UPCOMING) Row(Modifier.height(28.dp).border(1.dp, m.line, CircleShape).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = m.text); Text(Fmt.capitalized(Fmt.relativeDays(e.day)), style = Type.caption, color = m.text)
                    }
                    if (conflicted) StatusChip(ChipKind.CONFLICT)
                    if (e.status == EventStatus.CANCELLED) StatusChip(ChipKind.CANCELLED)
                    if (e.status == EventStatus.COMPLETED) StatusChip(ChipKind.CONFIRMED)
                }
            }
        }
        if (pending != null && store.vendor?.notifyChanges != false) Card(fill = m.accentTint, onClick = { nav.push(Routes.change(pending.id)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Icon(Icons.Outlined.Sync, null, Modifier.size(Dim.icon), tint = m.accentText)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Change request from ${client?.name ?: "the client"}", style = Type.bodyMdStrong, color = m.text)
                    Text("${pending.summary} · ${pending.via}, ${Fmt.weekdayDayMonthTime(pending.requestedAt)} · see what it moves", style = Type.caption, color = m.textMute)
                }
                Chevron()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) { StatTile("${e.guests}", "Guests", Modifier.weight(1f)); StatTile(Fmt.inr(e.quoted), "Quoted", Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                StatTile("${e.crewAssigned} of ${e.crewNeeded}", "Crew assigned", Modifier.weight(1f))
                StatTile(Fmt.inr(balance), if (overdue > 0) "Balance due · ${Fmt.inr(overdue)} overdue" else "Balance due", Modifier.weight(1f), figureColor = if (overdue > 0) m.danger else m.text)
            }
        }
        RowGroup {
            SectionRow(Icons.Outlined.CreditCard, "Payments", "${Fmt.inr(store.collected(e.id))} of ${Fmt.inr(e.quoted)}" + if (overdue > 0) " · overdue" else "", expanded = paymentsOpen) { paymentsOpen = !paymentsOpen }
            AnimatedVisibility(paymentsOpen) {
                Column {
                    store.milestonesFor(e.id).forEach { ms ->
                        Hairline()
                        PaymentRow(ms, paymentSubtitle(ms), subtitleColor = if (ms.effectiveStatus == MilestoneStatus.OVERDUE) m.danger else m.textMute, onClick = { if (ms.isOpen) record = ms })
                    }
                    Row(Modifier.padding(Space.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        MButton("Record payment", { record = store.milestonesFor(e.id).firstOrNull { it.isOpen } ?: run { nav.push(Routes.schedule(e.id)); null } }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, compact = true, icon = Icons.Outlined.CurrencyRupee)
                        Text("Schedule", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { nav.push(Routes.schedule(e.id)) })
                    }
                }
            }
            Hairline()
            SectionRow(Icons.Outlined.Groups, "Crew", "${e.crewAssigned} of ${e.crewNeeded} · ${store.conflictedMember(e)?.let { "${it.firstName}'s team conflicted" } ?: "assigned"}") { nav.push(Routes.assign(e.id)) }
            Hairline()
            SectionRow(Icons.Outlined.Inventory2, "Inventory", store.inventorySummary(e)) { holds = true }
            Hairline()
            SectionRow(Icons.Outlined.ListAlt, "Runsheet", store.runsheetSummary(e)) { nav.push(Routes.runsheet(e.id)) }
        }
        if (client != null) Box(Modifier.clickable { editClient = true }) {
            ContactRow(client, "${client.phone.ifEmpty { "No phone" }} · ${if (client.isRepeat) "repeat client" else "first event"}", onCall = { dial(ctx, client.phone) }, onMessage = { whatsapp(ctx, client.phone) })
        }
    }
    record?.let { ms -> ModalBottomSheet(onDismissRequest = { record = null }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { RecordPaymentSheet(store, ms) { record = null } } }
    if (holds) EventHoldsSheet(store, e) { holds = false }
    if (change) LogChangeRequestSheet(store, e, onLogged = { nav.push(Routes.change(it.id)) }) { change = false }
    if (editClient && client != null) ClientSheet(store, client) { editClient = false }
    if (cancelConfirm) ModalBottomSheet(onDismissRequest = { cancelConfirm = false }, containerColor = m.surface) {
        SheetContent("Cancel the ${e.name} on ${Fmt.dayMonthLong(e.day)}?", "${store.assignedCrew(e.id).size} crew bookings and the inventory holds are released. ${client?.name ?: "The client"} is not notified automatically.", actions = {
            MButton("Cancel the ${e.type.word}", { store.cancelEvent(e.id); cancelConfirm = false; nav.back() }, kind = ButtonKind.DESTRUCTIVE)
            MButton("Keep it", { cancelConfirm = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {}
    }
}

@Composable
private fun SectionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, expanded: Boolean? = null, onClick: () -> Unit) {
    ListRow(title, subtitle = summary, onClick = onClick, leading = { IconTile(icon) }) { Chevron(Modifier.rotate(if (expanded == true) 90f else 0f)) }
}

fun paymentSubtitle(m: Milestone): String = when (m.effectiveStatus) {
    MilestoneStatus.PAID -> "Paid ${Fmt.weekdayDayMonth(m.paidOn ?: m.due)}" + (m.method?.let { " · ${it.label}" } ?: "") + (m.reference?.let { " · ref $it" } ?: "")
    MilestoneStatus.OVERDUE -> "Due ${Fmt.weekdayDayMonth(m.due)} · ${Fmt.daysLate(m.due)} days late"
    MilestoneStatus.PENDING -> "Due ${Fmt.weekdayDayMonth(m.due)} · ${Fmt.relativeDays(m.due)}"
}

fun dial(ctx: android.content.Context, phone: String) = runCatching { if (phone.isNotBlank()) ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:+${Fmt.phoneDigits(phone)}"))) }
fun whatsapp(ctx: android.content.Context, phone: String, text: String = "") = runCatching {
    if (phone.isNotBlank()) ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/${Fmt.phoneDigits(phone)}" + if (text.isEmpty()) "" else "?text=${android.net.Uri.encode(text)}")))
}
fun sms(ctx: android.content.Context, phone: String, text: String) = runCatching {
    if (phone.isNotBlank()) ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:+${Fmt.phoneDigits(phone)}")).putExtra("sms_body", text))
}
fun shareText(ctx: android.content.Context, subject: String, text: String) = runCatching {
    ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_SUBJECT, subject); putExtra(android.content.Intent.EXTRA_TEXT, text) }, subject))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(title: String, subtitle: String?, initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit, minDate: LocalDate? = null, maxDate: LocalDate? = null) {
    val millis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = millis, initialDisplayedMonthMillis = millis, selectableDates = object : androidx.compose.material3.SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
            return (minDate == null || !d.isBefore(minDate)) && (maxDate == null || !d.isAfter(maxDate))
        }
    })
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = M.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetContent(title, subtitle, actions = { MButton("Done", { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; onDismiss() }) }) {
            DatePicker(state, showModeToggle = false, title = null, headline = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceSheet(title: String, options: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = M.surface) {
        SheetContent(title) {
            RowGroup {
                options.forEachIndexed { i, o ->
                    if (i > 0) Hairline()
                    ListRow(label(o), selected = o == selected, onClick = { onPick(o); onDismiss() }) { if (o == selected) Icon(Icons.Outlined.Check, null, tint = M.accentText) }
                }
            }
        }
    }
}

/** B3 · Create event step 1 and B4 · step 2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(store: AppStore, nav: Nav) {
    val m = M
    var step by remember { mutableStateOf(1) }
    var showDate by remember { mutableStateOf(false) }; var showType by remember { mutableStateOf(false) }; var showAmount by remember { mutableStateOf(false) }
    val d = store.draft
    val conflicts = d.date?.let { store.eventsOn(it) }.orEmpty()
    val packageTotal = store.packages.filter { it.id in d.packageIds }.sumOf { it.price }
    val step1Valid = d.clientName.isNotBlank() && d.type != null && d.date != null
    val step2Valid = d.venue.isNotBlank() && d.quoted > 0
    Screen(
        title = "New event", largeTitle = false, onBack = { if (step == 2) step = 1 else nav.back() }, spacing = Space.lg,
        actions = { Text("Save draft", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { store.show(Toast.Kind.SUCCESS, "Draft saved"); nav.back() }.padding(Space.sm)) },
        docked = {
            if (step == 2) Card(padding = Space.md) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Running total · ${d.packageIds.size} packages", style = Type.caption, color = m.textMute); Text(Fmt.inr(packageTotal), style = Type.bodyMdStrong, color = m.text)
                    }
                    if (d.quoted > 0) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${Fmt.inr(d.quoted - packageTotal)} margin", style = Type.caption, color = if (d.quoted >= packageTotal) m.textMute else m.danger); Text("on ${Fmt.inr(d.quoted)} quoted", style = Type.caption, color = m.textMute)
                    }
                }
            }
            if (step == 1) MButton("Continue", { step = 2 }, enabled = step1Valid)
            else MButton("Create event", { store.createEvent(d)?.let { e -> nav.back(); nav.push(Routes.event(e.id)) } }, enabled = step2Valid)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text("Step $step of 2 · ${if (step == 1) "Client and date" else "Venue and pricing"}", style = Type.caption, color = m.textMute)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(m.accent)); Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (step == 2) m.accent else m.line))
            }
        }
        if (step == 1) {
            InputField("Client name", d.clientName, { v -> store.updateDraft { it.copy(clientName = v) } }, placeholder = "Surname the family goes by")
            PhoneField(d.phone, { v -> store.updateDraft { it.copy(phone = v) } })
            PickerField("Event type", d.type?.label ?: "", { showType = true })
            PickerField("Date", d.date?.let { Fmt.weekdayLongYear(it) } ?: "", { showDate = true }, placeholder = "Pick a date", trailing = Icons.Outlined.CalendarToday, highlighted = d.date != null)
            if (d.date != null && conflicts.isNotEmpty()) {
                val chairs = store.inventory.firstOrNull { it.name.contains("chair", true) }?.let { "${it.committed(d.date)} of ${it.total} ${it.unit}" }
                val whenWord = if (conflicts.all { Cal.hour(it.start) >= 17 }) "that evening" else "that day"
                val words = listOf("", "One", "Two", "Three", "Four", "Five")
                val lead = store.crew.firstOrNull { mm -> mm.isLead && mm.bookings.any { id -> conflicts.any { it.id == id } } }
                val committed = listOfNotNull(lead?.let { "${it.firstName}'s team" }, chairs).joinToString(" and ")
                InlineConflictWarning(
                    (if (conflicts.size == 1) "${conflicts[0].name} already runs $whenWord." else "${words.getOrNull(conflicts.size) ?: conflicts.size} events already run $whenWord.") + (if (committed.isEmpty()) "" else " $committed are committed."),
                    conflicts, "See ${Fmt.dayMonth(d.date)} timeline",
                ) { nav.push(Routes.day(d.date)) }
            }
        } else {
            InputField("Venue", d.venue, { v -> store.updateDraft { it.copy(venue = v) } }, placeholder = "Venue, area", trailing = Icons.Outlined.LocationOn)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Guest count", style = Type.bodyMd, color = m.text); Text("Catering by the venue", style = Type.caption, color = m.textMute) }
                MStepper(d.guests, { v -> store.updateDraft { it.copy(guests = v) } }, range = 10..5000)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Packages · select all that apply", style = Type.caption, color = m.textMute)
                if (store.packages.isEmpty()) Text("No package templates yet — add them in Profile.", style = Type.caption, color = m.textMute)
                else RowGroup {
                    store.packages.forEachIndexed { i, p ->
                        if (i > 0) Hairline()
                        val on = p.id in d.packageIds
                        ListRow(p.name, subtitle = "${Fmt.inr(p.price)} · ${p.detail}", selected = on, onClick = { store.updateDraft { it.copy(packageIds = if (on) it.packageIds - p.id else it.packageIds + p.id) } }) {
                            Icon(if (on) Icons.Outlined.Check else Icons.Outlined.Add, null, Modifier.size(18.dp), tint = if (on) m.accentText else m.line)
                        }
                    }
                }
            }
            AmountInput(d.quoted, focused = false, label = "Quoted amount", onClick = { showAmount = true })
        }
    }
    if (showType) ChoiceSheet("Event type", EventType.entries, d.type, { it.label }, { t -> store.updateDraft { it.copy(type = t) } }) { showType = false }
    if (showDate) DateSheet("Event date", "Conflicts are checked as soon as you pick.", d.date ?: Cal.nextSaturday, { p -> store.updateDraft { it.copy(date = p) } }, { showDate = false }, minDate = Cal.todayDate)
    if (showAmount) ModalBottomSheet(onDismissRequest = { showAmount = false }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetContent("Quoted amount", "What the client pays in total", actions = { MButton("Done", { showAmount = false }) }) {
            AmountInput(d.quoted, label = null); NumericKeypad(d.quoted, { v -> store.updateDraft { it.copy(quoted = v) } })
        }
    }
}

@Composable
fun InlineConflictWarning(sentence: String, events: List<Event>, linkTitle: String?, onLink: (() -> Unit)?) {
    val m = M
    Column(Modifier.fillMaxWidth().clip(CardShape).background(m.dangerTint).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) { Icon(Icons.Outlined.WarningAmber, null, Modifier.size(Dim.icon), tint = m.danger); Text(sentence, style = Type.bodyMd, color = m.text) }
        Column(Modifier.padding(start = Dim.icon + Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            events.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Box(Modifier.size(Dim.dot).clip(CircleShape).background(m.event(e.hue))); Text(e.name, style = Type.caption, color = m.text); Text("${Fmt.timeRange(e.start, e.end)} · ${e.crewAssigned} crew", style = Type.caption, color = m.textMute)
                }
            }
        }
        if (linkTitle != null && onLink != null) Text(linkTitle, style = Type.buttonSm, color = m.accentText, modifier = Modifier.padding(start = Dim.icon + Space.md, top = Space.xs).clickable(onClick = onLink))
    }
}

/** B5 · Runsheet — used on the wedding day, standing at the venue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunsheetScreen(store: AppStore, nav: Nav, eventId: String) {
    val m = M
    val e = store.event(eventId) ?: return
    val tasks = store.tasks.filter { it.eventId == eventId }
    val blocks = tasks.groupBy { it.block }
    val defaultBlocks = listOf("Setup", "Dressing", "Event")
    var marker by remember { mutableStateOf("09:00") }
    var addSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }; var newCrew by remember { mutableStateOf("") }; var newBlock by remember { mutableStateOf("Setup") }
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val ctx = LocalContext.current
    Screen(
        title = "Runsheet", subtitle = "${e.name} · ${Fmt.weekdayLong(e.day)}", onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Share with crew") }, { menu = false; shareText(ctx, "Runsheet · ${e.name}", buildString { append("Runsheet · ${e.name} · ${Fmt.weekdayLong(e.day)}\n"); blocks.forEach { (b, l) -> append("\n$b ${l.first().blockTime}\n"); l.forEach { append("${if (it.done) "✓" else "○"} ${it.title} — ${it.crew} · ${it.duration}\n") } } }) })
                    DropdownMenuItem({ Text("Mark all done") }, { menu = false; tasks.filter { !it.done }.forEach { store.toggleTask(it.id) } })
                }
            }
        },
        docked = { MButton("Add task", { addSheet = true }, kind = ButtonKind.SECONDARY, icon = Icons.Outlined.Add) },
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            store.runsheetMarkers.forEach { mk ->
                val on = marker == mk
                Box(Modifier.height(36.dp).clip(CircleShape).background(if (on) m.accent else m.surface).border(1.dp, if (on) Color.Transparent else m.line, CircleShape).clickable { marker = mk }.padding(horizontal = Space.md), contentAlignment = Alignment.Center) {
                    Text(mk, style = Type.buttonSm, color = if (on) m.textOnPrimary else m.text)
                }
            }
        }
        if (tasks.isEmpty()) EmptyState("No tasks yet", "Add the setup, dressing and event-time tasks so the crew knows what happens when.", Modifier.padding(top = 80.dp), "Add task", Icons.Outlined.Add, true) { addSheet = true }
        blocks.forEach { (name, list) ->
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                SectionHeading(if (list.first().blockTime.isEmpty()) name else "$name · ${list.first().blockTime}", trailing = "${list.count { it.done }} of ${list.size} done")
                RowGroup {
                    list.forEachIndexed { i, t ->
                        if (i > 0) Hairline(56.dp)
                        var rowMenu by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().heightIn(min = Dim.row).padding(horizontal = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            Box(Modifier.size(Dim.touch).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); store.toggleTask(t.id) }, contentAlignment = Alignment.Center) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (t.done) m.accent else Color.Transparent).border(1.5.dp, if (t.done) m.accent else m.lineInput, CircleShape), contentAlignment = Alignment.Center) {
                                    if (t.done) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = m.textOnPrimary)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(t.title, style = Type.bodyMdStrong, color = if (t.done) m.textMute else m.text, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                                Text("${t.crew} · ${t.duration}", style = Type.caption, color = m.textMute)
                            }
                            Box {
                                IconButton44(Icons.Outlined.DragHandle, { rowMenu = true }, tint = m.textMute)
                                DropdownMenu(rowMenu, { rowMenu = false }) {
                                    DropdownMenuItem({ Text("Move up") }, { rowMenu = false; store.moveTask(t.id, -1) })
                                    DropdownMenuItem({ Text("Move down") }, { rowMenu = false; store.moveTask(t.id, 1) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (addSheet) {
        val names = blocks.keys.toList().ifEmpty { defaultBlocks }
        ModalBottomSheet(onDismissRequest = { addSheet = false }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            SheetContent("Add task", "It joins the block you pick.", actions = {
                MButton("Add task", { store.addTask(eventId, if (newBlock in names) newBlock else names[0], newTitle.trim(), newCrew.ifBlank { "Unassigned" }); newTitle = ""; newCrew = ""; addSheet = false }, enabled = newTitle.isNotBlank())
            }) {
                InputField("Task", newTitle, { newTitle = it }, placeholder = "Hang the entrance florals")
                InputField("Crew", newCrew, { newCrew = it }, placeholder = "Kavita · 2 crew")
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text("Block", style = Type.caption, color = m.textMute); SegmentedControl(names, if (newBlock in names) newBlock else names[0], { it }, { newBlock = it }) }
            }
        }
    }
}

/** B6 · Change request — show consequences, not a form. */
@Composable
fun ChangeRequestScreen(store: AppStore, nav: Nav, requestId: String) {
    val m = M
    val cr = store.changeRequest(requestId) ?: return
    val e = store.event(cr.eventId) ?: return
    val client = store.clientFor(e)
    val margin = (cr.revisedQuote - e.quoted) - cr.addedCost
    var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    Screen(
        title = "Change request", largeTitle = false, onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    if (client != null && client.phone.isNotBlank()) DropdownMenuItem({ Text("Call ${client.name}") }, { menu = false; dial(ctx, client.phone) })
                    DropdownMenuItem({ Text("Open event") }, { menu = false; nav.push(Routes.event(e.id)) })
                }
            }
        },
        docked = if (cr.status == ChangeRequestStatus.PENDING) ({
            MButton("Approve and send revised quote", { store.approveChangeRequest(cr.id); nav.back() })
            MButton("Decline change", { store.declineChangeRequest(cr.id); nav.back() }, kind = ButtonKind.TERTIARY, compact = true)
        }) else null,
    ) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("${cr.summary.trim()} ${Cal.daysBetween(Cal.todayDate, e.day)} days out.", style = Type.headingMd, color = m.text)
                Text("${e.name} · ${client?.name ?: "Client"} via ${cr.via}, ${Fmt.weekdayDayMonthTime(cr.requestedAt)}", style = Type.caption, color = m.textMute)
                if (cr.status != ChangeRequestStatus.PENDING) StatusChip(if (cr.status == ChangeRequestStatus.APPROVED) ChipKind.CONFIRMED else ChipKind.CANCELLED)
            }
        }
        RowGroup {
            cr.impacts.forEachIndexed { i, imp ->
                if (i > 0) Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = Dim.row).padding(horizontal = Space.lg, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    IconTile(when (imp.icon) { "people" -> Icons.Outlined.Groups; "transport" -> Icons.Outlined.LocalShipping; else -> Icons.Outlined.Inventory2 }, tint = if (imp.isShort) m.danger else m.textMute, background = if (imp.isShort) m.dangerTint else Color.Transparent)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(imp.resource, style = Type.bodyMdStrong, color = m.text); Text(imp.detail, style = Type.caption, color = if (imp.isShort) m.danger else m.textMute) }
                    Text(imp.delta, style = Type.bodyTabularStrong, color = m.text)
                }
            }
        }
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                TotalRow("Original quote", e.quoted); TotalRow("Added cost to you", cr.addedCost)
                HorizontalDivider(color = m.line)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Revised quote", style = Type.bodyMdStrong, color = m.text, modifier = Modifier.weight(1f)); Text(Fmt.inr(cr.revisedQuote), style = Type.headingLg, color = m.text) }
                Text("${cr.guestsDelta} guests at ₹${cr.perGuest} · ${Fmt.inr(margin)} margin on the change", style = Type.caption, color = if (margin < 0) m.danger else m.textMute)
            }
        }
    }
}

@Composable
fun TotalRow(label: String, amount: Int, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (strong) Type.bodyMdStrong else Type.bodyMd, color = if (strong) M.text else M.textMute, modifier = Modifier.weight(1f))
        Text(Fmt.inr(amount), style = Type.bodyTabularStrong, color = M.text)
    }
}
