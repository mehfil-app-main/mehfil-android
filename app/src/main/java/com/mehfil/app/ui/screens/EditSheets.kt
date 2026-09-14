package com.mehfil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.*
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type

// Sheets that create or edit records. Each saves through the store and calls onDismiss.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = M.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { content() }
}

@Composable
fun CrewMemberSheet(store: AppStore, existing: CrewMember?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: CrewRole.HELPER) }
    var isLead by remember { mutableStateOf(existing?.isLead ?: false) }
    var rate by remember { mutableStateOf(existing?.dayRate?.toString() ?: "") }
    EditSheet(onDismiss) {
        SheetContent(if (existing == null) "Add crew member" else "Edit crew member", existing?.let { "${it.bookings.size} bookings" }, actions = {
            MButton(if (existing == null) "Add to roster" else "Save", {
                store.saveCrewMember(CrewMember(existing?.id ?: Cal.newId(), name.trim(), role, isLead, rate.toIntOrNull() ?: role.defaultDayRate, existing?.avatar, existing?.bookings ?: emptyList(), existing?.note)); onDismiss()
            }, enabled = name.isNotBlank())
            if (existing != null) MButton("Remove from roster", { store.deleteCrewMember(existing.id); onDismiss() }, kind = ButtonKind.DESTRUCTIVE, compact = true)
        }) {
            InputField("Name", name, { name = it }, placeholder = "Suresh Yadav")
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text("Role", style = Type.caption, color = M.textMute); SegmentedControl(CrewRole.entries, role, { it.label }, { role = it }) }
            InputField("Day rate", rate, { rate = it.filter(Char::isDigit) }, placeholder = "${role.defaultDayRate}", keyboard = KeyboardType.Number)
            ToggleRow("Team lead", null, isLead) { isLead = it }
        }
    }
}

@Composable
fun InventoryItemSheet(store: AppStore, existing: InventoryItem?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var total by remember { mutableStateOf(existing?.total?.toString() ?: "") }
    var detail by remember { mutableStateOf(existing?.detail ?: "") }
    EditSheet(onDismiss) {
        SheetContent(if (existing == null) "Add item" else "Edit stock", existing?.let { "${it.holds.size} holds" }, actions = {
            MButton(if (existing == null) "Add item" else "Save", { store.saveItem(InventoryItem(existing?.id ?: Cal.newId(), name.trim(), total.toIntOrNull() ?: 0, detail, existing?.holds ?: emptyList())); onDismiss() },
                enabled = name.isNotBlank() && (total.toIntOrNull() ?: 0) > 0)
            if (existing != null) MButton("Remove item", { store.deleteItem(existing.id); onDismiss() }, kind = ButtonKind.DESTRUCTIVE, compact = true)
        }) {
            InputField("Item", name, { name = it }, placeholder = "Chiavari chairs")
            InputField("Total in stock", total, { total = it.filter(Char::isDigit) }, placeholder = "600", keyboard = KeyboardType.Number)
            InputField("Detail", detail, { detail = it }, placeholder = "Gold · with cushions")
        }
    }
}

/** Hold quantities of every item for one event (B2 → Inventory). */
@Composable
fun EventHoldsSheet(store: AppStore, event: Event, onDismiss: () -> Unit) {
    val qty = remember { mutableStateOf(store.inventory.associate { i -> i.id to (i.holds.firstOrNull { it.eventId == event.id }?.qty ?: 0) }) }
    EditSheet(onDismiss) {
        SheetContent("Inventory for ${event.name}", "${Fmt.weekdayDayMonth(event.day)} · what this event holds", actions = {
            MButton("Save holds", { store.inventory.forEach { store.setHold(it.id, event.id, qty.value[it.id] ?: 0) }; onDismiss() })
        }) {
            if (store.inventory.isEmpty()) EmptyState("No inventory yet", "Add items from Team → Inventory first.")
            else RowGroup {
                store.inventory.forEachIndexed { i, item ->
                    if (i > 0) Hairline()
                    val held = qty.value[item.id] ?: 0
                    val others = item.committed(event.day) - (item.holds.firstOrNull { it.eventId == event.id }?.qty ?: 0)
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.name, style = Type.bodyMdStrong, color = M.text)
                            Text("${others + held} of ${item.total} on this date", style = Type.caption, color = if (others + held > item.total) M.danger else M.textMute)
                        }
                        MStepper(held, { qty.value = qty.value + (item.id to it) }, range = 0..(item.total * 2), step = if (item.total >= 100) 10 else 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseSheet(store: AppStore, eventId: String, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }; var detail by remember { mutableStateOf("") }; var amount by remember { mutableStateOf(0) }
    EditSheet(onDismiss) {
        SheetContent("Add expense", store.event(eventId)?.name, actions = {
            MButton(if (amount > 0) "Add ${Fmt.inr(amount)}" else "Add expense", { store.addExpense(eventId, name.trim(), detail, amount); onDismiss() }, enabled = name.isNotBlank() && amount > 0)
        }) {
            InputField("What", name, { name = it }, placeholder = "Flowers")
            InputField("Detail", detail, { detail = it }, placeholder = "Marigold, roses · Ghazipur mandi")
            AmountInput(amount, label = "Amount")
            NumericKeypad(amount, { amount = it })
        }
    }
}

@Composable
fun PackageSheet(store: AppStore, existing: Package?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }; var detail by remember { mutableStateOf(existing?.detail ?: "") }; var price by remember { mutableStateOf(existing?.price ?: 0) }
    EditSheet(onDismiss) {
        SheetContent(if (existing == null) "New package" else "Edit package", actions = {
            MButton("Save", { store.savePackage(Package(existing?.id ?: Cal.newId(), name.trim(), detail, price)); onDismiss() }, enabled = name.isNotBlank() && price > 0)
            if (existing != null) MButton("Delete package", { store.deletePackage(existing.id); onDismiss() }, kind = ButtonKind.DESTRUCTIVE, compact = true)
        }) {
            InputField("Name", name, { name = it }, placeholder = "Stage & backdrop")
            InputField("What's included", detail, { detail = it }, placeholder = "12 m truss, 40 par cans")
            AmountInput(price, label = "Price")
            NumericKeypad(price, { price = it })
        }
    }
}

@Composable
fun BusinessProfileSheet(store: AppStore, onDismiss: () -> Unit) {
    val v = store.vendor
    var owner by remember { mutableStateOf(v?.ownerName ?: "") }; var business by remember { mutableStateOf(v?.businessName ?: "") }
    var phone by remember { mutableStateOf(v?.phone ?: "") }; var area by remember { mutableStateOf(v?.area ?: "") }
    var since by remember { mutableStateOf(v?.since ?: "") }; var target by remember { mutableStateOf(v?.seasonTarget?.toString() ?: "") }
    EditSheet(onDismiss) {
        SheetContent("Business details", actions = {
            MButton("Save", { store.updateProfile { it.copy(businessName = business.trim(), ownerName = owner.trim(), phone = phone, area = area, since = since, seasonTarget = target.filter(Char::isDigit).toIntOrNull() ?: it.seasonTarget) }; onDismiss() },
                enabled = business.isNotBlank() && owner.isNotBlank())
        }) {
            InputField("Business name", business, { business = it })
            InputField("Your name", owner, { owner = it })
            InputField("Phone", phone, { phone = it }, keyboard = KeyboardType.Phone)
            InputField("Area", area, { area = it }, placeholder = "Sector 44, Gurugram")
            InputField("Operating since", since, { since = it }, placeholder = "since 2018")
            InputField("Season target (₹)", target, { target = it.filter(Char::isDigit) }, placeholder = "6000000", helper = "The bar on the season summary measures bookings against this.", keyboard = KeyboardType.Number)
        }
    }
}

@Composable
fun PaymentDetailsSheet(store: AppStore, onDismiss: () -> Unit) {
    var upi by remember { mutableStateOf(store.vendor?.upi ?: "") }; var bank by remember { mutableStateOf(store.vendor?.bankLabel ?: "") }
    EditSheet(onDismiss) {
        SheetContent("Payment details", "Shown to clients on the shared page and in reminders.", actions = {
            MButton("Save", { store.updateProfile { it.copy(upi = upi.trim(), bankLabel = bank.trim()) }; onDismiss() })
        }) {
            InputField("UPI ID", upi, { upi = it }, placeholder = "mehfil@upi")
            InputField("Bank account label", bank, { bank = it }, placeholder = "HDFC ••4471", helper = "Only a label — never store the full account number here.")
        }
    }
}

@Composable
fun NotificationsSheet(store: AppStore, onDismiss: () -> Unit) {
    val v = store.vendor
    EditSheet(onDismiss) {
        SheetContent("Notifications", "What Mehfil should raise on the home screen.", actions = { MButton("Done", onDismiss) }) {
            RowGroup {
                ToggleRow("Payments", "Overdue and due-this-week milestones", v?.notifyPayments ?: true) { on -> store.updateProfile { it.copy(notifyPayments = on) } }
                Hairline()
                ToggleRow("Conflicts", "Crew or inventory double-booked", v?.notifyConflicts ?: true) { on -> store.updateProfile { it.copy(notifyConflicts = on) } }
                Hairline()
                ToggleRow("Change requests", "Guest count or scope changes from clients", v?.notifyChanges ?: true) { on -> store.updateProfile { it.copy(notifyChanges = on) } }
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = if (subtitle == null) 0.dp else Space.lg, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = if (subtitle == null) Type.bodyMd else Type.bodyMdStrong, color = M.text)
            if (subtitle != null) Text(subtitle, style = Type.caption, color = M.textMute)
        }
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = M.accent))
    }
}

@Composable
fun ClientSheet(store: AppStore, client: Client, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(client.name) }; var phone by remember { mutableStateOf(client.phone) }; var isRepeat by remember { mutableStateOf(client.isRepeat) }
    EditSheet(onDismiss) {
        SheetContent("Edit client", actions = { MButton("Save", { store.updateClient(client.copy(name = name.trim(), phone = phone, isRepeat = isRepeat)); onDismiss() }, enabled = name.isNotBlank()) }) {
            InputField("Name", name, { name = it })
            InputField("Phone", phone, { phone = it }, keyboard = KeyboardType.Phone)
            ToggleRow("Repeat client", null, isRepeat) { isRepeat = it }
        }
    }
}

/** Logs a client's change request and computes what it moves (B6). */
@Composable
fun LogChangeRequestSheet(store: AppStore, event: Event, onLogged: (ChangeRequest) -> Unit, onDismiss: () -> Unit) {
    var delta by remember { mutableStateOf(50) }; var via by remember { mutableStateOf("WhatsApp") }
    EditSheet(onDismiss) {
        SheetContent("Log a change request", "${event.name} · ${event.guests} guests today", actions = {
            MButton("See what it moves", { store.logChangeRequest(event.id, delta, via)?.let { onDismiss(); onLogged(it) } })
        }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Add guests", style = Type.bodyMd, color = M.text); Text("${event.guests} becomes ${event.guests + delta}", style = Type.caption, color = M.textMute) }
                MStepper(delta, { delta = it }, range = 10..2000)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text("Asked via", style = Type.caption, color = M.textMute); SegmentedControl(listOf("WhatsApp", "Call", "In person"), via, { it }, { via = it }) }
            Text("Mehfil works out the chairs, tables, helpers and transport the change needs from your holds and roster, then shows the revised quote.", style = Type.caption, color = M.textMute)
        }
    }
}
