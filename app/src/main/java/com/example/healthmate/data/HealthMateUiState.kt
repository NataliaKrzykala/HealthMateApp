package com.example.healthmate.data

data class HealthMateUiState (
    val areCredentialsWrong: Boolean = false,
    val loginAlreadyExists: Boolean = false,
    val isPasswordVisible: Boolean = false
)