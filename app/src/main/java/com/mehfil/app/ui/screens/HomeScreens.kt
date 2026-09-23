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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.Cal
import com.mehfil.app.model.Event
import com.mehfil.app.model.Fmt
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
    val v = store.vendor
    val saturday = Cal.nextSaturday
    val saturdayEvents = store.eventsOn(saturday)
    val attention: Event? = saturdayEvents.firstOrNull { store.isConflicted(it) } ?: store.upcomingEvents.firstOrNull { store.isConflicted(it) } ?: store.upcomingEvents.firstOrNull { it.day >= Cal.todayDate }
    val greeting = when { Cal.today.hour < 12 -> "Good morning"; Cal.today.hour < 17 -> "Good afternoon"; else -> "Good evening" }
    Screen(
        title = "$greeting, ${v?.ownerFirstName ?: "there"}", subtitle = "${Fmt.weekdayLong(Cal.todayDate)} · ${if (saturdayEvents.isEmpty()) "nothing" else "${saturdayEvents.size} event${if (saturdayEvents.size == 1) "" else "s"}"} this Saturday", isRoot = true,
        headerTrailing = { CircleAction(Icons.Outlined.BarChart, { nav.push(Routes.season) }, contentDescription = "Season summary") },
    ) {
        val conflict = store.seasonConflict
        if (conflict != null && v?.notifyConflicts != false) {
            val (a, b, member) = conflict
            ConflictBanner("${a.name} and ${b.name} both need ${member.firstName}'s team on ${Fmt.weekdayDayMonth(a.day)}.") { nav.pushIn(Tab.TEAM, Routes.assign(a.id)) }
        }
        Card(onClick = { nav.tab(Tab.MONEY) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(if (store.totalOutstanding == 0) "Nothing outstanding" else "Outstanding across ${store.eventsWithOpenMoney} event${if (store.eventsWithOpenMoney == 1) "" else "s"}", style = Type.caption, color = m.textMute)
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
            SectionHeading(attention?.let { "Needs attention · ${Fmt.weekdayShort(it.day)}, ${Fmt.relativeDays(it.day)}" } ?: "Next up", link = "Calendar", onLink = { nav.push(Routes.calendar) })
            if (attention != null) {
                EventCard(attention, store.clientFor(attention), store.paymentState(attention), onClick = { nav.push(Routes.event(attention.id)) }, conflicted = store.isConflicted(attention))
                val others = store.eventsOn(attention.day).filter { it.id != attention.id }
                if (others.isNotEmpty()) Text("Also ${Fmt.weekdayShort(attention.day)} · " + others.joinToString(" · ") { "${it.name} ${Fmt.time(it.start)}" }, style = Type.caption, color = m.textMute)
            } else if (store.loaded) {
                EmptyState("No events booked", "Create your first event and Mehfil starts watching the dates, the money and the crew.", Modifier.padding(vertical = Space.xl), "Create event", Icons.Outlined.Add, true) { nav.pushIn(Tab.EVENTS, Routes.create) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            StatTile("${store.eventsInMonth(Cal.todayDate.monthValue, Cal.todayDate.year).size}", "Events this month", Modifier.weight(1f)) { nav.tab(Tab.EVENTS) }
            StatTile("${store.crewOnDuty(Cal.todayDate)}", "Crew on duty today", Modifier.weight(1f)) { nav.tab(Tab.TEAM) }
            StatTile("${store.openEnquiries.size}", "Open enquiries", Modifier.weight(1f)) { nav.tab(Tab.EVENTS) }
        }
    }
}

/** A2 · Calendar month — see the shape of the month. */
@Composable
fun CalendarMonthScreen(store: AppStore, nav: Nav) {
    var month by rememberSaveableMonth { YearMonth.from(Cal.todayDate) }
    var selected by remember { mutableStateOf(store.eventsOn(Cal.nextSaturday).firstOrNull()?.day?.takeIf { YearMonth.from(it) == YearMonth.from(Cal.todayDate) } ?: Cal.todayDate) }
    val monthEvents = store.eventsInMonth(month.monthValue, month.year)
    val byDay = monthEvents.groupBy { it.day.dayOfMonth }
    val conflictDays = monthEvents.filter { store.isConflicted(it) }.map { it.day.dayOfMonth }.toSet()
    val dayEvents = store.eventsOn(selected)
    fun shift(n: Int) { month = month.plusMonths(n.toLong()); selected = month.atDay(1) }
    Screen(
        title = Fmt.monthYear(month.atDay(1)), subtitle = if (monthEvents.isEmpty()) "No events this month" else "${monthEvents.size} event${if (monthEvents.size == 1) "" else "s"} · ${Fmt.inr(monthEvents.sumOf { it.quoted })} booked", onBack = nav::back, spacing = Space.lg,
        actions = { CircleAction(Icons.Outlined.ChevronLeft, { shift(-1) }); CircleAction(Icons.Outlined.ChevronRight, { shift(1) }) },
    ) {
        Box(Modifier.pointerInput(Unit) { detectHorizontalDragGestures { _, dx -> if (dx < -30) shift(1) else if (dx > 30) shift(-1) } }) {
            CalendarGrid(month, selected, { selected = it }, byDay, conflictDays)
        }
        SectionHeading(Fmt.weekdayLong(selected), Modifier.padding(top = Space.sm),
            trailing = if (dayEvents.isEmpty()) "No events" else "${dayEvents.size} events" + if (store.conflictsOn(selected).isEmpty()) "" else " · overlap",
            link = if (dayEvents.isEmpty()) null else "Timeline", onLink = { nav.push(Routes.day(selected)) })
        if (dayEvents.isEmpty()) Text("Nothing booked on this day.", style = Type.bodyMd, color = M.textMute, modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl), textAlign = TextAlign.Center)
        else Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            dayEvents.forEach { e -> EventCard(e, store.clientFor(e), store.paymentState(e), onClick = { nav.push(Routes.event(e.id)) }, compact = true, conflicted = store.isConflicted(e)) }
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
        title = Fmt.weekdayLong(day), subtitle = "${evs.size} events · ${evs.sumOf { it.crewAssigned }} crew" + (overlap.maxOfOrNull { it.start }?.let { " · overlap from ${Fmt.time(it)}" } ?: ""), onBack = nav::back,
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
        if (evs.isEmpty()) EmptyState("Nothing on this day", "Pick another date from the calendar.", Modifier.padding(top = 120.dp))
        else TimelineStrip(evs, onTap = { nav.push(Routes.event(it.id)) })
    }
}

/** A4 · Season summary — the screen a vendor shows their spouse. */
@Composable
fun SeasonSummaryScreen(store: AppStore, nav: Nav) {
    val m = M
    val s = store.season
    val target = store.vendor?.seasonTarget ?: 0
    val pct = if (target == 0) 0 else (s.booked * 100.0 / target).roundToInt()
    Screen(title = s.label, subtitle = "October to February · ${s.count} events booked", onBack = nav::back, spacing = Space.lg) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Booked this season", style = Type.caption, color = m.textMute)
                Text(Fmt.inr(s.booked), style = Type.displayHero, color = m.text)
                if (target > 0) {
                    ProgressBar(s.booked.toFloat() / target, Modifier.padding(top = Space.xs))
                    Text("$pct% of the ${Fmt.inr(target)} target · ${Fmt.inr(maxOf(0, target - s.booked))} to go", style = Type.caption, color = m.textMute)
                } else Text("Set a season target in Profile → Business details.", style = Type.caption, color = m.textMute)
            }
        }
        Card { Column(verticalArrangement = Arrangement.spacedBy(Space.md)) { Text("Booked value by month", style = Type.caption, color = m.textMute); MonthBars(s.months) } }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            StatTile("${s.count}", "Events booked", Modifier.weight(1f))
            StatTile(Fmt.inr(if (s.count == 0) 0 else s.booked / s.count / 100 * 100), "Average ticket", Modifier.weight(1f))
        }
        val last = store.vendor?.lastSeasonBooked
        if (last != null && last > 0) {
            val delta = ((s.booked - last) * 100.0 / last).roundToInt()
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Icon(if (delta >= 0) Icons.Outlined.ArrowOutward else Icons.Outlined.SouthEast, null, Modifier.size(Dim.icon), tint = m.accentText)
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text("${if (delta >= 0) "+" else ""}$delta%", style = Type.displayMd, color = m.text)
                        Text("against last season", style = Type.bodyMd, color = m.text)
                        Text("${Fmt.inr(last)} across ${store.vendor?.lastSeasonEvents ?: 0} events by this point last year", style = Type.caption, color = m.textMute)
                    }
                }
            }
        }
    }
}

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
