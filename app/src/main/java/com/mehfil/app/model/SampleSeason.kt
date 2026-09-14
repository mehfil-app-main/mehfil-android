package com.mehfil.app.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The design's content model (NOTES.md), offered as a sample season during onboarding and for demos.
 * Every date is shifted by whole weeks so the focal Saturday lands on the next Saturday from today.
 */
object SampleSeason {
    private val anchor: LocalDate = LocalDate.of(2026, 11, 14)   // Kapoor sangeet in the design

    fun profile() = VendorProfile("Anand Mehra", "Mehfil Decor", "98110 08123", "Sector 44, Gurugram", "since 2018", "mehfil@upi", "HDFC ••4471", 6_000_000, 3_580_000, 9)

    fun data(): VendorData {
        val shift = Cal.daysBetween(anchor, Cal.nextSaturday).toLong()
        fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day).plusDays(shift)
        fun dt(y: Int, m: Int, day: Int, h: Int, min: Int = 0): LocalDateTime = LocalDateTime.of(y, m, day, h, min).plusDays(shift)
        val now = Cal.today

        val leela = Venue("The Leela Ambience", "Gurugram", "Grand Ballroom")
        val ashiyana = Venue("Ashiyana Farms", "Chattarpur")
        val sky = Venue("Sky Banquets", "Sector 29")
        val taj = Venue("Taj Palace", "New Delhi")

        val clients = listOf(
            Client("kapoor", "Vikram Kapoor", "98110 42760", false, 1, now), Client("sharma", "Ritu Sharma", "98100 51234", true, 2, now),
            Client("gill", "Harpreet Gill", "98180 66210", true, 8, now), Client("fernandes", "Melissa Fernandes", "98910 30045", false, 6, now),
            Client("reddy", "Lakshmi Reddy", "98990 12876", true, 7, now), Client("ahmed", "Farah Ahmed", "98710 40320", true, 3, now),
        )
        val U = EventStatus.UPCOMING; val C = EventStatus.COMPLETED
        val events = listOf(
            Event("kapoor-sangeet", "Kapoor sangeet", "kapoor", EventType.SANGEET, dt(2026, 11, 14, 17), dt(2026, 11, 14, 23, 30), leela, 450, 640_000, EventHue.ONE, U, 12, 14, "2 coordinators, 6 decorators, 6 helpers", "250 chairs · 120 drapes · 24 par cans", now),
            Event("sharma-mehendi", "Sharma mehendi", "sharma", EventType.MEHENDI, dt(2026, 11, 14, 12), dt(2026, 11, 14, 17), ashiyana, 200, 210_000, EventHue.TWO, U, 8, 8, "1 coordinator, 3 decorators, 4 helpers", "80 chairs · 90 drapes · 15 tables", now),
            Event("gill-engagement", "Gill engagement", "gill", EventType.ENGAGEMENT, dt(2026, 11, 14, 19), dt(2026, 11, 14, 23), sky, 180, 195_000, EventHue.THREE, U, 8, 8, "1 coordinator, 3 decorators, 4 helpers", "50 chairs · 80 drapes · 16 par cans", now),
            Event("fernandes-reception", "Fernandes reception", "fernandes", EventType.RECEPTION, dt(2026, 11, 21, 19), dt(2026, 11, 22, 0), taj, 600, 980_000, EventHue.FOUR, U, 18, 18, null, "600 chairs · 160 drapes · 60 tables", now),
            Event("reddy-haldi", "Reddy haldi", "reddy", EventType.HALDI, dt(2026, 11, 29, 10), dt(2026, 11, 29, 14), ashiyana, 150, 180_000, EventHue.ONE, U, 6, 6, null, "150 chairs · 40 drapes", now),
            Event("sharma-reception", "Sharma reception", "sharma", EventType.RECEPTION, dt(2026, 12, 5, 19), dt(2026, 12, 6, 0), taj, 500, 760_000, EventHue.TWO, U, 16, 16, null, "500 chairs · 140 drapes · 50 tables", now),
            Event("ahmed-nikah", "Ahmed nikah", "ahmed", EventType.NIKAH, dt(2026, 12, 12, 11), dt(2026, 12, 12, 16), leela, 350, 420_000, EventHue.THREE, U, 10, 10, null, "350 chairs · 100 drapes", now),
            Event("reddy-reception", "Reddy reception", "reddy", EventType.RECEPTION, dt(2026, 12, 19, 19), dt(2026, 12, 20, 0), sky, 300, 340_000, EventHue.FOUR, U, 12, 12, null, "300 chairs · 80 drapes", now),
            Event("ahmed-reception", "Ahmed reception", "ahmed", EventType.RECEPTION, dt(2027, 1, 9, 19), dt(2027, 1, 10, 0), leela, 400, 260_000, EventHue.ONE, U, 0, 12, null, "Not planned yet", now),
            Event("ahmed-mehendi", "Ahmed mehendi", "ahmed", EventType.MEHENDI, dt(2026, 10, 3, 12), dt(2026, 10, 3, 17), ashiyana, 120, 160_000, EventHue.TWO, C, 6, 6, null, "120 chairs · 40 drapes", now),
            Event("reddy-engagement", "Reddy engagement", "reddy", EventType.ENGAGEMENT, dt(2026, 10, 10, 18), dt(2026, 10, 10, 23), sky, 160, 145_000, EventHue.THREE, C, 7, 7, null, "160 chairs · 60 drapes", now),
            Event("gill-sangeet", "Gill sangeet", "gill", EventType.SANGEET, dt(2026, 10, 24, 18), dt(2026, 10, 25, 0), leela, 300, 310_000, EventHue.FOUR, C, 12, 12, null, "300 chairs · 100 drapes", now),
        )
        val P = MilestoneStatus.PAID; val N = MilestoneStatus.PENDING
        val milestones = listOf(
            Milestone("kap-1", "kapoor-sangeet", "Booking advance", 160_000, d(2026, 10, 2), P, MilestoneKind.ADVANCE, d(2026, 10, 2), PaymentMethod.UPI, "2810…4471"),
            Milestone("kap-2", "kapoor-sangeet", "Second instalment", 240_000, d(2026, 11, 7), N, MilestoneKind.INSTALMENT),
            Milestone("kap-3", "kapoor-sangeet", "Balance", 160_000, d(2026, 11, 14), N, MilestoneKind.BALANCE),
            Milestone("kap-4", "kapoor-sangeet", "Post-event settlement", 80_000, d(2026, 11, 30), N, MilestoneKind.SETTLEMENT),
            Milestone("shm-1", "sharma-mehendi", "Booking advance", 105_000, d(2026, 9, 20), P, MilestoneKind.ADVANCE, d(2026, 9, 20), PaymentMethod.BANK),
            Milestone("shm-2", "sharma-mehendi", "Balance", 105_000, d(2026, 11, 14), N, MilestoneKind.BALANCE),
            Milestone("gil-1", "gill-engagement", "Booking advance", 60_000, d(2026, 10, 1), P, MilestoneKind.ADVANCE, d(2026, 10, 1), PaymentMethod.UPI),
            Milestone("gil-2", "gill-engagement", "Balance", 135_000, d(2026, 11, 14), N, MilestoneKind.BALANCE),
            Milestone("fer-1", "fernandes-reception", "Booking advance", 490_000, d(2026, 9, 5), P, MilestoneKind.ADVANCE, d(2026, 9, 5), PaymentMethod.BANK),
            Milestone("fer-2", "fernandes-reception", "Second instalment", 245_000, d(2026, 11, 18), N, MilestoneKind.INSTALMENT),
            Milestone("fer-3", "fernandes-reception", "Balance", 245_000, d(2026, 11, 21), N, MilestoneKind.BALANCE),
            Milestone("rh-1", "reddy-haldi", "Booking advance", 45_000, d(2026, 10, 15), P, MilestoneKind.ADVANCE, d(2026, 10, 15), PaymentMethod.CASH),
            Milestone("rh-2", "reddy-haldi", "Balance", 135_000, d(2026, 11, 29), N, MilestoneKind.BALANCE),
            Milestone("shr-1", "sharma-reception", "Advance", 190_000, d(2026, 11, 1), N, MilestoneKind.ADVANCE),
            Milestone("shr-2", "sharma-reception", "Second instalment", 285_000, d(2026, 11, 28), N, MilestoneKind.INSTALMENT),
            Milestone("shr-3", "sharma-reception", "Balance", 285_000, d(2026, 12, 5), N, MilestoneKind.BALANCE),
            Milestone("an-1", "ahmed-nikah", "Booking advance", 105_000, d(2026, 10, 20), P, MilestoneKind.ADVANCE, d(2026, 10, 20), PaymentMethod.UPI),
            Milestone("an-2", "ahmed-nikah", "Second instalment", 157_500, d(2026, 11, 28), N, MilestoneKind.INSTALMENT),
            Milestone("rr-1", "reddy-reception", "Booking advance", 85_000, d(2026, 12, 5), N, MilestoneKind.ADVANCE),
            Milestone("am-1", "ahmed-mehendi", "Full payment", 160_000, d(2026, 10, 3), P, MilestoneKind.BALANCE, d(2026, 10, 3), PaymentMethod.UPI),
            Milestone("re-1", "reddy-engagement", "Full payment", 145_000, d(2026, 10, 10), P, MilestoneKind.BALANCE, d(2026, 10, 10), PaymentMethod.BANK),
            Milestone("gs-1", "gill-sangeet", "Full payment", 310_000, d(2026, 10, 24), P, MilestoneKind.BALANCE, d(2026, 10, 24), PaymentMethod.UPI),
        )
        val reminders = listOf(
            SentReminder("r1", "kap-2", ReminderTone.GENTLE, ReminderChannel.WHATSAPP, dt(2026, 11, 9, 10, 15), "seen 10:40 · no reply"),
            SentReminder("r2", "kap-2", ReminderTone.STANDARD, ReminderChannel.SMS, dt(2026, 11, 11, 9, 2), "delivered"),
        )
        val K = "kapoor-sangeet"; val S = "sharma-mehendi"; val G = "gill-engagement"
        fun cm(id: String, name: String, role: CrewRole, lead: Boolean = false, avatar: Int? = null, bookings: List<String> = emptyList(), note: String? = null) =
            CrewMember(id, name, role, lead, role.defaultDayRate, avatar, bookings, note)
        val crew = listOf(
            cm("priya", "Priya Malhotra", CrewRole.COORDINATOR, avatar = 7, bookings = listOf(K)),
            cm("rohit", "Rohit Verma", CrewRole.COORDINATOR, avatar = 1, bookings = listOf(S), note = "coordinator"),
            cm("suresh", "Suresh Yadav", CrewRole.DECORATOR, true, 5, listOf(K, G), "lead"),
            cm("manoj", "Manoj Kumar", CrewRole.DECORATOR, avatar = 8, bookings = listOf(K)),
            cm("deepak", "Deepak Singh", CrewRole.DECORATOR, avatar = 4, bookings = listOf(K)),
            cm("arjun", "Arjun Mehta", CrewRole.DECORATOR, avatar = 2, bookings = listOf(K)),
            cm("kavita", "Kavita Rani", CrewRole.DECORATOR, avatar = 6, bookings = listOf(S), note = "free from 17:30"),
            cm("nitin", "Nitin Saini", CrewRole.DECORATOR, bookings = listOf(G)), cm("pooja", "Pooja Bisht", CrewRole.DECORATOR, bookings = listOf(G)),
            cm("ramesh", "Ramesh", CrewRole.HELPER, avatar = 3, bookings = listOf(K)), cm("sunil", "Sunil", CrewRole.HELPER, note = "free all day"),
            cm("vikram-s", "Vikram Singh", CrewRole.HELPER, avatar = 1, bookings = listOf(K)), cm("anil", "Anil", CrewRole.HELPER, bookings = listOf(G)),
            cm("ravi", "Ravi", CrewRole.HELPER, bookings = listOf(G)), cm("mohan", "Mohan", CrewRole.HELPER, bookings = listOf(S)),
            cm("ajay", "Ajay", CrewRole.HELPER, bookings = listOf(K)), cm("bhola", "Bhola", CrewRole.HELPER, bookings = listOf(K)),
            cm("chandan", "Chandan", CrewRole.HELPER, bookings = listOf(K)), cm("dinesh", "Dinesh", CrewRole.HELPER, bookings = listOf(K)),
            cm("gopal", "Gopal", CrewRole.HELPER, bookings = listOf(K)), cm("harish", "Harish", CrewRole.HELPER, bookings = listOf(S)),
            cm("imran", "Imran", CrewRole.HELPER, bookings = listOf(S)), cm("jagdish", "Jagdish", CrewRole.HELPER, bookings = listOf(S)),
            cm("karan", "Karan", CrewRole.HELPER, bookings = listOf(S)), cm("prakash", "Prakash", CrewRole.HELPER, bookings = listOf(S)),
            cm("lalit", "Lalit", CrewRole.HELPER, bookings = listOf(G)), cm("naveen", "Naveen", CrewRole.HELPER, bookings = listOf(G)),
            cm("om", "Om Prakash", CrewRole.HELPER, bookings = listOf(G)),
        )
        val A = AttendanceState.ARRIVED; val CF = AttendanceState.CONFIRMED; val AB = AttendanceState.ABSENT
        val attendance = listOf(
            Attendance(K, mapOf("priya" to A, "suresh" to A, "manoj" to A, "deepak" to CF, "arjun" to CF, "ramesh" to A, "vikram-s" to AB, "ajay" to A, "bhola" to A, "chandan" to A, "dinesh" to A, "gopal" to CF)),
            Attendance(S, mapOf("rohit" to A, "kavita" to A, "mohan" to CF)),
            Attendance(G, mapOf("suresh" to CF, "anil" to CF, "ravi" to CF)),
        )
        val d14 = d(2026, 11, 14); val d21 = d(2026, 11, 21); val d29 = d(2026, 11, 29); val d5 = d(2026, 12, 5); val d12 = d(2026, 12, 12); val d19 = d(2026, 12, 19)
        val inventory = listOf(
            InventoryItem("drapes", "Drape panels", 240, "3 m velvet · 12 colours", listOf(Hold(K, 120, d14), Hold(S, 90, d14), Hold(G, 80, d14), Hold("fernandes-reception", 160, d21), Hold("sharma-reception", 140, d5), Hold("ahmed-nikah", 100, d12))),
            InventoryItem("tables", "Round tables", 60, "6 ft · seats 10", listOf(Hold(K, 45, d14), Hold(S, 15, d14), Hold("fernandes-reception", 60, d21), Hold("sharma-reception", 50, d5))),
            InventoryItem("chairs", "Chiavari chairs", 600, "Gold · with cushions", listOf(Hold(K, 250, d14), Hold(S, 80, d14), Hold(G, 50, d14), Hold("fernandes-reception", 600, d21), Hold("reddy-haldi", 150, d29), Hold("sharma-reception", 500, d5), Hold("ahmed-nikah", 350, d12), Hold("reddy-reception", 300, d19))),
            InventoryItem("parcans", "Par cans", 48, "LED · RGBW", listOf(Hold(K, 24, d14), Hold(G, 16, d14), Hold("fernandes-reception", 40, d21))),
            InventoryItem("risers", "Stage risers", 24, "4 × 8 ft · 2 ft high", listOf(Hold(K, 12, d14), Hold(G, 6, d14), Hold("fernandes-reception", 20, d21))),
        )
        val taskRows = listOf(
            Triple("Setup", "09:00–14:00", Triple("Unload and set stage risers", "Suresh's team · 6 crew", "1 h 30")) to true,
            Triple("Setup", "09:00–14:00", Triple("Rig par cans on truss", "Deepak, Arjun", "2 h")) to true,
            Triple("Setup", "09:00–14:00", Triple("Hang 120 drape panels", "Manoj's team · 4 crew", "3 h")) to false,
            Triple("Dressing", "14:00–17:00", Triple("Floral mandap and entrance", "Kavita · 3 crew", "2 h 30")) to false,
            Triple("Dressing", "14:00–17:00", Triple("Lay 45 round tables", "Ramesh, Sunil, Vikram", "1 h")) to false,
            Triple("Event", "17:00–23:30", Triple("Lights and sound check", "Deepak · 30 min", "before 16:30")) to false,
            Triple("Event", "17:00–23:30", Triple("Welcome florals at guest arrival", "Kavita · 2 crew", "17:00")) to false,
            Triple("Event", "17:00–23:30", Triple("Pack-down and load trucks", "All crew", "from 23:30")) to false,
        )
        val tasks = taskRows.mapIndexed { i, (t, done) -> RunsheetTask("t${i + 1}", K, t.first, t.second, t.third.first, t.third.second, t.third.third, done, i) }
        val expenses = listOf(
            Expense("e1", G, "Agency crew · conflict cover", "8 crew from Shakti Events · 2 days", 38_400), Expense("e2", G, "Flowers", "Marigold, roses, foliage · Ghazipur mandi", 42_000),
            Expense("e3", G, "Lighting and truss hire", "4 m truss · 24 par cans", 35_000), Expense("e4", G, "Fabric and drapes", "Cleaning and 40 new panels", 28_000),
            Expense("e5", G, "Venue surcharge", "Sky Banquets · outside vendor fee", 52_000), Expense("e6", G, "Transport", "2 trucks · Chattarpur to Sector 29", 12_500),
        )
        val changeRequests = listOf(
            ChangeRequest("cr1", K, "Add 200 guests. 450 becomes 650.", 200, listOf(
                ChangeImpact("Chiavari chairs", "380 → 500 of 600 held on ${Fmt.dayMonth(d14)}", "+120", false, "inventory"),
                ChangeImpact("Round tables", "All 60 held · hire 20 more", "+20 · ₹8,000", true, "inventory"),
                ChangeImpact("Helpers", "Ramesh, Sunil, Anil, Ravi", "+4 · ₹3,200", false, "people"),
                ChangeImpact("Transport", "Second truck, chairs and tables", "₹4,500", false, "transport"),
            ), 15_700, 710_000, 350, ChangeRequestStatus.PENDING, now.withHour(8).withMinute(12), "WhatsApp"),
        )
        val enquiries = listOf(
            Enquiry("q1", "Anjali Kapoor", "98110 77120", "Referral · Vikram Kapoor", "₹3,00,000 – ₹5,00,000", d5, 250, "Sister's engagement. Pastel florals, saw the Fernandes reel. Wants a quote by Friday.", EnquiryStatus.OPEN, now),
            Enquiry("q2", "Neha Bhatia", "98200 41100", "Instagram", "₹1,50,000 – ₹3,00,000", d(2027, 1, 16), 150, "Haldi at home, Chattarpur.", EnquiryStatus.OPEN, now),
            Enquiry("q3", "Rohan Kapadia", "98330 20011", "Wedding planner", "Over ₹10,00,000", d(2027, 1, 30), 700, "Destination-style reception, two stages.", EnquiryStatus.OPEN, now),
        )
        return VendorData(clients, events, milestones, crew, inventory, tasks, attendance, reminders, expenses, enquiries, defaultPackages(), changeRequests)
    }

    fun defaultPackages() = listOf(
        Package("stage", "Stage & backdrop", "12 m truss, 40 par cans", 180_000), Package("mandap", "Floral mandap", "fresh marigold and jasmine", 95_000),
        Package("drapes", "Drapes & ceiling", "160 panels", 130_000), Package("arch", "Entrance arch", "4 m floral arch", 40_000),
        Package("lighting", "Lighting & sound", "par cans, moving heads, PA", 75_000), Package("seating", "Seating & tables", "chiavari chairs, round tables, linen", 60_000),
    )
    val enquirySources = listOf("Referral", "Instagram", "WhatsApp", "Wedding planner", "Walk-in", "Website")
    val budgetRanges = listOf("Under ₹1,50,000", "₹1,50,000 – ₹3,00,000", "₹3,00,000 – ₹5,00,000", "₹5,00,000 – ₹10,00,000", "Over ₹10,00,000")
}
