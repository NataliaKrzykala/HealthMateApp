package com.example.healthmate

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthmate.R
import com.example.healthmate.ui.AccountScreen
import com.example.healthmate.ui.HealthMateViewModel
import com.example.healthmate.ui.LogInScreen
import com.example.healthmate.ui.MainPanelScreen
import com.example.healthmate.ui.RegisterScreen
import com.example.healthmate.ui.StartScreen
import com.example.healthmate.ui.SettingsScreen


enum class HealthMateScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    LogIn(title = R.string.log_in),
    Register(title = R.string.register),
    MainPanel(title = R.string.view_main_panel),
    Statistics(title = R.string.view_statistics),
    Settings(title = R.string.settings),
    Account(title = R.string.view_account),
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthMateAppBar(
    currentScreen: HealthMateScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(text = stringResource(currentScreen.title), style = MaterialTheme.typography.displayMedium) },
        /*title = { /* Your empty Text composable */ },*/
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        } ,
        actions = {
            IconButton(onClick = navigateToSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    )
}

@Composable
fun HealthMateApp(
    viewModel: HealthMateViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = HealthMateScreen.valueOf(
        backStackEntry?.destination?.route ?: HealthMateScreen.Start.name
    )

    Scaffold(
        topBar = {
            HealthMateAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                navigateToSettings = { navController.navigate(HealthMateScreen.Settings.name) }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        NavHost(
            navController = navController,
            startDestination = HealthMateScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(route = HealthMateScreen.Start.name) {
                StartScreen(
                    onLogInButtonClicked = { navController.navigate(HealthMateScreen.LogIn.name) },
                    onRegisterButtonClicked = { navController.navigate(HealthMateScreen.Register.name) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
            composable(route = HealthMateScreen.LogIn.name) {
                LogInScreen(
                    onLogInButtonClicked = { navController.navigate(HealthMateScreen.MainPanel.name) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = HealthMateScreen.Register.name) {
                RegisterScreen(
                    onSubmitButtonClicked = { navController.navigate(HealthMateScreen.MainPanel.name) },
                    /*onCancelButtonClicked = { cancelAndNavigateToStart(viewModel, navController) },*/
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = HealthMateScreen.MainPanel.name) {
                MainPanelScreen(
                    onStatisticsButtonClicked = { navController.navigate(HealthMateScreen.Statistics.name) },
                    onAccountButtonClicked = { navController.navigate(HealthMateScreen.Account.name) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = HealthMateScreen.Account.name) {
                AccountScreen(
                    onLogOutButtonClicked = { cancelAndNavigateToStart(viewModel, navController) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = HealthMateScreen.Settings.name) {
                SettingsScreen(
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }

    }
}



private fun cancelAndNavigateToStart(
    viewModel: HealthMateViewModel,
    navController: NavHostController
) {
    /*viewModel.resetViewModel()*/
    navController.popBackStack(HealthMateScreen.Start.name, inclusive = false)
}