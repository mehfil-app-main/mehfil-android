package com.mehfil.app.model

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehfil.app.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

data class Toast(val id: String, val kind: Kind, val message: String) { enum class Kind { SUCCESS, ERROR, INFO } }

/** Single observable store over the repository. Navigation lives in the NavController. */
class AppStore(app: Application) : AndroidViewModel(app) {
    val auth = AuthService(app)
    val repo: Repository = if (BuildConfig.HAS_FIREBASE_CONFIG) FirestoreRepository() else LocalRepository(app)
    private val prefs = app.getSharedPreferences("mehfil", Context.MODE_PRIVATE)

    var uid by mutableStateOf<String?>(null); private set
    var vendor by mutableStateOf<VendorProfile?>(null); private set
    var profileChecked by mutableStateOf(false); private set
    var data by mutableStateOf(VendorData()); private set
    var loaded by mutableStateOf(false); private set
    var toast by mutableStateOf<Toast?>(null); private set

    var appearance by mutableStateOf(Appearance.valueOf(prefs.getString("appearance", null) ?: Appearance.SYSTEM.name)); private set
    var draft by mutableStateOf(loadDraft()); private set

    val needsOnboarding get() = uid != null && profileChecked && vendor == null

    // MARK: Session

    fun bind(newUid: String) {
        if (uid == newUid) return
        uid = newUid; profileChecked = false; loaded = false; vendor = null; data = VendorData()
        viewModelScope.launch {
            try { vendor = repo.profile(newUid) } catch (e: Exception) { show(Toast.Kind.ERROR, "Couldn't load your profile: ${e.message}") }
            profileChecked = true
            repo.observe(newUid) { snapshot -> data = snapshot; loaded = true }
        }
    }
    fun unbind() { repo.stopObserving(); uid = null; vendor = null; profileChecked = false; loaded = false; data = VendorData() }

    suspend fun completeOnboarding(profile: VendorProfile, loadSample: Boolean) {
        val id = uid ?: return
        try {
            var p = profile
            if (loadSample) {
                val s = SampleSeason.profile()
                p = p.copy(since = s.since, upi = s.upi, bankLabel = s.bankLabel, lastSeasonBooked = s.lastSeasonBooked, lastSeasonEvents = s.lastSeasonEvents)
                repo.write(id, Mapping.writes(SampleSeason.data()))
            } else repo.write(id, SampleSeason.defaultPackages().map { WriteOp.Set(Coll.packages, it.id, Mapping.of(it)) })
            repo.saveProfile(id, p); vendor = p
        } catch (e: Exception) { show(Toast.Kind.ERROR, "Couldn't save your profile: ${e.message}") }
    }
    fun updateProfile(change: (VendorProfile) -> VendorProfile) {
        val id = uid ?: return; val p = change(vendor ?: return); vendor = p
        viewModelScope.launch { try { repo.saveProfile(id, p) } catch (e: Exception) { show(Toast.Kind.ERROR, "Couldn't save: ${e.message}") } }
    }
    suspend fun loadSampleSeason() { val id = uid ?: return; try { repo.write(id, Mapping.writes(SampleSeason.data())); show(Toast.Kind.SUCCESS, "Sample season loaded") } catch (e: Exception) { show(Toast.Kind.ERROR, "Couldn't load the sample: ${e.message}") } }
    suspend fun eraseAllData() { val id = uid ?: return; repo.deleteEverything(id) }

    private fun save(ops: List<WriteOp>, success: String? = null) {
        val id = uid ?: return
        viewModelScope.launch {
            try { repo.write(id, ops); if (success != null) show(Toast.Kind.SUCCESS, success) }
            catch (e: Exception) { show(Toast.Kind.ERROR, "Couldn't save: ${e.message}") }
        }
    }
    private fun save(op: WriteOp, success: String? = null) = save(listOf(op), success)

    fun chooseAppearance(a: Appearance) { appearance = a; prefs.edit().putString("appearance", a.name).apply() }
    fun updateDraft(f: (EventDraft) -> EventDraft) { draft = f(draft); saveDraft() }
    fun replaceDraft(d: EventDraft) { draft = d; saveDraft() }
    private fun loadDraft() = EventDraft(
        prefs.getString("d.client", "") ?: "", prefs.getString("d.phone", "") ?: "",
        prefs.getString("d.type", null)?.let { s -> runCatching { EventType.valueOf(s) }.getOrNull() },
        prefs.getString("d.date", null)?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() },
        prefs.getString("d.venue", "") ?: "", prefs.getInt("d.guests", 300), prefs.getStringSet("d.packages", emptySet()) ?: emptySet(), prefs.getInt("d.quoted", 0), prefs.getString("d.enquiry", null),
    )
    private fun saveDraft() = prefs.edit().putString("d.client", draft.clientName).putString("d.phone", draft.phone).putString("d.type", draft.type?.name)
        .putString("d.date", draft.date?.toString()).putString("d.venue", draft.venue).putInt("d.guests", draft.guests)
        .putStringSet("d.packages", draft.packageIds).putInt("d.quoted", draft.quoted).putString("d.enquiry", draft.fromEnquiryId).apply()

    private var toastJob: Job? = null
    fun show(kind: Toast.Kind, message: String) {
        val t = Toast(Cal.newId(), kind, message); toast = t
        toastJob?.cancel(); toastJob = viewModelScope.launch { delay(2800); if (toast?.id == t.id) toast = null }
    }

    // MARK: Collections

    val events get() = data.events
    val clients get() = data.clients
    val milestones get() = data.milestones
    val crew get() = data.crew.sortedWith(compareBy({ it.role.ordinal }, { it.name }))
    val inventory get() = data.inventory
    val tasks get() = data.tasks.sortedBy { it.order }
    val reminders get() = data.reminders
    val expenses get() = data.expenses
    val enquiries get() = data.enquiries.sortedByDescending { it.createdAt }
    val packages get() = data.packages
    val changeRequests get() = data.changeRequests

    fun event(id: String) = events.firstOrNull { it.id == id }
    fun client(id: String) = clients.firstOrNull { it.id == id }
    fun clientFor(e: Event) = client(e.clientId)
    fun member(id: String) = crew.firstOrNull { it.id == id }
    fun item(id: String) = inventory.firstOrNull { it.id == id }
    fun enquiry(id: String) = enquiries.firstOrNull { it.id == id }
    fun milestonesFor(eventId: String) = milestones.filter { it.eventId == eventId }.sortedBy { it.due }
    fun expensesFor(eventId: String) = expenses.filter { it.eventId == eventId }
    fun remindersFor(milestoneId: String) = reminders.filter { it.milestoneId == milestoneId }.sortedBy { it.sentAt }
    fun pendingChangeRequest(eventId: String) = changeRequests.firstOrNull { it.eventId == eventId && it.status == ChangeRequestStatus.PENDING }
    fun changeRequest(id: String) = changeRequests.firstOrNull { it.id == id }

    val upcomingEvents get() = events.filter { it.status == EventStatus.UPCOMING }.sortedBy { it.start }
    val completedEvents get() = events.filter { it.status == EventStatus.COMPLETED }.sortedByDescending { it.start }
    val openEnquiries get() = enquiries.filter { it.status == EnquiryStatus.OPEN }
    fun eventsOn(day: LocalDate) = events.filter { it.day == day && it.status != EventStatus.CANCELLED }.sortedBy { it.start }
    fun eventsInMonth(month: Int, year: Int) = events.filter { it.status != EventStatus.CANCELLED && it.start.monthValue == month && it.start.year == year }.sortedBy { it.start }
    fun conflictsOn(day: LocalDate): List<Event> { val list = eventsOn(day); return list.filter { e -> list.any { it.id != e.id && it.start < e.end && e.start < it.end } } }
    private fun overlap(a: Event, b: Event) = a.id != b.id && a.start < b.end && b.start < a.end

    /** A crew conflict is a fact about bookings: someone is booked on two events that overlap in time. */
    fun isConflicted(e: Event): Boolean = e.status == EventStatus.UPCOMING &&
        crew.any { m -> e.id in m.bookings && m.bookings.any { other -> event(other)?.let { overlap(e, it) } == true } }
    /** The first double-booking in the season: (event, other event, member). */
    val seasonConflict: Triple<Event, Event, CrewMember>?
        get() {
            for (m in crew) {
                val booked = m.bookings.mapNotNull { event(it) }.filter { it.status == EventStatus.UPCOMING && it.day >= Cal.todayDate }.sortedBy { it.start }
                for (a in booked) booked.firstOrNull { overlap(a, it) }?.let { return Triple(a, it, m) }
            }
            return null
        }
    fun crewOnDuty(day: LocalDate): Int { val ids = eventsOn(day).map { it.id }.toSet(); return crew.count { m -> m.bookings.any { it in ids } } }

    // MARK: Money

    fun paymentState(e: Event): PaymentState {
        val ms = milestonesFor(e.id)
        return when {
            ms.any { it.effectiveStatus == MilestoneStatus.OVERDUE } -> PaymentState.OVERDUE
            ms.isNotEmpty() && ms.all { it.status == MilestoneStatus.PAID } -> PaymentState.PAID
            ms.any { it.status == MilestoneStatus.PAID } -> PaymentState.PARTIAL
            else -> PaymentState.PENDING
        }
    }
    fun collected(eventId: String) = milestonesFor(eventId).filter { it.status == MilestoneStatus.PAID }.sumOf { it.amount }
    fun balanceDue(eventId: String) = milestonesFor(eventId).filter { it.isOpen }.sumOf { it.amount }
    fun overdueAmount(eventId: String) = milestonesFor(eventId).filter { it.effectiveStatus == MilestoneStatus.OVERDUE }.sumOf { it.amount }

    val openMilestones get() = milestones.filter { it.isOpen && event(it.eventId)?.status != EventStatus.CANCELLED }.sortedBy { it.due }
    val overdue get() = openMilestones.filter { it.effectiveStatus == MilestoneStatus.OVERDUE }.sortedBy { event(it.eventId)?.day ?: it.due }
    val dueThisWeek get() = openMilestones.filter { it.effectiveStatus != MilestoneStatus.OVERDUE && it.due in Cal.thisWeek }
    val upcomingMilestones get() = openMilestones.filter { it.effectiveStatus != MilestoneStatus.OVERDUE && it !in dueThisWeek }
    val totalOutstanding get() = openMilestones.sumOf { it.amount }
    val overdueTotal get() = overdue.sumOf { it.amount }
    val eventsWithOpenMoney get() = openMilestones.map { it.eventId }.toSet().size
    val overdueSurnames get() = overdue.sortedByDescending { it.amount }.mapNotNull { event(it.eventId)?.surname }.distinct().joinToString(", ")

    fun recordPayment(milestoneId: String, amount: Int, method: PaymentMethod, date: LocalDate, reference: String?) {
        var m = milestones.firstOrNull { it.id == milestoneId } ?: return
        val ops = mutableListOf<WriteOp>()
        if (amount < m.amount) {
            val rest = m.copy(id = Cal.newId(), amount = m.amount - amount, status = MilestoneStatus.PENDING, paidOn = null, method = null, reference = null)
            ops += WriteOp.Set(Coll.milestones, rest.id, Mapping.of(rest)); m = m.copy(amount = amount)
        }
        m = m.copy(status = MilestoneStatus.PAID, paidOn = date, method = method, reference = reference)
        ops += WriteOp.Set(Coll.milestones, m.id, Mapping.of(m))
        save(ops, "Recorded ${Fmt.inr(amount)}")
    }
    fun addMilestone(eventId: String, name: String, amount: Int, due: LocalDate) {
        val m = Milestone(Cal.newId(), eventId, name, amount, due, MilestoneStatus.PENDING, MilestoneKind.INSTALMENT)
        save(WriteOp.Set(Coll.milestones, m.id, Mapping.of(m)), "Milestone added")
    }
    fun sendReminder(milestoneId: String, tone: ReminderTone, channel: ReminderChannel) {
        val r = SentReminder(Cal.newId(), milestoneId, tone, channel, Cal.today, if (channel == ReminderChannel.WHATSAPP) "sent" else "delivered")
        save(WriteOp.Set(Coll.reminders, r.id, Mapping.of(r)), "Sent on ${channel.label}")
    }
    fun reminderMessage(m: Milestone, tone: ReminderTone): String {
        val e = event(m.eventId) ?: return ""; val c = clientFor(e) ?: return ""; val v = vendor ?: return ""
        val first = c.firstName; val amount = Fmt.inr(m.amount); val due = Fmt.dayMonthLong(m.due); val eventDate = Fmt.dayMonthLong(e.day); val name = m.name.lowercase()
        val pay = if (v.upi.isEmpty()) "by bank transfer" else "by UPI to ${v.upi} or by bank transfer"
        return when (tone) {
            ReminderTone.GENTLE -> "Hi $first, hope the preparations are going well! Just a gentle nudge — the $name of $amount for the ${e.type.word} was due on $due. Whenever convenient this week works. Thanks so much — ${v.ownerFirstName}, ${v.businessName}"
            ReminderTone.STANDARD -> "Hi $first, a reminder that the $name of $amount for the ${e.type.word} on $eventDate was due on $due. You can pay $pay. Thank you — ${v.ownerFirstName}, ${v.businessName}"
            ReminderTone.FIRM -> "$first, the $name of $amount for the ${e.type.word} on $eventDate is ${Fmt.daysLate(m.due)} days overdue. We need it cleared by tomorrow to hold the crew and inventory for your date. Please pay ${if (v.upi.isEmpty()) "today" else "by UPI to ${v.upi} today"}. — ${v.ownerFirstName}, ${v.businessName}"
        }
    }
    fun addExpense(eventId: String, name: String, detail: String, amount: Int) { val x = Expense(Cal.newId(), eventId, name, detail, amount); save(WriteOp.Set(Coll.expenses, x.id, Mapping.of(x)), "Expense added") }
    fun deleteExpense(id: String) = save(WriteOp.Delete(Coll.expenses, id))

    // MARK: Events

    fun toggleTask(id: String) { val t = tasks.firstOrNull { it.id == id } ?: return; save(WriteOp.Set(Coll.tasks, id, Mapping.of(t.copy(done = !t.done)))) }
    fun moveTask(id: String, delta: Int) {
        val t = tasks.firstOrNull { it.id == id } ?: return
        val block = tasks.filter { it.eventId == t.eventId && it.block == t.block }.toMutableList()
        val i = block.indexOfFirst { it.id == id }; val j = i + delta
        if (j !in block.indices) return
        block[i] = block[j].also { block[j] = block[i] }
        val base = block.minOf { it.order }
        save(block.mapIndexed { k, x -> WriteOp.Set(Coll.tasks, x.id, Mapping.of(x.copy(order = base + k))) })
    }
    fun addTask(eventId: String, block: String, title: String, crewText: String) {
        val siblings = tasks.filter { it.eventId == eventId }
        val t = RunsheetTask(Cal.newId(), eventId, block, siblings.firstOrNull { it.block == block }?.blockTime ?: "", title, crewText, "—", false, (siblings.maxOfOrNull { it.order } ?: -1) + 1)
        save(WriteOp.Set(Coll.tasks, t.id, Mapping.of(t)), "Task added")
    }
    fun runsheetSummary(e: Event): String { val ts = tasks.filter { it.eventId == e.id }; return if (ts.isEmpty()) "No tasks yet" else "${ts.size} tasks · ${ts.count { it.done }} done" }
    val runsheetMarkers = listOf("09:00", "12:00", "14:00", "17:00", "21:00", "23:30")

    fun approveChangeRequest(id: String) {
        val cr = changeRequest(id) ?: return; val e = event(cr.eventId) ?: return
        save(listOf(WriteOp.Set(Coll.events, e.id, Mapping.of(e.copy(guests = e.guests + cr.guestsDelta, quoted = cr.revisedQuote))),
            WriteOp.Set(Coll.changeRequests, cr.id, Mapping.of(cr.copy(status = ChangeRequestStatus.APPROVED)))), "Revised quote sent to ${clientFor(e)?.firstName ?: "the client"}")
    }
    fun declineChangeRequest(id: String) { val cr = changeRequest(id) ?: return; save(WriteOp.Set(Coll.changeRequests, id, Mapping.of(cr.copy(status = ChangeRequestStatus.DECLINED))), "Change declined") }
    /** Builds a change request from a guest delta using the vendor's actual holds and roster (B6 reasoning). */
    fun logChangeRequest(eventId: String, guestsDelta: Int, via: String): ChangeRequest? {
        val e = event(eventId) ?: return null; val day = e.day
        val impacts = mutableListOf<ChangeImpact>(); var added = 0
        inventory.firstOrNull { it.name.contains("chair", true) }?.let { chairs ->
            val c = chairs.committed(day); val new = c + guestsDelta
            if (new > chairs.total) { val hire = new - chairs.total; added += hire * 40; impacts += ChangeImpact(chairs.name, "All ${chairs.total} held · hire $hire more", "+$guestsDelta · ${Fmt.inr(hire * 40)}", true, "inventory") }
            else impacts += ChangeImpact(chairs.name, "$c → $new of ${chairs.total} held on ${Fmt.dayMonth(day)}", "+$guestsDelta", false, "inventory")
        }
        inventory.firstOrNull { it.name.contains("table", true) }?.let { tables ->
            val extra = ceil(guestsDelta / 10.0).toInt(); val c = tables.committed(day); val new = c + extra
            if (new > tables.total) { val hire = new - tables.total; added += hire * 400; impacts += ChangeImpact(tables.name, "All ${tables.total} held · hire $hire more", "+$extra · ${Fmt.inr(hire * 400)}", true, "inventory") }
            else impacts += ChangeImpact(tables.name, "$c → $new of ${tables.total} held", "+$extra", false, "inventory")
        }
        val helpers = maxOf(1, guestsDelta / 50)
        val free = crew.filter { it.role == CrewRole.HELPER && it.bookings.none { id -> event(id)?.let { o -> overlap(e, o) || o.day == e.day } == true } }.take(helpers)
        val helperCost = helpers * (free.firstOrNull()?.dayRate ?: CrewRole.HELPER.defaultDayRate); added += helperCost
        impacts += ChangeImpact("Helpers", if (free.isEmpty()) "Hire $helpers day-wage" else free.joinToString(", ") { it.firstName }, "+$helpers · ${Fmt.inr(helperCost)}", free.size < helpers, "people")
        if (impacts.any { it.isShort } || guestsDelta >= 150) { added += 4_500; impacts += ChangeImpact("Transport", "Second truck, chairs and tables", Fmt.inr(4_500), false, "transport") }
        val perGuest = maxOf(200, if (e.guests == 0) 350 else (e.quoted / e.guests) / 4)
        val revised = ((e.quoted + guestsDelta * perGuest) / 1000) * 1000
        val cr = ChangeRequest(Cal.newId(), e.id, "Add $guestsDelta guests. ${e.guests} becomes ${e.guests + guestsDelta}.", guestsDelta, impacts, added, revised, perGuest, ChangeRequestStatus.PENDING, Cal.today, via)
        save(WriteOp.Set(Coll.changeRequests, cr.id, Mapping.of(cr)))
        return cr
    }
    fun cancelEvent(id: String) {
        val e = event(id) ?: return
        val ops = mutableListOf<WriteOp>(WriteOp.Set(Coll.events, id, Mapping.of(e.copy(status = EventStatus.CANCELLED))))
        crew.filter { id in it.bookings }.forEach { ops += WriteOp.Set(Coll.crew, it.id, Mapping.of(it.copy(bookings = it.bookings - id))) }
        inventory.filter { i -> i.holds.any { it.eventId == id } }.forEach { ops += WriteOp.Set(Coll.inventory, it.id, Mapping.of(it.copy(holds = it.holds.filter { h -> h.eventId != id }))) }
        save(ops, "${e.name} cancelled")
    }
    fun markCompleted(id: String) { val e = event(id) ?: return; save(WriteOp.Set(Coll.events, id, Mapping.of(e.copy(status = EventStatus.COMPLETED))), "${e.name} marked completed") }

    fun createEvent(d: EventDraft): Event? {
        val type = d.type ?: return null; val date = d.date ?: return null
        val clientName = d.clientName.trim(); if (clientName.isEmpty()) return null
        val surname = clientName.substringAfterLast(' ')
        val hue = EventHue.entries[events.size % 4]
        val ops = mutableListOf<WriteOp>()
        val existing = clients.firstOrNull { it.surname.equals(surname, true) }
        val clientId = if (existing != null) { ops += WriteOp.Set(Coll.clients, existing.id, Mapping.of(existing.copy(isRepeat = true, phone = d.phone.ifBlank { existing.phone }))); existing.id }
        else Cal.newId().also { ops += WriteOp.Set(Coll.clients, it, Mapping.of(Client(it, clientName, d.phone, false, (clients.size % 8) + 1, Cal.today))) }
        val start = date.atTime(18, 0); val end = start.plusHours(5)
        val parts = d.venue.split(",").map { it.trim() }
        val e = Event(Cal.newId(), "$surname ${type.word}", clientId, type, start, end, Venue(parts.firstOrNull() ?: d.venue, parts.getOrNull(1) ?: ""), d.guests, d.quoted, hue,
            EventStatus.UPCOMING, 0, maxOf(6, d.guests / 40), null, null, Cal.today)
        ops += WriteOp.Set(Coll.events, e.id, Mapping.of(e))
        val advance = d.quoted / 4
        val m1 = Milestone(Cal.newId(), e.id, "Booking advance", advance, Cal.todayDate, MilestoneStatus.PENDING, MilestoneKind.ADVANCE)
        val m2 = Milestone(Cal.newId(), e.id, "Balance", d.quoted - advance, date, MilestoneStatus.PENDING, MilestoneKind.BALANCE)
        ops += WriteOp.Set(Coll.milestones, m1.id, Mapping.of(m1)); ops += WriteOp.Set(Coll.milestones, m2.id, Mapping.of(m2))
        d.fromEnquiryId?.let { q -> enquiry(q)?.let { ops += WriteOp.Set(Coll.enquiries, it.id, Mapping.of(it.copy(status = EnquiryStatus.CONVERTED))) } }
        save(ops, "${e.name} created")
        replaceDraft(EventDraft())
        return e
    }
    fun updateClient(c: Client) = save(WriteOp.Set(Coll.clients, c.id, Mapping.of(c)), "Saved")

    // MARK: Clients

    data class ClientStats(val eventCount: Int, val lifetimeValue: Int, val nextEventDate: LocalDate?)
    fun stats(c: Client): ClientStats {
        val evs = events.filter { it.clientId == c.id && it.status != EventStatus.CANCELLED }
        return ClientStats(evs.size, evs.sumOf { it.quoted }, evs.filter { it.status == EventStatus.UPCOMING && it.day >= Cal.todayDate }.minOfOrNull { it.day })
    }
    val lifetimeValue get() = events.filter { it.status != EventStatus.CANCELLED }.sumOf { it.quoted }

    // MARK: Enquiries / packages

    fun saveEnquiry(q: Enquiry) = save(WriteOp.Set(Coll.enquiries, q.id, Mapping.of(q)), "Enquiry saved")
    fun closeEnquiry(id: String) { val q = enquiry(id) ?: return; save(WriteOp.Set(Coll.enquiries, id, Mapping.of(q.copy(status = EnquiryStatus.CLOSED))), "Enquiry closed") }
    fun savePackage(p: Package) = save(WriteOp.Set(Coll.packages, p.id, Mapping.of(p)), "Saved")
    fun deletePackage(id: String) = save(WriteOp.Delete(Coll.packages, id))

    // MARK: Crew

    fun attendanceFor(eventId: String) = data.attendance.firstOrNull { it.id == eventId }?.states.orEmpty()
    fun cycleAttendance(eventId: String, crewId: String) {
        val a = data.attendance.firstOrNull { it.id == eventId } ?: Attendance(eventId, emptyMap())
        val next = (a.states[crewId] ?: AttendanceState.CONFIRMED).next
        save(WriteOp.Set(Coll.attendance, eventId, Mapping.of(a.copy(states = a.states + (crewId to next)))))
    }
    fun assignedCrew(eventId: String) = crew.filter { eventId in it.bookings }
    fun toggleAssignment(crewId: String, eventId: String) {
        val m = member(crewId) ?: return; val e = event(eventId) ?: return
        val on = eventId in m.bookings
        save(listOf(WriteOp.Set(Coll.crew, m.id, Mapping.of(m.copy(bookings = if (on) m.bookings - eventId else m.bookings + eventId))),
            WriteOp.Set(Coll.events, e.id, Mapping.of(e.copy(crewAssigned = maxOf(0, e.crewAssigned + if (on) -1 else 1))))))
    }
    fun moveTeam(crewId: String, to: String, from: String) {
        val m = member(crewId) ?: return
        val ops = mutableListOf<WriteOp>(WriteOp.Set(Coll.crew, m.id, Mapping.of(m.copy(bookings = (m.bookings - from).let { if (to in it) it else it + to }))))
        event(from)?.let { ops += WriteOp.Set(Coll.events, it.id, Mapping.of(it.copy(crewAssigned = maxOf(0, it.crewAssigned - 1)))) }
        save(ops, "${m.firstName}'s team moved to ${event(to)?.surname}")
    }
    fun saveCrewMember(m: CrewMember) = save(WriteOp.Set(Coll.crew, m.id, Mapping.of(m)), "Saved")
    fun deleteCrewMember(id: String) = save(WriteOp.Delete(Coll.crew, id))
    fun crewConflictNote(e: Event): String? {
        val others = eventsOn(e.day).filter { it.id != e.id }; if (others.isEmpty()) return null
        return crew.firstOrNull { m -> e.id in m.bookings && m.bookings.any { id -> others.any { it.id == id } } }?.let { "${it.firstName}'s team conflicted" }
    }

    // MARK: Inventory

    fun saveItem(i: InventoryItem) = save(WriteOp.Set(Coll.inventory, i.id, Mapping.of(i)), "Saved")
    fun deleteItem(id: String) = save(WriteOp.Delete(Coll.inventory, id))
    fun setHold(itemId: String, eventId: String, qty: Int) {
        val i = item(itemId) ?: return; val e = event(eventId) ?: return
        val holds = i.holds.filter { it.eventId != eventId } + (if (qty > 0) listOf(Hold(eventId, qty, e.day)) else emptyList())
        save(WriteOp.Set(Coll.inventory, i.id, Mapping.of(i.copy(holds = holds))))
    }
    fun inventorySummary(e: Event): String {
        val holds = inventory.mapNotNull { i -> i.holds.firstOrNull { it.eventId == e.id }?.let { "${it.qty} ${i.unit}" } }
        return if (holds.isEmpty()) e.inventorySummary ?: "Not planned yet" else holds.take(3).joinToString(" · ")
    }

    // MARK: Season

    data class SeasonSummary(val booked: Int, val count: Int, val months: List<Pair<String, Int>>, val label: String)
    val season: SeasonSummary
        get() {
            val start = Cal.seasonStart(Cal.todayDate); val end = start.plusMonths(5)
            val inSeason = events.filter { it.status != EventStatus.CANCELLED && it.day >= start && it.day < end }
            val months = (0 until 5).map { i -> val m = start.plusMonths(i.toLong()); Fmt.monthShort(m) to inSeason.filter { it.start.monthValue == m.monthValue && it.start.year == m.year }.sumOf { it.quoted } }
            return SeasonSummary(inSeason.sumOf { it.quoted }, inSeason.size, months, Cal.seasonLabel(Cal.todayDate))
        }
}
