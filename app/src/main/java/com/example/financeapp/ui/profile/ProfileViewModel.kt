package com.example.financeapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.local.SettingsRepository
import com.example.financeapp.domain.model.UserProfile
import com.example.financeapp.domain.repository.IAuthRepository
import com.example.financeapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val authRepository: IAuthRepository,
) : ViewModel() {
    private val userProfile = MutableStateFlow<UserProfile?>(null)
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) {
                    userProfile.value = null
                    isLoading.value = false
                    errorMessage.value = null
                } else {
                    isLoading.value = true
                    errorMessage.value = null
                    runCatching { userRepository.getUserDetails(user.uid) }
                        .onSuccess { profile ->
                            userProfile.value = profile
                            profile?.let {
                                viewModelScope.launch {
                                    settingsRepository.setSelectedAvatarId(it.avatarId)
                                }
                            }
                        }
                        .onFailure { error ->
                            errorMessage.value = error.message ?: "Unable to load profile"
                        }
                    isLoading.value = false
                }
            }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                userProfile,
                isLoading,
                errorMessage,
            ) { settings, profile, loading, error ->
                ProfileUiState(
                    uid = profile?.uid.orEmpty(),
                    displayName = profile?.displayName.orEmpty(),
                    email = profile?.email.orEmpty(),
                    memberSince = profile?.memberSince ?: 0L,
                    isDarkMode = settings.isDarkMode,
                    language = settings.language,
                    biometricsEnabled = settings.biometricsEnabled,
                    selectedAvatarId = settings.selectedAvatarId,
                    isLoading = loading,
                    errorMessage = error,
                )
            }.collect { _state.value = it }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricsEnabled(enabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun updateAvatar(newAvatarId: String) {
        viewModelScope.launch {
            // Local update for instant UI feedback
            settingsRepository.setSelectedAvatarId(newAvatarId)

            // Cloud update
            val uid = state.value.uid
            if (uid.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { userRepository.updateAvatar(uid, newAvatarId) }
                        .onFailure { error ->
                            // Optional: handle failure (e.g., revert local or show error)
                        }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsRepository.clearUserScopedSettings()
            authRepository.signOut()
        }
    }
}

data class ProfileUiState(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val memberSince: Long = 0L,
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val biometricsEnabled: Boolean = false,
    val selectedAvatarId: String = "avatar_1",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
