package com.altomedia.beruang.ui.profile

import androidx.lifecycle.ViewModel
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Lightweight session holder giving composables access to the current uid. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val profileRepo: ProfileRepository
) : ViewModel() {
    val uid: StateFlow<String?> = MutableStateFlow(profileRepo.currentUid)
    fun sync() { (uid as MutableStateFlow).value = profileRepo.currentUid }

    /** Signs out and clears cached profile data so a subsequent login starts clean. */
    fun signOut() {
        profileRepo.clearCache()
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        (uid as MutableStateFlow).value = null
    }
}
