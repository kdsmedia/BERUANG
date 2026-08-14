package com.altomedia.beruang.data.repo

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth
) {
    val currentUid get() = auth.currentUserOrNull()?.id
    val isSignedIn get() = auth.currentSessionOrNull() != null

    suspend fun signIn(email: String, password: String) {
        try { auth.signInWith(Email) { this.email = email; this.password = password } }
        catch (e: Exception) { throw mapError(e) }
    }

    suspend fun signUp(name: String, email: String, password: String) {
        if (password.length < 6) throw AuthError.WeakPassword
        try {
            // The handle_new_user trigger creates the profile row on signup,
            // reading full_name from user metadata.
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", name)
                }
            }
        } catch (e: Exception) { throw mapError(e) }
    }

    suspend fun signOut() { auth.signOut() }

    private fun mapError(e: Throwable): AuthError {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("Invalid login", ignoreCase = true) ||
            msg.contains("Credentials", ignoreCase = true) -> AuthError.InvalidCredentials
            msg.contains("already registered", ignoreCase = true) ||
            msg.contains("already in use", ignoreCase = true) -> AuthError.EmailInUse
            msg.contains("Password", ignoreCase = true) && msg.contains("short", ignoreCase = true) -> AuthError.WeakPassword
            else -> AuthError.Generic(msg.ifBlank { "Authentication failed" })
        }
    }
}

sealed class AuthError(message: String) : Exception(message) {
    data object WeakPassword : AuthError("Password must be at least 6 characters.")
    data object InvalidCredentials : AuthError("Invalid email or password.")
    data object EmailInUse : AuthError("That email is already in use.")
    class Generic(message: String) : AuthError(message)
}

