package com.mehfil.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehfil.app.model.AppStore
import com.mehfil.app.ui.components.ToastView
import com.mehfil.app.ui.screens.*
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import java.time.LocalDate

enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    EVENTS("events", "Events", Icons.Outlined.CalendarMonth),
    MONEY("money", "Money", Icons.Outlined.CreditCard),
    TEAM("team", "Team", Icons.Outlined.Groups),
    PROFILE("profile", "Profile", Icons.Outlined.Person),
}

/** Route helpers: every pushed destination in the app (spec §2). */
object Routes {
    const val calendar = "calendar"; const val season = "season"; const val create = "create"; const val inventory = "inventory"
    const val clients = "clients"; const val enquiry = "enquiry"; const val crew = "crew"
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
            if (isRoot) {
                androidx.compose.foundation.layout.Column {
                    HorizontalDivider(thickness = 1.dp, color = m.line)
                    NavigationBar(containerColor = m.surface, tonalElevation = 0.dp) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = selected == t, onClick = { nav.tab(t) },
                                icon = { Icon(t.icon, t.title) }, label = { Text(t.title, style = Type.microCap.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp))) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = m.accentText, selectedTextColor = m.accentText, indicatorColor = m.accentTint,
                                    unselectedIconColor = m.textMute, unselectedTextColor = m.textMute,
                                ),
                            )
                        }
                    }
                }
            }
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
                composable("day/{date}", listOf(navArgument("date") { type = NavType.StringType })) { DayDetailScreen(store, nav, LocalDate.parse(it.arguments!!.getString("date"))) }
                composable(Routes.season) { SeasonSummaryScreen(store, nav) }
                composable("event/{id}") { EventDetailScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable(Routes.create) { CreateEventScreen(store, nav) }
                composable("runsheet/{id}") { RunsheetScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable("change/{id}") { ChangeRequestScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable("schedule/{id}") { PaymentScheduleScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable("expenses/{id}") { ExpensesScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable("assign/{id}") { AssignCrewScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable("attendance/{id}") { AttendanceScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable(Routes.inventory) { InventoryScreen(store, nav) }
                composable("item/{id}") { ItemDetailScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable(Routes.clients) { ClientListScreen(store, nav) }
                composable("shared/{id}") { SharedEventPageScreen(store, nav, it.arguments!!.getString("id")!!) }
                composable(Routes.enquiry) { EnquiryIntakeScreen(store, nav) }
                composable(Routes.crew) { CrewListScreen(store, nav, pushed = true) }
            }
            AnimatedVisibility(
                visible = store.toast != null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
            ) { store.toast?.let { ToastView(it, Modifier.padding(horizontal = Space.xl)) } }
        }
    }
}
