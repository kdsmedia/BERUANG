package com.altomedia.beruang.data

import com.altomedia.beruang.data.repo.AuthRepository
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.GroupsRepository
import com.altomedia.beruang.data.repo.MessagesRepository
import com.altomedia.beruang.data.repo.NotificationsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import com.altomedia.beruang.data.repo.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    @Provides @Singleton fun storage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton fun profileRepo(db: FirebaseFirestore, auth: FirebaseAuth) = ProfileRepository(db, auth)
    @Provides @Singleton fun feedRepo(db: FirebaseFirestore, auth: FirebaseAuth, storage: FirebaseStorage) = FeedRepository(db, auth, storage)
    @Provides @Singleton fun friendsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = FriendsRepository(db, auth)
    @Provides @Singleton fun messagesRepo(db: FirebaseFirestore, auth: FirebaseAuth) = MessagesRepository(db, auth)
    @Provides @Singleton fun groupsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = GroupsRepository(db, auth)
    @Provides @Singleton fun notifsRepo(db: FirebaseFirestore, auth: FirebaseAuth) = NotificationsRepository(db, auth)
    @Provides @Singleton fun storageRepo(storage: FirebaseStorage, auth: FirebaseAuth) = StorageRepository(storage, auth)
    @Provides @Singleton fun authRepo(auth: FirebaseAuth) = AuthRepository(auth)
}
