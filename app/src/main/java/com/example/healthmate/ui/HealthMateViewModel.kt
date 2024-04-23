package com.example.healthmate.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.healthmate.data.HealthMateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HealthMateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HealthMateUiState())
    val uiState: StateFlow<HealthMateUiState> = _uiState.asStateFlow()

    var name by mutableStateOf("")
        private set

    var usernameRegister by mutableStateOf("")
        private set

    var passwordRegister by mutableStateOf("")
        private set

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun updateUserName(nameInput: String){
        name = nameInput
    }

    fun updateUserLogin(usernameInput: String){
        username = usernameInput
    }

    fun updateUserPassword(passwordInput: String){
        password = passwordInput
    }
    fun updateUserLoginRegister(usernameInput: String){
        usernameRegister = usernameInput
    }

    fun updateUserPasswordRegister(passwordInput: String){
        passwordRegister = passwordInput
    }

    fun isAuthenticationWrong() {
        val updatedUiState = if (username.equals("admin") && password.equals("admin")) {
            _uiState.value.copy(areCredentialsWrong = false)
        } else {
            _uiState.value.copy(areCredentialsWrong = true)
        }
        _uiState.value = updatedUiState
    }

    fun isLoginWrong(){
        val updatedUiState = if (usernameRegister.equals("admin")) {
            /*_uiState.update { currentState ->
                currentState.copy(loginAlreadyExists = true)
            }*/
           //HealthMateUiState(loginAlreadyExists = true)
            _uiState.value.copy(loginAlreadyExists = true)
        } else {
            /*_uiState.update { currentState ->
                currentState.copy(loginAlreadyExists = false)
            }*/
           //HealthMateUiState(loginAlreadyExists = false)
            _uiState.value.copy(loginAlreadyExists = false)
        }
        _uiState.value = updatedUiState
    }

    fun togglePasswordVisibility() {
        val updatedUiState = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
        _uiState.value = updatedUiState
    }
}