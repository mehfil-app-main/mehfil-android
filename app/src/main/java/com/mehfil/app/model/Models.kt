package com.mehfil.app.model

import java.time.LocalDate
import java.time.LocalDateTime

// Core domain types (spec §3 / NOTES content model)

enum class EventType(val label: String) {
    SANGEET("Sangeet"), MEHENDI("Mehendi"), RECEPTION("Reception"), HALDI("Haldi"), NIKAH("Nikah"), ENGAGEMENT("Engagement");
    val word get() = label.lowercase()
}

enum class EventStatus { UPCOMING, ENQUIRY, COMPLETED, CANCELLED }

enum class PaymentState { PAID, PARTIAL, OVERDUE, PENDING }

/** Event colour is assigned per event at creation, cycling 1→4. Ruby is excluded (NOTES ruling 2). */
enum class EventHue { ONE, TWO, THREE, FOUR }

data class Venue(val name: String, val area: String, val hall: String? = null) {
    val short get() = "$name, $area"
    /** Without the leading article, for tight card lines. */
    val shortName get() = if (name.startsWith("The ")) name.removePrefix("The ") else name
    val compact get() = "$shortName, $area"
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
    val hasCrewConflict: Boolean,
    val inventorySummary: String,
    val runsheetSummary: String,
    /** Role breakdown of the crew plan; daily-wage crew are outside the roster. */
    val crewPlan: String = "",
) {
    val surname get() = name.substringBefore(' ')
    val day: LocalDate get() = start.toLocalDate()
    val isCompleted get() = status == EventStatus.COMPLETED
}

data class Client(
    val id: String,
    val name: String,
    val phone: String,
    val isRepeat: Boolean,
    val eventCount: Int,
    val lifetimeValue: Int,
    val nextEventDate: LocalDate?,
    val avatar: Int,
) {
    val firstName get() = name.substringBefore(' ')
    val surname get() = name.substringAfterLast(' ')
}

enum class MilestoneStatus { PAID, PENDING, OVERDUE }
enum class MilestoneKind(val label: String) { ADVANCE("Advance"), INSTALMENT("Instalment"), BALANCE("Balance"), SETTLEMENT("Settlement") }

enum class PaymentMethod(val label: String) { UPI("UPI"), BANK("Bank transfer"), CASH("Cash"), CHEQUE("Cheque") }

data class Milestone(
    val id: String,
    val eventId: String,
    val name: String,
    val amount: Int,
    val due: LocalDate,
    val status: MilestoneStatus,
    val kind: MilestoneKind,
    val paidOn: LocalDate? = null,
    val method: PaymentMethod? = null,
    val reference: String? = null,
) {
    val isOpen get() = status != MilestoneStatus.PAID
}

enum class ReminderTone(val label: String) { GENTLE("Gentle"), STANDARD("Standard"), FIRM("Firm") }
enum class ReminderChannel(val label: String) { WHATSAPP("WhatsApp"), SMS("SMS") }

data class SentReminder(val id: String, val tone: ReminderTone, val sent: LocalDateTime, val channel: ReminderChannel, val outcome: String)

enum class CrewRole(val label: String, val plural: String, val dayRate: Int) {
    COORDINATOR("Coordinator", "Coordinators", 2500),
    DECORATOR("Decorator", "Decorators", 1200),
    HELPER("Helper", "Helpers", 800);
}

enum class AttendanceState(val label: String) {
    CONFIRMED("Confirmed"), ARRIVED("Arrived"), ABSENT("Absent");
    val next get() = when (this) { CONFIRMED -> ARRIVED; ARRIVED -> ABSENT; ABSENT -> CONFIRMED }
}

data class CrewMember(
    val id: String,
    val name: String,
    val role: CrewRole,
    val isLead: Boolean,
    val avatar: Int?,
    /** Event ids the member is booked on for the focal day. */
    val bookedOn: List<String>,
    val availabilityNote: String? = null,
) {
    val firstName get() = name.substringBefore(' ')
}

data class Hold(val eventId: String, val qty: Int, val date: LocalDate)

data class InventoryItem(val id: String, val name: String, val total: Int, val detail: String, val holds: List<Hold>) {
    fun committed(day: LocalDate) = holds.filter { it.date == day }.sumOf { it.qty }
    fun isOver(day: LocalDate) = committed(day) > total
}

data class RunsheetTask(
    val id: String, val eventId: String, val block: String, val blockTime: String,
    val title: String, val crew: String, val duration: String, val done: Boolean,
)

data class Expense(val id: String, val name: String, val detail: String, val amount: Int)
data class Package(val id: String, val name: String, val detail: String, val price: Int)
data class ChangeImpact(val id: String, val resource: String, val detail: String, val delta: String, val isShort: Boolean, val icon: String)
data class Enquiry(val id: String, val name: String, val source: String, val budget: String, val date: LocalDate?, val guests: Int, val notes: String)

/** New-event draft (spec §5: back navigation never loses entered data). */
data class EventDraft(
    val clientName: String = "",
    val phone: String = "",
    val type: EventType? = null,
    val date: LocalDate? = null,
    val venue: String = "",
    val guests: Int = 300,
    val packageIds: Set<String> = emptySet(),
    val quoted: Int = 0,
)

enum class Appearance(val label: String) { SYSTEM("Follows system"), LIGHT("Light"), DARK("Dark") }
