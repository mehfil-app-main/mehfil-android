package com.mehfil.app.domain

import com.mehfil.app.model.ChangeImpact
import com.mehfil.app.model.ChangeRequest
import com.mehfil.app.model.ChangeRequestStatus
import com.mehfil.app.model.CrewRole
import com.mehfil.app.model.Event
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.InventoryItem
import com.mehfil.app.model.Cal
import kotlin.math.ceil

/**
 * Turns "the client wants N more guests" into a costed change request: what runs short, what it costs to
 * cover, and the revised quote.
 *
 * The rates below were hard-coded at their call site, which made them invisible and unchangeable. They are a
 * commercial policy, not an implementation detail, so a buyer can price their own market by editing [Rates]
 * alone. Figures are the Delhi-NCR day rates the product spec was written against.
 */
object ChangeRequestPricing {

    data class Rates(
        /** Per-chair hire when the vendor's own stock is fully committed. */
        val chairHire: Int = 40,
        /** Per-table hire. One table seats [guestsPerTable]. */
        val tableHire: Int = 400,
        val guestsPerTable: Int = 10,
        /** One extra helper covers this many additional guests. */
        val guestsPerHelper: Int = 50,
        /** A second truck, charged once, when anything runs short or the party grows past [bulkGuestThreshold]. */
        val transport: Int = 4_500,
        val bulkGuestThreshold: Int = 150,
        /** Per-head price floor, and the fallback when the event has no guest count to divide by. */
        val minimumPerGuest: Int = 200,
        val fallbackPerGuest: Int = 350,
        /** Marginal guests are charged a quarter of the average head rate; quotes round to this. */
        val marginalShare: Int = 4,
        val quoteRounding: Int = 1_000,
    )

    /** What the caller needs to persist. Pure data — the store decides whether to write it. */
    data class Result(val request: ChangeRequest, val impacts: List<ChangeImpact>)

    fun price(
        event: Event,
        guestsDelta: Int,
        inventory: List<InventoryItem>,
        availableHelpers: List<com.mehfil.app.model.CrewMember>,
        via: String,
        rates: Rates = Rates(),
    ): ChangeRequest {
        val day = event.day
        val impacts = mutableListOf<ChangeImpact>()
        var added = 0

        inventory.firstOrNull { it.name.contains("chair", ignoreCase = true) }?.let { chairs ->
            val committed = chairs.committed(day)
            val wanted = committed + guestsDelta
            if (wanted > chairs.total) {
                val hire = wanted - chairs.total
                added += hire * rates.chairHire
                impacts += ChangeImpact(chairs.name, "All ${chairs.total} held · hire $hire more", "+$guestsDelta · ${Fmt.inr(hire * rates.chairHire)}", true, IMPACT_INVENTORY)
            } else {
                impacts += ChangeImpact(chairs.name, "$committed → $wanted of ${chairs.total} held on ${Fmt.dayMonth(day)}", "+$guestsDelta", false, IMPACT_INVENTORY)
            }
        }

        inventory.firstOrNull { it.name.contains("table", ignoreCase = true) }?.let { tables ->
            val extra = ceil(guestsDelta / rates.guestsPerTable.toDouble()).toInt()
            val committed = tables.committed(day)
            val wanted = committed + extra
            if (wanted > tables.total) {
                val hire = wanted - tables.total
                added += hire * rates.tableHire
                impacts += ChangeImpact(tables.name, "All ${tables.total} held · hire $hire more", "+$extra · ${Fmt.inr(hire * rates.tableHire)}", true, IMPACT_INVENTORY)
            } else {
                impacts += ChangeImpact(tables.name, "$committed → $wanted of ${tables.total} held", "+$extra", false, IMPACT_INVENTORY)
            }
        }

        val helpersNeeded = maxOf(1, guestsDelta / rates.guestsPerHelper)
        val free = availableHelpers.take(helpersNeeded)
        val helperCost = helpersNeeded * (free.firstOrNull()?.dayRate ?: CrewRole.HELPER.defaultDayRate)
        added += helperCost
        impacts += ChangeImpact(
            resource = "Helpers",
            detail = if (free.isEmpty()) "Hire $helpersNeeded day-wage" else free.joinToString(", ") { it.firstName },
            delta = "+$helpersNeeded · ${Fmt.inr(helperCost)}",
            isShort = free.size < helpersNeeded,
            icon = IMPACT_PEOPLE,
        )

        if (impacts.any { it.isShort } || guestsDelta >= rates.bulkGuestThreshold) {
            added += rates.transport
            impacts += ChangeImpact("Transport", "Second truck, chairs and tables", Fmt.inr(rates.transport), false, IMPACT_TRANSPORT)
        }

        val perGuest = perGuestRate(event, rates)
        val revised = ((event.quoted + guestsDelta * perGuest) / rates.quoteRounding) * rates.quoteRounding
        return ChangeRequest(
            id = Cal.newId(),
            eventId = event.id,
            summary = "Add $guestsDelta guests. ${event.guests} becomes ${event.guests + guestsDelta}.",
            guestsDelta = guestsDelta,
            impacts = impacts,
            addedCost = added,
            revisedQuote = revised,
            perGuest = perGuest,
            status = ChangeRequestStatus.PENDING,
            requestedAt = Cal.today,
            via = via,
        )
    }

    /**
     * Extra guests are charged a share of the average head rate, not the full one: the fixed costs (stage,
     * lighting, coordination) are already covered by the original quote.
     */
    fun perGuestRate(event: Event, rates: Rates = Rates()): Int =
        maxOf(
            rates.minimumPerGuest,
            if (event.guests == 0) rates.fallbackPerGuest else (event.quoted / event.guests) / rates.marginalShare,
        )

    const val IMPACT_INVENTORY = "inventory"
    const val IMPACT_PEOPLE = "people"
    const val IMPACT_TRANSPORT = "transport"
}
