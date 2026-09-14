package com.mehfil.app.model

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class Toast(val id: String, val kind: Kind, val message: String) {
    enum class Kind { SUCCESS, ERROR, INFO }
}

/** Single observable store: seed data, mutations and the toast. Navigation lives in the NavController. */
class AppStore(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mehfil", Context.MODE_PRIVATE)

    var events by mutableStateOf(Seed.events); private set
    var clients by mutableStateOf(Seed.clients); private set
    var milestones by mutableStateOf(Seed.milestones); private set
    var crew by mutableStateOf(Seed.crew); private set
    var inventory by mutableStateOf(Seed.inventory); private set
    var tasks by mutableStateOf(Seed.runsheet); private set
    var sentReminders by mutableStateOf(Seed.sentReminders); private set
    var attendance by mutableStateOf(Seed.attendance); private set
    var pendingChangeRequest by mutableStateOf(true); private set
    var toast by mutableStateOf<Toast?>(null); private set

    var appearance by mutableStateOf(Appearance.valueOf(prefs.getString("appearance", null) ?: Appearance.SYSTEM.name))
        private set
    var draft by mutableStateOf(loadDraft()); private set

    fun chooseAppearance(a: Appearance) { appearance = a; prefs.edit().putString("appearance", a.name).apply() }
    fun updateDraft(f: (EventDraft) -> EventDraft) { draft = f(draft); saveDraft() }

    private fun loadDraft(): EventDraft = EventDraft(
        clientName = prefs.getString("d.client", "") ?: "",
        phone = prefs.getString("d.phone", "") ?: "",
        type = prefs.getString("d.type", null)?.let { runCatching { EventType.valueOf(it) }.getOrNull() },
        date = prefs.getString("d.date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        venue = prefs.getString("d.venue", "") ?: "",
        guests = prefs.getInt("d.guests", 300),
        packageIds = prefs.getStringSet("d.packages", emptySet()) ?: emptySet(),
        quoted = prefs.getInt("d.quoted", 0),
    )
    private fun saveDraft() = prefs.edit()
        .putString("d.client", draft.clientName).putString("d.phone", draft.phone)
        .putString("d.type", draft.type?.name).putString("d.date", draft.date?.toString())
        .putString("d.venue", draft.venue).putInt("d.guests", draft.guests)
        .putStringSet("d.packages", draft.packageIds).putInt("d.quoted", draft.quoted).apply()

    private var toastJob: Job? = null
    fun show(kind: Toast.Kind, message: String) {
        val t = Toast(UUID.randomUUID().toString(), kind, message)
        toast = t
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(2800); if (toast?.id == t.id) toast = null }
    }

    // Lookups
    fun event(id: String) = events.firstOrNull { it.id == id }
    fun client(id: String) = clients.firstOrNull { it.id == id }
    fun clientFor(e: Event) = client(e.clientId)
    fun member(id: String) = crew.firstOrNull { it.id == id }
    fun item(id: String) = inventory.firstOrNull { it.id == id }
    fun milestonesFor(eventId: String) = milestones.filter { it.eventId == eventId }.sortedBy { it.due }
    fun milestone(id: String) = milestones.firstOrNull { it.id == id }

    val upcomingEvents get() = events.filter { it.status == EventStatus.UPCOMING }.sortedBy { it.start }
    val completedEvents get() = events.filter { it.status == EventStatus.COMPLETED }.sortedByDescending { it.start }
    fun eventsOn(day: LocalDate) = events.filter { it.day == day && it.status != EventStatus.CANCELLED }.sortedBy { it.start }
    fun eventsInMonth(month: Int, year: Int) = upcomingEvents.filter { it.start.monthValue == month && it.start.year == year }
    /** Events on the same day with overlapping hours (A3, B3, E3). */
    fun conflictsOn(day: LocalDate): List<Event> {
        val list = eventsOn(day)
        return list.filter { e -> list.any { it.id != e.id && it.start < e.end && e.start < it.end } }
    }
    val seasonConflict: Pair<Event, Event>?
        get() {
            val a = event("kapoor-sangeet"); val b = event("gill-engagement")
            return if (a != null && b != null && a.hasCrewConflict && b.hasCrewConflict) a to b else null
        }

    // Money
    fun paymentState(e: Event): PaymentState {
        val ms = milestonesFor(e.id)
        return when {
            ms.any { it.status == MilestoneStatus.OVERDUE } -> PaymentState.OVERDUE
            ms.isNotEmpty() && ms.all { it.status == MilestoneStatus.PAID } -> PaymentState.PAID
            ms.any { it.status == MilestoneStatus.PAID } -> PaymentState.PARTIAL
            else -> PaymentState.PENDING
        }
    }
    fun collected(eventId: String) = milestonesFor(eventId).filter { it.status == MilestoneStatus.PAID }.sumOf { it.amount }
    fun balanceDue(eventId: String) = milestonesFor(eventId).filter { it.isOpen }.sumOf { it.amount }
    fun overdueAmount(eventId: String) = milestonesFor(eventId).filter { it.status == MilestoneStatus.OVERDUE }.sumOf { it.amount }

    val openMilestones get() = milestones.filter { it.isOpen }.sortedBy { it.due }
    val overdue get() = openMilestones.filter { it.status == MilestoneStatus.OVERDUE || it.due < Cal.todayDate }
        .sortedBy { event(it.eventId)?.start?.toLocalDate() ?: it.due }
    val dueThisWeek get() = openMilestones.filter { it !in overdue && it.due in Cal.thisWeek }
    val upcomingMilestones get() = openMilestones.filter { it !in overdue && it !in dueThisWeek }
    val totalOutstanding get() = openMilestones.sumOf { it.amount }
    val overdueTotal get() = overdue.sumOf { it.amount }
    val eventsWithOpenMoney get() = openMilestones.map { it.eventId }.toSet().size
    val overdueSurnames get() = overdue.sortedByDescending { it.amount }.mapNotNull { event(it.eventId)?.surname }.distinct().joinToString(", ")

    fun recordPayment(milestoneId: String, amount: Int, method: PaymentMethod, date: LocalDate, reference: String?) {
        val i = milestones.indexOfFirst { it.id == milestoneId }; if (i < 0) return
        val m = milestones[i]
        val list = milestones.toMutableList()
        list[i] = m.copy(status = MilestoneStatus.PAID, paidOn = date, method = method, reference = reference, amount = minOf(amount, m.amount))
        if (amount < m.amount) list.add(i + 1, m.copy(id = "$milestoneId-rest", amount = m.amount - amount, status = MilestoneStatus.PENDING))
        milestones = list
        show(Toast.Kind.SUCCESS, "Recorded ${Fmt.inr(amount)}")
    }
    fun addMilestone(eventId: String, name: String, amount: Int, due: LocalDate) {
        milestones = milestones + Milestone(UUID.randomUUID().toString(), eventId, name, amount, due, MilestoneStatus.PENDING, MilestoneKind.INSTALMENT)
        show(Toast.Kind.SUCCESS, "Milestone added")
    }
    fun sendReminder(milestoneId: String, tone: ReminderTone, channel: ReminderChannel) {
        val r = SentReminder(UUID.randomUUID().toString(), tone, Cal.today, channel, if (channel == ReminderChannel.WHATSAPP) "sent" else "delivered")
        sentReminders = sentReminders + (milestoneId to (sentReminders[milestoneId].orEmpty() + r))
        show(Toast.Kind.SUCCESS, "Sent on ${channel.label}")
    }
    /** Composed reminder copy — real text for each tone (spec C4). */
    fun reminderMessage(m: Milestone, tone: ReminderTone): String {
        val e = event(m.eventId) ?: return ""; val c = clientFor(e) ?: return ""
        val first = c.firstName; val amount = Fmt.inr(m.amount); val due = Fmt.dayMonthLong(m.due)
        val eventDate = Fmt.dayMonthLong(e.day); val name = m.name.lowercase()
        return when (tone) {
            ReminderTone.GENTLE -> "Hi $first, hope the preparations are going well! Just a gentle nudge — the $name of $amount for the ${e.type.word} was due on $due. Whenever convenient this week works. Thanks so much — ${Seed.vendorFirstName}, ${Seed.businessName}"
            ReminderTone.STANDARD -> "Hi $first, a reminder that the $name of $amount for the ${e.type.word} on $eventDate was due on $due. You can pay by UPI to ${Seed.upi} or by bank transfer. Thank you — ${Seed.vendorFirstName}, ${Seed.businessName}"
            ReminderTone.FIRM -> "$first, the $name of $amount for the ${e.type.word} on $eventDate is ${Fmt.daysLate(m.due)} days overdue. We need it cleared by tomorrow to hold the crew and inventory for your date. Please pay by UPI to ${Seed.upi} today. — ${Seed.vendorFirstName}, ${Seed.businessName}"
        }
    }

    // Events
    fun toggleTask(id: String) {
        tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
        tasks.firstOrNull { it.id == id }?.let { syncRunsheetSummary(it.eventId) }
    }
    fun moveTask(id: String, delta: Int) {
        val t = tasks.first { it.id == id }
        val block = tasks.filter { it.eventId == t.eventId && it.block == t.block }.toMutableList()
        val i = block.indexOfFirst { it.id == id }; val j = i + delta
        if (j !in block.indices) return
        block.removeAt(i); block.add(j, t)
        val others = tasks.filter { !(it.eventId == t.eventId && it.block == t.block) }
        val order = mapOf("Setup" to 0, "Dressing" to 1, "Event" to 2)
        tasks = (others + block).sortedBy { order[it.block] ?: 9 }
    }
    fun addTask(eventId: String, block: String, title: String, crewText: String) {
        val time = tasks.firstOrNull { it.eventId == eventId && it.block == block }?.blockTime ?: ""
        tasks = tasks + RunsheetTask(UUID.randomUUID().toString(), eventId, block, time, title, crewText, "—", false)
        syncRunsheetSummary(eventId); show(Toast.Kind.SUCCESS, "Task added")
    }
    private fun syncRunsheetSummary(eventId: String) {
        val ts = tasks.filter { it.eventId == eventId }
        events = events.map { if (it.id == eventId) it.copy(runsheetSummary = "${ts.size} tasks · setup 09:00 · ${ts.count { t -> t.done }} done") else it }
    }
    fun approveChangeRequest(eventId: String) {
        events = events.map { if (it.id == eventId) it.copy(guests = 650, quoted = 710_000, inventorySummary = "500 chairs · 120 drapes · 24 par cans") else it }
        pendingChangeRequest = false; show(Toast.Kind.SUCCESS, "Revised quote sent to Vikram")
    }
    fun declineChangeRequest() { pendingChangeRequest = false; show(Toast.Kind.INFO, "Change declined") }
    fun cancelEvent(id: String) {
        events = events.map { if (it.id == id) it.copy(status = EventStatus.CANCELLED) else it }
        show(Toast.Kind.INFO, "${event(id)?.name} cancelled")
    }
    fun createEvent(d: EventDraft): Event? {
        val type = d.type ?: return null; val date = d.date ?: return null
        if (d.clientName.isBlank()) return null
        val surname = d.clientName.trim().substringAfterLast(' ')
        val hue = EventHue.entries[events.size % 4]
        val existing = clients.firstOrNull { it.surname.equals(surname, ignoreCase = true) }
        val clientId = existing?.id ?: (surname.lowercase() + "-" + clients.size).also { id ->
            clients = clients + Client(id, d.clientName, d.phone, false, 1, d.quoted, date, (clients.size % 8) + 1)
        }
        val start = date.atTime(18, 0); val end = start.plusHours(5)
        val parts = d.venue.split(",").map { it.trim() }
        val venue = Venue(parts.firstOrNull() ?: d.venue, parts.getOrNull(1) ?: "")
        val e = Event("${surname.lowercase()}-${type.word}-${date}", "$surname ${type.word}", clientId, type, start, end, venue, d.guests, d.quoted, hue,
            EventStatus.UPCOMING, 0, maxOf(6, d.guests / 40), conflictsOn(date).isNotEmpty(), "Not planned yet", "No tasks yet")
        events = events + e
        milestones = milestones + Milestone("${e.id}-adv", e.id, "Booking advance", d.quoted / 4, Cal.todayDate, MilestoneStatus.PENDING, MilestoneKind.ADVANCE) +
            Milestone("${e.id}-bal", e.id, "Balance", d.quoted - d.quoted / 4, date, MilestoneStatus.PENDING, MilestoneKind.BALANCE)
        draft = EventDraft(); saveDraft()
        show(Toast.Kind.SUCCESS, "${e.name} created")
        return e
    }

    // Crew
    fun attendanceFor(eventId: String) = attendance[eventId].orEmpty()
    fun cycleAttendance(eventId: String, crewId: String) {
        val cur = attendance[eventId]?.get(crewId) ?: AttendanceState.CONFIRMED
        attendance = attendance + (eventId to (attendance[eventId].orEmpty() + (crewId to cur.next)))
    }
    fun assignedCrew(eventId: String) = crew.filter { eventId in it.bookedOn }
    fun toggleAssignment(crewId: String, eventId: String) {
        val m = member(crewId) ?: return
        val on = eventId in m.bookedOn
        crew = crew.map { if (it.id == crewId) it.copy(bookedOn = if (on) it.bookedOn - eventId else it.bookedOn + eventId) else it }
        events = events.map { if (it.id == eventId) it.copy(crewAssigned = it.crewAssigned + if (on) -1 else 1) else it }
    }
    fun moveTeam(crewId: String, to: String, from: String) {
        val m = member(crewId) ?: return
        crew = crew.map { if (it.id == crewId) it.copy(bookedOn = (it.bookedOn - from).let { b -> if (to in b) b else b + to }) else it }
        events = events.map { if (it.id == to || it.id == from) it.copy(hasCrewConflict = false) else it }
        show(Toast.Kind.SUCCESS, "${m.firstName}'s team moved to ${event(to)?.surname}")
    }
    /** "Suresh's team conflicted" when a roster member is double-booked that day; otherwise the same-day events. */
    fun crewConflictNote(e: Event): String? {
        val others = eventsOn(e.day).filter { it.id != e.id }
        if (others.isEmpty()) return null
        crew.firstOrNull { m -> e.id in m.bookedOn && m.bookedOn.any { id -> others.any { it.id == id } } }?.let { return "${it.firstName}'s team conflicted" }
        return if (e.hasCrewConflict) "same day as " + others.joinToString(" · ") { it.surname } else null
    }
}
