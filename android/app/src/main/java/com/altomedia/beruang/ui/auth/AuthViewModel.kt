package com.altomedia.beruang.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.repo.AuthError
import com.altomedia.beruang.data.repo.AuthRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _session = MutableStateFlow(FirebaseAuth.getInstance().currentUser)
    val session: StateFlow<FirebaseUser?> = _session.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { _session.value = it.currentUser }

    init { FirebaseAuth.getInstance().addAuthStateListener(authListener) }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun login(phone: String, pass: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null; _info.value = null
        try {
            authRepo.signIn(pseudoEmail(phone), pass)
            profileRepo.loadMyProfile()
        } catch (e: AuthError) { _error.value = e.message }
        catch (e: Exception) { _error.value = e.message } finally { _loading.value = false }
    }

    fun signUp(name: String, phone: String, pass: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null; _info.value = null
        try {
            authRepo.signUp(name, pseudoEmail(phone), pass)
            _info.value = "Account created. You can now log in."
        } catch (e: AuthError) { _error.value = e.message }
        catch (e: Exception) { _error.value = e.message } finally { _loading.value = false }
    }

    fun clear() { _error.value = null; _info.value = null }

    /**
     * Normalizes a phone number to the Indonesian `08xxxxxxxx` format (digits only).
     * 62812… → 0812…  |  812… → 0812…  |  0812… → kept.
     */
    private fun normalizePhone(phone: String): String {
        val d = phone.filter { it.isDigit() }
        return when {
            d.isEmpty() -> ""
            d.startsWith("0") -> d
            d.startsWith("62") -> "0" + d.drop(2)
            else -> "0$d"
        }
    }

    /** Returns a synthetic email used for Firebase Email/Password auth (no OTP needed). */
    private fun pseudoEmail(phone: String): String {
        val n = normalizePhone(phone)
        require(n.startsWith("08") && n.length in 9..14) { "Nomor HP harus diawali 08." }
        return "$n@beruang.phone"
    }
}
