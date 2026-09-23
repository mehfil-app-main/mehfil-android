package com.mehfil.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehfil.app.model.AppStore
import com.mehfil.app.ui.components.ToastView
import com.mehfil.app.ui.screens.*
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate

/**
 * The five tab destinations. Icons are the filled weight and deliberately mirror the iOS build's SF Symbols
 * — house, calendar, creditcard, person.2, person — so the two apps read as one product. `People` is two
 * figures, not `Groups`' three, because iOS uses `person.2`.
 */
enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    EVENTS("events", "Events", Icons.Filled.CalendarMonth),
    MONEY("money", "Money", Icons.Filled.CreditCard),
    TEAM("team", "Team", Icons.Filled.People),
    PROFILE("profile", "Profile", Icons.Filled.Person),
}

/**
 * The tab bar: a floating capsule that clears the content, matching the iOS build rather than Material's
 * full-width bar. The accent marks the selected tab through its icon and label (NOTES: one accent, spent
 * sparingly); the pill behind it is neutral, so the accent is never doubled.
 */
@Composable
private fun FloatingTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val m = M
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dim.gutter, vertical = Space.sm)) {
        Surface(shape = CircleShape, color = m.surface, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Tab.entries.forEach { t ->
                    val on = t == selected
                    Column(
                        Modifier.weight(1f)
                            .clip(CircleShape)
                            .background(if (on) m.segmentTrack else Color.Transparent)
                            .selectable(selected = on, role = Role.Tab) { onSelect(t) }
                            .padding(vertical = Space.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.xxs),
                    ) {
                        Icon(t.icon, null, Modifier.size(Dim.icon), tint = if (on) m.accent else m.text)
                        Text(t.title, style = Type.microCap, color = if (on) m.accent else m.text, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Route helpers: every pushed destination in the app (spec §2). */
object Routes {
    const val calendar = "calendar"; const val season = "season"; const val create = "create"; const val inventory = "inventory"
    const val clients = "clients"; const val crew = "crew"; const val packages = "packages"; const val account = "account"
    fun enquiry(id: String? = null) = if (id == null) "enquiry/new" else "enquiry/$id"
    fun day(d: LocalDate) = "day/$d"
    fun event(id: String) = "event/$id"
    fun runsheet(id: String) = "runsheet/$id"
    fun change(id: String) = "change/$id"
    fun schedule(id: String) = "schedule/$id"
    fun expenses(id: String) = "expenses/$id"
    fun assign(id: String) = "assign/$id"
    fun attendance(id: String) = "attendance/$id"
    fun item(id: String) = "item/$id"
    fun shared(id: String) = "shared/$id"
}

private const val ARG_ID = "id"
private const val ARG_DATE = "date"
private const val NEW_ENQUIRY = "new"

/**
 * Renders [content] with the route's id, or pops back if the argument is missing.
 *
 * Compose Navigation types arguments as a Bundle, so every id-bearing route previously ended in `!!`. A route
 * typo or a malformed deep link would have crashed rather than returning the user to a screen that exists.
 */
@Composable
private fun NavBackStackEntry.id(nav: Nav, content: @Composable (String) -> Unit) {
    val value = arguments?.getString(ARG_ID)
    if (value.isNullOrEmpty()) LaunchedEffect(Unit) { nav.back() } else content(value)
}

/** As [id], but for the day route: an unparseable date sends the user back rather than throwing. */
@Composable
private fun NavBackStackEntry.date(nav: Nav, content: @Composable (LocalDate) -> Unit) {
    val value = arguments?.getString(ARG_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (value == null) LaunchedEffect(Unit) { nav.back() } else content(value)
}

class Nav(val controller: NavHostController, val store: AppStore, private val select: (Tab) -> Unit) {
    fun push(route: String) = controller.navigate(route)
    fun back() { controller.popBackStack() }
    fun tab(t: Tab) {
        select(t)
        controller.navigate(t.route) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true; restoreState = true
        }
    }
    /** Switch tab, then push (A1 → D2 etc.). */
    fun pushIn(t: Tab, route: String) { tab(t); controller.navigate(route) }
}

@Composable
fun MehfilApp(store: AppStore) {
    val controller = rememberNavController()
    var selected by remember { mutableStateOf(Tab.HOME) }
    val nav = remember(controller) { Nav(controller, store) { selected = it } }
    val entry by controller.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isRoot = Tab.entries.any { it.route == route }
    val m = M

    Scaffold(
        containerColor = m.ground,
        bottomBar = {
            // Pushed screens drop the tab bar (NOTES tab bar policy).
            if (isRoot) FloatingTabBar(selected) { nav.tab(it) }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(m.ground).padding(bottom = if (isRoot) padding.calculateBottomPadding() else 0.dp)) {
            NavHost(controller, startDestination = Tab.HOME.route) {
                composable(Tab.HOME.route) { HomeScreen(store, nav) }
                composable(Tab.EVENTS.route) { EventListScreen(store, nav) }
                composable(Tab.MONEY.route) { ReceivablesScreen(store, nav) }
                composable(Tab.TEAM.route) { CrewListScreen(store, nav, pushed = false) }
                composable(Tab.PROFILE.route) { ProfileScreen(store, nav) }

                composable(Routes.calendar) { CalendarMonthScreen(store, nav) }
                composable("day/{$ARG_DATE}", listOf(navArgument(ARG_DATE) { type = NavType.StringType })) { entry ->
                    entry.date(nav) { DayDetailScreen(store, nav, it) }
                }
                composable(Routes.season) { SeasonSummaryScreen(store, nav) }
                composable("event/{$ARG_ID}") { entry -> entry.id(nav) { EventDetailScreen(store, nav, it) } }
                composable(Routes.create) { CreateEventScreen(store, nav) }
                composable("runsheet/{$ARG_ID}") { entry -> entry.id(nav) { RunsheetScreen(store, nav, it) } }
                composable("change/{$ARG_ID}") { entry -> entry.id(nav) { ChangeRequestScreen(store, nav, it) } }
                composable("schedule/{$ARG_ID}") { entry -> entry.id(nav) { PaymentScheduleScreen(store, nav, it) } }
                composable("expenses/{$ARG_ID}") { entry -> entry.id(nav) { ExpensesScreen(store, nav, it) } }
                composable("assign/{$ARG_ID}") { entry -> entry.id(nav) { AssignCrewScreen(store, nav, it) } }
                composable("attendance/{$ARG_ID}") { entry -> entry.id(nav) { AttendanceScreen(store, nav, it) } }
                composable(Routes.inventory) { InventoryScreen(store, nav) }
                composable("item/{$ARG_ID}") { entry -> entry.id(nav) { ItemDetailScreen(store, nav, it) } }
                composable(Routes.clients) { ClientListScreen(store, nav) }
                composable("shared/{$ARG_ID}") { entry -> entry.id(nav) { SharedEventPageScreen(store, nav, it) } }
                composable("enquiry/{$ARG_ID}") { entry ->
                    // "new" is the sentinel for an unsaved enquiry; the screen takes null for that case.
                    entry.id(nav) { EnquiryIntakeScreen(store, nav, it.takeIf { id -> id != NEW_ENQUIRY }) }
                }
                composable(Routes.crew) { CrewListScreen(store, nav, pushed = true) }
                composable(Routes.packages) { PackagesScreen(store, nav) }
                composable(Routes.account) { AccountScreen(store, nav) }
            }
            AnimatedVisibility(
                visible = store.toast != null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
            ) { store.toast?.let { ToastView(it, Modifier.padding(horizontal = Space.xl)) } }
        }
    }
}
