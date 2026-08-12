package com.altomedia.beruang.ui.nav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.altomedia.beruang.ui.home.HomeScreen
import com.altomedia.beruang.ui.friends.FriendsScreen
import com.altomedia.beruang.ui.groups.GroupsScreen
import com.altomedia.beruang.ui.messages.MessagesScreen
import com.altomedia.beruang.ui.messages.ChatScreen
import com.altomedia.beruang.ui.notifs.NotificationsScreen
import com.altomedia.beruang.ui.profile.ProfileScreen
import com.altomedia.beruang.ui.theme.*

@Composable
fun RootNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in setOf(
        Routes.Home, Routes.Friends, Routes.Messages, Routes.Groups, Routes.Notifs, Routes.Profile
    )

    // On any main tab other than Home, the device back button returns to Home
    // (instead of exiting the app). Sub-routes (chat / profile view) pop normally.
    val tabRoutes = setOf(Routes.Friends, Routes.Messages, Routes.Groups, Routes.Notifs, Routes.Profile)
    BackHandler(enabled = currentRoute in tabRoutes) {
        nav.navigate(Routes.Home) {
            popUpTo(Routes.Home) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomBar(currentRoute) { route ->
                nav.navigate(route) {
                    popUpTo(Routes.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(nav, startDestination = Routes.Home) {
                composable(Routes.Home) { HomeScreen(onAlerts = { nav.navigate(Routes.Notifs) }) }
                composable(Routes.Friends) { FriendsScreen(openChat = { uid -> nav.navigate(Routes.chat(uid)) }, openProfile = { uid -> nav.navigate(Routes.profileView(uid)) }) }
                composable(Routes.Messages) { MessagesScreen(openChat = { uid -> nav.navigate(Routes.chat(uid)) }) }
                composable(Routes.Groups) { GroupsScreen() }
                composable(Routes.Notifs) { NotificationsScreen(openProfile = { uid -> nav.navigate(Routes.profileView(uid)) }) }
                composable(Routes.Profile) { ProfileScreen(uid = null) }
                composable(
                    "${Routes.Chat}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) { entry ->
                    ChatScreen(
                        partnerUid = entry.arguments?.getString("uid") ?: "",
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(
                    "${Routes.ProfileView}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) { entry ->
                    ProfileScreen(uid = entry.arguments?.getString("uid"))
                }
            }
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Surface(color = Surface, tonalElevation = 0.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Tab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    Modifier.weight(1f).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavigationIcon(tab, selected, onNavigate)
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        color = if (selected) Green else Muted,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationIcon(tab: Tab, selected: Boolean, onNavigate: (String) -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(onClick = { onNavigate(tab.route) }) {
            Icon(
                if (selected) tab.selectedIcon else tab.icon,
                contentDescription = tab.label,
                tint = if (selected) Green else Text
            )
        }
        if (tab == Tab.Friends) {
            val vm = androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.notifs.BadgesViewModel>()
            val count by vm.frPending.collectAsState()
            BadgeDot(count)
        }
        if (tab == Tab.Messages) {
            val vm = androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.notifs.BadgesViewModel>()
            val count by vm.msgUnread.collectAsState()
            BadgeDot(count)
        }
    }
}

@Composable
private fun BadgeDot(count: Int) {
    if (count <= 0) return
    Box(
        Modifier.size(16.dp).clip(CircleShape).background(Danger),
        contentAlignment = Alignment.Center
    ) { Text(if (count > 99) "99+" else count.toString(), fontSize = 9.sp, color = Surface, fontWeight = FontWeight.Bold) }
}
