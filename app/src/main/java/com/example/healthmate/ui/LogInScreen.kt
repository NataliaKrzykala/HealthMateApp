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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.HealthMateApp
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun LogInScreen(
    healthMateViewModel: HealthMateViewModel = viewModel(),
    onLogInButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
){
    val healthMateUiState by healthMateViewModel.uiState.collectAsState()
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val isWrongCheck = healthMateUiState.areCredentialsWrong

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
            onUserLoginChanged = { healthMateViewModel.updateUserLogin(it) },
            onUserPasswordChanged = { healthMateViewModel.updateUserPassword(it) },
            onLogInButtonClicked = {healthMateViewModel.isAuthenticationWrong()},
            username = healthMateViewModel.username,
            password = healthMateViewModel.password,
            /*onKeyboardDone = { healthMateViewModel.checkUserGuess() },*/
            isWrong = healthMateUiState.areCredentialsWrong,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(mediumPadding),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                healthMateViewModel.isAuthenticationWrong()
                if (!healthMateUiState.areCredentialsWrong) {
                    // Navigate to another page or perform any action on successful login
                    onLogInButtonClicked()
                } else {
                    // Handle incorrect credentials case, if needed
                }
            }
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
    onLogInButtonClicked: (String) -> Unit,
    username: String,
    password: String,
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
                onValueChange = onUserLoginChanged,
                label = {
                    /*if (isWrong) {
                        Text(stringResource(R.string.wrong_username))
                    } else {*/
                        Text(stringResource(R.string.enter_username))
                    /*}*/
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = stringResource(R.string.username))},
                /*isError = isWrong,*/
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
                onValueChange = onUserPasswordChanged,
                label = {
                    if (isWrong) {
                        Text(stringResource(R.string.wrong_password))
                    } else {
                        Text(stringResource(R.string.enter_password))
                    }
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = stringResource(R.string.password))},
                visualTransformation = PasswordVisualTransformation(),
                isError = isWrong,
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

@Preview
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
}

