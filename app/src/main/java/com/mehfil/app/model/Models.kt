package com.mehfil.app.model

import java.time.LocalDate
import java.time.LocalDateTime

// Core domain types. Storage schema (field names, enum strings, timestamps) is defined in backend/DATA_MODEL.md
// and implemented by Mapping.kt; both apps read and write exactly that shape.

enum class EventType(val label: String) {
    SANGEET("Sangeet"), MEHENDI("Mehendi"), RECEPTION("Reception"), HALDI("Haldi"), NIKAH("Nikah"), ENGAGEMENT("Engagement");
    val word get() = label.lowercase()
    val code get() = name.lowercase()
}

enum class EventStatus { UPCOMING, COMPLETED, CANCELLED; val code get() = name.lowercase() }
enum class PaymentState { PAID, PARTIAL, OVERDUE, PENDING }
enum class EventHue { ONE, TWO, THREE, FOUR; val code get() = ordinal + 1 }

data class Venue(val name: String, val area: String, val hall: String? = null) {
    val short get() = if (area.isEmpty()) name else "$name, $area"
    val shortName get() = if (name.startsWith("The ")) name.removePrefix("The ") else name
    val compact get() = if (area.isEmpty()) shortName else "$shortName, $area"
}

data class Event(
    val id: String,
    val name: String,
    val clientId: String,
    val type: EventType,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val venue: Venue,
    val guests: Int,
    val quoted: Int,
    val hue: EventHue,
    val status: EventStatus,
    val crewAssigned: Int,
    val crewNeeded: Int,
    val crewPlan: String? = null,
    val inventorySummary: String? = null,
    val createdAt: LocalDateTime = Cal.today,
) {
    val surname get() = name.substringBefore(' ')
    val day: LocalDate get() = start.toLocalDate()
    val isCompleted get() = status == EventStatus.COMPLETED
}

data class Client(val id: String, val name: String, val phone: String, val isRepeat: Boolean, val avatar: Int, val createdAt: LocalDateTime = Cal.today) {
    val firstName get() = name.substringBefore(' ')
    val surname get() = name.substringAfterLast(' ')
}

enum class MilestoneStatus { PAID, PENDING, OVERDUE; val code get() = name.lowercase() }
enum class MilestoneKind(val label: String) { ADVANCE("Advance"), INSTALMENT("Instalment"), BALANCE("Balance"), SETTLEMENT("Settlement"); val code get() = name.lowercase() }
enum class PaymentMethod(val label: String) { UPI("UPI"), BANK("Bank transfer"), CASH("Cash"), CHEQUE("Cheque"); val code get() = name.lowercase() }

data class Milestone(
    val id: String, val eventId: String, val name: String, val amount: Int, val due: LocalDate,
    val status: MilestoneStatus, val kind: MilestoneKind,
    val paidOn: LocalDate? = null, val method: PaymentMethod? = null, val reference: String? = null,
) {
    val isOpen get() = status != MilestoneStatus.PAID
    /** Overdue is a fact about the date, not a stored flag. */
    val effectiveStatus get() = if (status == MilestoneStatus.PENDING && due < Cal.todayDate) MilestoneStatus.OVERDUE else status
}

enum class ReminderTone(val label: String) { GENTLE("Gentle"), STANDARD("Standard"), FIRM("Firm"); val code get() = name.lowercase() }
enum class ReminderChannel(val label: String) { WHATSAPP("WhatsApp"), SMS("SMS"); val code get() = name.lowercase() }
data class SentReminder(val id: String, val milestoneId: String, val tone: ReminderTone, val channel: ReminderChannel, val sentAt: LocalDateTime, val outcome: String)

enum class CrewRole(val label: String, val plural: String, val defaultDayRate: Int) {
    COORDINATOR("Coordinator", "Coordinators", 2500), DECORATOR("Decorator", "Decorators", 1200), HELPER("Helper", "Helpers", 800);
    val code get() = name.lowercase()
}
enum class AttendanceState(val label: String) {
    CONFIRMED("Confirmed"), ARRIVED("Arrived"), ABSENT("Absent");
    val next get() = when (this) { CONFIRMED -> ARRIVED; ARRIVED -> ABSENT; ABSENT -> CONFIRMED }
    val code get() = name.lowercase()
}

data class CrewMember(
    val id: String, val name: String, val role: CrewRole, val isLead: Boolean, val dayRate: Int,
    val avatar: Int?, val bookings: List<String>, val note: String? = null,
) { val firstName get() = name.substringBefore(' ') }

data class Attendance(val id: String, val states: Map<String, AttendanceState>)
data class Hold(val eventId: String, val qty: Int, val date: LocalDate)
data class InventoryItem(val id: String, val name: String, val total: Int, val detail: String, val holds: List<Hold>) {
    fun committed(day: LocalDate) = holds.filter { it.date == day }.sumOf { it.qty }
    fun isOver(day: LocalDate) = committed(day) > total
    val unit get() = name.lowercase().substringAfterLast(' ')
}
data class RunsheetTask(val id: String, val eventId: String, val block: String, val blockTime: String, val title: String, val crew: String, val duration: String, val done: Boolean, val order: Int)
data class Expense(val id: String, val eventId: String, val name: String, val detail: String, val amount: Int)
data class Package(val id: String, val name: String, val detail: String, val price: Int)
data class ChangeImpact(val resource: String, val detail: String, val delta: String, val isShort: Boolean, val icon: String)
enum class ChangeRequestStatus { PENDING, APPROVED, DECLINED; val code get() = name.lowercase() }
data class ChangeRequest(
    val id: String, val eventId: String, val summary: String, val guestsDelta: Int, val impacts: List<ChangeImpact>,
    val addedCost: Int, val revisedQuote: Int, val perGuest: Int, val status: ChangeRequestStatus, val requestedAt: LocalDateTime, val via: String,
)
enum class EnquiryStatus { OPEN, CONVERTED, CLOSED; val code get() = name.lowercase() }
data class Enquiry(
    val id: String, val name: String, val phone: String, val source: String, val budget: String, val date: LocalDate?,
    val guests: Int, val notes: String, val status: EnquiryStatus = EnquiryStatus.OPEN, val createdAt: LocalDateTime = Cal.today,
)

data class VendorProfile(
    val ownerName: String, val businessName: String, val phone: String, val area: String, val since: String,
    val upi: String = "", val bankLabel: String = "", val seasonTarget: Int = 6_000_000,
    val lastSeasonBooked: Int? = null, val lastSeasonEvents: Int? = null,
    val notifyPayments: Boolean = true, val notifyConflicts: Boolean = true, val notifyChanges: Boolean = true,
    val createdAt: LocalDateTime = Cal.today,
) {
    val ownerFirstName get() = ownerName.substringBefore(' ')
    val monogram get() = businessName.take(1).uppercase()
}

data class EventDraft(
    val clientName: String = "", val phone: String = "", val type: EventType? = null, val date: LocalDate? = null,
    val venue: String = "", val guests: Int = 300, val packageIds: Set<String> = emptySet(), val quoted: Int = 0, val fromEnquiryId: String? = null,
)

enum class Appearance(val label: String) { SYSTEM("Follows system"), LIGHT("Light"), DARK("Dark") }

data class VendorData(
    val clients: List<Client> = emptyList(), val events: List<Event> = emptyList(), val milestones: List<Milestone> = emptyList(),
    val crew: List<CrewMember> = emptyList(), val inventory: List<InventoryItem> = emptyList(), val tasks: List<RunsheetTask> = emptyList(),
    val attendance: List<Attendance> = emptyList(), val reminders: List<SentReminder> = emptyList(), val expenses: List<Expense> = emptyList(),
    val enquiries: List<Enquiry> = emptyList(), val packages: List<Package> = emptyList(), val changeRequests: List<ChangeRequest> = emptyList(),
)
