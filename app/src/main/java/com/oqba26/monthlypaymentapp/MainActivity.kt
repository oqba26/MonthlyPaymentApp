@file:OptIn(ExperimentalMaterial3Api::class)
package com.oqba26.monthlypaymentapp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oqba26.monthlypaymentapp.ui.components.PermissionsDialog
import com.oqba26.monthlypaymentapp.ui.components.UpdateDialog
import com.oqba26.monthlypaymentapp.ui.screens.ArchiveScreen
import com.oqba26.monthlypaymentapp.ui.screens.AuthScreen
import com.oqba26.monthlypaymentapp.ui.screens.PaidListScreen
import com.oqba26.monthlypaymentapp.ui.screens.PersonDetailScreen
import com.oqba26.monthlypaymentapp.ui.screens.PersonScreen
import com.oqba26.monthlypaymentapp.ui.screens.SettingsScreen
import com.oqba26.monthlypaymentapp.ui.theme.MonthlyPaymentManagement2Theme
import com.oqba26.monthlypaymentapp.utils.UpdateInfo
import com.oqba26.monthlypaymentapp.utils.UpdateManager
import com.oqba26.monthlypaymentapp.viewmodel.ContactViewModel
import com.oqba26.monthlypaymentapp.viewmodel.AuthViewModel
import com.oqba26.monthlypaymentapp.viewmodel.AuthState
import com.oqba26.monthlypaymentapp.viewmodel.PersonListType
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val personViewModel: PersonViewModel = hiltViewModel()

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
    val authViewModel: AuthViewModel = hiltViewModel()
    val contactViewModel: ContactViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Update and Permission States
    val updateManager = remember { UpdateManager(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            showPermissionsDialog = false
        }
    }

    LaunchedEffect(Unit) {
        delay(2000.milliseconds)
        updateInfo = updateManager.checkForUpdate()
        
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            showPermissionsDialog = true
        }
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.toastMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        launch {
            viewModel.infoMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // UI elements for update and permissions
    if (showPermissionsDialog) {
        PermissionsDialog(
            onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
            onExitClick = { (context as? ComponentActivity)?.finish() }
        )
    }

    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            onDismiss = { updateInfo = null },
            onConfirm = {
                val id = updateManager.downloadAndInstall(info.url, "MonthlyPaymentApp_v${info.versionName}.apk")
                if (id != -1L) {
                    isDownloading = true
                    scope.launch {
                        updateManager.getDownloadProgress(id).collect { progress ->
                            downloadProgress = progress
                            if (progress >= 1f) isDownloading = false
                        }
                    }
                    updateInfo = null
                }
            }
        )
    }

    if (isDownloading) {
        Dialog(onDismissRequest = { }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "در حال دریافت به‌روزرسانی",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // استفاده از when ساده به جای Crossfade برای جلوگیری از پرش‌های لحظه‌ای
    when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.Unauthenticated -> AuthScreen()
        is AuthState.Authenticated -> AuthenticatedContent(viewModel, contactViewModel)
    }
}


@Composable
fun AuthenticatedContent(viewModel: PersonViewModel, contactViewModel: ContactViewModel) {
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
                showExitDialog = false
            }
        )
    }

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    var showConfirmMoveDialog by remember { mutableStateOf(false) }

    if (showConfirmMoveDialog) {
        Dialog(
            onDismissRequest = { showConfirmMoveDialog = false }
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "تایید نهایی جابجایی",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "آیا از ثبت تغییرات در ترتیب افراد مطمئن هستید؟",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val listType = when (navController.currentBackStackEntry?.destination?.route) {
                                        Screen.Paid.route -> PersonListType.PAID
                                        else -> PersonListType.UNPAID
                                    }
                                    viewModel.onEvent(PersonScreenEvent.CommitReorder(listType))
                                    viewModel.onEvent(PersonScreenEvent.ClearSelection)
                                    showConfirmMoveDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("بله، ذخیره کن")
                            }

                            Button(
                                onClick = { showConfirmMoveDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("لغو")
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when {
                        isSelectionMode -> "انتخاب شده‌ها"
                        currentRoute == Screen.Settings.route -> "تنظیمات"
                        else -> "مدیریت حقوق ماهانه"
                    }
                    Text(titleText)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelectionMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                    actionIconContentColor = if (isSelectionMode) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                ),
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.onEvent(PersonScreenEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val listType = when (currentRoute) {
                            Screen.Paid.route -> PersonListType.PAID
                            else -> PersonListType.UNPAID
                        }
                        IconButton(onClick = { viewModel.onEvent(PersonScreenEvent.MoveSelected(-1, listType)) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                        }
                        IconButton(onClick = { viewModel.onEvent(PersonScreenEvent.MoveSelected(1, listType)) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                        }
                        IconButton(onClick = { showConfirmMoveDialog = true }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm Move", tint = Color.Green)
                        }
                    } else if (currentRoute == Screen.Unpaid.route) {
                        FilledIconButton(
                            onClick = { viewModel.onAddPersonClicked() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن شخص")
                        }
                        FilledIconButton(
                            onClick = { contactViewModel.onBulkSmsClicked() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Bulk SMS",
                                modifier = Modifier.graphicsLayer(scaleX = -1f)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(navController = navController)
            }
        },
        /* Floating Action Button removed and moved to TopAppBar actions */
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Unpaid.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Unpaid.route) {
                PersonScreen(
                    viewModel = viewModel,
                    contactViewModel = contactViewModel,
                    navController = navController
                )
            }
            composable(Screen.Paid.route) {
                PaidListScreen(
                    viewModel = viewModel,
                    contactViewModel = contactViewModel,
                    navController = navController
                )
            }
            composable(Screen.Archive.route) {
                ArchiveScreen(viewModel)
            }
            composable(Screen.Settings.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                SettingsScreen(onLogout = { authViewModel.logout() })
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
                        contactViewModel = contactViewModel,
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
    Dialog(
        onDismissRequest = onDismiss
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.exit_app),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.exit_app_confirm),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onConfirmExit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.no))
                        }
                    }
                }
            }
        }
    }
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
