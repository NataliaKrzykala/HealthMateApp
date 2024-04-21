package com.example.healthmate.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.healthmate.data.HealthMateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthMateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HealthMateUiState())
    val uiState: StateFlow<HealthMateUiState> = _uiState.asStateFlow()

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun isAuthenticationWrong() {
        val updatedUiState = if (username.equals("admin") && password.equals("admin")) {
            _uiState.value.copy(areCredentialsWrong = false)
        } else {
            _uiState.value.copy(areCredentialsWrong = true)
        }
        _uiState.value = updatedUiState
    }
}