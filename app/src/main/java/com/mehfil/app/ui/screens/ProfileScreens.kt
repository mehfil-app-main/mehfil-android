package com.mehfil.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mehfil.app.BuildConfig
import com.mehfil.app.R
import com.mehfil.app.model.*
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.Routes
import com.mehfil.app.ui.Tab
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.CardShape
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Palette
import com.mehfil.app.ui.theme.Radius
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate

private fun String?.nilIfEmpty() = if (this.isNullOrBlank()) null else this

/** E4 · Profile and settings — deliberately short. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(store: AppStore, nav: Nav) {
    val m = M
    val v = store.vendor
    var appearanceSheet by remember { mutableStateOf(false) }
    var businessSheet by remember { mutableStateOf(false) }
    var paymentSheet by remember { mutableStateOf(false) }
    var notifySheet by remember { mutableStateOf(false) }
    val repeat = store.clients.count { it.isRepeat }
    val account = store.auth.account
    Screen(title = "Profile", subtitle = listOfNotNull(v?.businessName, v?.area?.split(",")?.last()?.trim().nilIfEmpty()).joinToString(" · "), isRoot = true, spacing = Space.lg) {
        Card(onClick = { businessSheet = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(Radius.md)).background(m.emphasisBg), contentAlignment = Alignment.Center) { Text(v?.monogram ?: "M", style = Type.headingLg, color = m.emphasisText) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(v?.businessName ?: "Your business", style = Type.headingSm, color = m.text)
                    Text(listOfNotNull(v?.ownerName.nilIfEmpty(), v?.area.nilIfEmpty(), v?.since.nilIfEmpty()).joinToString(" · "), style = Type.caption, color = m.textMute)
                }
                Chevron()
            }
        }
        RowGroup {
            ProfileRow(Icons.Outlined.Groups, "Team", "${store.crew.size} crew · ${store.crew.count { it.role == CrewRole.COORDINATOR }} coordinators") { nav.push(Routes.crew) }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.Person, "Clients", if (store.clients.isEmpty()) "None yet" else "${store.clients.size} clients · $repeat repeat") { nav.push(Routes.clients) }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.Layers, "Package templates", if (store.packages.isEmpty()) "None yet" else "${store.packages.size} templates · from ${Fmt.inr(store.packages.minOf { it.price })}") { nav.push(Routes.packages) }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.CreditCard, "Payment details", listOfNotNull(v?.upi.nilIfEmpty()?.let { "UPI $it" }, v?.bankLabel.nilIfEmpty()).joinToString(" · ").ifEmpty { "Add your UPI ID" }) { paymentSheet = true }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.LightMode, "Appearance", if (store.appearance == Appearance.SYSTEM) "${if (m.isDark) "Dark" else "Light"} · follows system" else store.appearance.label) { appearanceSheet = true }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.NotificationsNone, "Notifications", notifySummary(v)) { notifySheet = true }
            Hairline(68.dp)
            ProfileRow(if (store.auth.isCloud) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid, "Account", account?.let { it.providerLabel + (it.email?.let { e -> " · $e" } ?: "") } ?: "") { nav.push(Routes.account) }
        }
        Text("Mehfil ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = Type.caption, color = m.textMute, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
    if (appearanceSheet) ModalBottomSheet(onDismissRequest = { appearanceSheet = false }, containerColor = m.surface) {
        SheetContent("Appearance", actions = { MButton("Done", { appearanceSheet = false }) }) {
            RowGroup {
                Appearance.entries.forEachIndexed { i, a ->
                    if (i > 0) Hairline()
                    ListRow(a.label, selected = store.appearance == a, onClick = { store.chooseAppearance(a) }) { if (store.appearance == a) Icon(Icons.Outlined.Check, null, tint = m.accentText) }
                }
            }
        }
    }
    if (businessSheet) BusinessProfileSheet(store) { businessSheet = false }
    if (paymentSheet) PaymentDetailsSheet(store) { paymentSheet = false }
    if (notifySheet) NotificationsSheet(store) { notifySheet = false }
}

private fun notifySummary(v: VendorProfile?): String {
    if (v == null) return ""
    val on = listOfNotNull(if (v.notifyPayments) "Payments" else null, if (v.notifyConflicts) "conflicts" else null, if (v.notifyChanges) "change requests" else null)
    return if (on.isEmpty()) "All off" else on.joinToString(", ")
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListRow(title, subtitle = subtitle, onClick = onClick, leading = { IconTile(icon) }) { Chevron() }
}

/** Package templates — the menu the create-event flow picks from. */
@Composable
fun PackagesScreen(store: AppStore, nav: Nav) {
    val m = M
    var editing by remember { mutableStateOf<Package?>(null) }
    var adding by remember { mutableStateOf(false) }
    Screen(title = "Package templates", subtitle = if (store.packages.isEmpty()) "None yet" else "${store.packages.size} templates", onBack = nav::back, spacing = Space.lg,
        docked = { MButton("New package", { adding = true }, kind = ButtonKind.SECONDARY, icon = Icons.Outlined.Add) }) {
        if (store.packages.isEmpty()) EmptyState("No packages yet", "Templates make quoting a new event a three-tap job.", Modifier.padding(top = 120.dp), "New package", Icons.Outlined.Add, true) { adding = true }
        else RowGroup {
            store.packages.forEachIndexed { i, p ->
                if (i > 0) Hairline()
                ListRow(p.name, subtitle = p.detail, onClick = { editing = p }) { Text(Fmt.inr(p.price), style = Type.bodyTabularStrong, color = m.text); Chevron() }
            }
        }
    }
    if (adding) PackageSheet(store, null) { adding = false }
    editing?.let { p -> PackageSheet(store, p) { editing = null } }
}

/** E1 · Client list — search, rows with event count and lifetime value; repeat clients carry a marker. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientListScreen(store: AppStore, nav: Nav) {
    val m = M
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Client?>(null) }
    val list = store.clients.filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }.sortedByDescending { store.stats(it).lifetimeValue }
    val lifetime = store.lifetimeValue
    val repeat = store.clients.count { it.isRepeat }
    Screen(
        title = "Clients", subtitle = if (store.clients.isEmpty()) "No clients yet" else "${store.clients.size} clients · ${Fmt.inr(lifetime)} lifetime", onBack = nav::back, spacing = Space.lg,
        actions = { CircleAction(Icons.Outlined.Add, { nav.push(Routes.enquiry()) }, tint = m.accentText, contentDescription = "New enquiry") },
    ) {
        if (store.clients.isEmpty()) EmptyState("No clients yet", "Each client is added with their first event or enquiry.", Modifier.padding(top = 140.dp), "Create event", Icons.Outlined.Add, true) { nav.pushIn(Tab.EVENTS, Routes.create) }
        else {
            InputField("Search", query, { query = it }, placeholder = "Search by name or phone", trailing = Icons.Outlined.Search)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                StatTile("$repeat of ${store.clients.size}", "Repeat clients", Modifier.weight(1f))
                StatTile(Fmt.inr(lifetime / store.clients.size / 100 * 100), "Average lifetime value", Modifier.weight(1f))
            }
            RowGroup {
                list.forEachIndexed { i, c ->
                    if (i > 0) Hairline(68.dp)
                    val s = store.stats(c)
                    Box(Modifier.combinedClickable(onClick = {
                        (store.upcomingEvents.firstOrNull { it.clientId == c.id } ?: store.events.firstOrNull { it.clientId == c.id })?.let { nav.push(Routes.event(it.id)) } ?: run { editing = c }
                    }, onLongClick = { editing = c })) {
                        ListRow(c.name, subtitle = "${s.eventCount} event${if (s.eventCount == 1) "" else "s"}" + (s.nextEventDate?.let { " · next ${Fmt.dayMonth(it)}" } ?: ""), leading = { Avatar(c.avatar, c.name) }) {
                            if (c.isRepeat) Icon(Icons.Outlined.StarBorder, null, Modifier.size(14.dp), tint = m.accentText)
                            Text(Fmt.inr(s.lifetimeValue), style = Type.bodyTabularStrong, color = m.text)
                            Chevron()
                        }
                    }
                }
            }
            Text("Long-press a client to edit their details.", style = Type.caption, color = m.textMute)
        }
    }
    editing?.let { c -> ClientSheet(store, c) { editing = null } }
}

/** E3 · Enquiry intake — conflict check on the tentative date, convert to event. */
@Composable
fun EnquiryIntakeScreen(store: AppStore, nav: Nav, enquiryId: String?) {
    val m = M
    val existing = remember(enquiryId) { enquiryId?.let { store.enquiry(it) } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var source by remember { mutableStateOf(existing?.source ?: SampleSeason.enquirySources[0]) }
    var budget by remember { mutableStateOf(existing?.budget ?: SampleSeason.budgetRanges[2]) }
    var date by remember { mutableStateOf<LocalDate?>(existing?.date) }
    var guests by remember { mutableStateOf(existing?.guests ?: 200) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showDate by remember { mutableStateOf(false) }; var showSource by remember { mutableStateOf(false) }; var showBudget by remember { mutableStateOf(false) }
    val conflicts = date?.let { store.eventsOn(it) }.orEmpty()
    val canSave = name.isNotBlank()
    fun save(): String {
        val q = Enquiry(existing?.id ?: Cal.newId(), name.trim(), phone, source, budget, date, guests, notes, existing?.status ?: EnquiryStatus.OPEN, existing?.createdAt ?: Cal.today)
        store.saveEnquiry(q); return q.id
    }
    Screen(
        title = if (existing == null) "New enquiry" else "Enquiry", largeTitle = false, onBack = nav::back, spacing = Space.lg,
        actions = { Text("Save", style = Type.buttonSm, color = if (canSave) m.accentText else m.disabledText, modifier = Modifier.clickable(enabled = canSave) { save(); nav.back() }.padding(Space.sm)) },
        docked = {
            MButton("Convert to event", {
                val id = save()
                store.replaceDraft(EventDraft(clientName = name.trim(), phone = phone, type = null, date = date, venue = "", guests = guests, packageIds = emptySet(), quoted = 0, fromEnquiryId = id))
                nav.back(); nav.pushIn(Tab.EVENTS, Routes.create)
            }, enabled = canSave)
        },
    ) {
        InputField("Name", name, { name = it }, placeholder = "Who is asking")
        InputField("Phone", phone, { phone = it }, placeholder = "98100 00000", keyboard = KeyboardType.Phone)
        PickerField("Source", source, { showSource = true })
        PickerField("Budget range", budget, { showBudget = true })
        PickerField("Tentative date", date?.let { Fmt.weekdayLongYear(it) } ?: "", { showDate = true }, placeholder = "Pick a date", trailing = Icons.Outlined.CalendarToday, highlighted = date != null)
        val d = date
        if (d != null && conflicts.isNotEmpty()) {
            val c = conflicts[0]
            InlineConflictWarning(if (conflicts.size == 1) "${c.name} runs ${if (Cal.hour(c.start) >= 17) "that evening" else "that day"} at ${c.venue.name} with ${c.crewAssigned} crew." else "${conflicts.size} events already run on ${Fmt.dayMonth(d)}.", conflicts, "See ${Fmt.dayMonth(d)} timeline") { nav.push(Routes.day(d)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Guest estimate", style = Type.bodyMd, color = m.text); Text("Rough is fine", style = Type.caption, color = m.textMute) }
            MStepper(guests, { guests = it }, range = 10..5000)
        }
        InputField("Notes", notes, { notes = it }, singleLine = false, minHeight = 72.dp)
        if (existing != null && existing.status == EnquiryStatus.OPEN) MButton("Close enquiry", { store.closeEnquiry(existing.id); nav.back() }, kind = ButtonKind.TERTIARY, compact = true)
    }
    if (showSource) ChoiceSheet("Source", SampleSeason.enquirySources, source, { it }, { source = it }) { showSource = false }
    if (showBudget) ChoiceSheet("Budget range", SampleSeason.budgetRanges, budget, { it }, { budget = it }) { showBudget = false }
    if (showDate) DateSheet("Tentative date", "Conflicts are checked as soon as you pick.", date ?: Cal.nextSaturday, { date = it }, { showDate = false }, minDate = Cal.todayDate)
}

/** E2 · Shared event page — the client's read-only page. Cream ground, the only gradient mesh, always light. */
@Composable
fun SharedEventPageScreen(store: AppStore, nav: Nav, eventId: String) {
    val e = store.event(eventId) ?: return
    val v = store.vendor ?: return
    val collected = store.collected(e.id)
    val due = store.milestonesFor(e.id).filter { it.isOpen }
    val holds = store.inventory.mapNotNull { item -> item.holds.firstOrNull { it.eventId == e.id }?.let { item.name to it.qty } }
    val packages = store.packages
    val ctx = LocalContext.current
    val ink = Palette.ink; val mute = Palette.inkMute
    fun dueLine() = due.joinToString(" · ") { "${Fmt.inr(it.amount)} ${if (it.due <= Cal.todayDate) "due now" else if (it.kind == MilestoneKind.BALANCE) "on the day" else "by ${Fmt.dayMonth(it.due)}"}" }.ifEmpty { "Fully paid — thank you" }
    fun shareBody(): String {
        var s = "${v.businessName} · for the ${e.surname} family\n\n${e.name}\n${Fmt.weekdayLong(e.day)} · from ${Fmt.time(e.start)}\n${listOfNotNull(e.venue.short, e.venue.hall).joinToString(" · ")}\n"
        s += "\nReceived ${Fmt.inr(collected)} of ${Fmt.inr(e.quoted)}"
        if (due.isNotEmpty()) s += "\n" + due.joinToString(" · ") { "${Fmt.inr(it.amount)} ${if (it.due <= Cal.todayDate) "due now" else "by ${Fmt.dayMonth(it.due)}"}" }
        if (v.upi.isNotBlank()) s += "\nUPI ${v.upi}"
        s += "\n\n${v.ownerName} · ${v.phone}"
        return s
    }
    Box(Modifier.fillMaxSize().background(Palette.canvasCream)) {
        // Mesh: documented hues at alpha over canvas-cream (tokens.css --color-mesh-*)
        Box(Modifier.fillMaxWidth().height(420.dp).background(Brush.radialGradient(listOf(Palette.primarySubdued.copy(alpha = 0.85f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(900f, 120f), radius = 900f)))
        Box(Modifier.fillMaxWidth().height(420.dp).background(Brush.radialGradient(listOf(Palette.magenta.copy(alpha = 0.42f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(250f, 220f), radius = 750f)))
        Box(Modifier.fillMaxWidth().height(420.dp).background(Brush.radialGradient(listOf(Palette.ruby.copy(alpha = 0.14f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(80f, 40f), radius = 600f)))
        Box(Modifier.fillMaxWidth().height(420.dp).background(Brush.radialGradient(listOf(Palette.lemon.copy(alpha = 0.16f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(1000f, 400f), radius = 650f)))
        Box(Modifier.fillMaxWidth().height(520.dp).background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Palette.canvasCream)))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = Dim.gutter), verticalAlignment = Alignment.CenterVertically) {
                CircleAction(Icons.AutoMirrored.Outlined.ArrowBack, nav::back, tint = ink, contentDescription = "Back")
                Spacer(Modifier.weight(1f))
                CircleAction(Icons.Outlined.IosShare, { shareText(ctx, e.name, shareBody()) }, tint = ink, contentDescription = "Share")
            }
            Column(Modifier.padding(horizontal = Space.xl).padding(top = Space.lg, bottom = Space.xxl), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("${v.businessName.uppercase()} · FOR THE ${e.surname.uppercase()} FAMILY", style = Type.microCap.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.6f, androidx.compose.ui.unit.TextUnitType.Sp)), color = mute)
                    Text(e.name, style = Type.displayHero, color = ink)
                    Text("${Fmt.weekdayLong(e.day)} · from ${Fmt.time(e.start)}", style = Type.bodyLg, color = ink)
                    Text(listOfNotNull(e.venue.short, e.venue.hall).joinToString(" · "), style = Type.bodyLg, color = Palette.inkSecondary)
                }
                SharedSection("What we are bringing") {
                    if (holds.isEmpty() && packages.isEmpty()) SharedLine("Full décor as quoted", "")
                    packages.take(4).forEach { SharedLine(it.name, it.detail) }
                    holds.forEach { (n, q) -> SharedLine("$q ${n.lowercase()}", "") }
                    SharedLine("Seating for ${e.guests} guests", "")
                }
                SharedSection("On the day") {
                    SharedLine("Setup begins", Fmt.time(e.start.minusHours(8))); SharedLine("Venue ready for your walkthrough", Fmt.time(e.start.minusMinutes(30)))
                    SharedLine("Guests arrive", Fmt.time(e.start)); SharedLine("Pack-down", Fmt.time(e.end))
                }
                Column(Modifier.fillMaxWidth().clip(CardShape).background(Palette.canvasCream).border(1.dp, Palette.lemon.copy(alpha = 0.25f), CardShape).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row { Text("Received", style = Type.bodyLg, color = ink, modifier = Modifier.weight(1f)); Text("${Fmt.inr(collected)} of ${Fmt.inr(e.quoted)}", style = Type.bodyLg, color = ink) }
                    ProgressBar(collected.toFloat() / maxOf(1, e.quoted), Modifier.height(4.dp), tint = ink)
                    Text(dueLine() + if (v.upi.isBlank()) "" else " · UPI ${v.upi}", style = Type.caption, color = mute)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(R.drawable.mood_1, R.drawable.mood_2, R.drawable.mood_3).forEach { Image(painterResource(it), null, Modifier.weight(1f).aspectRatio(4f / 3f).clip(RoundedCornerShape(Radius.sm)), contentScale = ContentScale.Crop) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(Radius.sm)).background(Palette.brandDark), contentAlignment = Alignment.Center) { Text(v.monogram, style = Type.headingSm, color = Palette.onPrimary) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${v.ownerName} · ${v.businessName}", style = Type.bodyMdStrong, color = ink, maxLines = 1)
                        Text(listOfNotNull(v.phone.nilIfEmpty(), v.area.nilIfEmpty()).joinToString(" · "), style = Type.caption, color = mute, maxLines = 1)
                    }
                    if (v.phone.isNotBlank()) Row(Modifier.height(40.dp).clip(CircleShape).border(1.dp, ink, CircleShape).clickable { whatsapp(ctx, v.phone) }.padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(16.dp), tint = ink); Text("Chat", style = Type.buttonSm, color = ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) { Text(title, style = Type.headingSm, color = Palette.ink); content() }
}

@Composable
private fun SharedLine(l: String, r: String) {
    Row(Modifier.fillMaxWidth()) { Text(l, style = Type.bodyLg, color = Palette.ink, modifier = Modifier.weight(1f)); Text(r, style = Type.bodyLg, color = Palette.inkMute) }
}
