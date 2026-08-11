package com.altomedia.beruang.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUid get() = auth.currentUser?.uid
    val isSignedIn get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String) {
        try { auth.signInWithEmailAndPassword(email, password).await() }
        catch (e: Exception) { throw mapError(e) }
    }

    suspend fun signUp(name: String, email: String, password: String) {
        if (password.length < 6) throw AuthError.WeakPassword
        try {
            // profile auto-created by Firestore trigger on auth user creation (see README/friendsRules)
            // We pass full_name via displayName; the cloud function (not bundled) writes profiles.
            // As a safety net the app also writes the profile on first load (ProfileRepository.loadMyProfile).
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build()
            )?.await()
        } catch (e: Exception) { throw mapError(e) }
    }

    suspend fun signOut() { auth.signOut() }

    private fun mapError(e: Throwable): AuthError {
        // CONFIGURATION_NOT_FOUND: project exists & API key valid, but the
        // Email/Password sign-in provider is not enabled in the Firebase Console
        // (Authentication → Sign-in method → Email/Password). This is a backend
        // config issue, not a code/app issue — no APK change can fix it.
        val msg = e.message.orEmpty()
        if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
            return AuthError.ConfigurationNotFound
        }
        return when (e) {
            is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
            is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
            else -> AuthError.Generic(msg.ifBlank { "Authentication failed" })
        }
    }
}

sealed class AuthError(message: String) : Exception(message) {
    data object WeakPassword : AuthError("Password must be at least 6 characters.")
    data object InvalidCredentials : AuthError("Invalid email or password.")
    data object EmailInUse : AuthError("That email is already in use.")
    data object ConfigurationNotFound : AuthError(
        "Login belum bisa dipakai. Aktifkan sign-in Email/Password di Firebase Console " +
        "(Authentication → Sign-in method → Email/Password → Enable)."
    )
    class Generic(message: String) : AuthError(message)
}
