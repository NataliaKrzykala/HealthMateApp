package com.example.healthmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.healthmate.R
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.HealthMateApp
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.Uzytkownik
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun LogInScreen(
    bluetoothViewModel: BluetoothViewModel,
    onLogInButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothHandler: BluetoothHandler
){
    val healthMateUiState by bluetoothViewModel.uiState.collectAsState()
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    // Stany do walidacji pól
    val isUsernameValid = bluetoothViewModel.username.isNotBlank() &&
            bluetoothViewModel.username.all { it.isLetterOrDigit() || it in "_-#" } &&  bluetoothViewModel.username.lowercase() != "null" &&
            bluetoothViewModel.username != "0" && bluetoothViewModel.username.length <= 15
    val isPasswordValid = bluetoothViewModel.password.isNotBlank() &&
            bluetoothViewModel.password.all { it.isLetterOrDigit() || it in "!@#*-_?" }  &&  bluetoothViewModel.password.lowercase() != "null" &&
            bluetoothViewModel.password != "0" && bluetoothViewModel.password.length <= 15

    // Przycisk aktywny tylko, gdy wszystkie pola są poprawne
    val isFormValid = isUsernameValid && isPasswordValid


    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogInLayout(
            onUserLoginChanged = { bluetoothViewModel.updateUserLogin(it) },
            onUserPasswordChanged = { bluetoothViewModel.updateUserPassword(it) },
            username = bluetoothViewModel.username,
            password = bluetoothViewModel.password,
            onPasswordVisibilityToggle = { bluetoothViewModel.togglePasswordVisibility() },
            isPasswordVisible = healthMateUiState.isPasswordVisible,
            isWrong = healthMateUiState.areCredentialsWrong,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(mediumPadding),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (bluetoothHandler.isNetworkAvailable()) {
                    bluetoothViewModel.attemptLogin(
                        onSuccess = onLogInButtonClicked,
                        onFailure = {
                            //bluetoothViewModel.resetLoginState()
                        }
                    )
                }
            },
            enabled = isFormValid
        ) {
            Text(text = stringResource(R.string.log_in))
            /*Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()*/
        }
    }
}

@Composable
fun LogInLayout(
    onUserLoginChanged: (String) -> Unit,
    onUserPasswordChanged: (String) -> Unit,
    username: String,
    password: String,
    onPasswordVisibilityToggle: () -> Unit,
    isPasswordVisible: Boolean,
    isWrong: Boolean,
    modifier: Modifier = Modifier
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Column(
        modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Card(
            modifier = Modifier
                .wrapContentSize(Alignment.Center),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(mediumPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(mediumPadding)
            ) {
                Text(
                    text = stringResource(R.string.log_in),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
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
                        if (username.any { !it.isLetterOrDigit() && it !in "_-#" }) {
                            Text(stringResource(R.string.invalid_characters_message))
                        } else {
                            Text(stringResource(R.string.enter_username))
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.username)
                        )
                    },
                    isError = username.any { !it.isLetterOrDigit() && it !in "_-#" },
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
                        if (isWrong) {
                            Text(stringResource(R.string.wrong_password))
                        } else if(password.any { !it.isLetterOrDigit() && it !in "!@#$%^&*-_?" }){
                            Text(stringResource(R.string.invalid_characters_message))
                        } else {
                            Text(stringResource(R.string.enter_password))
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.password)
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onPasswordVisibilityToggle) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    isError = isWrong || password.any { !it.isLetterOrDigit() && it !in "!@#$%^&*-_?" },
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
}

/*@Preview
@Composable
fun LogInPreview() {
    HealthMateTheme {
        LogInScreen(
            onLogInButtonClicked = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}*/

