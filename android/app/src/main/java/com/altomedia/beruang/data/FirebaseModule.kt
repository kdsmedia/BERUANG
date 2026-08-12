package com.altomedia.beruang.data

import com.altomedia.beruang.data.repo.AccountsRepository
import com.altomedia.beruang.data.repo.AuthRepository
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.GroupsRepository
import com.altomedia.beruang.data.repo.MessagesRepository
import com.altomedia.beruang.data.repo.NotificationsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun auth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton fun profileRepo(db: FirebaseFirestore, auth: FirebaseAuth) = ProfileRepository(db, auth)
    @Provides @Singleton fun accountsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = AccountsRepository(db, auth)
    @Provides @Singleton fun feedRepo(db: FirebaseFirestore, auth: FirebaseAuth, accounts: AccountsRepository) = FeedRepository(db, auth, accounts)
    @Provides @Singleton fun friendsRepo(db: FirebaseFirestore, auth: FirebaseAuth, accounts: AccountsRepository) = FriendsRepository(db, auth, accounts)
    @Provides @Singleton fun messagesRepo(db: FirebaseFirestore, auth: FirebaseAuth) = MessagesRepository(db, auth)
    @Provides @Singleton fun groupsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = GroupsRepository(db, auth)
    @Provides @Singleton fun notifsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = NotificationsRepository(db, auth)
    @Provides @Singleton fun authRepo(auth: FirebaseAuth) = AuthRepository(auth)
}
