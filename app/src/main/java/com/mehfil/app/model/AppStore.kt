package com.mehfil.app.model

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehfil.app.BuildConfig
import com.mehfil.app.R
import com.mehfil.app.domain.ChangeRequestPricing
import com.mehfil.app.domain.ClientStats
import com.mehfil.app.domain.ReminderComposer
import com.mehfil.app.domain.EventDefaults
import com.mehfil.app.domain.SeasonSummary
import com.mehfil.app.domain.VendorView
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
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val reminders = ReminderComposer(app)

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
            try { vendor = repo.profile(newUid) } catch (e: Exception) { report(e, R.string.error_load_profile) }
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
        } catch (e: Exception) { report(e, R.string.error_save_profile) }
    }
    fun updateProfile(change: (VendorProfile) -> VendorProfile) {
        val id = uid ?: return; val p = change(vendor ?: return); vendor = p
        viewModelScope.launch { try { repo.saveProfile(id, p) } catch (e: Exception) { report(e, R.string.error_save) } }
    }
    /**
     * Deletes exactly the records the sample season wrote. Its ids are fixed ("kapoor", "kapoor-sangeet", …),
     * so anything the vendor created themselves is untouched — unlike [eraseAllData], which clears everything.
     */
    suspend fun removeSampleSeason() {
        val id = uid ?: return
        try {
            repo.write(id, Mapping.writes(SampleSeason.data()).map { WriteOp.Delete(it.collection, it.id) })
            show(Toast.Kind.SUCCESS, string(R.string.sample_removed))
        } catch (e: Exception) { report(e, R.string.error_remove_sample) }
    }

    suspend fun loadSampleSeason() {
        val id = uid ?: return
        try {
            repo.write(id, Mapping.writes(SampleSeason.data()))
            show(Toast.Kind.SUCCESS, string(R.string.sample_loaded))
        } catch (e: Exception) { report(e, R.string.error_load_sample) }
    }
    suspend fun eraseAllData() { val id = uid ?: return; repo.deleteEverything(id) }

    private fun save(ops: List<WriteOp>, success: String? = null) {
        val id = uid ?: return
        viewModelScope.launch {
            try { repo.write(id, ops); if (success != null) show(Toast.Kind.SUCCESS, success) }
            catch (e: Exception) { report(e, R.string.error_save) }
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
        val t = Toast(Cal.newId(), kind, message)
        toast = t
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(TOAST_MILLIS)
            if (toast?.id == t.id) toast = null
        }
    }

    /**
     * Firestore snapshot listeners outlive the ViewModel unless they are removed, which keeps the callback —
     * and through it this store — reachable after the Activity is finished.
     */
    override fun onCleared() {
        repo.stopObserving()
        super.onCleared()
    }

    private companion object {
        const val TOAST_MILLIS = 2_800L
        /** Bundled avatar images, cycled so consecutive clients look different. */
        const val AVATAR_COUNT = 8
        const val TAG = "AppStore"
        const val PREFS_NAME = "mehfil"
    }

    /**
     * Shows a message the user can act on and logs the detail. Exception text is deliberately not surfaced:
     * it is untranslatable and exposes storage internals.
     */
    private fun report(t: Throwable, @StringRes fallback: Int) {
        val error = AppError.from(t)
        Log.w(TAG, "Operation failed: ${error::class.simpleName}", t)
        show(
            Toast.Kind.ERROR,
            when (error) {
                is AppError.Offline -> string(R.string.error_offline)
                is AppError.NotAllowed -> string(R.string.error_not_allowed)
                is AppError.Unexpected -> string(fallback)
            },
        )
    }

    private fun string(@StringRes id: Int): String = getApplication<Application>().getString(id)

    /**
     * The read model. `derivedStateOf` means the orderings, indices and money buckets are rebuilt when [data]
     * changes and not on every recomposition — the screens read several of these per frame.
     */
    private val viewState = derivedStateOf { VendorView(data, Cal.todayDate) }
    private val view: VendorView get() = viewState.value

    // Collections and lookups. Thin delegation on purpose: the rules live in VendorView, which is pure Kotlin
    // and unit-tested, while this class stays responsible for session, persistence and user feedback.

    val events get() = view.events
    val clients get() = view.clients
    val milestones get() = view.milestones
    val crew get() = view.crew
    val inventory get() = view.inventory
    val tasks get() = view.tasks
    val sentReminders get() = view.reminders
    val expenses get() = view.expenses
    val enquiries get() = view.enquiries
    val packages get() = view.packages
    val changeRequests get() = view.changeRequests

    fun event(id: String) = view.event(id)
    fun client(id: String) = view.client(id)
    fun clientFor(e: Event) = view.clientFor(e)
    fun member(id: String) = view.member(id)
    fun item(id: String) = view.item(id)
    fun enquiry(id: String) = view.enquiry(id)
    fun milestonesFor(eventId: String) = view.milestonesFor(eventId)
    fun expensesFor(eventId: String) = view.expensesFor(eventId)
    fun remindersFor(milestoneId: String) = view.remindersFor(milestoneId)
    fun pendingChangeRequest(eventId: String) = view.pendingChangeRequest(eventId)
    fun changeRequest(id: String) = view.changeRequest(id)

    val upcomingEvents get() = view.upcomingEvents
    val completedEvents get() = view.completedEvents
    val openEnquiries get() = view.openEnquiries
    fun eventsOn(day: LocalDate) = view.eventsOn(day)
    fun eventsInMonth(month: Int, year: Int) = view.eventsInMonth(month, year)
    fun conflictsOn(day: LocalDate) = view.conflictsOn(day)
    fun isConflicted(e: Event) = view.isConflicted(e)
    val seasonConflict get() = view.seasonConflict
    fun crewOnDuty(day: LocalDate) = view.crewOnDuty(day)

    fun paymentState(e: Event) = view.paymentState(e)
    fun collected(eventId: String) = view.collected(eventId)
    fun balanceDue(eventId: String) = view.balanceDue(eventId)
    fun overdueAmount(eventId: String) = view.overdueAmount(eventId)
    val openMilestones get() = view.openMilestones
    val overdue get() = view.overdue
    val dueThisWeek get() = view.dueThisWeek
    val upcomingMilestones get() = view.upcomingMilestones
    val totalOutstanding get() = view.totalOutstanding
    val overdueTotal get() = view.overdueTotal
    val eventsWithOpenMoney get() = view.eventsWithOpenMoney
    val overdueSurnames get() = view.overdueSurnames
    val lifetimeValue get() = view.lifetimeValue
    fun stats(c: Client) = view.stats(c)
    fun attendanceFor(eventId: String) = view.attendanceFor(eventId)
    fun assignedCrew(eventId: String) = view.assignedCrew(eventId)
    val season: SeasonSummary get() = view.season

    // MARK: Money

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
        val e = event(m.eventId) ?: return ""
        val c = clientFor(e) ?: return ""
        val v = vendor ?: return ""
        return reminders.compose(tone, m, e, c, v)
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
    /**
     * Builds and stores a costed change request. The pricing lives in [ChangeRequestPricing] so the rates are
     * visible and testable; this method only decides which helpers are actually free on the day.
     */
    fun logChangeRequest(eventId: String, guestsDelta: Int, via: String): ChangeRequest? {
        val e = event(eventId) ?: return null
        val freeHelpers = crew.filter { m ->
            m.role == CrewRole.HELPER && m.bookings.none { id -> event(id)?.day == e.day }
        }
        val cr = ChangeRequestPricing.price(e, guestsDelta, inventory, freeHelpers, via)
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

    /**
     * Books an event from the create-flow draft: reuses an existing client when the surname matches, opens the
     * advance and balance milestones, and closes the originating enquiry. Defaults come from [EventDefaults].
     */
    fun createEvent(d: EventDraft): Event? {
        val clientName = d.clientName.trim()
        if (clientName.isEmpty() || d.type == null || d.date == null) return null
        val surname = clientName.substringAfterLast(' ')
        val ops = mutableListOf<WriteOp>()

        val existing = clients.firstOrNull { it.surname.equals(surname, ignoreCase = true) }
        val clientId = if (existing != null) {
            ops += WriteOp.Set(Coll.clients, existing.id, Mapping.of(existing.copy(isRepeat = true, phone = d.phone.ifBlank { existing.phone })))
            existing.id
        } else {
            Cal.newId().also { id ->
                ops += WriteOp.Set(Coll.clients, id, Mapping.of(Client(id, clientName, d.phone, false, (clients.size % AVATAR_COUNT) + 1, Cal.today)))
            }
        }

        val e = EventDefaults.newEvent(d, clientId, surname, events.size) ?: return null
        ops += WriteOp.Set(Coll.events, e.id, Mapping.of(e))

        val advance = EventDefaults.advance(d.quoted)
        val bookingAdvance = Milestone(Cal.newId(), e.id, "Booking advance", advance, Cal.todayDate, MilestoneStatus.PENDING, MilestoneKind.ADVANCE)
        val balance = Milestone(Cal.newId(), e.id, "Balance", d.quoted - advance, d.date, MilestoneStatus.PENDING, MilestoneKind.BALANCE)
        ops += WriteOp.Set(Coll.milestones, bookingAdvance.id, Mapping.of(bookingAdvance))
        ops += WriteOp.Set(Coll.milestones, balance.id, Mapping.of(balance))

        d.fromEnquiryId?.let { q -> enquiry(q)?.let { ops += WriteOp.Set(Coll.enquiries, it.id, Mapping.of(it.copy(status = EnquiryStatus.CONVERTED))) } }

        save(ops, "${e.name} created")
        replaceDraft(EventDraft())
        return e
    }

    fun updateClient(c: Client) = save(WriteOp.Set(Coll.clients, c.id, Mapping.of(c)), "Saved")

    // MARK: Enquiries / packages

    fun saveEnquiry(q: Enquiry) = save(WriteOp.Set(Coll.enquiries, q.id, Mapping.of(q)), "Enquiry saved")
    fun closeEnquiry(id: String) { val q = enquiry(id) ?: return; save(WriteOp.Set(Coll.enquiries, id, Mapping.of(q.copy(status = EnquiryStatus.CLOSED))), "Enquiry closed") }
    fun savePackage(p: Package) = save(WriteOp.Set(Coll.packages, p.id, Mapping.of(p)), "Saved")
    fun deletePackage(id: String) = save(WriteOp.Delete(Coll.packages, id))

    // MARK: Crew

    fun cycleAttendance(eventId: String, crewId: String) {
        val a = data.attendance.firstOrNull { it.id == eventId } ?: Attendance(eventId, emptyMap())
        val next = (a.states[crewId] ?: AttendanceState.CONFIRMED).next
        save(WriteOp.Set(Coll.attendance, eventId, Mapping.of(a.copy(states = a.states + (crewId to next)))))
    }
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
    /** The member double-booked across this event and another that day, or null when the day is clean. */
    fun conflictedMember(e: Event) = view.conflictedMember(e)

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

}
