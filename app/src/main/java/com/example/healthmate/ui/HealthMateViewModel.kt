package com.example.healthmate.ui

import androidx.lifecycle.ViewModel
import com.example.healthmate.data.HealthMateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HealthMateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HealthMateUiState())
    val uiState: StateFlow<HealthMateUiState> = _uiState.asStateFlow()

    fun resetViewModel() {
        _uiState.value = HealthMateUiState()
    }
}