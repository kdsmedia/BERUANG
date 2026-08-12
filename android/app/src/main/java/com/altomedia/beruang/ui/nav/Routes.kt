package com.altomedia.beruang.ui.nav

import androidx.annotation.DrawableRes
import com.altomedia.beruang.R

enum class Tab(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int
) {
    Home("home", "Home", R.drawable.nav_home_off, R.drawable.nav_home_on),
    Friends("friends", "Friends", R.drawable.nav_friends_off, R.drawable.nav_friends_on),
    Messages("messages", "Messages", R.drawable.nav_messages_off, R.drawable.nav_messages_on),
    Groups("groups", "Groups", R.drawable.nav_groups_off, R.drawable.nav_groups_on),
    Me("profile", "Me", R.drawable.nav_profile_off, R.drawable.nav_profile_on)
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
    const val QrScanner = "qrScanner"
    fun chat(uid: String) = "chat/$uid"
    fun profileView(uid: String) = "profileView/$uid"
}
