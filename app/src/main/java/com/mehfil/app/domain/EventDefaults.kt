package com.mehfil.app.domain

import com.mehfil.app.model.Event
import com.mehfil.app.model.EventDraft
import com.mehfil.app.model.EventHue
import com.mehfil.app.model.EventStatus
import com.mehfil.app.model.Venue
import com.mehfil.app.model.Cal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The assumptions Mehfil makes when a vendor books an event from the two-field create flow, in one place.
 *
 * These were inline magic numbers at the call site. They are defaults a vendor edits afterwards, not rules —
 * but they decide what the first screen after booking says, so they are worth being explicit about.
 */
object EventDefaults {

    /** Indian wedding functions run into the evening; 18:00 is the spec's assumed start (§3). */
    const val START_HOUR = 18
    const val DURATION_HOURS = 5L

    /** One crew member per 40 guests, never fewer than a six-person team. */
    const val GUESTS_PER_CREW = 40
    const val MINIMUM_CREW = 6

    /** The booking advance is a quarter of the quote; the balance falls due on the day. */
    const val ADVANCE_SHARE = 4

    fun window(date: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        val start = date.atTime(START_HOUR, 0)
        return start to start.plusHours(DURATION_HOURS)
    }

    fun crewNeeded(guests: Int) = maxOf(MINIMUM_CREW, guests / GUESTS_PER_CREW)

    fun advance(quoted: Int) = quoted / ADVANCE_SHARE

    /**
     * "Sector 44, Gurugram" → venue name and area. A single-part entry is treated as the name, so a vendor
     * typing just "Leela Ambience" still gets a usable record.
     */
    fun venue(input: String): Venue {
        val parts = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return Venue(parts.firstOrNull() ?: input, parts.getOrNull(1) ?: "")
    }

    /** Event colours cycle so two events on one day are always distinguishable on the calendar. */
    fun hue(existingEventCount: Int): EventHue = EventHue.entries[existingEventCount % EventHue.entries.size]

    fun newEvent(draft: EventDraft, clientId: String, surname: String, existingEventCount: Int): Event? {
        val type = draft.type ?: return null
        val date = draft.date ?: return null
        val (start, end) = window(date)
        return Event(
            id = Cal.newId(),
            name = "$surname ${type.word}",
            clientId = clientId,
            type = type,
            start = start,
            end = end,
            venue = venue(draft.venue),
            guests = draft.guests,
            quoted = draft.quoted,
            hue = hue(existingEventCount),
            status = EventStatus.UPCOMING,
            crewAssigned = 0,
            crewNeeded = crewNeeded(draft.guests),
            crewPlan = null,
            inventorySummary = null,
            createdAt = Cal.today,
        )
    }
}
