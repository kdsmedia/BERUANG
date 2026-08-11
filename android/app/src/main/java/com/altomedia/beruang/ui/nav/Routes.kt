package com.altomedia.beruang.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("home", "Home", Icons.Outlined.House, Icons.Filled.House),
    Friends("friends", "Friends", Icons.Outlined.Group, Icons.Filled.Group),
    Messages("messages", "Messages", Icons.AutoMirrored.Outlined.Comment, Icons.AutoMirrored.Filled.Comment),
    Groups("groups", "Groups", Icons.Outlined.Group, Icons.Filled.Group),
    Alerts("notifs", "Alerts", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    Me("profile", "Me", Icons.Outlined.Person, Icons.Filled.Person)
}

object Routes {
    const val Home = "home"
    const val Friends = "friends"
    const val Messages = "messages"
    const val Groups = "groups"
    const val Notifs = "notifs"
    const val Profile = "profile"
    const val Chat = "chat/{uid}"
    const val ProfileView = "profileView/{uid}"
    fun chat(uid: String) = "chat/$uid"
    fun profileView(uid: String) = "profileView/$uid"
}
