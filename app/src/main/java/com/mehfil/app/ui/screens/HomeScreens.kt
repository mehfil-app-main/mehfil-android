package com.mehfil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.Cal
import com.mehfil.app.model.EventStatus
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.Seed
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.Routes
import com.mehfil.app.ui.Tab
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** A1 · Home — answer "what do I need to worry about today" in under three seconds. */
@Composable
fun HomeScreen(store: AppStore, nav: Nav) {
    val m = M
    val saturday = Cal.nextSaturday
    val saturdayEvents = store.eventsOn(saturday)
    // Home ranks urgency above chronology (NOTES Phase 2): the conflicted event leads.
    val attention = saturdayEvents.firstOrNull { it.hasCrewConflict } ?: store.upcomingEvents.firstOrNull()
    val greeting = when { Cal.today.hour < 12 -> "Good morning"; Cal.today.hour < 17 -> "Good afternoon"; else -> "Good evening" }
    Screen(
        title = "$greeting, ${Seed.vendorFirstName}", subtitle = "${Fmt.weekdayLong(Cal.todayDate)} · ${saturdayEvents.size} events this Saturday", isRoot = true,
        headerTrailing = { CircleAction(Icons.Outlined.BarChart, { nav.push(Routes.season) }, contentDescription = "Season summary") },
    ) {
        store.seasonConflict?.let { (a, b) ->
            ConflictBanner("${a.name} and ${b.name} both need Suresh's decor team on ${Fmt.weekdayDayMonth(a.day)}.") { nav.pushIn(Tab.TEAM, Routes.assign(a.id)) }
        }
        Card(onClick = { nav.tab(Tab.MONEY) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text("Outstanding across ${store.eventsWithOpenMoney} events", style = Type.caption, color = m.textMute)
                    Text(Fmt.inr(store.totalOutstanding), style = Type.displayHero, color = m.text)
                    if (store.overdueTotal > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(14.dp), tint = m.danger)
                        Text("${Fmt.inr(store.overdueTotal)} overdue · ${store.overdueSurnames}", style = Type.caption, color = m.danger)
                    } else Text("Nothing overdue", style = Type.caption, color = m.textMute)
                }
                Chevron()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            SectionHeading(attention?.let { "Needs attention · ${Fmt.weekdayShort(it.day)}urday, ${Fmt.relativeDays(it.day)}" } ?: "Next up", link = "Calendar", onLink = { nav.push(Routes.calendar) })
            attention?.let { e ->
                EventCard(e, store.clientFor(e), store.paymentState(e), onClick = { nav.push(Routes.event(e.id)) })
                val others = saturdayEvents.filter { it.id != e.id }
                if (others.isNotEmpty()) Text("Also Saturday · " + others.joinToString(" · ") { "${it.name} ${Fmt.time(it.start)}" }, style = Type.caption, color = m.textMute)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            StatTile("${store.eventsInMonth(Cal.todayDate.monthValue, Cal.todayDate.year).size}", "Events this month", Modifier.weight(1f)) { nav.tab(Tab.EVENTS) }
            StatTile("${Seed.crewOnDutyToday}", "Crew on duty today", Modifier.weight(1f)) { nav.tab(Tab.TEAM) }
            StatTile("${Seed.openEnquiries}", "Open enquiries", Modifier.weight(1f)) { nav.pushIn(Tab.PROFILE, Routes.enquiry) }
        }
    }
}

/** A2 · Calendar month — see the shape of the month. */
@Composable
fun CalendarMonthScreen(store: AppStore, nav: Nav) {
    var month by remember { mutableStateOf(YearMonth.of(2026, 11)) }
    var selected by remember { mutableStateOf(LocalDate.of(2026, 11, 14)) }
    val monthEvents = store.eventsInMonth(month.monthValue, month.year)
    val byDay = monthEvents.groupBy { it.day.dayOfMonth }
    val conflictDays = monthEvents.filter { it.hasCrewConflict }.map { it.day.dayOfMonth }.toSet()
    val dayEvents = store.eventsOn(selected)
    fun shift(n: Int) { month = month.plusMonths(n.toLong()); selected = month.atDay(1) }
    Screen(
        title = Fmt.monthYear(month.atDay(1)), subtitle = "${monthEvents.size} events · ${Fmt.inr(monthEvents.sumOf { it.quoted })} booked", onBack = nav::back, spacing = Space.lg,
        actions = { CircleAction(Icons.Outlined.ChevronLeft, { shift(-1) }); CircleAction(Icons.Outlined.ChevronRight, { shift(1) }) },
    ) {
        Box(Modifier.pointerInput(Unit) { detectHorizontalDragGestures { _, dx -> if (dx < -30) shift(1) else if (dx > 30) shift(-1) } }) {
            CalendarGrid(month, selected, { selected = it }, byDay, conflictDays)
        }
        SectionHeading(
            Fmt.weekdayLong(selected), Modifier.padding(top = Space.sm),
            trailing = if (dayEvents.isEmpty()) "No events" else "${dayEvents.size} events" + if (store.conflictsOn(selected).isEmpty()) "" else " · two overlap",
            link = if (dayEvents.isEmpty()) null else "Timeline", onLink = { nav.push(Routes.day(selected)) },
        )
        if (dayEvents.isEmpty()) Text("Nothing booked on this day.", style = Type.bodyMd, color = M.textMute, modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl), textAlign = TextAlign.Center)
        else Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            dayEvents.forEach { e -> EventCard(e, store.clientFor(e), store.paymentState(e), onClick = { nav.push(Routes.event(e.id)) }, compact = true) }
        }
    }
}

/** A3 · Calendar day detail — overlapping commitments render side by side. */
@Composable
fun DayDetailScreen(store: AppStore, nav: Nav, day: LocalDate) {
    val evs = store.eventsOn(day)
    val overlap = store.conflictsOn(day)
    var menu by remember { mutableStateOf(false) }
    Screen(
        title = Fmt.weekdayLong(day), subtitle = "${evs.size} events · ${evs.sumOf { it.crewAssigned }} crew" + (overlap.maxOfOrNull { it.start }?.let { " · two overlap from ${Fmt.time(it)}" } ?: ""), onBack = nav::back,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Assign crew") }, { menu = false; evs.firstOrNull()?.let { nav.pushIn(Tab.TEAM, Routes.assign(it.id)) } }, leadingIcon = { Icon(Icons.Outlined.Groups, null) })
                    DropdownMenuItem({ Text("Check inventory") }, { menu = false; nav.pushIn(Tab.TEAM, Routes.inventory) }, leadingIcon = { Icon(Icons.Outlined.Inventory2, null) })
                }
            }
        },
    ) {
        TimelineStrip(evs, onTap = { nav.push(Routes.event(it.id)) })
    }
}

/** A4 · Season summary — the screen a vendor shows their spouse. */
@Composable
fun SeasonSummaryScreen(store: AppStore, nav: Nav) {
    val m = M
    val live = store.events.filter { it.status != EventStatus.CANCELLED }
    val booked = live.sumOf { it.quoted }; val count = live.size
    val pct = (booked * 100.0 / Seed.seasonTarget).roundToInt()
    val delta = ((booked - Seed.lastSeasonBooked) * 100.0 / Seed.lastSeasonBooked).roundToInt()
    Screen(title = "Season 2026–27", subtitle = "October to February · $count events booked", onBack = nav::back, spacing = Space.lg) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Booked this season", style = Type.caption, color = m.textMute)
                Text(Fmt.inr(booked), style = Type.displayHero, color = m.text)
                ProgressBar(booked.toFloat() / Seed.seasonTarget, Modifier.padding(top = Space.xs))
                Text("$pct% of the ${Fmt.inr(Seed.seasonTarget)} target · ${Fmt.inr(Seed.seasonTarget - booked)} to go", style = Type.caption, color = m.textMute)
            }
        }
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text("Booked value by month", style = Type.caption, color = m.textMute)
                MonthBars(Seed.seasonMonths)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            StatTile("$count", "Events booked", Modifier.weight(1f))
            StatTile(Fmt.inr(if (count == 0) 0 else booked / count / 100 * 100), "Average ticket", Modifier.weight(1f))
        }
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(Dim.icon), tint = m.accentText)
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text("${if (delta >= 0) "+" else ""}$delta%", style = Type.displayMd, color = m.text)
                    Text("against last season", style = Type.bodyMd, color = m.text)
                    Text("${Fmt.inr(Seed.lastSeasonBooked)} across ${Seed.lastSeasonEvents} events by this point in 2025", style = Type.caption, color = m.textMute)
                }
            }
        }
    }
}

/** Single-series bars in accent; lakh notation because full grouping does not fit a 60pt column (NOTES). */
@Composable
fun MonthBars(months: List<Pair<String, Int>>) {
    val m = M
    val maxV = maxOf(1, months.maxOf { it.second })
    Row(Modifier.fillMaxWidth().height(Dim.chart + 48.dp), horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.Bottom) {
        months.forEach { (name, v) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(if (v > 0) Fmt.lakh(v) else "—", style = Type.caption, color = if (v > 0) m.text else m.textMute)
                Box(Modifier.fillMaxWidth().height(if (v > 0) maxOf(4.dp, Dim.chart * v / maxV) else 2.dp).clip(RoundedCornerShape(3.dp)).background(if (v > 0) m.accent else m.line))
                Text(name, style = Type.caption, color = m.textMute)
            }
        }
    }
}
