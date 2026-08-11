package com.altomedia.beruang.data.repo

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    suspend fun uploadAvatar(uri: Uri): String {
        val path = "avatars/${uid()}/avatar-${UUID.randomUUID()}.img"
        val ref = storage.getReference(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
