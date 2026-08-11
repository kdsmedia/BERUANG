package com.altomedia.beruang.data

import android.content.Context
import com.altomedia.beruang.data.repo.AuthRepository
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.GroupsRepository
import com.altomedia.beruang.data.repo.LocalStorageRepository
import com.altomedia.beruang.data.repo.MessagesRepository
import com.altomedia.beruang.data.repo.NotificationsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun localStorage(@ApplicationContext ctx: Context, auth: FirebaseAuth) = LocalStorageRepository(ctx, auth)

    @Provides @Singleton fun profileRepo(db: FirebaseFirestore, auth: FirebaseAuth) = ProfileRepository(db, auth)
    @Provides @Singleton fun feedRepo(db: FirebaseFirestore, auth: FirebaseAuth) = FeedRepository(db, auth)
    @Provides @Singleton fun friendsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = FriendsRepository(db, auth)
    @Provides @Singleton fun messagesRepo(db: FirebaseFirestore, auth: FirebaseAuth) = MessagesRepository(db, auth)
    @Provides @Singleton fun groupsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = GroupsRepository(db, auth)
    @Provides @Singleton fun notifsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = NotificationsRepository(db, auth)
    @Provides @Singleton fun authRepo(auth: FirebaseAuth) = AuthRepository(auth)
}
