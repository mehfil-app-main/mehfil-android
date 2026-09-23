package com.mehfil.app.domain

import com.mehfil.app.model.Client
import com.mehfil.app.model.CrewMember
import com.mehfil.app.model.CrewRole
import com.mehfil.app.model.Event
import com.mehfil.app.model.EventHue
import com.mehfil.app.model.EventStatus
import com.mehfil.app.model.EventType
import com.mehfil.app.model.Milestone
import com.mehfil.app.model.MilestoneKind
import com.mehfil.app.model.MilestoneStatus
import com.mehfil.app.model.PaymentState
import com.mehfil.app.model.Venue
import com.mehfil.app.model.VendorData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The rules a vendor makes money on: what counts as a double-booking, and what counts as overdue.
 * [TODAY] is fixed so the suite does not change meaning as the calendar moves.
 */
class VendorViewTest {

    private fun event(
        id: String,
        day: LocalDate = TODAY.plusDays(7),
        startHour: Int = 18,
        durationHours: Long = 5,
        status: EventStatus = EventStatus.UPCOMING,
        quoted: Int = 100_000,
        clientId: String = "c1",
    ): Event {
        val start = day.atTime(startHour, 0)
        return Event(
            id = id, name = "$id event", clientId = clientId, type = EventType.SANGEET,
            start = start, end = start.plusHours(durationHours), venue = Venue("Hall", "Area"),
            guests = 200, quoted = quoted, hue = EventHue.ONE, status = status,
            crewAssigned = 0, crewNeeded = 6, crewPlan = null, inventorySummary = null,
            createdAt = LocalDateTime.of(TODAY, java.time.LocalTime.NOON),
        )
    }

    private fun member(id: String, vararg bookings: String) = CrewMember(
        id = id, name = "Member $id", role = CrewRole.HELPER, isLead = false,
        dayRate = 900, avatar = null, bookings = bookings.toList(), note = null,
    )

    private fun milestone(id: String, eventId: String, amount: Int, due: LocalDate, status: MilestoneStatus) =
        Milestone(id, eventId, "Balance", amount, due, status, MilestoneKind.BALANCE)

    private fun view(data: VendorData) = VendorView(data, TODAY)

    // ---- Conflicts ----

    @Test
    fun `member booked on two overlapping events is a conflict`() {
        val a = event("a", startHour = 17, durationHours = 6)   // 17:00–23:00
        val b = event("b", startHour = 19, durationHours = 4)   // 19:00–23:00
        val v = view(VendorData(events = listOf(a, b), crew = listOf(member("m", "a", "b"))))

        assertTrue(v.isConflicted(a))
        assertTrue(v.isConflicted(b))
        assertEquals("m", v.seasonConflict?.third?.id)
    }

    @Test
    fun `same day but non-overlapping is not a conflict`() {
        val morning = event("a", startHour = 9, durationHours = 3)    // 09:00–12:00
        val evening = event("b", startHour = 18, durationHours = 4)   // 18:00–22:00
        val v = view(VendorData(events = listOf(morning, evening), crew = listOf(member("m", "a", "b"))))

        assertFalse(v.isConflicted(morning))
        assertFalse(v.isConflicted(evening))
        assertNull(v.seasonConflict)
    }

    @Test
    fun `two events overlap but no shared crew is not a conflict`() {
        val a = event("a", startHour = 17, durationHours = 6)
        val b = event("b", startHour = 19, durationHours = 4)
        val v = view(VendorData(events = listOf(a, b), crew = listOf(member("m1", "a"), member("m2", "b"))))

        assertFalse(v.isConflicted(a))
        assertNull(v.seasonConflict)
    }

    @Test
    fun `cancelled events never conflict`() {
        val a = event("a", startHour = 17, durationHours = 6)
        val cancelled = event("b", startHour = 19, durationHours = 4, status = EventStatus.CANCELLED)
        val v = view(VendorData(events = listOf(a, cancelled), crew = listOf(member("m", "a", "b"))))

        assertFalse(v.isConflicted(a))
    }

    @Test
    fun `season conflict ignores events already past`() {
        val past = event("a", day = TODAY.minusDays(3), startHour = 17, durationHours = 6)
        val alsoPast = event("b", day = TODAY.minusDays(3), startHour = 19, durationHours = 4)
        val v = view(VendorData(events = listOf(past, alsoPast), crew = listOf(member("m", "a", "b"))))

        assertNull(v.seasonConflict)
    }

    // ---- Money ----

    @Test
    fun `an unpaid milestone due before today is overdue`() {
        val e = event("a")
        val v = view(
            VendorData(
                events = listOf(e),
                milestones = listOf(milestone("m", "a", 50_000, TODAY.minusDays(2), MilestoneStatus.PENDING)),
            ),
        )

        assertEquals(PaymentState.OVERDUE, v.paymentState(e))
        assertEquals(50_000, v.overdueTotal)
        assertEquals(50_000, v.totalOutstanding)
    }

    @Test
    fun `a paid milestone is never overdue however old`() {
        val e = event("a")
        val v = view(
            VendorData(
                events = listOf(e),
                milestones = listOf(milestone("m", "a", 50_000, TODAY.minusDays(90), MilestoneStatus.PAID)),
            ),
        )

        assertEquals(PaymentState.PAID, v.paymentState(e))
        assertEquals(0, v.overdueTotal)
        assertEquals(0, v.totalOutstanding)
    }

    @Test
    fun `part paid reads as partial and only the open half is outstanding`() {
        val e = event("a")
        val v = view(
            VendorData(
                events = listOf(e),
                milestones = listOf(
                    milestone("paid", "a", 40_000, TODAY.minusDays(30), MilestoneStatus.PAID),
                    milestone("open", "a", 60_000, TODAY.plusDays(30), MilestoneStatus.PENDING),
                ),
            ),
        )

        assertEquals(PaymentState.PARTIAL, v.paymentState(e))
        assertEquals(40_000, v.collected("a"))
        assertEquals(60_000, v.balanceDue("a"))
        assertEquals(0, v.overdueTotal)
    }

    @Test
    fun `money owed against a cancelled event is not counted as outstanding`() {
        val cancelled = event("a", status = EventStatus.CANCELLED)
        val v = view(
            VendorData(
                events = listOf(cancelled),
                milestones = listOf(milestone("m", "a", 50_000, TODAY.minusDays(2), MilestoneStatus.PENDING)),
            ),
        )

        assertEquals(0, v.totalOutstanding)
        assertTrue(v.overdue.isEmpty())
    }

    @Test
    fun `milestone buckets are disjoint`() {
        val e = event("a")
        val v = view(
            VendorData(
                events = listOf(e),
                milestones = listOf(
                    milestone("late", "a", 10_000, TODAY.minusDays(5), MilestoneStatus.PENDING),
                    milestone("later", "a", 20_000, TODAY.plusDays(60), MilestoneStatus.PENDING),
                ),
            ),
        )

        val all = v.overdue + v.dueThisWeek + v.upcomingMilestones
        assertEquals(v.openMilestones.size, all.size)
        assertEquals(all.map { it.id }.toSet().size, all.size)
    }

    // ---- Client value ----

    @Test
    fun `lifetime value counts every event except cancelled ones`() {
        val client = Client("c1", "Vikram Kapoor", "9811000000", false, 1, LocalDateTime.of(TODAY, java.time.LocalTime.NOON))
        val v = view(
            VendorData(
                clients = listOf(client),
                events = listOf(
                    event("a", quoted = 100_000),
                    event("b", quoted = 250_000, status = EventStatus.COMPLETED),
                    event("c", quoted = 999_000, status = EventStatus.CANCELLED),
                ),
            ),
        )

        val stats = v.stats(client)
        assertEquals(2, stats.eventCount)
        assertEquals(350_000, stats.lifetimeValue)
        assertEquals(350_000, v.lifetimeValue)
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 11, 12)
    }
}
