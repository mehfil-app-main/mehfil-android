package com.mehfil.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.Cal
import com.mehfil.app.model.Client
import com.mehfil.app.model.Event
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.Milestone
import com.mehfil.app.model.PaymentState
import com.mehfil.app.ui.theme.CardShape
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Radius
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate
import java.time.YearMonth

// MARK: - Screen scaffold: ground · header (large or compact) · scrolling content · optional docked action bar

@Composable
fun Screen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Large title in content (collections) vs compact centred title (single statement / form) — NOTES title rule. */
    largeTitle: Boolean = true,
    /** Hero screens draw their own header; the compact title appears only once scrolled. */
    heroHeader: Boolean = false,
    isRoot: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    headerTrailing: (@Composable () -> Unit)? = null,
    docked: (@Composable ColumnScope.() -> Unit)? = null,
    spacing: Dp = Space.xl,
    bottomPadding: Dp = Space.xxl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = M
    val scroll = rememberScrollState()
    val collapsed by remember { derivedStateOf { scroll.value > 180 } }
    Box(modifier.fillMaxSize().background(m.ground)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))
            if (!isRoot) {
                // Compact chrome: back · centred title (compact screens, or large-title screens once scrolled) · actions
                Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = Dim.gutter)) {
                    if (onBack != null) CircleAction(Icons.AutoMirrored.Outlined.ArrowBack, onBack, Modifier.align(Alignment.CenterStart), contentDescription = "Back")
                    if (!largeTitle && !heroHeader || collapsed) {
                        Text(title, style = Type.headingSm, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp))
                    }
                    if (actions != null) Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) { actions() }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = Dim.gutter)
                    .padding(top = if (isRoot) Space.lg else if (largeTitle) Space.sm else Space.lg, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (largeTitle && !heroHeader) ScreenHeader(title, subtitle, headerTrailing)
                content()
            }
            if (docked != null) {
                Column(
                    Modifier.fillMaxWidth().background(m.ground).padding(horizontal = Dim.gutter).padding(top = Space.md, bottom = Space.sm)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) { docked() }
            }
        }
        // Root screens: title chip floats in once the header scrolls away
        AnimatedVisibility(visible = isRoot && collapsed, modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Space.xs), enter = fadeIn(), exit = fadeOut()) {
            Surface(shape = CircleShape, color = m.surface, shadowElevation = 2.dp, border = BorderStroke(1.dp, m.line)) {
                Text(title, style = Type.headingSm, color = m.text, modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm))
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String?, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Type.displayMd, color = M.text, modifier = Modifier.weight(1f))
            if (trailing != null) trailing()
        }
        if (subtitle != null) Text(subtitle, style = Type.bodyMd, color = M.textMute)
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null, trailingColor: Color = M.textMute, link: String? = null, onLink: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Type.caption, color = M.textMute, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Type.caption, color = trailingColor)
        if (link != null && onLink != null) Text(link, style = Type.caption, color = M.accentText, modifier = Modifier.padding(start = Space.sm).clickable(onClick = onLink))
    }
}

@Composable
fun StatTile(figure: String, label: String, modifier: Modifier = Modifier, figureColor: Color = M.text, onClick: (() -> Unit)? = null) {
    Card(modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(figure, style = Type.displayMd, color = figureColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = Type.caption, color = M.textMute)
        }
    }
}

// MARK: - 03 Event card

@Composable
fun EventCard(event: Event, client: Client?, paymentState: PaymentState, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val m = M
    val conflicted = event.hasCrewConflict
    Card(modifier, fill = if (conflicted) m.dangerTint else m.surface, onClick = onClick) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.width(Dim.edge).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(m.event(event.hue)))
            if (!compact) Column(Modifier.width(44.dp)) {
                Text(event.start.dayOfMonth.toString(), style = Type.displayMd, color = if (conflicted) m.danger else m.text)
                Text(Fmt.monthShort(event.day), style = Type.caption, color = m.textMute)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(event.name, style = Type.headingSm, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (compact) {
                    Text("${Fmt.timeRange(event.start, event.end)} · ${event.venue.shortName}", style = Type.caption, color = m.textMute, maxLines = 2)
                } else {
                    if (client != null) Text(client.name, style = Type.bodyMd, color = m.textSecondary, maxLines = 1)
                    Text("${event.venue.compact} · ${Fmt.time(event.start)}", style = Type.caption, color = m.textMute, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        StatusChip(if (conflicted) ChipKind.CONFLICT else ChipKind.from(paymentState))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Icon(Icons.Outlined.Groups, null, Modifier.size(16.dp), tint = m.textMute)
                            Text("${event.crewAssigned} crew", style = Type.caption, color = m.textMute)
                        }
                    }
                }
            }
            if (compact) StatusChip(if (conflicted) ChipKind.CONFLICT else ChipKind.from(paymentState), Modifier.align(Alignment.CenterVertically))
            Chevron(Modifier.align(Alignment.CenterVertically))
        }
    }
}

// MARK: - 04 Payment row

@Composable
fun PaymentRow(
    milestone: Milestone, subtitle: String, modifier: Modifier = Modifier, title: String? = null, subtitleColor: Color = M.textMute,
    onTrailing: (() -> Unit)? = null, trailingIcon: ImageVector = Icons.Outlined.Send, onClick: (() -> Unit)? = null,
) {
    val m = M
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 76.dp).padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title ?: milestone.name, style = Type.bodyMdStrong, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Type.caption, color = subtitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(Fmt.inr(milestone.amount), style = Type.bodyTabularStrong, color = m.text)
            StatusChip(ChipKind.from(milestone.status))
        }
        if (onTrailing != null) IconButton44(trailingIcon, onTrailing, modifier = Modifier.offset(x = Space.sm))
    }
}

// MARK: - 05 Calendar grid

@Composable
fun CalendarGrid(month: YearMonth, selected: LocalDate, onSelect: (LocalDate) -> Unit, eventsByDay: Map<Int, List<Event>>, conflictDays: Set<Int>, today: LocalDate = Cal.todayDate) {
    val m = M
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value % 7 // Sunday-first
    val cells: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(Modifier.fillMaxWidth()) { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, style = Type.caption, color = m.textMute, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
        padded.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { d ->
                    if (d == null) Spacer(Modifier.weight(1f).height(Dim.calendarCell)) else {
                        val isSel = d == selected; val isToday = d == today; val conflict = d.dayOfMonth in conflictDays
                        val evs = eventsByDay[d.dayOfMonth].orEmpty(); val past = d < today
                        Column(Modifier.weight(1f).height(Dim.calendarCell).clickable { onSelect(d) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                Modifier.size(Dim.calendarNumeral).clip(CircleShape).background(if (isSel) (if (conflict) m.danger else m.accent) else Color.Transparent)
                                    .border(1.dp, if (isToday && !isSel) m.accentText else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${d.dayOfMonth}", style = Type.bodyTabular, color = if (isSel) m.textOnPrimary else if (conflict) m.danger else if (past) m.textMute else m.text) }
                            Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                evs.take(3).forEach { Box(Modifier.size(6.dp).clip(CircleShape).background(m.event(it.hue))) }
                                if (evs.size > 3) Text("+${evs.size - 3}", style = Type.microCap, color = m.textMute)
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 06 Timeline strip (overlapping events share the row at half width)

@Composable
fun TimelineStrip(events: List<Event>, onTap: (Event) -> Unit, startHour: Int = 6, endHour: Int = 24) {
    val m = M
    val hourH = Dim.timelineHour
    val lanes = remember(events) { laneAssignments(events) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Column(Modifier.width(Dim.timelineGutter).offset(y = (-9).dp)) {
            (startHour..endHour).forEach { h -> Box(Modifier.height(hourH)) { Text(String.format("%02d:00", h % 24), style = Type.caption, color = m.textMute) } }
        }
        Box(Modifier.weight(1f).height(hourH * (endHour - startHour) + 4.dp)) {
            Column { (startHour..endHour).forEach { _ -> Box(Modifier.height(hourH)) { Box(Modifier.fillMaxWidth().height(1.dp).background(m.line)) } } }
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = maxWidth
                events.forEach { e ->
                    val (lane, count) = lanes[e.id] ?: (0 to 1)
                    val w = (width - Space.xs * (count - 1)) / count
                    val top = hourH * (Cal.hour(e.start) - startHour).toFloat()
                    val endH = if (Cal.hour(e.end) == 0.0) 24.0 else Cal.hour(e.end)
                    val h = hourH * (endH - Cal.hour(e.start)).toFloat()
                    Surface(
                        onClick = { onTap(e) }, shape = RoundedCornerShape(Radius.md), color = m.surface, shadowElevation = 1.dp, border = BorderStroke(1.dp, m.line),
                        modifier = Modifier.offset(x = (w + Space.xs) * lane, y = top).width(w).height(maxOf(h, hourH)),
                    ) {
                        Row(Modifier.padding(horizontal = Space.sm, vertical = Space.sm).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Box(Modifier.width(Dim.edge).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(m.event(e.hue)))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(e.name, style = Type.bodyMdStrong, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${Fmt.timeRange(e.start, e.end)} · ${e.venue.short} · ${e.crewAssigned} crew", style = Type.caption, color = m.textMute)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun laneAssignments(events: List<Event>): Map<String, Pair<Int, Int>> {
    val result = mutableMapOf<String, Pair<Int, Int>>()
    val groups = mutableListOf<MutableList<Event>>()
    for (e in events.sortedBy { it.start }) {
        val last = groups.lastOrNull()
        if (last != null && last.any { it.end > e.start }) last.add(e) else groups.add(mutableListOf(e))
    }
    for (g in groups) {
        val laneEnds = mutableListOf<java.time.LocalDateTime>(); val lanes = mutableMapOf<String, Int>()
        for (e in g) {
            val i = laneEnds.indexOfFirst { it <= e.start }
            if (i >= 0) { laneEnds[i] = e.end; lanes[e.id] = i } else { laneEnds.add(e.end); lanes[e.id] = laneEnds.size - 1 }
        }
        for (e in g) result[e.id] = (lanes[e.id] ?: 0) to laneEnds.size
    }
    return result
}

// MARK: - Date strip (D1 / D4)

@Composable
fun DateStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit, conflictDays: Set<LocalDate>, start: LocalDate = Cal.todayDate) {
    val m = M
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        (0..6).forEach { i ->
            val d = start.plusDays(i.toLong()); val sel = d == selected; val conflict = d in conflictDays
            Column(
                Modifier.weight(1f).height(Dim.touch).clip(RoundedCornerShape(Radius.md)).background(if (sel) (if (conflict) m.danger else m.accent) else m.surface)
                    .border(1.dp, if (sel) Color.Transparent else m.line, RoundedCornerShape(Radius.md)).clickable { onSelect(d) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(Fmt.weekdayShort(d), style = Type.microCap, color = if (sel) m.textOnPrimary else m.textMute)
                Text("${d.dayOfMonth}", style = Type.bodyTabularStrong, color = if (sel) m.textOnPrimary else if (conflict) m.danger else m.text)
            }
        }
    }
}

// MARK: - 15 Bottom sheet scaffold content (used inside ModalBottomSheet)

@Composable
fun SheetContent(title: String, subtitle: String? = null, actions: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxWidth().padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(horizontal = Space.xl).padding(top = Space.sm, bottom = Space.lg), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(title, style = Type.headingMd, color = M.text)
                if (subtitle != null) Text(subtitle, style = Type.caption, color = M.textMute)
            }
            content()
        }
        if (actions != null) Column(Modifier.padding(horizontal = Space.xl).padding(bottom = Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) { actions() }
    }
}

// MARK: - Client contact row (B2 / E2)

@Composable
fun ContactRow(client: Client, note: String, onCall: () -> Unit, onMessage: () -> Unit) {
    Card(padding = Space.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Avatar(client.avatar, client.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(client.name, style = Type.bodyMdStrong, color = M.text)
                Text(note, style = Type.caption, color = M.textMute)
            }
            CircleAction(Icons.Outlined.Phone, onCall, tint = M.accentText, contentDescription = "Call")
            CircleAction(Icons.Outlined.ChatBubbleOutline, onMessage, tint = M.accentText, contentDescription = "Message")
        }
    }
}

@Composable
fun BoxScope.FloatingAction(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = M.accent, shadowElevation = 6.dp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = Dim.gutter, bottom = Space.lg)) {
        Row(Modifier.padding(horizontal = Space.lg, vertical = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(icon, null, Modifier.size(18.dp), tint = M.textOnPrimary)
            Text(title, style = Type.buttonMd, color = M.textOnPrimary)
        }
    }
}
