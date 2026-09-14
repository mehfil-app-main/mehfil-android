package com.mehfil.app.model

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Document ↔ model mapping for the schema in backend/DATA_MODEL.md. Documents are plain maps: dates are
 * java.util.Date on the way in (Firestore stores them as Timestamps), and any of Timestamp / Date / epoch millis
 * on the way out (the local JSON store keeps millis).
 */
object Coll {
    const val clients = "clients"; const val events = "events"; const val milestones = "milestones"; const val crew = "crew"
    const val inventory = "inventory"; const val tasks = "tasks"; const val attendance = "attendance"; const val reminders = "reminders"
    const val expenses = "expenses"; const val enquiries = "enquiries"; const val packages = "packages"; const val changeRequests = "changeRequests"
    val all = listOf(clients, events, milestones, crew, inventory, tasks, attendance, reminders, expenses, enquiries, packages, changeRequests)
}

typealias Doc = Map<String, Any?>

private val zone: ZoneId get() = ZoneId.systemDefault()
private fun LocalDateTime.toDate(): Date = Date.from(atZone(zone).toInstant())
private fun LocalDate.toDate(): Date = Date.from(atStartOfDay(zone).toInstant())
private fun Any?.asDateTime(): LocalDateTime? = when (this) {
    is Timestamp -> LocalDateTime.ofInstant(toDate().toInstant(), zone)
    is Date -> LocalDateTime.ofInstant(toInstant(), zone)
    is Number -> LocalDateTime.ofInstant(Instant.ofEpochMilli(toLong()), zone)
    else -> null
}
private fun Any?.asDate(): LocalDate? = asDateTime()?.toLocalDate()
private fun Any?.int(default: Int = 0): Int = (this as? Number)?.toInt() ?: default
private fun Any?.str(default: String = ""): String = this as? String ?: default
private fun Any?.bool(default: Boolean = false): Boolean = this as? Boolean ?: default
private inline fun <reified E : Enum<E>> Any?.enum(default: E): E = (this as? String)?.let { s -> enumValues<E>().firstOrNull { it.name.equals(s, true) } } ?: default
private inline fun <reified E : Enum<E>> Any?.enumOrNull(): E? = (this as? String)?.let { s -> enumValues<E>().firstOrNull { it.name.equals(s, true) } }

object Mapping {
    // ---- to document ----
    fun of(c: Client): Doc = mapOf("id" to c.id, "name" to c.name, "phone" to c.phone, "isRepeat" to c.isRepeat, "avatar" to c.avatar, "createdAt" to c.createdAt.toDate())
    fun of(e: Event): Doc = mapOf(
        "id" to e.id, "name" to e.name, "clientId" to e.clientId, "type" to e.type.code, "start" to e.start.toDate(), "end" to e.end.toDate(),
        "venue" to mapOf("name" to e.venue.name, "area" to e.venue.area, "hall" to e.venue.hall), "guests" to e.guests, "quoted" to e.quoted, "hue" to e.hue.code,
        "status" to e.status.code, "crewAssigned" to e.crewAssigned, "crewNeeded" to e.crewNeeded, "crewPlan" to e.crewPlan, "inventorySummary" to e.inventorySummary, "createdAt" to e.createdAt.toDate(),
    )
    fun of(m: Milestone): Doc = mapOf(
        "id" to m.id, "eventId" to m.eventId, "name" to m.name, "amount" to m.amount, "due" to m.due.toDate(), "status" to m.status.code, "kind" to m.kind.code,
        "paidOn" to m.paidOn?.toDate(), "method" to m.method?.code, "reference" to m.reference,
    )
    fun of(m: CrewMember): Doc = mapOf("id" to m.id, "name" to m.name, "role" to m.role.code, "isLead" to m.isLead, "dayRate" to m.dayRate, "avatar" to m.avatar, "bookings" to m.bookings, "note" to m.note)
    fun of(a: Attendance): Doc = mapOf("id" to a.id, "states" to a.states.mapValues { it.value.code })
    fun of(i: InventoryItem): Doc = mapOf("id" to i.id, "name" to i.name, "total" to i.total, "detail" to i.detail, "holds" to i.holds.map { mapOf("eventId" to it.eventId, "qty" to it.qty, "date" to it.date.toDate()) })
    fun of(t: RunsheetTask): Doc = mapOf("id" to t.id, "eventId" to t.eventId, "block" to t.block, "blockTime" to t.blockTime, "title" to t.title, "crew" to t.crew, "duration" to t.duration, "done" to t.done, "order" to t.order)
    fun of(r: SentReminder): Doc = mapOf("id" to r.id, "milestoneId" to r.milestoneId, "tone" to r.tone.code, "channel" to r.channel.code, "sentAt" to r.sentAt.toDate(), "outcome" to r.outcome)
    fun of(x: Expense): Doc = mapOf("id" to x.id, "eventId" to x.eventId, "name" to x.name, "detail" to x.detail, "amount" to x.amount)
    fun of(q: Enquiry): Doc = mapOf("id" to q.id, "name" to q.name, "phone" to q.phone, "source" to q.source, "budget" to q.budget, "date" to q.date?.toDate(), "guests" to q.guests, "notes" to q.notes, "status" to q.status.code, "createdAt" to q.createdAt.toDate())
    fun of(p: Package): Doc = mapOf("id" to p.id, "name" to p.name, "detail" to p.detail, "price" to p.price)
    fun of(c: ChangeRequest): Doc = mapOf(
        "id" to c.id, "eventId" to c.eventId, "summary" to c.summary, "guestsDelta" to c.guestsDelta,
        "impacts" to c.impacts.map { mapOf("resource" to it.resource, "detail" to it.detail, "delta" to it.delta, "isShort" to it.isShort, "icon" to it.icon) },
        "addedCost" to c.addedCost, "revisedQuote" to c.revisedQuote, "perGuest" to c.perGuest, "status" to c.status.code, "requestedAt" to c.requestedAt.toDate(), "via" to c.via,
    )
    fun of(v: VendorProfile): Doc = mapOf(
        "ownerName" to v.ownerName, "businessName" to v.businessName, "phone" to v.phone, "area" to v.area, "since" to v.since, "upi" to v.upi, "bankLabel" to v.bankLabel,
        "seasonTarget" to v.seasonTarget, "lastSeasonBooked" to v.lastSeasonBooked, "lastSeasonEvents" to v.lastSeasonEvents,
        "notifyPayments" to v.notifyPayments, "notifyConflicts" to v.notifyConflicts, "notifyChanges" to v.notifyChanges, "createdAt" to v.createdAt.toDate(),
    )

    // ---- from document ----
    fun client(d: Doc) = Client(d["id"].str(), d["name"].str(), d["phone"].str(), d["isRepeat"].bool(), d["avatar"].int(1), d["createdAt"].asDateTime() ?: Cal.today)
    fun event(d: Doc): Event {
        val v = d["venue"] as? Map<*, *>
        return Event(
            d["id"].str(), d["name"].str(), d["clientId"].str(), d["type"].enum(EventType.RECEPTION), d["start"].asDateTime() ?: Cal.today, d["end"].asDateTime() ?: Cal.today,
            Venue(v?.get("name").str(), v?.get("area").str(), v?.get("hall") as? String), d["guests"].int(), d["quoted"].int(),
            EventHue.entries[(d["hue"].int(1) - 1).coerceIn(0, 3)], d["status"].enum(EventStatus.UPCOMING), d["crewAssigned"].int(), d["crewNeeded"].int(),
            d["crewPlan"] as? String, d["inventorySummary"] as? String, d["createdAt"].asDateTime() ?: Cal.today,
        )
    }
    fun milestone(d: Doc) = Milestone(
        d["id"].str(), d["eventId"].str(), d["name"].str(), d["amount"].int(), d["due"].asDate() ?: Cal.todayDate, d["status"].enum(MilestoneStatus.PENDING), d["kind"].enum(MilestoneKind.INSTALMENT),
        d["paidOn"].asDate(), d["method"].enumOrNull<PaymentMethod>(), d["reference"] as? String,
    )
    fun crew(d: Doc) = CrewMember(d["id"].str(), d["name"].str(), d["role"].enum(CrewRole.HELPER), d["isLead"].bool(), d["dayRate"].int(800), (d["avatar"] as? Number)?.toInt(),
        (d["bookings"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(), d["note"] as? String)
    fun attendance(d: Doc) = Attendance(d["id"].str(), (d["states"] as? Map<*, *>)?.entries?.associate { (it.key as String) to it.value.enum(AttendanceState.CONFIRMED) } ?: emptyMap())
    fun inventory(d: Doc) = InventoryItem(d["id"].str(), d["name"].str(), d["total"].int(), d["detail"].str(),
        (d["holds"] as? List<*>)?.mapNotNull { h -> (h as? Map<*, *>)?.let { Hold(it["eventId"].str(), it["qty"].int(), it["date"].asDate() ?: Cal.todayDate) } } ?: emptyList())
    fun task(d: Doc) = RunsheetTask(d["id"].str(), d["eventId"].str(), d["block"].str(), d["blockTime"].str(), d["title"].str(), d["crew"].str(), d["duration"].str(), d["done"].bool(), d["order"].int())
    fun reminder(d: Doc) = SentReminder(d["id"].str(), d["milestoneId"].str(), d["tone"].enum(ReminderTone.STANDARD), d["channel"].enum(ReminderChannel.WHATSAPP), d["sentAt"].asDateTime() ?: Cal.today, d["outcome"].str())
    fun expense(d: Doc) = Expense(d["id"].str(), d["eventId"].str(), d["name"].str(), d["detail"].str(), d["amount"].int())
    fun enquiry(d: Doc) = Enquiry(d["id"].str(), d["name"].str(), d["phone"].str(), d["source"].str(), d["budget"].str(), d["date"].asDate(), d["guests"].int(), d["notes"].str(), d["status"].enum(EnquiryStatus.OPEN), d["createdAt"].asDateTime() ?: Cal.today)
    fun pkg(d: Doc) = Package(d["id"].str(), d["name"].str(), d["detail"].str(), d["price"].int())
    fun changeRequest(d: Doc) = ChangeRequest(
        d["id"].str(), d["eventId"].str(), d["summary"].str(), d["guestsDelta"].int(),
        (d["impacts"] as? List<*>)?.mapNotNull { i -> (i as? Map<*, *>)?.let { ChangeImpact(it["resource"].str(), it["detail"].str(), it["delta"].str(), it["isShort"].bool(), it["icon"].str("inventory")) } } ?: emptyList(),
        d["addedCost"].int(), d["revisedQuote"].int(), d["perGuest"].int(350), d["status"].enum(ChangeRequestStatus.PENDING), d["requestedAt"].asDateTime() ?: Cal.today, d["via"].str("WhatsApp"),
    )
    fun profile(d: Doc) = VendorProfile(
        d["ownerName"].str(), d["businessName"].str(), d["phone"].str(), d["area"].str(), d["since"].str(), d["upi"].str(), d["bankLabel"].str(), d["seasonTarget"].int(6_000_000),
        (d["lastSeasonBooked"] as? Number)?.toInt(), (d["lastSeasonEvents"] as? Number)?.toInt(), d["notifyPayments"].bool(true), d["notifyConflicts"].bool(true), d["notifyChanges"].bool(true), d["createdAt"].asDateTime() ?: Cal.today,
    )

    /** Rebuilds the full picture from raw documents per collection. */
    fun vendorData(docs: Map<String, List<Doc>>) = VendorData(
        clients = docs[Coll.clients].orEmpty().map(::client), events = docs[Coll.events].orEmpty().map(::event), milestones = docs[Coll.milestones].orEmpty().map(::milestone),
        crew = docs[Coll.crew].orEmpty().map(::crew), inventory = docs[Coll.inventory].orEmpty().map(::inventory), tasks = docs[Coll.tasks].orEmpty().map(::task),
        attendance = docs[Coll.attendance].orEmpty().map(::attendance), reminders = docs[Coll.reminders].orEmpty().map(::reminder), expenses = docs[Coll.expenses].orEmpty().map(::expense),
        enquiries = docs[Coll.enquiries].orEmpty().map(::enquiry), packages = docs[Coll.packages].orEmpty().map(::pkg), changeRequests = docs[Coll.changeRequests].orEmpty().map(::changeRequest),
    )

    /** Turns a whole VendorData into writes (used to load the sample season). */
    fun writes(v: VendorData): List<WriteOp> =
        v.clients.map { WriteOp.Set(Coll.clients, it.id, of(it)) } + v.events.map { WriteOp.Set(Coll.events, it.id, of(it)) } +
            v.milestones.map { WriteOp.Set(Coll.milestones, it.id, of(it)) } + v.crew.map { WriteOp.Set(Coll.crew, it.id, of(it)) } +
            v.inventory.map { WriteOp.Set(Coll.inventory, it.id, of(it)) } + v.tasks.map { WriteOp.Set(Coll.tasks, it.id, of(it)) } +
            v.attendance.map { WriteOp.Set(Coll.attendance, it.id, of(it)) } + v.reminders.map { WriteOp.Set(Coll.reminders, it.id, of(it)) } +
            v.expenses.map { WriteOp.Set(Coll.expenses, it.id, of(it)) } + v.enquiries.map { WriteOp.Set(Coll.enquiries, it.id, of(it)) } +
            v.packages.map { WriteOp.Set(Coll.packages, it.id, of(it)) } + v.changeRequests.map { WriteOp.Set(Coll.changeRequests, it.id, of(it)) }
}

sealed class WriteOp(val collection: String, val id: String) {
    class Set(collection: String, id: String, val doc: Doc) : WriteOp(collection, id)
    class Delete(collection: String, id: String) : WriteOp(collection, id)
}
