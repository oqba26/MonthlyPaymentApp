package com.oqba26.monthlypaymentapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.oqba26.monthlypaymentapp.core.manager.BackupManager
import com.oqba26.monthlypaymentapp.core.manager.SnapshotInfo
import com.oqba26.monthlypaymentapp.utils.PersianDigitsTransformation
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianDateTime
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.oqba26.monthlypaymentapp.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val defaultAmount by viewModel.defaultPaymentAmount.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()

    var backupJsonToRestore by remember { mutableStateOf<String?>(null) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var snapshotToRestore by remember { mutableStateOf<SnapshotInfo?>(null) }
    val snapshots by viewModel.snapshots.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        val jsonString = viewModel.createBackupJsonSuspend()
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(jsonString)
                            }
                        }
                        Toast.makeText(context, "پشتیبان‌گیری با موفقیت انجام شد", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "خطا در ایجاد فایل پشتیبان", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            backupJsonToRestore = reader.readText()
                        }
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "خطا در خواندن فایل", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.titleMedium
        )

        DefaultAmountSetting(
            defaultAmount = defaultAmount,
            onSave = { viewModel.saveDefaultPaymentAmount(it) }
        )

        val isOperatorMode by viewModel.isOperatorMode.collectAsState()
        val adminPin by viewModel.adminPin.collectAsState()

        OperatorModeSetting(
            isOperatorMode = isOperatorMode,
            adminPin = adminPin,
            onToggleMode = { viewModel.setOperatorMode(it) },
            onChangePin = { viewModel.setAdminPin(it) }
        )

        val reminderDay by viewModel.reminderDay.collectAsState()
        ReminderDaySetting(
            currentDay = reminderDay,
            onSave = { viewModel.saveReminderDay(it) }
        )

        val cardNumbers by viewModel.cardNumbers.collectAsState()
        CardNumbersSetting(
            cardNumbers = cardNumbers,
            isOperatorMode = isOperatorMode,
            onAdd = { viewModel.addCardNumber(it) },
            onRemove = { viewModel.removeCardNumber(it) }
        )

        FontSelectionDropdown(
            selectedFont = selectedFont,
            onFontSelected = { viewModel.onFontSelected(it) }
        )

        if (!isOperatorMode) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "پشتیبان‌گیری و بازیابی",
                style = MaterialTheme.typography.titleMedium
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                        val fileName = "payment_backup_${simpleDateFormat.format(Date())}.json"
                        backupLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تهیه پشتیبان", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بازیابی فایل", fontSize = 13.sp)
                }
            }

            // تور نجات: نسخه‌هایی که خودِ برنامه قبل از عملیات پرخطر گرفته است.
            OutlinedButton(
                onClick = {
                    viewModel.loadSnapshots()
                    showSnapshotDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auto_backup_button), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.logout))
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showSnapshotDialog) {
        SnapshotListDialog(
            snapshots = snapshots,
            onSelect = { snapshotToRestore = it },
            onDismiss = { showSnapshotDialog = false }
        )
    }

    snapshotToRestore?.let { snapshot ->
        Dialog(onDismissRequest = { snapshotToRestore = null }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.restore_warning),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimestampToPersianDateTime(snapshot.timestamp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.auto_backup_restore_confirm),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.restoreSnapshot(snapshot)
                                    snapshotToRestore = null
                                    showSnapshotDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text(stringResource(R.string.yes)) }

                            Button(onClick = { snapshotToRestore = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }
        }
    }

    backupJsonToRestore?.let { json ->
        Dialog(onDismissRequest = { backupJsonToRestore = null }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.restore_warning),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = stringResource(R.string.restore_warning_text),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.restoreFromBackupJson(json)
                                    backupJsonToRestore = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text(stringResource(R.string.yes)) }

                            Button(onClick = { backupJsonToRestore = null }) { Text(stringResource(R.string.cancel)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * لیست نسخه‌های پشتیبان خودکار.
 *
 * تاریخ‌ها شمسی و اعداد فارسی نمایش داده می‌شوند تا با بقیه‌ی اپ یکدست باشد.
 */
@Composable
private fun SnapshotListDialog(
    snapshots: List<SnapshotInfo>,
    onSelect: (SnapshotInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auto_backup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.auto_backup_description),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (snapshots.isEmpty()) {
                        Text(
                            text = stringResource(R.string.auto_backup_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        snapshots.forEach { snapshot ->
                            OutlinedButton(
                                onClick = { onSelect(snapshot) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = formatTimestampToPersianDateTime(snapshot.timestamp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.auto_backup_item_summary,
                                            snapshot.personCount.toString().toPersianDigits(),
                                            snapshot.paymentCount.toString().toPersianDigits()
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = stringResource(snapshotReasonLabel(snapshot.reason)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}

/** ترجمه‌ی علتِ ذخیره‌شده در نام فایل به متن قابل فهم کاربر. */
private fun snapshotReasonLabel(reason: String): Int = when (reason) {
    BackupManager.REASON_BEFORE_SYNC -> R.string.auto_backup_reason_before_sync
    BackupManager.REASON_BEFORE_RESTORE -> R.string.auto_backup_reason_before_restore
    BackupManager.REASON_APP_UPGRADE -> R.string.auto_backup_reason_app_upgrade
    else -> R.string.auto_backup_reason_unknown
}

@Composable
fun DefaultAmountSetting(defaultAmount: Double, onSave: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var amountText by remember(defaultAmount, isEditing) { mutableStateOf(defaultAmount.toLong().toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        AnimatedContent(targetState = isEditing, label = "EditAmount") { editing ->
            if (editing) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { value ->
                            val digitsOnly = value.filter { it.isDigit() || it in '\u0660'..'\u0669' || it in '\u06f0'..'\u06f9' }
                            amountText = digitsOnly.map { 
                                when (it) {
                                    in '\u0660'..'\u0669' -> (it.code - '\u0660'.code + '0'.code).toChar()
                                    in '\u06f0'..'\u06f9' -> (it.code - '\u06f0'.code + '0'.code).toChar()
                                    else -> it
                                }
                            }.joinToString("")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مبلغ پیش‌فرض (تومان)") },
                        visualTransformation = PersianNumberVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { isEditing = false }) { Text("لغو") }
                        Button(onClick = { onSave(amountText); isEditing = false }) { Text("ذخیره") }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مبلغ پیش فرض پرداخت", style = MaterialTheme.typography.bodyLarge)
                        Text("${formatNumberAsPersian(defaultAmount)} تومان", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { isEditing = true }) { Icon(Icons.Filled.Edit, null) }
                }
            }
        }
    }
}

@Composable
fun ReminderDaySetting(currentDay: Int?, onSave: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var dayText by remember(currentDay, isEditing) { mutableStateOf(currentDay?.toString() ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        AnimatedContent(targetState = isEditing, label = "EditReminder") { editing ->
            if (editing) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it.filter { c -> c.isDigit() }.take(2) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("روز یادآور (۱ تا ۳۱)") },
                        visualTransformation = PersianDigitsTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { isEditing = false }) { Text("لغو") }
                        Button(onClick = { onSave(dayText); isEditing = false }) { Text("ذخیره") }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("روز یادآور ماهانه", style = MaterialTheme.typography.bodyLarge)
                        Text(if (currentDay != null) "روز ${currentDay.toString().toPersianDigits()} هر ماه" else "غیرفعال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { isEditing = true }) { Icon(Icons.Filled.Edit, null) }
                }
            }
        }
    }
}

@Composable
fun CardNumbersSetting(
    cardNumbers: List<String>,
    isOperatorMode: Boolean = false,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newCardText by remember { mutableStateOf("") }
    var cardToRemove by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("شماره کارت‌های بانکی", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (!isOperatorMode) {
                    IconButton(onClick = { isAdding = true }) { Icon(Icons.Default.Add, null) }
                }
            }
            if (isAdding && !isOperatorMode) {
                OutlinedTextField(
                    value = newCardText,
                    onValueChange = { newCardText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("شماره ۱۶ رقمی") },
                    visualTransformation = PersianDigitsTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { isAdding = false; newCardText = "" }) { Text("لغو") }
                    Button(onClick = { if (newCardText.length >= 16) { onAdd(newCardText); isAdding = false; newCardText = "" } }) { Text("افزودن") }
                }
            }
            cardNumbers.forEach { card ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(card))
                            Toast.makeText(context, "شماره کارت کپی شد", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        card.toPersianDigits(),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(card))
                        Toast.makeText(context, "شماره کارت کپی شد", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی شماره کارت")
                    }
                    if (!isOperatorMode) {
                        IconButton(onClick = { cardToRemove = card }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    cardToRemove?.let { card ->
        ConfirmDeleteDialog(
            title = "حذف شماره کارت",
            message = "آیا از حذف شماره کارت ${card.toPersianDigits()} مطمئن هستید؟",
            onConfirm = {
                onRemove(card)
                cardToRemove = null
            },
            onDismiss = { cardToRemove = null }
        )
    }
}

@Composable
fun OperatorModeSetting(
    isOperatorMode: Boolean,
    adminPin: String,
    onToggleMode: (Boolean) -> Unit,
    onChangePin: (String) -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf<PinAction?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOperatorMode) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOperatorMode) Icons.Default.Lock else Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (isOperatorMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "حالت دسترسی برنامه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOperatorMode) "وضعیت فعلی: حالت اپراتور (محدود)" else "وضعیت فعلی: حالت مدیریت (کامل)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOperatorMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            if (isOperatorMode) {
                Button(
                    onClick = {
                        pinAction = PinAction.UNLOCK_ADMIN
                        enteredPin = ""
                        errorMessage = ""
                        showPinDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروج از حالت اپراتور (ورود رمز مدیر)")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onToggleMode(true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("فعال‌سازی حالت اپراتور", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            pinAction = PinAction.CHANGE_PIN
                            enteredPin = ""
                            newPinInput = ""
                            errorMessage = ""
                            showPinDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تغییر رمز مدیر", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        Dialog(onDismissRequest = { showPinDialog = false }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (pinAction == PinAction.UNLOCK_ADMIN) "ورود رمز مدیریت" else "تغییر رمز مدیریت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (pinAction == PinAction.UNLOCK_ADMIN || pinAction == PinAction.CHANGE_PIN) {
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = { enteredPin = it.filter { c -> c.isDigit() } },
                                label = { Text(if (pinAction == PinAction.CHANGE_PIN) "رمز فعلی" else "رمز عبور مدیر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (pinAction == PinAction.CHANGE_PIN) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it.filter { c -> c.isDigit() } },
                                label = { Text("رمز جدید") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (errorMessage.isNotBlank()) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showPinDialog = false }) { Text("لغو") }
                            Button(onClick = {
                                if (pinAction == PinAction.UNLOCK_ADMIN) {
                                    if (enteredPin == adminPin) {
                                        onToggleMode(false)
                                        showPinDialog = false
                                    } else {
                                        errorMessage = "رمز عبور اشتباه است"
                                    }
                                } else if (pinAction == PinAction.CHANGE_PIN) {
                                    if (enteredPin != adminPin) {
                                        errorMessage = "رمز فعلی اشتباه است"
                                    } else if (newPinInput.length < 4) {
                                        errorMessage = "رمز جدید باید حداقل ۴ رقم باشد"
                                    } else {
                                        onChangePin(newPinInput)
                                        showPinDialog = false
                                    }
                                }
                            }) { Text("تأیید") }
                        }
                    }
                }
            }
        }
    }
}

private enum class PinAction { UNLOCK_ADMIN, CHANGE_PIN }

@Composable
fun FontSelectionDropdown(selectedFont: String, onFontSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val fonts = listOf("Estedad", "Vazirmatn", "BYekan", "Sahel", "IranianSans")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("فونت برنامه: $selectedFont", fontSize = 14.sp)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fonts.forEach { fontName ->
                DropdownMenuItem(text = { Text(fontName) }, onClick = { onFontSelected(fontName); expanded = false })
            }
        }
    }
}
