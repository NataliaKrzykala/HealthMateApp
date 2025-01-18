package com.example.healthmate.data

data class HealthMateUiState (
    val areCredentialsWrong: Boolean = false,
    val loginAlreadyExists: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val device: Urzadzenie = Urzadzenie(1, "null", "null", "null", "null", "null"),
    val user: Uzytkownik = Uzytkownik(imie = "null", login = "null", haslo = "null"),
    //val requiresInternet: Boolean = false
)