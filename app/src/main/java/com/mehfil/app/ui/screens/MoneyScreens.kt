package com.mehfil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.*
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.Routes
import com.mehfil.app.ui.Tab
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** C1 · Receivables — Overdue always renders first regardless of chronology. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivablesScreen(store: AppStore, nav: Nav) {
    val m = M
    val open = store.openMilestones
    var reminder by remember { mutableStateOf<Milestone?>(null) }
    Screen(title = "Receivables", subtitle = if (open.isEmpty()) "All paid up" else "${store.eventsWithOpenMoney} events · ${open.size} open milestones", isRoot = true, spacing = Space.lg) {
        if (open.isEmpty()) {
            val none = store.upcomingEvents.isEmpty()
            EmptyState(if (none) "Nothing to collect yet" else "Nothing outstanding", if (none) "Milestones appear here as soon as you book an event." else "Every milestone across your ${store.upcomingEvents.size} events is paid up.",
                Modifier.padding(top = 160.dp), if (none) "Create event" else "View payment schedules", if (none) Icons.Outlined.Add else null, none) {
                store.upcomingEvents.firstOrNull()?.let { nav.push(Routes.schedule(it.id)) } ?: nav.pushIn(Tab.EVENTS, Routes.create)
            }
        } else {
            Column(Modifier.padding(bottom = Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text("Total outstanding across ${store.eventsWithOpenMoney} event${if (store.eventsWithOpenMoney == 1) "" else "s"}", style = Type.caption, color = m.textMute)
                Text(Fmt.inr(store.totalOutstanding), style = Type.displayHero, color = m.text)
                if (store.overdueTotal > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(14.dp), tint = m.danger); Text("${Fmt.inr(store.overdueTotal)} overdue · ${store.overdueSurnames}", style = Type.caption, color = m.danger)
                }
            }
            MilestoneGroup(store, nav, "Overdue", store.overdue, action = { reminder = it })
            MilestoneGroup(store, nav, "Due this week", store.dueThisWeek)
            MilestoneGroup(store, nav, "Upcoming", store.upcomingMilestones)
        }
    }
    reminder?.let { ms -> ModalBottomSheet(onDismissRequest = { reminder = null }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { SendReminderSheet(store, ms) { reminder = null } } }
}

@Composable
private fun MilestoneGroup(store: AppStore, nav: Nav, title: String, list: List<Milestone>, action: ((Milestone) -> Unit)? = null) {
    if (list.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        SectionHeading(title, trailing = "${Fmt.inr(list.sumOf { it.amount })} · ${list.size}")
        RowGroup {
            list.forEachIndexed { i, ms ->
                if (i > 0) Hairline()
                PaymentRow(ms, subtitle = if (ms.effectiveStatus == MilestoneStatus.OVERDUE) "${ms.kind.label} · ${Fmt.daysLate(ms.due)} days late" else "${ms.kind.label} · ${Fmt.weekdayDayMonth(ms.due)}",
                    title = store.event(ms.eventId)?.name, subtitleColor = if (ms.effectiveStatus == MilestoneStatus.OVERDUE) M.danger else M.textMute,
                    onTrailing = action?.let { a -> { a(ms) } }, onClick = { nav.push(Routes.schedule(ms.eventId)) })
            }
        }
    }
}

/** C2 · Event payment schedule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScheduleScreen(store: AppStore, nav: Nav, eventId: String) {
    val m = M
    val e = store.event(eventId) ?: return
    val ms = store.milestonesFor(eventId)
    val collected = store.collected(eventId); val overdue = store.overdueAmount(eventId)
    val pct = if (e.quoted == 0) 0 else (collected * 100.0 / e.quoted).roundToInt()
    var record by remember { mutableStateOf<Milestone?>(null) }
    var add by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }; var newAmount by remember { mutableStateOf(0) }; var newDue by remember { mutableStateOf(e.day) }; var showDue by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Screen(
        title = "Payment schedule", subtitle = "${e.name} · ${Fmt.weekdayLong(e.day)}", onBack = nav::back, spacing = Space.lg,
        actions = {
            Box {
                CircleAction(Icons.Outlined.MoreHoriz, { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Expenses and margin") }, { menu = false; nav.push(Routes.expenses(eventId)) })
                    DropdownMenuItem({ Text("Open event") }, { menu = false; nav.push(Routes.event(eventId)) })
                }
            }
        },
        docked = { MButton("Record payment", { record = ms.firstOrNull { it.isOpen } }, icon = Icons.Outlined.CurrencyRupee, enabled = ms.any { it.isOpen }) },
    ) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Collected", style = Type.caption, color = m.textMute)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(Fmt.inr(collected), style = Type.displayMd, color = m.text); Text("of ${Fmt.inr(e.quoted)} · $pct%", style = Type.caption, color = m.textMute, modifier = Modifier.padding(bottom = 4.dp))
                }
                ProgressBar(collected.toFloat() / maxOf(1, e.quoted))
                Text("${Fmt.inr(maxOf(0, e.quoted - collected))} to collect" + if (overdue > 0) " · ${Fmt.inr(overdue)} overdue" else "", style = Type.caption, color = m.textMute)
            }
        }
        RowGroup {
            ms.forEachIndexed { i, x -> if (i > 0) Hairline(); PaymentRow(x, paymentSubtitle(x), subtitleColor = if (x.effectiveStatus == MilestoneStatus.OVERDUE) m.danger else m.textMute, onClick = { if (x.isOpen) record = x }) }
            if (ms.isNotEmpty()) Hairline()
            Row(Modifier.fillMaxWidth().height(Dim.row).clickable { add = true }.padding(horizontal = Space.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = m.accentText); Text("Add milestone", style = Type.buttonSm, color = m.accentText)
            }
        }
    }
    record?.let { x -> ModalBottomSheet(onDismissRequest = { record = null }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { RecordPaymentSheet(store, x) { record = null } } }
    if (add) ModalBottomSheet(onDismissRequest = { add = false }, containerColor = m.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetContent("Add milestone", e.name, actions = { MButton("Add milestone", { store.addMilestone(eventId, newName.trim(), newAmount, newDue); newName = ""; newAmount = 0; add = false }, enabled = newName.isNotBlank() && newAmount > 0) }) {
            InputField("Name", newName, { newName = it }, placeholder = "Third instalment")
            PickerField("Due", Fmt.weekdayLongYear(newDue), { showDue = true }, trailing = Icons.Outlined.CalendarToday)
            AmountInput(newAmount, label = "Amount"); NumericKeypad(newAmount, { newAmount = it })
        }
    }
    if (showDue) DateSheet("Due date", null, newDue, { newDue = it }, { showDue = false })
}

/** C3 · Record payment — completable in under 20 seconds while the money changes hands. */
@Composable
fun RecordPaymentSheet(store: AppStore, milestone: Milestone, onDone: () -> Unit) {
    val m = M
    var amount by remember { mutableStateOf(milestone.amount) }
    var method by remember { mutableStateOf(PaymentMethod.UPI) }
    var date by remember { mutableStateOf(Cal.todayDate) }
    var reference by remember { mutableStateOf("") }
    var showReference by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    val e = store.event(milestone.eventId)
    SheetContent("Record payment", "${e?.name ?: ""} · ${Fmt.inr(store.balanceDue(milestone.eventId))} balance due", actions = {
        MButton(if (amount > 0) "Record ${Fmt.inr(amount)}" else "Record payment", { store.recordPayment(milestone.id, amount, method, date, reference.ifBlank { null }); onDone() }, enabled = amount in 1..milestone.amount)
    }) {
        AmountInput(amount, error = if (amount > milestone.amount) "More than the ${Fmt.inr(milestone.amount)} due on this milestone" else null)
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            PaymentMethod.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    row.forEach { pm ->
                        val on = method == pm
                        Box(Modifier.weight(1f).height(Dim.buttonCompact).clip(CircleShape).background(if (on) m.accentSoftBg else Color.Transparent).border(1.dp, if (on) Color.Transparent else m.accentText, CircleShape).clickable { method = pm }, contentAlignment = Alignment.Center) {
                            Text(pm.label, style = Type.buttonSm, color = if (on) m.accentSoftText else m.accentText)
                        }
                    }
                }
            }
        }
        RowGroup { ListRow(if (date == Cal.todayDate) "Today, ${Fmt.weekdayDayMonth(date)}" else Fmt.weekdayLong(date), subtitle = "Date received", onClick = { showDate = true }, leading = { IconTile(Icons.Outlined.CalendarToday) }) { Chevron() } }
        if (showReference) InputField("Reference", reference, { reference = it }, placeholder = "UTR or cheque number")
        else Row(Modifier.clickable { showReference = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = m.accentText); Text("Add reference", style = Type.buttonSm, color = m.accentText)
        }
        NumericKeypad(amount, { amount = it })
    }
    if (showDate) DateSheet("Date received", null, date, { date = it }, { showDate = false }, maxDate = Cal.todayDate)
}

/** C4 · Send reminder — tone selector with real composed copy, channel picker, history. */
@Composable
fun SendReminderSheet(store: AppStore, milestone: Milestone, onDone: () -> Unit) {
    val m = M
    var tone by remember { mutableStateOf(ReminderTone.STANDARD) }
    var channel by remember { mutableStateOf(ReminderChannel.WHATSAPP) }
    var message by remember { mutableStateOf(store.reminderMessage(milestone, tone)) }
    var edited by remember { mutableStateOf(false) }
    val e = store.event(milestone.eventId); val client = e?.let { store.clientFor(it) }
    val ctx = LocalContext.current
    LaunchedEffect(tone) { edited = false; message = store.reminderMessage(milestone, tone) }
    SheetContent("Send reminder", "${milestone.name} · ${Fmt.inr(milestone.amount)} · ${Fmt.daysLate(milestone.due)} days late", actions = {
        MButton("Send on ${channel.label}", {
            store.sendReminder(milestone.id, tone, channel)
            if (client != null) { if (channel == ReminderChannel.WHATSAPP) whatsapp(ctx, client.phone, message) else sms(ctx, client.phone, message) }
            onDone()
        }, icon = Icons.Outlined.Send)
    }) {
        if (client != null && e != null) Card(padding = Space.md) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Avatar(client.avatar, client.name)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(client.name, style = Type.bodyMdStrong, color = m.text); Text("${e.name} · ${Fmt.weekdayDayMonth(e.day)} · ${client.phone.ifEmpty { "no phone" }}", style = Type.caption, color = m.textMute) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text("Tone", style = Type.caption, color = m.textMute); SegmentedControl(ReminderTone.entries, tone, { it.label }, { tone = it }) }
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            InputField("Message", message, { message = it; edited = true }, singleLine = false, minHeight = 120.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = m.textMute); Text("${if (edited) "Edited" else "Tap to edit"} · ${message.length} characters", style = Type.caption, color = m.textMute)
            }
        }
        val history = store.remindersFor(milestone.id)
        if (history.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text("Sent before", style = Type.caption, color = m.textMute)
            RowGroup {
                history.forEachIndexed { i, r ->
                    if (i > 0) Hairline()
                    ListRow("${r.tone.label} · ${Fmt.weekdayDayMonthTime(r.sentAt)}", subtitle = "${r.channel.label} · ${r.outcome}", leading = { IconTile(if (r.channel == ReminderChannel.WHATSAPP) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.Send) })
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text("Send via", style = Type.caption, color = m.textMute); SegmentedControl(ReminderChannel.entries, channel, { it.label }, { channel = it }) }
    }
}

/** C5 · Event expenses — margin as a large figure; loss state gets the danger treatment. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(store: AppStore, nav: Nav, eventId: String) {
    val m = M
    val e = store.event(eventId) ?: return
    val expenses = store.expensesFor(eventId)
    val total = expenses.sumOf { it.amount }; val margin = e.quoted - total; val loss = margin < 0
    val pct = if (e.quoted == 0) 0.0 else margin * 100.0 / e.quoted
    var add by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    Screen(title = "Expenses", subtitle = "${e.name} · ${Fmt.weekdayLong(e.day)}", onBack = nav::back, spacing = Space.lg,
        docked = { MButton("Add expense", { add = true }, kind = ButtonKind.SECONDARY, icon = Icons.Outlined.Add) }) {
        if (expenses.isEmpty()) EmptyState("No expenses logged", "Add flowers, crew wages, hire and transport as they come in to see the real margin.", Modifier.padding(vertical = Space.xl), "Add expense", Icons.Outlined.Add, true) { add = true }
        else RowGroup {
            expenses.forEachIndexed { i, x ->
                if (i > 0) Hairline()
                Box(Modifier.combinedClickable(onClick = {}, onLongClick = { deleting = x })) { ListRow(x.name, subtitle = x.detail.ifEmpty { null }) { Text(Fmt.inr(x.amount), style = Type.bodyTabularStrong, color = m.text) } }
            }
            Hairline()
            Box(Modifier.background(m.surfacePressed)) { ListRow("Total cost", subtitle = "${expenses.size} entr${if (expenses.size == 1) "y" else "ies"}") { Text(Fmt.inr(total), style = Type.bodyTabularStrong, color = m.text) } }
        }
        Card(fill = if (loss) m.dangerTint else m.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                TotalRow("Quoted", e.quoted); TotalRow("Total cost", total)
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) { Text("Margin", style = Type.caption, color = m.textMute); Text(Fmt.inr(margin), style = Type.displayHero, color = if (loss) m.danger else m.text) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    if (loss) StatusChip(ChipKind.LOSS)
                    Text(if (loss) String.format(Locale.UK, "−%.1f%% · costs exceed the quote", abs(pct)) else String.format(Locale.UK, "%.1f%% margin on the quote", pct), style = Type.caption, color = m.textMute, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    if (add) ExpenseSheet(store, eventId) { add = false }
    deleting?.let { x -> EditSheet({ deleting = null }) { SheetContent("Delete ${x.name}?", Fmt.inr(x.amount), actions = { MButton("Delete", { store.deleteExpense(x.id); deleting = null }, kind = ButtonKind.DESTRUCTIVE); MButton("Keep", { deleting = null }, kind = ButtonKind.TERTIARY, compact = true) }) {} } }
}
