package com.example.healthmate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun StartScreen(
    onLogInButtonClicked: () -> Unit,
    onRegisterButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.width(300.dp)
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_large)))
            Button(
                onClick = onLogInButtonClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.log_in))
            }
            Button(
                onClick = onRegisterButtonClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.register))
            }
        }
    }
}

@Preview
@Composable
fun StartOrderPreview() {
    HealthMateTheme {
        StartScreen(
            onLogInButtonClicked = {},
            onRegisterButtonClicked = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}