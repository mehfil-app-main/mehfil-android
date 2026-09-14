package com.mehfil.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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

/** E4 · Profile and settings — deliberately short. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(store: AppStore, nav: Nav) {
    val m = M
    var appearanceSheet by remember { mutableStateOf(false) }
    val repeat = store.clients.count { it.isRepeat }
    Screen(title = "Profile", subtitle = "${Seed.businessName} · Gurugram", isRoot = true, spacing = Space.lg) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(Radius.md)).background(m.emphasisBg), contentAlignment = Alignment.Center) { Text("M", style = Type.headingLg, color = m.emphasisText) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(Seed.businessName, style = Type.headingSm, color = m.text)
                    Text("${Seed.vendorName} · ${Seed.businessArea} · ${Seed.businessSince}", style = Type.caption, color = m.textMute)
                }
                Chevron()
            }
        }
        RowGroup {
            ProfileRow(Icons.Outlined.Groups, "Team", "${store.crew.size + 1} members · ${store.crew.count { it.role == CrewRole.COORDINATOR }} coordinators") { nav.push(Routes.crew) }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.Person, "Clients", "${store.clients.size} clients · $repeat repeat") { nav.push(Routes.clients) }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.Layers, "Package templates", "${Seed.packages.size + 2} templates · from ${Fmt.inr(Seed.packages.minOf { it.price })}") { store.show(Toast.Kind.INFO, "Templates are coming with the next release") }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.CreditCard, "Payment details", "UPI ${Seed.upi} · ${Seed.bank}") { store.show(Toast.Kind.INFO, "Payment details are managed by your accountant") }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.LightMode, "Appearance", if (store.appearance == Appearance.SYSTEM) "${if (m.isDark) "Dark" else "Light"} · follows system" else store.appearance.label) { appearanceSheet = true }
            Hairline(68.dp)
            ProfileRow(Icons.Outlined.NotificationsNone, "Notifications", "Payments, conflicts, change requests") { store.show(Toast.Kind.INFO, "All three alert types are on") }
        }
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
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListRow(title, subtitle = subtitle, onClick = onClick, leading = { IconTile(icon) }) { Chevron() }
}

/** E1 · Client list — search, rows with event count and lifetime value; repeat clients carry a marker. */
@Composable
fun ClientListScreen(store: AppStore, nav: Nav) {
    val m = M
    var query by remember { mutableStateOf("") }
    val list = store.clients.filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }.sortedByDescending { it.lifetimeValue }
    val lifetime = store.clients.sumOf { it.lifetimeValue }
    val repeat = store.clients.count { it.isRepeat }
    Screen(
        title = "Clients", subtitle = if (store.clients.isEmpty()) "No clients yet" else "${store.clients.size} clients · ${Fmt.inr(lifetime)} lifetime", onBack = nav::back, spacing = Space.lg,
        actions = { CircleAction(Icons.Outlined.Add, { nav.push(Routes.enquiry) }, tint = m.accentText) },
    ) {
        InputField("Search", query, { query = it }, placeholder = "Search by name or phone", trailing = Icons.Outlined.Search)
        if (store.clients.isEmpty()) EmptyState("No clients yet", "Each client is added with their first event or enquiry.", Modifier.padding(top = 140.dp), "Create event") { nav.pushIn(Tab.EVENTS, Routes.create) }
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                StatTile("$repeat of ${store.clients.size}", "Repeat clients", Modifier.weight(1f))
                StatTile(Fmt.inr(if (store.clients.isEmpty()) 0 else lifetime / store.clients.size / 100 * 100), "Average lifetime value", Modifier.weight(1f))
            }
            RowGroup {
                list.forEachIndexed { i, c ->
                    if (i > 0) Hairline(68.dp)
                    ListRow(c.name, subtitle = "${c.eventCount} event${if (c.eventCount == 1) "" else "s"}" + (c.nextEventDate?.let { " · next ${Fmt.dayMonth(it)}" } ?: ""),
                        onClick = { (store.upcomingEvents.firstOrNull { it.clientId == c.id } ?: store.events.firstOrNull { it.clientId == c.id })?.let { nav.push(Routes.event(it.id)) } },
                        leading = { Avatar(c.avatar, c.name) }) {
                        if (c.isRepeat) Icon(Icons.Outlined.StarBorder, null, Modifier.size(14.dp), tint = m.accentText)
                        Text(Fmt.inr(c.lifetimeValue), style = Type.bodyTabularStrong, color = m.text)
                        Chevron()
                    }
                }
            }
        }
    }
}

/** E3 · Enquiry intake — conflict check on the tentative date, convert to event. */
@Composable
fun EnquiryIntakeScreen(store: AppStore, nav: Nav) {
    val m = M
    var name by remember { mutableStateOf(Seed.enquiry.name) }
    var source by remember { mutableStateOf(Seed.enquiry.source) }
    var budget by remember { mutableStateOf(Seed.enquiry.budget) }
    var date by remember { mutableStateOf<LocalDate?>(Seed.enquiry.date) }
    var guests by remember { mutableStateOf(Seed.enquiry.guests) }
    var notes by remember { mutableStateOf(Seed.enquiry.notes) }
    var showDate by remember { mutableStateOf(false) }; var showSource by remember { mutableStateOf(false) }; var showBudget by remember { mutableStateOf(false) }
    val conflicts = date?.let { store.eventsOn(it) }.orEmpty()
    Screen(
        title = "New enquiry", largeTitle = false, onBack = nav::back, spacing = Space.lg,
        actions = { Text("Save", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { store.show(Toast.Kind.SUCCESS, "Enquiry saved"); nav.back() }.padding(Space.sm)) },
        docked = {
            MButton("Convert to event", {
                store.updateDraft { EventDraft(clientName = name, type = EventType.ENGAGEMENT, date = date, guests = guests) }
                nav.back(); nav.pushIn(Tab.EVENTS, Routes.create)
            }, enabled = name.isNotBlank())
        },
    ) {
        InputField("Name", name, { name = it }, placeholder = "Who is asking")
        PickerField("Source", source, { showSource = true })
        PickerField("Budget range", budget, { showBudget = true })
        PickerField("Tentative date", date?.let { Fmt.weekdayLongYear(it) } ?: "", { showDate = true }, placeholder = "Pick a date", trailing = Icons.Outlined.CalendarToday, highlighted = date != null)
        if (date != null && conflicts.isNotEmpty()) {
            val c = conflicts[0]
            InlineConflictWarning(if (conflicts.size == 1) "${c.name} runs that evening at ${c.venue.name} with ${c.crewAssigned} crew." else "${conflicts.size} events already run on ${Fmt.dayMonth(date!!)}.", conflicts, "See ${Fmt.dayMonth(date!!)} timeline") { nav.push(Routes.day(date!!)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Guest estimate", style = Type.bodyMd, color = m.text); Text("Rough is fine", style = Type.caption, color = m.textMute) }
            MStepper(guests, { guests = it }, range = 10..5000)
        }
        InputField("Notes", notes, { notes = it }, singleLine = false, minHeight = 72.dp)
    }
    if (showSource) ChoiceSheet("Source", Seed.enquirySources, source, { it }, { source = it }) { showSource = false }
    if (showBudget) ChoiceSheet("Budget range", Seed.budgetRanges, budget, { it }, { budget = it }) { showBudget = false }
    if (showDate) DateSheet("Tentative date", "Conflicts are checked as soon as you pick.", date ?: LocalDate.of(2026, 12, 5), { date = it }) { showDate = false }
}

/** E2 · Shared event page — the client's read-only page. Cream ground, the only gradient mesh, always light. */
@Composable
fun SharedEventPageScreen(store: AppStore, nav: Nav, eventId: String) {
    val e = store.event(eventId) ?: return
    val client = store.clientFor(e) ?: return
    val collected = store.collected(e.id)
    val due = store.milestonesFor(e.id).filter { it.isOpen }
    val ctx = LocalContext.current
    val ink = Palette.ink; val mute = Palette.inkMute
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
                CircleAction(Icons.Outlined.IosShare, {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, "Your ${e.type.word} plan from ${Seed.businessName}: https://mehfil.app/e/${e.id}") }
                    ctx.startActivity(android.content.Intent.createChooser(send, e.name))
                }, tint = ink, contentDescription = "Share")
            }
            Column(Modifier.padding(horizontal = Space.xl).padding(top = Space.lg, bottom = Space.xxl), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("${Seed.businessName.uppercase()} · FOR THE ${e.surname.uppercase()} FAMILY", style = Type.microCap.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.6f, androidx.compose.ui.unit.TextUnitType.Sp)), color = mute)
                    Text(e.name, style = Type.displayHero, color = ink)
                    Text("${Fmt.weekdayLong(e.day)} · from ${Fmt.time(e.start)}", style = Type.bodyLg, color = ink)
                    Text(listOfNotNull(e.venue.short, e.venue.hall).joinToString(" · "), style = Type.bodyLg, color = Palette.inkSecondary)
                }
                SharedSection("What we are bringing") {
                    SharedLine("Stage with 12 m floral backdrop", "40 lights"); SharedLine("Marigold and jasmine mandap", "fresh")
                    SharedLine("Velvet ceiling drapes, 120 panels", "ivory"); SharedLine("Seating for ${e.guests} with ${store.item("tables")?.holds?.firstOrNull { it.eventId == e.id }?.qty ?: 45} round tables", "")
                }
                SharedSection("On the day") {
                    SharedLine("Setup begins", "09:00"); SharedLine("Venue ready for your walkthrough", "16:30"); SharedLine("Guests arrive", Fmt.time(e.start)); SharedLine("Pack-down", Fmt.time(e.end))
                }
                Column(Modifier.fillMaxWidth().clip(CardShape).background(Palette.canvasCream).border(1.dp, Palette.lemon.copy(alpha = 0.25f), CardShape).padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row { Text("Received", style = Type.bodyLg, color = ink, modifier = Modifier.weight(1f)); Text("${Fmt.inr(collected)} of ${Fmt.inr(e.quoted)}", style = Type.bodyLg, color = ink) }
                    ProgressBar(collected.toFloat() / maxOf(1, e.quoted), Modifier.height(4.dp), tint = ink)
                    Text(due.joinToString(" · ") { "${Fmt.inr(it.amount)} ${if (it.due <= Cal.todayDate) "due now" else if (it.kind == MilestoneKind.BALANCE) "on the day" else "by ${Fmt.dayMonth(it.due)}"}" } + " · UPI ${Seed.upi}", style = Type.caption, color = mute)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    listOf(R.drawable.mood_1, R.drawable.mood_2, R.drawable.mood_3).forEach { Image(painterResource(it), null, Modifier.weight(1f).aspectRatio(4f / 3f).clip(RoundedCornerShape(Radius.sm)), contentScale = ContentScale.Crop) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(Radius.sm)).background(Palette.brandDark), contentAlignment = Alignment.Center) { Text("M", style = Type.headingSm, color = Palette.onPrimary) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${Seed.vendorName} · ${Seed.businessName}", style = Type.bodyMdStrong, color = ink, maxLines = 1)
                        Text("${Seed.businessPhone} · ${Seed.businessArea}", style = Type.caption, color = mute, maxLines = 1)
                    }
                    Row(Modifier.height(40.dp).clip(CircleShape).border(1.dp, ink, CircleShape).clickable { whatsapp(ctx, Seed.businessPhone) }.padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
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
