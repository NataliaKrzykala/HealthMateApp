package com.example.healthmate.data

data class HealthMateUiState (
    val areCredentialsWrong: Boolean = false,
    val loginAlreadyExists: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val device: Int = 0,
    val rememberedDevices: List<String> = listOf()
)