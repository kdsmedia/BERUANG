package com.altomedia.beruang.ui.profile

import androidx.lifecycle.ViewModel
import com.altomedia.beruang.data.repo.AuthRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lightweight session holder giving composables access to the current uid. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    val uid: StateFlow<String?> = MutableStateFlow(profileRepo.currentUid)
    fun sync() { (uid as MutableStateFlow).value = profileRepo.currentUid }

    /** Signs out and clears cached profile data so a subsequent login starts clean. */
    fun signOut() {
        profileRepo.clearCache()
        MainScope().launch { runCatching { authRepo.signOut() } }
        (uid as MutableStateFlow).value = null
    }
}
