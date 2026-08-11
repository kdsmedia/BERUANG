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

    private fun mapError(e: Throwable): AuthError = when (e) {
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
        else -> AuthError.Generic(e.message ?: "Authentication failed")
    }
}

sealed class AuthError(message: String) : Exception(message) {
    data object WeakPassword : AuthError("Password must be at least 6 characters.")
    data object InvalidCredentials : AuthError("Invalid email or password.")
    data object EmailInUse : AuthError("That email is already in use.")
    class Generic(message: String) : AuthError(message)
}
