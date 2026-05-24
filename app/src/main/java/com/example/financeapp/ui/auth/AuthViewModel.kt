package com.example.financeapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.sync.FirestoreSyncService
import com.example.financeapp.domain.model.User
import com.example.financeapp.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val syncService: FirestoreSyncService,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    fun isSignedIn(): Boolean = authRepository.isSignedIn()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            authRepository.signIn(email.trim(), password).fold(
                onSuccess = { _authUiState.value = AuthUiState.Success(it) },
                onFailure = { _authUiState.value = AuthUiState.Error(it.message ?: "Sign in failed") },
            )
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            authRepository.signUp(email.trim(), password, displayName.trim()).fold(
                onSuccess = { _authUiState.value = AuthUiState.Success(it) },
                onFailure = { _authUiState.value = AuthUiState.Error(it.message ?: "Registration failed") },
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { _authUiState.value = AuthUiState.Success(it) },
                onFailure = { _authUiState.value = AuthUiState.Error(it.message ?: "Google sign-in failed") },
            )
        }
    }

    /**
     * Signs the current user out and immediately wipes the local Room cache.
     *
     * Clearing Room on sign-out is the second half of the session-isolation
     * strategy (the first half is the clear at the start of [FirestoreSyncService.syncAll]).
     * Without this call, the just-signed-out user's financial data would remain
     * in Room's SQLite file on the device, visible to whoever signs in next.
     */
    fun signOut() {
        viewModelScope.launch {
            // Wipe local cache BEFORE navigating away so the auth screens
            // never briefly show the previous user's data.
            syncService.clearAllUserData()
            authRepository.signOut()
            _authUiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_authUiState.value is AuthUiState.Error) {
            _authUiState.value = AuthUiState.Idle
        }
    }
}
