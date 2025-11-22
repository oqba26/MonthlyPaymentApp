@file:OptIn(ExperimentalMaterial3Api::class)

package com.oqba26.monthlypaymentapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oqba26.monthlypaymentapp.core.PaymentApplication
import com.oqba26.monthlypaymentapp.ui.screens.ArchiveScreen
import com.oqba26.monthlypaymentapp.ui.screens.AuthScreen
import com.oqba26.monthlypaymentapp.ui.screens.PaidListScreen
import com.oqba26.monthlypaymentapp.ui.screens.PersonDetailScreen
import com.oqba26.monthlypaymentapp.ui.screens.PersonScreen
import com.oqba26.monthlypaymentapp.ui.screens.SettingsScreen
import com.oqba26.monthlypaymentapp.ui.theme.MonthlyPaymentManagement2Theme
import com.oqba26.monthlypaymentapp.viewmodel.AuthState
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = application as PaymentApplication
            val personViewModel: PersonViewModel =
                viewModel(factory = application.personViewModelFactory)

            MonthlyPaymentManagement2Theme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainAppHost(personViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: PersonViewModel) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Crossfade(targetState = authState, label = "AuthScreenSwitch") { state ->
        when (state) {
            is AuthState.Loading -> LoadingScreen()
            is AuthState.Unauthenticated -> AuthScreen()
            is AuthState.Authenticated -> AuthenticatedContent(viewModel)
        }
    }
}


@Composable
fun AuthenticatedContent(viewModel: PersonViewModel) {
    val navController = rememberNavController()
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val showBottomBar = currentRoute in listOf(
        Screen.Unpaid.route,
        Screen.Paid.route,
        Screen.Archive.route,
        Screen.Settings.route
    )

    // هندل کردن دکمه Back
    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitDialog(
            onConfirmExit = { (context as? ComponentActivity)?.finish() },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showExitDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت حقوق ماهانه") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    if (currentRoute == Screen.Unpaid.route) {
                        Button(
                            onClick = { viewModel.onAddPersonClicked() },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("افزودن شخص")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Unpaid.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Unpaid.route) {
                PersonScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable(Screen.Paid.route) {
                PaidListScreen(viewModel, navController)
            }
            composable(Screen.Archive.route) {
                ArchiveScreen(viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onLogout = { viewModel.logout() })
            }
            composable(
                route = "person_detail/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.StringType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId")
                if (personId != null) {
                    PersonDetailScreen(
                        personId = personId,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Unpaid,
        Screen.Paid,
        Screen.Archive,
        Screen.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary // Match TopAppBar
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedTextColor = Color.White.copy(alpha = 0.7f),
                    unselectedIconColor = Color.White.copy(alpha = 0.7f)
                ),
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Unpaid : Screen("unpaid_list", "پرداخت نشده", Icons.AutoMirrored.Filled.List)
    data object Paid : Screen("paid_list", "پرداخت شده", Icons.Default.Paid)
    data object Archive : Screen("archive", "آرشیو", Icons.Default.Archive)
    data object Settings : Screen("settings", "تنظیمات", Icons.Default.Settings)
}

@Composable
fun ExitDialog(onConfirmExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خروج از برنامه") },
        text = { Text("آیا برای خروج از برنامه مطمئن هستید؟") },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("بله")
                }
                Button(onClick = onDismiss) {
                    Text("خیر")
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("در حال بررسی وضعیت...", modifier = Modifier.padding(top = 16.dp))
        }
    }
}