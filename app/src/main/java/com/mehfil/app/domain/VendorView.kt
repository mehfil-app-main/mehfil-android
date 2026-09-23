package com.mehfil.app.domain

import com.mehfil.app.model.Cal
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.ChangeRequest
import com.mehfil.app.model.ChangeRequestStatus
import com.mehfil.app.model.Client
import com.mehfil.app.model.CrewMember
import com.mehfil.app.model.EnquiryStatus
import com.mehfil.app.model.Event
import com.mehfil.app.model.EventStatus
import com.mehfil.app.model.Milestone
import com.mehfil.app.model.MilestoneStatus
import com.mehfil.app.model.PaymentState
import com.mehfil.app.model.VendorData
import java.time.LocalDate

/**
 * The read model: every ordering, index and bucket the UI needs, derived once per [VendorData] change.
 *
 * This exists because the screens read the same derived collections many times per frame. Computing them as
 * `get()` properties meant re-sorting the roster and re-filtering the milestone list on every recomposition,
 * and the lookups (`event(id)`) were linear scans inside loops. Everything here is computed eagerly in the
 * constructor and read as a plain field afterwards.
 *
 * Pure Kotlin on purpose — no Android types, no repository — so the rules that matter commercially (what
 * counts as a conflict, what counts as overdue) are unit-testable without a device. [today] is a parameter
 * rather than a call to the clock so tests can pin it.
 */
class VendorView(private val data: VendorData, private val today: LocalDate = Cal.todayDate) {

    val events: List<Event> = data.events
    val clients: List<Client> = data.clients
    val milestones: List<Milestone> = data.milestones
    val crew: List<CrewMember> = data.crew.sortedWith(compareBy({ it.role.ordinal }, { it.name }))
    val inventory = data.inventory
    val tasks = data.tasks.sortedBy { it.order }
    val reminders = data.reminders
    val expenses = data.expenses
    val enquiries = data.enquiries.sortedByDescending { it.createdAt }
    val packages = data.packages
    val changeRequests = data.changeRequests

    // Indices: the screens look records up by id inside list bodies, which was O(n) per row.
    private val eventsById = events.associateBy { it.id }
    private val clientsById = clients.associateBy { it.id }
    private val crewById = crew.associateBy { it.id }
    private val inventoryById = inventory.associateBy { it.id }
    private val enquiriesById = enquiries.associateBy { it.id }
    private val milestonesByEvent = milestones.groupBy { it.eventId }.mapValues { (_, ms) -> ms.sortedBy { it.due } }
    private val expensesByEvent = expenses.groupBy { it.eventId }
    private val remindersByMilestone = reminders.groupBy { it.milestoneId }.mapValues { (_, rs) -> rs.sortedBy { it.sentAt } }
    private val eventsByDay = events.filter { it.status != EventStatus.CANCELLED }.groupBy { it.day }.mapValues { (_, es) -> es.sortedBy { it.start } }

    fun event(id: String) = eventsById[id]
    fun client(id: String) = clientsById[id]
    fun clientFor(e: Event) = clientsById[e.clientId]
    fun member(id: String) = crewById[id]
    fun item(id: String) = inventoryById[id]
    fun enquiry(id: String) = enquiriesById[id]
    fun milestonesFor(eventId: String) = milestonesByEvent[eventId].orEmpty()
    fun expensesFor(eventId: String) = expensesByEvent[eventId].orEmpty()
    fun remindersFor(milestoneId: String) = remindersByMilestone[milestoneId].orEmpty()
    fun eventsOn(day: LocalDate) = eventsByDay[day].orEmpty()
    fun eventsInMonth(month: Int, year: Int) =
        events.filter { it.status != EventStatus.CANCELLED && it.start.monthValue == month && it.start.year == year }.sortedBy { it.start }

    fun pendingChangeRequest(eventId: String) = changeRequests.firstOrNull { it.eventId == eventId && it.status == ChangeRequestStatus.PENDING }
    fun changeRequest(id: String) = changeRequests.firstOrNull { it.id == id }

    val upcomingEvents = events.filter { it.status == EventStatus.UPCOMING }.sortedBy { it.start }
    val completedEvents = events.filter { it.status == EventStatus.COMPLETED }.sortedByDescending { it.start }
    val openEnquiries = enquiries.filter { it.status == EnquiryStatus.OPEN }

    // ---- Conflicts ----

    /** Two events clash when they overlap in time. Same-day but non-overlapping is not a conflict. */
    private fun overlap(a: Event, b: Event) = a.id != b.id && a.start < b.end && b.start < a.end

    private val conflictedEventIds: Set<String> = buildSet {
        for (m in crew) {
            val booked = m.bookings.mapNotNull { eventsById[it] }.filter { it.status == EventStatus.UPCOMING }
            for (a in booked) if (booked.any { overlap(a, it) }) add(a.id)
        }
    }

    /** A crew conflict is a fact about bookings: someone is booked on two events that overlap in time. */
    fun isConflicted(e: Event) = e.id in conflictedEventIds

    fun conflictsOn(day: LocalDate): List<Event> {
        val list = eventsOn(day)
        return list.filter { e -> list.any { overlap(e, it) } }
    }

    /** The first double-booking still ahead of us: (event, the other event, the member booked on both). */
    val seasonConflict: Triple<Event, Event, CrewMember>? = run {
        for (m in crew) {
            val booked = m.bookings.mapNotNull { eventsById[it] }
                .filter { it.status == EventStatus.UPCOMING && it.day >= today }
                .sortedBy { it.start }
            for (a in booked) booked.firstOrNull { overlap(a, it) }?.let { return@run Triple(a, it, m) }
        }
        null
    }

    fun crewOnDuty(day: LocalDate): Int {
        val ids = eventsOn(day).mapTo(mutableSetOf()) { it.id }
        return crew.count { m -> m.bookings.any { it in ids } }
    }

    fun assignedCrew(eventId: String) = crew.filter { eventId in it.bookings }

    /** The member double-booked across [e] and another event that day, for the inline warning on B2. */
    fun conflictedMember(e: Event): CrewMember? {
        val otherIds = eventsOn(e.day).filter { it.id != e.id }.mapTo(mutableSetOf()) { it.id }
        if (otherIds.isEmpty()) return null
        return crew.firstOrNull { m -> e.id in m.bookings && m.bookings.any { it in otherIds } }
    }

    // ---- Money ----

    fun paymentState(e: Event): PaymentState {
        val ms = milestonesFor(e.id)
        return when {
            ms.any { isOverdue(it) } -> PaymentState.OVERDUE
            ms.isNotEmpty() && ms.all { it.status == MilestoneStatus.PAID } -> PaymentState.PAID
            ms.any { it.status == MilestoneStatus.PAID } -> PaymentState.PARTIAL
            else -> PaymentState.PENDING
        }
    }

    fun collected(eventId: String) = milestonesFor(eventId).filter { it.status == MilestoneStatus.PAID }.sumOf { it.amount }
    fun balanceDue(eventId: String) = milestonesFor(eventId).filter { it.isOpen }.sumOf { it.amount }
    fun overdueAmount(eventId: String) = milestonesFor(eventId).filter { isOverdue(it) }.sumOf { it.amount }

    /** Single definition of "late", evaluated against this view's [today] rather than the clock. */
    fun isOverdue(m: Milestone) = m.effectiveStatusOn(today) == MilestoneStatus.OVERDUE

    val openMilestones = milestones
        .filter { it.isOpen && eventsById[it.eventId]?.status != EventStatus.CANCELLED }
        .sortedBy { it.due }
    val overdue = openMilestones
        .filter { isOverdue(it) }
        .sortedBy { eventsById[it.eventId]?.day ?: it.due }
    val dueThisWeek = openMilestones.filter { !isOverdue(it) && it.due in Cal.thisWeek }
    val upcomingMilestones = openMilestones - overdue.toSet() - dueThisWeek.toSet()
    val totalOutstanding = openMilestones.sumOf { it.amount }
    val overdueTotal = overdue.sumOf { it.amount }
    val eventsWithOpenMoney = openMilestones.mapTo(mutableSetOf()) { it.eventId }.size
    val overdueSurnames = overdue.sortedByDescending { it.amount }.mapNotNull { eventsById[it.eventId]?.surname }.distinct().joinToString(", ")

    val lifetimeValue = events.filter { it.status != EventStatus.CANCELLED }.sumOf { it.quoted }

    fun stats(c: Client): ClientStats {
        val evs = events.filter { it.clientId == c.id && it.status != EventStatus.CANCELLED }
        return ClientStats(
            eventCount = evs.size,
            lifetimeValue = evs.sumOf { it.quoted },
            nextEventDate = evs.filter { it.status == EventStatus.UPCOMING && it.day >= today }.minOfOrNull { it.day },
        )
    }

    fun attendanceFor(eventId: String) = data.attendance.firstOrNull { it.id == eventId }?.states.orEmpty()

    // ---- Season ----

    val season: SeasonSummary = run {
        val start = Cal.seasonStart(today)
        val end = start.plusMonths(SEASON_MONTHS)
        val inSeason = events.filter { it.status != EventStatus.CANCELLED && it.day >= start && it.day < end }
        val months = (0 until SEASON_MONTHS).map { i ->
            val m = start.plusMonths(i)
            Fmt.monthShort(m) to inSeason.filter { it.start.monthValue == m.monthValue && it.start.year == m.year }.sumOf { it.quoted }
        }
        SeasonSummary(inSeason.sumOf { it.quoted }, inSeason.size, months, Cal.seasonLabel(today))
    }

    private companion object {
        /** An Indian wedding season runs October to February — five months (spec §2). */
        const val SEASON_MONTHS = 5L
    }
}

data class ClientStats(val eventCount: Int, val lifetimeValue: Int, val nextEventDate: LocalDate?)

data class SeasonSummary(val booked: Int, val count: Int, val months: List<Pair<String, Int>>, val label: String)
