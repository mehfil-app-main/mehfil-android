package com.mehfil.app.domain

import com.mehfil.app.model.CrewMember
import com.mehfil.app.model.CrewRole
import com.mehfil.app.model.Event
import com.mehfil.app.model.EventDraft
import com.mehfil.app.model.EventHue
import com.mehfil.app.model.EventStatus
import com.mehfil.app.model.EventType
import com.mehfil.app.model.Hold
import com.mehfil.app.model.InventoryItem
import com.mehfil.app.model.Venue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** The commercial rules: what a change request costs, and what a new booking assumes. */
class PricingAndDefaultsTest {

    private val day: LocalDate = LocalDate.of(2026, 11, 14)

    private fun event(guests: Int = 400, quoted: Int = 600_000) = Event(
        id = "e1", name = "Kapoor sangeet", clientId = "c1", type = EventType.SANGEET,
        start = day.atTime(18, 0), end = day.atTime(23, 0), venue = Venue("Leela", "Gurugram"),
        guests = guests, quoted = quoted, hue = EventHue.ONE, status = EventStatus.UPCOMING,
        crewAssigned = 10, crewNeeded = 12, crewPlan = null, inventorySummary = null,
        createdAt = LocalDateTime.of(day, java.time.LocalTime.NOON),
    )

    private fun chairs(total: Int, held: Int) = InventoryItem(
        id = "chairs", name = "Chairs", total = total, detail = "",
        holds = if (held > 0) listOf(Hold("e1", held, day)) else emptyList(),
    )

    private fun helper(id: String) = CrewMember(id, "Helper $id", CrewRole.HELPER, false, 900, null, emptyList(), null)

    @Test
    fun `stock that covers the extra guests costs nothing to hire`() {
        val cr = ChangeRequestPricing.price(
            event = event(), guestsDelta = 50,
            inventory = listOf(chairs(total = 500, held = 400)),
            availableHelpers = listOf(helper("h1")), via = "WhatsApp",
        )

        val chairImpact = cr.impacts.first { it.resource == "Chairs" }
        assertTrue("450 of 500 still fits", !chairImpact.isShort)
        // Only the helper is chargeable: one helper covers 50 extra guests.
        assertEquals(900, cr.addedCost)
    }

    @Test
    fun `stock that runs short is hired and charged`() {
        val cr = ChangeRequestPricing.price(
            event = event(), guestsDelta = 200,
            inventory = listOf(chairs(total = 500, held = 400)),
            availableHelpers = emptyList(), via = "WhatsApp",
        )

        val chairImpact = cr.impacts.first { it.resource == "Chairs" }
        assertTrue("600 wanted against 500 owned", chairImpact.isShort)
        // 100 chairs at 40 + 4 helpers at the default day rate + the transport surcharge.
        val expected = 100 * 40 + 4 * CrewRole.HELPER.defaultDayRate + 4_500
        assertEquals(expected, cr.addedCost)
    }

    @Test
    fun `a large increase pulls in transport even when nothing is short`() {
        val withTransport = ChangeRequestPricing.price(
            event = event(), guestsDelta = 150,
            inventory = listOf(chairs(total = 5_000, held = 0)),
            availableHelpers = List(3) { helper("h$it") }, via = "Call",
        )
        assertTrue(withTransport.impacts.any { it.resource == "Transport" })

        val withoutTransport = ChangeRequestPricing.price(
            event = event(), guestsDelta = 20,
            inventory = listOf(chairs(total = 5_000, held = 0)),
            availableHelpers = listOf(helper("h1")), via = "Call",
        )
        assertTrue(withoutTransport.impacts.none { it.resource == "Transport" })
    }

    @Test
    fun `marginal guests are charged a quarter of the average head rate`() {
        // 600,000 over 400 guests is 1,500 a head; a quarter is 375.
        assertEquals(375, ChangeRequestPricing.perGuestRate(event(guests = 400, quoted = 600_000)))
    }

    @Test
    fun `the per-guest rate never falls below the floor`() {
        assertEquals(200, ChangeRequestPricing.perGuestRate(event(guests = 400, quoted = 100_000)))
    }

    @Test
    fun `an event with no guest count falls back rather than dividing by zero`() {
        assertEquals(350, ChangeRequestPricing.perGuestRate(event(guests = 0, quoted = 100_000)))
    }

    @Test
    fun `the revised quote is rounded to the nearest thousand below`() {
        val cr = ChangeRequestPricing.price(
            event = event(guests = 400, quoted = 600_000), guestsDelta = 50,
            inventory = emptyList(), availableHelpers = listOf(helper("h1")), via = "WhatsApp",
        )
        // 600,000 + 50 × 375 = 618,750 → 618,000
        assertEquals(618_000, cr.revisedQuote)
    }

    // ---- Booking defaults ----

    @Test
    fun `crew scales with guests but never below a six-person team`() {
        assertEquals(6, EventDefaults.crewNeeded(0))
        assertEquals(6, EventDefaults.crewNeeded(200))
        assertEquals(10, EventDefaults.crewNeeded(400))
    }

    @Test
    fun `the advance is a quarter of the quote`() {
        assertEquals(150_000, EventDefaults.advance(600_000))
    }

    @Test
    fun `a venue splits into name and area, and survives having neither`() {
        assertEquals(Venue("Leela Ambience", "Gurugram"), EventDefaults.venue("Leela Ambience, Gurugram"))
        assertEquals(Venue("Leela Ambience", ""), EventDefaults.venue("Leela Ambience"))
        assertEquals(Venue("", ""), EventDefaults.venue(""))
    }

    @Test
    fun `event colours cycle so two events on a day stay distinguishable`() {
        val hues = (0 until EventHue.entries.size + 1).map { EventDefaults.hue(it) }
        assertEquals(hues.first(), hues.last())
        assertEquals(EventHue.entries.size, hues.dropLast(1).toSet().size)
    }

    @Test
    fun `an incomplete draft does not produce an event`() {
        val draft = EventDraft(clientName = "Vikram Kapoor", type = null, date = day)
        assertNull(EventDefaults.newEvent(draft, "c1", "Kapoor", 0))

        val complete = EventDraft(clientName = "Vikram Kapoor", type = EventType.SANGEET, date = day, guests = 400)
        assertNotNull(EventDefaults.newEvent(complete, "c1", "Kapoor", 0))
    }

    @Test
    fun `a booked event runs from six in the evening for five hours`() {
        val (start, end) = EventDefaults.window(day)
        assertEquals(day.atTime(18, 0), start)
        assertEquals(day.atTime(23, 0), end)
    }
}
