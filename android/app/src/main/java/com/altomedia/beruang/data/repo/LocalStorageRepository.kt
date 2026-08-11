package com.altomedia.beruang.data.repo

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores media on the device's internal app storage (filesDir) instead of
 * Firebase Storage. Files are private to this app but persist across launches.
 *
 * Note: because files live only on the creating device, media is only visible
 * to the user who picked it; other users fall back to the dicebear avatar.
 */
@Singleton
class LocalStorageRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val auth: FirebaseAuth
) {
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    /** Copies the picked image into internal storage and returns a file:// uri string. */
    suspend fun saveAvatar(uri: Uri): String = withContext(Dispatchers.IO) {
        val u = uid()
        val dir = File(ctx.filesDir, "avatars").apply { mkdirs() }
        val dest = File(dir, "$u.png")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot read selected image")
        "file://${dest.absolutePath}"
    }

    /** Whether a stored local file:// path still exists on this device. */
    fun localFileExists(url: String?): Boolean {
        if (url == null || !url.startsWith("file://")) return false
        return runCatching { File(url.removePrefix("file://")).exists() }.getOrDefault(false)
    }
}
