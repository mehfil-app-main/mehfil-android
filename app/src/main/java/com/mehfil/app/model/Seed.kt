package com.mehfil.app.model

import com.mehfil.app.model.Cal.d
import com.mehfil.app.model.Cal.dt

// The content model is fixed by NOTES.md. Every string, number and date is real content (spec §6).
object Seed {
    const val vendorFirstName = "Anand"
    const val vendorName = "Anand Mehra"
    const val businessName = "Mehfil Decor"
    const val businessArea = "Sector 44, Gurugram"
    const val businessSince = "since 2018"
    const val businessPhone = "98110 08123"
    const val upi = "mehfil@upi"
    const val bank = "HDFC ••4471"
    /** Setup crew at the warehouse today; the app has no shift model yet. */
    const val crewOnDutyToday = 4
    const val openEnquiries = 3
    const val seasonTarget = 6_000_000
    const val lastSeasonBooked = 3_580_000
    const val lastSeasonEvents = 9
    val seasonMonths = listOf("Oct" to 615_000, "Nov" to 2_205_000, "Dec" to 1_520_000, "Jan" to 260_000, "Feb" to 0)
    val runsheetMarkers = listOf("09:00", "12:00", "14:00", "17:00", "21:00", "23:30")

    val leela = Venue("The Leela Ambience", "Gurugram", "Grand Ballroom")
    val ashiyana = Venue("Ashiyana Farms", "Chattarpur")
    val sky = Venue("Sky Banquets", "Sector 29")
    val taj = Venue("Taj Palace", "New Delhi")

    val clients = listOf(
        Client("kapoor", "Vikram Kapoor", "98110 42760", false, 1, 640_000, d(2026, 11, 14), 1),
        Client("sharma", "Ritu Sharma", "98100 51234", true, 2, 970_000, d(2026, 11, 14), 2),
        Client("gill", "Harpreet Gill", "98180 66210", true, 2, 505_000, d(2026, 11, 14), 8),
        Client("fernandes", "Melissa Fernandes", "98910 30045", false, 1, 980_000, d(2026, 11, 21), 6),
        Client("reddy", "Lakshmi Reddy", "98990 12876", true, 3, 665_000, d(2026, 11, 29), 7),
        Client("ahmed", "Farah Ahmed", "98710 40320", true, 3, 840_000, d(2026, 12, 12), 3),
    )

    val events = listOf(
        Event("kapoor-sangeet", "Kapoor sangeet", "kapoor", EventType.SANGEET, dt(2026, 11, 14, 17), dt(2026, 11, 14, 23, 30), leela, 450, 640_000, EventHue.ONE, EventStatus.UPCOMING, 12, 14, true, "250 chairs · 120 drapes · 24 par cans", "8 tasks · setup 09:00 · 2 done", "2 coordinators, 6 decorators, 6 helpers"),
        Event("sharma-mehendi", "Sharma mehendi", "sharma", EventType.MEHENDI, dt(2026, 11, 14, 12), dt(2026, 11, 14, 17), ashiyana, 200, 210_000, EventHue.TWO, EventStatus.UPCOMING, 8, 8, false, "80 chairs · 90 drapes · 15 tables", "5 tasks · setup 08:00", "1 coordinator, 3 decorators, 4 helpers"),
        Event("gill-engagement", "Gill engagement", "gill", EventType.ENGAGEMENT, dt(2026, 11, 14, 19), dt(2026, 11, 14, 23), sky, 180, 195_000, EventHue.THREE, EventStatus.UPCOMING, 8, 8, true, "50 chairs · 80 drapes · 16 par cans", "6 tasks · setup 14:00", "1 coordinator, 3 decorators, 4 helpers"),
        Event("fernandes-reception", "Fernandes reception", "fernandes", EventType.RECEPTION, dt(2026, 11, 21, 19), dt(2026, 11, 22, 0), taj, 600, 980_000, EventHue.FOUR, EventStatus.UPCOMING, 18, 18, false, "600 chairs · 160 drapes · 60 tables", "9 tasks · setup 10:00"),
        Event("reddy-haldi", "Reddy haldi", "reddy", EventType.HALDI, dt(2026, 11, 29, 10), dt(2026, 11, 29, 14), ashiyana, 150, 180_000, EventHue.ONE, EventStatus.UPCOMING, 6, 6, false, "150 chairs · 40 drapes", "4 tasks · setup 07:00"),
        Event("sharma-reception", "Sharma reception", "sharma", EventType.RECEPTION, dt(2026, 12, 5, 19), dt(2026, 12, 6, 0), taj, 500, 760_000, EventHue.TWO, EventStatus.UPCOMING, 16, 16, false, "500 chairs · 140 drapes · 50 tables", "8 tasks · setup 10:00"),
        Event("ahmed-nikah", "Ahmed nikah", "ahmed", EventType.NIKAH, dt(2026, 12, 12, 11), dt(2026, 12, 12, 16), leela, 350, 420_000, EventHue.THREE, EventStatus.UPCOMING, 10, 10, false, "350 chairs · 100 drapes", "6 tasks · setup 07:00"),
        Event("reddy-reception", "Reddy reception", "reddy", EventType.RECEPTION, dt(2026, 12, 19, 19), dt(2026, 12, 20, 0), sky, 300, 340_000, EventHue.FOUR, EventStatus.UPCOMING, 12, 12, false, "300 chairs · 80 drapes", "7 tasks · setup 11:00"),
        Event("ahmed-reception", "Ahmed reception", "ahmed", EventType.RECEPTION, dt(2027, 1, 9, 19), dt(2027, 1, 10, 0), leela, 400, 260_000, EventHue.ONE, EventStatus.UPCOMING, 0, 12, false, "Not planned yet", "No tasks yet"),
        Event("ahmed-mehendi", "Ahmed mehendi", "ahmed", EventType.MEHENDI, dt(2026, 10, 3, 12), dt(2026, 10, 3, 17), ashiyana, 120, 160_000, EventHue.TWO, EventStatus.COMPLETED, 6, 6, false, "120 chairs · 40 drapes", "5 tasks · all done"),
        Event("reddy-engagement", "Reddy engagement", "reddy", EventType.ENGAGEMENT, dt(2026, 10, 10, 18), dt(2026, 10, 10, 23), sky, 160, 145_000, EventHue.THREE, EventStatus.COMPLETED, 7, 7, false, "160 chairs · 60 drapes", "6 tasks · all done"),
        Event("gill-sangeet", "Gill sangeet", "gill", EventType.SANGEET, dt(2026, 10, 24, 18), dt(2026, 10, 25, 0), leela, 300, 310_000, EventHue.FOUR, EventStatus.COMPLETED, 12, 12, false, "300 chairs · 100 drapes", "8 tasks · all done"),
    )

    private val P = MilestoneStatus.PAID; private val N = MilestoneStatus.PENDING; private val O = MilestoneStatus.OVERDUE
    val milestones = listOf(
        Milestone("kap-1", "kapoor-sangeet", "Booking advance", 160_000, d(2026, 10, 2), P, MilestoneKind.ADVANCE, d(2026, 10, 2), PaymentMethod.UPI, "2810…4471"),
        Milestone("kap-2", "kapoor-sangeet", "Second instalment", 240_000, d(2026, 11, 7), O, MilestoneKind.INSTALMENT),
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
        Milestone("shr-1", "sharma-reception", "Advance", 190_000, d(2026, 11, 1), O, MilestoneKind.ADVANCE),
        Milestone("shr-2", "sharma-reception", "Second instalment", 285_000, d(2026, 11, 28), N, MilestoneKind.INSTALMENT),
        Milestone("shr-3", "sharma-reception", "Balance", 285_000, d(2026, 12, 5), N, MilestoneKind.BALANCE),
        Milestone("an-1", "ahmed-nikah", "Booking advance", 105_000, d(2026, 10, 20), P, MilestoneKind.ADVANCE, d(2026, 10, 20), PaymentMethod.UPI),
        Milestone("an-2", "ahmed-nikah", "Second instalment", 157_500, d(2026, 11, 28), N, MilestoneKind.INSTALMENT),
        Milestone("rr-1", "reddy-reception", "Booking advance", 85_000, d(2026, 12, 5), N, MilestoneKind.ADVANCE),
        Milestone("am-1", "ahmed-mehendi", "Full payment", 160_000, d(2026, 10, 3), P, MilestoneKind.BALANCE, d(2026, 10, 3), PaymentMethod.UPI),
        Milestone("re-1", "reddy-engagement", "Full payment", 145_000, d(2026, 10, 10), P, MilestoneKind.BALANCE, d(2026, 10, 10), PaymentMethod.BANK),
        Milestone("gs-1", "gill-sangeet", "Full payment", 310_000, d(2026, 10, 24), P, MilestoneKind.BALANCE, d(2026, 10, 24), PaymentMethod.UPI),
    )

    val sentReminders = mapOf(
        "kap-2" to listOf(
            SentReminder("r1", ReminderTone.GENTLE, dt(2026, 11, 9, 10, 15), ReminderChannel.WHATSAPP, "seen 10:40 · no reply"),
            SentReminder("r2", ReminderTone.STANDARD, dt(2026, 11, 11, 9, 2), ReminderChannel.SMS, "delivered"),
        )
    )

    val crew = listOf(
        CrewMember("priya", "Priya Malhotra", CrewRole.COORDINATOR, false, 7, listOf("kapoor-sangeet")),
        CrewMember("rohit", "Rohit Verma", CrewRole.COORDINATOR, false, 1, listOf("sharma-mehendi"), "coordinator"),
        CrewMember("suresh", "Suresh Yadav", CrewRole.DECORATOR, true, 5, listOf("kapoor-sangeet", "gill-engagement"), "lead"),
        CrewMember("manoj", "Manoj Kumar", CrewRole.DECORATOR, false, 8, listOf("kapoor-sangeet")),
        CrewMember("deepak", "Deepak Singh", CrewRole.DECORATOR, false, 4, listOf("kapoor-sangeet")),
        CrewMember("arjun", "Arjun Mehta", CrewRole.DECORATOR, false, 2, listOf("kapoor-sangeet")),
        CrewMember("kavita", "Kavita Rani", CrewRole.DECORATOR, false, 6, listOf("sharma-mehendi"), "free from 17:30"),
        CrewMember("ramesh", "Ramesh", CrewRole.HELPER, false, 3, listOf("kapoor-sangeet")),
        CrewMember("sunil", "Sunil", CrewRole.HELPER, false, null, emptyList(), "free all day"),
        CrewMember("vikram-s", "Vikram Singh", CrewRole.HELPER, false, 1, listOf("kapoor-sangeet")),
        CrewMember("anil", "Anil", CrewRole.HELPER, false, null, listOf("gill-engagement")),
        CrewMember("ravi", "Ravi", CrewRole.HELPER, false, null, listOf("gill-engagement")),
        CrewMember("mohan", "Mohan", CrewRole.HELPER, false, null, listOf("sharma-mehendi")),
    )

    val attendance: Map<String, Map<String, AttendanceState>> = mapOf(
        "kapoor-sangeet" to mapOf(
            "priya" to AttendanceState.ARRIVED, "suresh" to AttendanceState.ARRIVED, "manoj" to AttendanceState.ARRIVED, "deepak" to AttendanceState.CONFIRMED,
            "arjun" to AttendanceState.CONFIRMED, "ramesh" to AttendanceState.ARRIVED, "vikram-s" to AttendanceState.ABSENT, "sunil" to AttendanceState.CONFIRMED,
        ),
        "sharma-mehendi" to mapOf("rohit" to AttendanceState.ARRIVED, "kavita" to AttendanceState.ARRIVED, "mohan" to AttendanceState.CONFIRMED),
        "gill-engagement" to mapOf("suresh" to AttendanceState.CONFIRMED, "anil" to AttendanceState.CONFIRMED, "ravi" to AttendanceState.CONFIRMED),
    )

    val inventory: List<InventoryItem> = run {
        val d14 = d(2026, 11, 14); val d21 = d(2026, 11, 21); val d29 = d(2026, 11, 29); val d5 = d(2026, 12, 5); val d12 = d(2026, 12, 12); val d19 = d(2026, 12, 19)
        listOf(
            InventoryItem("drapes", "Drape panels", 240, "3 m velvet · 12 colours", listOf(
                Hold("kapoor-sangeet", 120, d14), Hold("sharma-mehendi", 90, d14), Hold("gill-engagement", 80, d14),
                Hold("fernandes-reception", 160, d21), Hold("sharma-reception", 140, d5), Hold("ahmed-nikah", 100, d12))),
            InventoryItem("tables", "Round tables", 60, "6 ft · seats 10", listOf(
                Hold("kapoor-sangeet", 45, d14), Hold("sharma-mehendi", 15, d14), Hold("fernandes-reception", 60, d21), Hold("sharma-reception", 50, d5))),
            InventoryItem("chairs", "Chiavari chairs", 600, "Gold · with cushions", listOf(
                Hold("kapoor-sangeet", 250, d14), Hold("sharma-mehendi", 80, d14), Hold("gill-engagement", 50, d14),
                Hold("fernandes-reception", 600, d21), Hold("reddy-haldi", 150, d29), Hold("sharma-reception", 500, d5),
                Hold("ahmed-nikah", 350, d12), Hold("reddy-reception", 300, d19))),
            InventoryItem("parcans", "Par cans", 48, "LED · RGBW", listOf(
                Hold("kapoor-sangeet", 24, d14), Hold("gill-engagement", 16, d14), Hold("fernandes-reception", 40, d21))),
            InventoryItem("risers", "Stage risers", 24, "4 × 8 ft · 2 ft high", listOf(
                Hold("kapoor-sangeet", 12, d14), Hold("gill-engagement", 6, d14), Hold("fernandes-reception", 20, d21))),
        )
    }

    val runsheet = listOf(
        RunsheetTask("t1", "kapoor-sangeet", "Setup", "09:00–14:00", "Unload and set stage risers", "Suresh's team · 6 crew", "1 h 30", true),
        RunsheetTask("t2", "kapoor-sangeet", "Setup", "09:00–14:00", "Rig par cans on truss", "Deepak, Arjun", "2 h", true),
        RunsheetTask("t3", "kapoor-sangeet", "Setup", "09:00–14:00", "Hang 120 drape panels", "Manoj's team · 4 crew", "3 h", false),
        RunsheetTask("t4", "kapoor-sangeet", "Dressing", "14:00–17:00", "Floral mandap and entrance", "Kavita · 3 crew", "2 h 30", false),
        RunsheetTask("t5", "kapoor-sangeet", "Dressing", "14:00–17:00", "Lay 45 round tables", "Ramesh, Sunil, Vikram", "1 h", false),
        RunsheetTask("t6", "kapoor-sangeet", "Event", "17:00–23:30", "Lights and sound check", "Deepak · 30 min", "before 16:30", false),
        RunsheetTask("t7", "kapoor-sangeet", "Event", "17:00–23:30", "Welcome florals at guest arrival", "Kavita · 2 crew", "17:00", false),
        RunsheetTask("t8", "kapoor-sangeet", "Event", "17:00–23:30", "Pack-down and load trucks", "All crew", "from 23:30", false),
    )

    val gillExpenses = listOf(
        Expense("e1", "Agency crew · conflict cover", "8 crew from Shakti Events · 2 days", 38_400),
        Expense("e2", "Flowers", "Marigold, roses, foliage · Ghazipur mandi", 42_000),
        Expense("e3", "Lighting and truss hire", "4 m truss · 24 par cans", 35_000),
        Expense("e4", "Fabric and drapes", "Cleaning and 40 new panels", 28_000),
        Expense("e5", "Venue surcharge", "Sky Banquets · outside vendor fee", 52_000),
        Expense("e6", "Transport", "2 trucks · Chattarpur to Sector 29", 12_500),
    )

    val packages = listOf(
        Package("stage", "Stage & backdrop", "12 m truss, 40 par cans", 180_000),
        Package("mandap", "Floral mandap", "fresh marigold and jasmine", 95_000),
        Package("drapes", "Drapes & ceiling", "160 panels", 130_000),
        Package("arch", "Entrance arch", "4 m floral arch", 40_000),
    )

    val changeImpacts = listOf(
        ChangeImpact("c1", "Chiavari chairs", "380 → 500 of 600 held on 14 Nov", "+120", false, "box"),
        ChangeImpact("c2", "Round tables", "All 60 held · hire 20 more", "+20 · ₹8,000", true, "box"),
        ChangeImpact("c3", "Helpers", "Ramesh, Sunil, Anil, Ravi", "+4 · ₹3,200", false, "people"),
        ChangeImpact("c4", "Transport", "Second truck, chairs and tables", "₹4,500", false, "truck"),
    )

    val enquiry = Enquiry("q1", "Anjali Kapoor", "Referral · Vikram Kapoor", "₹3,00,000 – ₹5,00,000", d(2026, 12, 5), 250,
        "Sister's engagement. Pastel florals, saw the Fernandes reel. Wants a quote by Friday.")
    val enquirySources = listOf("Referral · Vikram Kapoor", "Instagram", "WhatsApp", "Wedding planner", "Walk-in")
    val budgetRanges = listOf("Under ₹1,50,000", "₹1,50,000 – ₹3,00,000", "₹3,00,000 – ₹5,00,000", "₹5,00,000 – ₹10,00,000", "Over ₹10,00,000")
}
