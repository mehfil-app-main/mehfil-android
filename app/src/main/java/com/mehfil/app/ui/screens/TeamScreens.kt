package com.mehfil.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.*
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.Routes
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.CardShape
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate

/** D1 · Crew list — grouped by role, availability for the selected date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrewListScreen(store: AppStore, nav: Nav, pushed: Boolean) {
    val m = M
    var day by rememberSaveableDate { Cal.todayDate }
    val dayIds = store.eventsOn(day).map { it.id }
    fun booked(c: CrewMember) = c.bookings.filter { it in dayIds }.mapNotNull { store.event(it) }
    val conflictDays = store.events.filter { store.isConflicted(it) }.map { it.day }.toSet()
    var menu by remember { mutableStateOf(false) }
    var addSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CrewMember?>(null) }
    val menuBox: @Composable () -> Unit = {
        Box {
            CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Add crew member") }, { menu = false; addSheet = true })
                DropdownMenuItem({ Text("Attendance") }, { menu = false; (store.eventsOn(day).firstOrNull() ?: store.upcomingEvents.firstOrNull())?.let { nav.push(Routes.attendance(it.id)) } })
                DropdownMenuItem({ Text("Inventory") }, { menu = false; nav.push(Routes.inventory) })
            }
        }
    }
    Screen(
        title = "Team", subtitle = if (store.crew.isEmpty()) "No crew yet" else "${store.crew.size} crew · ${store.crewOnDuty(Cal.todayDate)} on duty today", isRoot = !pushed, spacing = Space.lg,
        onBack = if (pushed) nav::back else null, actions = if (pushed) ({ menuBox() }) else null, headerTrailing = if (pushed) null else menuBox,
    ) {
        DateStrip(day, { day = it }, conflictDays)
        if (store.crew.isEmpty()) EmptyState("No crew added", "Add your coordinators, decorators and helpers with their day rates.", Modifier.padding(top = 120.dp), "Add crew member", Icons.Outlined.Add, true) { addSheet = true }
        else CrewRole.entries.forEach { role ->
            val members = store.crew.filter { it.role == role }
            if (members.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                SectionHeading(role.plural, trailing = "${Fmt.weekdayDayMonth(day)} · ${members.count { booked(it).isNotEmpty() }} of ${members.size} booked")
                RowGroup {
                    members.forEachIndexed { i, c ->
                        if (i > 0) Hairline(68.dp)
                        val b = booked(c)
                        Box(Modifier.combinedClickable(onClick = { (b.firstOrNull() ?: store.eventsOn(day).firstOrNull())?.let { nav.push(Routes.assign(it.id)) } ?: run { editing = c } }, onLongClick = { editing = c })) {
                            ListRow(c.name, subtitle = "${if (c.isLead) "Lead ${role.label.lowercase()}" else role.label} · ${Fmt.inr(c.dayRate)}/day", leading = { Avatar(c.avatar, c.name) }) {
                                Text(if (b.isEmpty()) "Available" else b.joinToString(" · ") { it.surname }, style = Type.caption, color = if (b.size > 1) m.danger else if (b.isEmpty()) m.accentText else m.textMute); Chevron()
                            }
                        }
                    }
                }
            }
        }
    }
    if (addSheet) CrewMemberSheet(store, null) { addSheet = false }
    editing?.let { c -> CrewMemberSheet(store, c) { editing = null } }
}

/** D2 · Assign crew — booked members are listed, not disabled; tapping opens the comparison. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignCrewScreen(store: AppStore, nav: Nav, eventId: String) {
    val m = M
    val e = store.event(eventId) ?: return
    val others = store.eventsOn(e.day).filter { it.id != e.id }
    val otherIds = others.map { it.id }.toSet()
    val available = store.crew.filter { c -> c.bookings.none { it in otherIds } }
    val booked = store.crew.filter { c -> c.bookings.any { it in otherIds } }
    val assigned = store.assignedCrew(e.id)
    val plan = e.crewPlan ?: CrewRole.entries.joinToString(", ") { r -> "${assigned.count { it.role == r }} ${r.plural.lowercase()}" }
    val short = maxOf(0, e.crewNeeded - e.crewAssigned)
    val clash = store.crew.firstOrNull { c -> e.id in c.bookings && c.bookings.any { it in otherIds } }
    val clashEvent = clash?.let { c -> others.firstOrNull { it.id in c.bookings } }
    var compare by remember { mutableStateOf<CrewMember?>(null) }
    var addSheet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Screen(
        title = "Assign crew", subtitle = "${e.name} · ${Fmt.weekdayLong(e.day)}", onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Add crew member") }, { menu = false; addSheet = true })
                    DropdownMenuItem({ Text("Attendance") }, { menu = false; nav.push(Routes.attendance(e.id)) })
                    DropdownMenuItem({ Text("Open event") }, { menu = false; nav.push(Routes.event(e.id)) })
                }
            }
        },
    ) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Assigned for ${Fmt.weekdayDayMonth(e.day)}", style = Type.caption, color = m.textMute)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("${e.crewAssigned}", style = Type.displayHero, color = m.text); Text("of ${e.crewNeeded} needed · $plan", style = Type.caption, color = m.textMute, modifier = Modifier.weight(1f).padding(bottom = 6.dp))
                }
                ProgressBar(e.crewAssigned.toFloat() / maxOf(1, e.crewNeeded))
                Text((if (short > 0) "$short short" else "Fully staffed") + (clash?.let { " · ${it.firstName}'s team also on ${clashEvent?.surname ?: "another event"}" } ?: ""), style = Type.caption, color = if (short > 0 || clash != null) m.danger else m.textMute)
            }
        }
        if (store.crew.isEmpty()) EmptyState("No crew on the roster", "Add your team first, then assign them here.", actionTitle = "Add crew member", actionIcon = Icons.Outlined.Add, primary = true) { addSheet = true }
        else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                SectionHeading("Available on ${Fmt.weekdayDayMonth(e.day)}", trailing = "${available.size}")
                RowGroup {
                    available.forEachIndexed { i, c ->
                        if (i > 0) Hairline(68.dp)
                        val on = e.id in c.bookings
                        ListRow(c.name, subtitle = "${c.role.label} · ${if (on) "assigned" else c.note ?: "free"}", selected = on, leading = { Avatar(c.avatar, c.name) }) { AssignToggle(on) { store.toggleAssignment(c.id, e.id) } }
                    }
                }
            }
            if (booked.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                SectionHeading("Booked that day", trailing = "tap to compare")
                RowGroup {
                    booked.forEachIndexed { i, c ->
                        if (i > 0) Hairline(68.dp)
                        val on = e.id in c.bookings
                        val other = others.firstOrNull { it.id in c.bookings }
                        ListRow(c.name, subtitle = (other?.let { "${it.name} ${Fmt.timeRange(it.start, it.end)}" } ?: "") + (c.note?.let { " · $it" } ?: ""), subtitleColor = if (on) m.danger else m.textMute, selected = on, onClick = { compare = c }, leading = { Avatar(c.avatar, c.name) }) { AssignToggle(on) { compare = c } }
                    }
                }
            }
        }
    }
    if (addSheet) CrewMemberSheet(store, null) { addSheet = false }
    compare?.let { c -> val other = others.firstOrNull { it.id in c.bookings }; if (other != null) ModalBottomSheet(onDismissRequest = { compare = null }, containerColor = m.surface) { CrewComparisonSheet(store, c, e, other) { compare = null } } }
}

@Composable
private fun AssignToggle(on: Boolean, onClick: () -> Unit) {
    val m = M
    Box(Modifier.size(Dim.touch).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) m.accent else Color.Transparent).border(1.dp, if (on) Color.Transparent else m.accentText, CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (on) Icons.Outlined.Check else Icons.Outlined.Add, null, Modifier.size(16.dp), tint = if (on) m.textOnPrimary else m.accentText)
        }
    }
}

/** D2 comparison sheet — both events with the consequence of choosing each. */
@Composable
fun CrewComparisonSheet(store: AppStore, member: CrewMember, target: Event, other: Event, onDone: () -> Unit) {
    val m = M
    var choice by remember { mutableStateOf(target.id) }
    fun agency(e: Event) = maxOf(1, e.crewNeeded / 2) * CrewRole.DECORATOR.defaultDayRate * 2 * 2
    @Composable fun option(e: Event, consequence: String) {
        val sel = choice == e.id
        Row(Modifier.fillMaxWidth().clip(CardShape).background(if (sel) m.surfaceSelected else m.surface).border(1.dp, if (sel) m.accent else m.line, CardShape).clickable { choice = e.id }.padding(Space.lg).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.width(Dim.edge).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(m.event(e.hue)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(e.name, style = Type.headingSm, color = m.text)
                Text("${Fmt.timeRange(e.start, e.end)} · ${e.guests} guests · ${Fmt.inr(e.quoted)}", style = Type.caption, color = m.textMute)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) { Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(14.dp), tint = m.danger); Text(consequence, style = Type.caption, color = m.danger) }
            }
            Icon(if (sel) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(22.dp), tint = if (sel) m.accentText else m.line)
        }
    }
    val chosen = if (choice == other.id) other else target; val dropped = if (choice == other.id) target else other
    SheetContent("${member.firstName}'s team is booked twice", actions = {
        MButton("Move the team to ${chosen.surname}", { store.moveTeam(member.id, chosen.id, dropped.id); onDone() })
        MButton("Keep on ${other.surname}", onDone, kind = ButtonKind.TERTIARY, compact = true)
    }) {
        option(target, "${other.surname} then needs agency crew · ${Fmt.inr(agency(other))}")
        option(other, "${target.surname} then needs agency crew · ${Fmt.inr(agency(target))}")
    }
}

/** D3 · Attendance — three-state control with single-tap cycling. */
@Composable
fun AttendanceScreen(store: AppStore, nav: Nav, initialEventId: String) {
    val m = M
    val e0 = store.event(initialEventId) ?: return
    val todays = store.eventsOn(e0.day)
    var eventId by rememberSaveable { mutableStateOf(initialEventId) }
    val e = store.event(eventId) ?: e0
    val members = store.assignedCrew(e.id); val states = store.attendanceFor(e.id)
    val arrived = members.count { states[it.id] == AttendanceState.ARRIVED }
    val confirmed = members.count { (states[it.id] ?: AttendanceState.CONFIRMED) == AttendanceState.CONFIRMED }
    val absent = members.count { states[it.id] == AttendanceState.ABSENT }
    val haptic = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Screen(
        title = "Attendance", subtitle = "${Fmt.weekdayLong(e0.day)} · ${todays.size} event${if (todays.size == 1) "" else "s"}", onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Open runsheet") }, { menu = false; nav.push(Routes.runsheet(e.id)) })
                    DropdownMenuItem({ Text("Assign crew") }, { menu = false; nav.push(Routes.assign(e.id)) })
                }
            }
        },
    ) {
        if (todays.size > 1) SegmentedControl(todays, e, { it.surname }, { eventId = it.id })
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Text("$arrived of ${members.size}", style = Type.displayMd, color = m.text)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("arrived at ${e.venue.name}", style = Type.bodyMd, color = m.text); Text("$confirmed confirmed on the way" + if (absent > 0) " · $absent absent" else "", style = Type.caption, color = m.textMute) }
            }
        }
        if (members.isEmpty()) EmptyState("Nobody assigned yet", "Assign crew to ${e.name} and mark them here on the morning.", actionTitle = "Assign crew") { nav.push(Routes.assign(e.id)) }
        else RowGroup {
            members.forEachIndexed { i, c ->
                if (i > 0) Hairline(68.dp)
                val s = states[c.id] ?: AttendanceState.CONFIRMED
                ListRow(c.name, subtitle = c.role.label + if (c.isLead) " · lead" else "", leading = { Avatar(c.avatar, c.name) }) { TriState(s) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); store.cycleAttendance(e.id, c.id) } }
            }
        }
    }
}

/** D4 · Inventory — commitment bars per item; over-committed items sorted to the top. */
@Composable
fun InventoryScreen(store: AppStore, nav: Nav) {
    val m = M
    var day by rememberSaveableDate { Cal.todayDate }
    var addSheet by remember { mutableStateOf(false) }
    val conflictDays = store.inventory.flatMap { item -> item.holds.map { it.date }.filter { item.isOver(it) } }.toSet()
    val items = store.inventory.sortedWith(compareByDescending<InventoryItem> { it.isOver(day) }.thenByDescending { it.committed(day).toFloat() / maxOf(1, it.total) })
    val over = items.count { it.isOver(day) }
    var menu by remember { mutableStateOf(false) }
    Screen(
        title = "Inventory", subtitle = if (store.inventory.isEmpty()) "Nothing tracked yet" else "${Fmt.weekdayLong(day)} · ${if (over == 0) "nothing over" else "$over item${if (over == 1) "" else "s"} over"}", onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Add item") }, { menu = false; addSheet = true })
                    DropdownMenuItem({ Text("Crew for this date") }, { menu = false; nav.push(Routes.crew) })
                }
            }
        },
    ) {
        DateStrip(day, { day = it }, conflictDays)
        if (store.inventory.isEmpty()) EmptyState("No inventory tracked", "Add chairs, drapes, rigs and tables to see what is committed on any date.", Modifier.padding(top = 120.dp), "Add item", Icons.Outlined.Add, true) { addSheet = true }
        else RowGroup {
            items.forEachIndexed { i, item ->
                if (i > 0) Hairline()
                val c = item.committed(day); val isOver = c > item.total
                Column(Modifier.fillMaxWidth().clickable { nav.push(Routes.item(item.id)) }.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = Type.bodyMdStrong, color = m.text, modifier = Modifier.weight(1f))
                        Text(if (isOver) "$c of ${item.total} · ${c - item.total} over" else "$c of ${item.total}", style = Type.bodyTabularStrong, color = if (isOver) m.danger else m.textMute); Chevron()
                    }
                    CommitmentBar(c, item.total)
                    val holds = item.holds.filter { it.date == day }
                    Text(if (holds.isEmpty()) "Free on this date" else holds.joinToString(" · ") { "${store.event(it.eventId)?.surname ?: ""} ${it.qty}" }, style = Type.caption, color = m.textMute)
                }
            }
        }
    }
    if (addSheet) InventoryItemSheet(store, null) { addSheet = false }
}

/** D5 · Item detail — 30-day commitment strip and the events holding the item. */
@Composable
fun ItemDetailScreen(store: AppStore, nav: Nav, itemId: String) {
    val m = M
    val item = store.item(itemId) ?: return
    val days = (0..30).map { Cal.todayDate.plusDays(it.toLong()) }
    val values = days.map { item.committed(it) }
    val peakI = values.indices.maxByOrNull { values[it] } ?: 0
    val holds = item.holds.filter { store.event(it.eventId)?.status == EventStatus.UPCOMING }.sortedBy { it.date }
    var edit by remember { mutableStateOf(false) }
    Screen(title = item.name, subtitle = "${item.total} in stock" + if (item.detail.isEmpty()) "" else " · ${item.detail}", onBack = nav::back, spacing = Space.lg,
        actions = { CircleAction(Icons.Outlined.Edit, { edit = true }) }) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Row {
                    Text("Committed · next 30 days", style = Type.caption, color = m.textMute, modifier = Modifier.weight(1f))
                    if (values[peakI] > 0) Text("peak ${values[peakI]} on ${Fmt.weekdayDayMonth(days[peakI])}", style = Type.caption, color = if (values[peakI] > item.total) m.danger else m.textMute)
                }
                val maxV = maxOf(item.total, values.max())
                Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    values.forEach { v -> Box(Modifier.weight(1f).height(if (v == 0) 2.dp else maxOf(4.dp, 72.dp * v / maxV)).clip(RoundedCornerShape(1.5.dp)).background(if (v == 0) m.line else if (v > item.total) m.danger else m.accent)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf(0, 9, 19, 30).forEach { Text(Fmt.dayMonth(days[it]), style = Type.caption, color = m.textMute) } }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) { Legend(m.accent, "Committed"); Legend(m.danger, "Over ${item.total}") }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            SectionHeading("Events holding ${item.name.lowercase()}", trailing = if (holds.isEmpty()) "none" else "${holds.size} · ${holds.sumOf { it.qty }} ${item.unit}")
            if (holds.isEmpty()) Text("Set holds from an event's Inventory row.", style = Type.caption, color = m.textMute)
            else RowGroup {
                holds.forEachIndexed { i, h ->
                    if (i > 0) Hairline(68.dp)
                    store.event(h.eventId)?.let { e -> ListRow(e.name, subtitle = "${Fmt.weekdayDayMonth(h.date)} · ${e.venue.name}", onClick = { nav.push(Routes.event(e.id)) }, leading = { IconTile(Icons.Outlined.CalendarToday) }) { Text("${h.qty}", style = Type.bodyTabularStrong, color = m.text); Chevron() } }
                }
            }
        }
    }
    if (edit) InventoryItemSheet(store, item) { edit = false }
}

@Composable
private fun Legend(c: Color, t: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) { Box(Modifier.size(8.dp).clip(CircleShape).background(c)); Text(t, style = Type.caption, color = M.textMute) }
}
