package com.example.healthmate.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.R
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun RegisterScreen(
    bluetoothViewModel: BluetoothViewModel,
    onSubmitButtonClicked: () -> Unit,
    bluetoothHandler: BluetoothHandler,
    modifier: Modifier = Modifier
){
    val healthMateUiState by bluetoothViewModel.uiState.collectAsState()
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    // Stany do walidacji pól
    val isNameValid = bluetoothViewModel.name.isNotBlank() && bluetoothViewModel.name.all { it.isLetter() } && bluetoothViewModel.name.lowercase() != "null" && bluetoothViewModel.name.length <= 15
    val isUsernameValid = bluetoothViewModel.usernameRegister.isNotBlank() &&
            bluetoothViewModel.usernameRegister.all { it.isLetterOrDigit() || it in "_-#" } &&  bluetoothViewModel.usernameRegister.lowercase() != "null" &&
            bluetoothViewModel.usernameRegister != "0" && bluetoothViewModel.usernameRegister.length <= 15
    val isPasswordValid = bluetoothViewModel.passwordRegister.isNotBlank() &&
            bluetoothViewModel.passwordRegister.all { it.isLetterOrDigit() || it in "!@#*-_?" }  &&  bluetoothViewModel.passwordRegister.lowercase() != "null" &&
            bluetoothViewModel.passwordRegister != "0" && bluetoothViewModel.passwordRegister.length <= 15

    // Przycisk aktywny tylko, gdy wszystkie pola są poprawne
    val isFormValid = isNameValid && isUsernameValid && isPasswordValid

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegisterLayout(
            onUserNameChanged = { bluetoothViewModel.updateUserName(it) },
            onUserLoginChanged = { bluetoothViewModel.updateUserLoginRegister(it) },
            onUserPasswordChanged = { bluetoothViewModel.updateUserPasswordRegister(it) },
            name = bluetoothViewModel.name,
            username = bluetoothViewModel.usernameRegister,
            password = bluetoothViewModel.passwordRegister,
            onPasswordVisibilityToggle = { bluetoothViewModel.togglePasswordVisibility() },
            isPasswordVisible = healthMateUiState.isPasswordVisible,
            isWrong = healthMateUiState.loginAlreadyExists,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(mediumPadding),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (bluetoothHandler.isNetworkAvailable()) {
                    bluetoothViewModel.attemptRegistration(
                        onSuccess = onSubmitButtonClicked,
                        onFailure = {
                            Log.e("Register", "User already EXISTS!!!")
                        }
                    )
                }
            },
            enabled = isFormValid
        ) {
            Text(text = stringResource(R.string.register))
            /*Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()*/
        }
    }
}

@Composable
fun RegisterLayout(
    onUserNameChanged: (String) -> Unit,
    onUserLoginChanged: (String) -> Unit,
    onUserPasswordChanged: (String) -> Unit,
    name: String,
    username: String,
    password: String,
    onPasswordVisibilityToggle: () -> Unit,
    isPasswordVisible: Boolean,
    /*onKeyboardDone: () -> Unit,*/
    isWrong: Boolean,
    modifier: Modifier = Modifier
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(mediumPadding)
        ) {
            Text(
                text = stringResource(R.string.register),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = name,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                onValueChange = { input ->
                    if (input.all { it.isLetter() }) {
                        onUserNameChanged(input)
                    } else {
                        onUserNameChanged(input)
                    }
                },

                label = {
                    if (name.any { !it.isLetter() }) {
                        Text(stringResource(R.string.invalid_characters_message))
                    } else {
                        Text(stringResource(R.string.enter_name))
                    }
                },

//                label = {
//                    /*if (isWrong) {
//                        Text(stringResource(R.string.wrong_username))
//                    } else {*/
//                    Text(stringResource(R.string.enter_name))
//                    /*}*/
//                },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = stringResource(
                    R.string.name)
                )
                },
                isError = name.any { !it.isLetter() },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
                /*keyboardActions = KeyboardActions(
                    onDone = { }
                )*/
            )
            OutlinedTextField(
                value = username,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                onValueChange = { input ->
                    if (input.all { it.isLetterOrDigit() || it in "_-#" }) {
                        onUserLoginChanged(input)
                    } else {
                        onUserLoginChanged(input)
                    }
                },
                label = {
                    if (isWrong) {
                        Text(stringResource(R.string.wrong_username))
                    } else if (username.any { !it.isLetterOrDigit() && it !in "_-#" }) {
                        Text(stringResource(R.string.invalid_characters_message))
                    } else {
                        Text(stringResource(R.string.create_login))
                    }
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = stringResource(
                    R.string.username)
                )
                },
                isError = isWrong || username.any { !it.isLetterOrDigit() && it !in "_-#" },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
                /*keyboardActions = KeyboardActions(
                    onDone = { }
                )*/
            )
            OutlinedTextField(
                value = password,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                onValueChange = { input ->
                    if (input.all { it.isLetterOrDigit() || it in "!@#$%^&*-_?" }) {
                        onUserPasswordChanged(input)
                    } else {
                        onUserPasswordChanged(input) // Wciąż zmieniamy wartość, aby użytkownik widział wprowadzony tekst
                    }
                },
                label = {
                    if (password.any { !it.isLetterOrDigit() && it !in "!@#$%^&*-_?" }) {
                        Text(stringResource(R.string.invalid_characters_message))
                    } else {
                        Text(stringResource(R.string.create_password))
                    }
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = stringResource(
                    R.string.password)
                )
                },
               /* visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },*/
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                //visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                isError = password.any { !it.isLetterOrDigit() && it !in "!@#$%^&*-_?" },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
                /*keyboardActions = KeyboardActions(
                    onDone = { }
                )*/
            )

        }
    }
}

/*@Preview
@Composable
fun RegisterPreview() {
    HealthMateTheme {
        RegisterScreen(
            onSubmitButtonClicked = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}*/