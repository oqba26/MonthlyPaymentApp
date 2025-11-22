@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.monthlypaymentapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.monthlypaymentapp.core.PaymentApplication
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PaymentApplication
    val viewModel: SettingsViewModel = viewModel(factory = app.settingsViewModelFactory)
    val coroutineScope = rememberCoroutineScope()

    val defaultAmount by viewModel.defaultPaymentAmount.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()

    var backupJsonToRestore by remember { mutableStateOf<String?>(null) }

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
                        Toast.makeText(
                            context,
                            "پشتیبان‌گیری با موفقیت انجام شد",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "خطا در ایجاد فایل پشتیبان",
                            Toast.LENGTH_LONG
                        ).show()
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "تنظیمات عمومی",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            DefaultAmountSetting(
                defaultAmount = defaultAmount,
                onSave = { viewModel.saveDefaultPaymentAmount(it) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            FontSelectionDropdown(
                selectedFont = selectedFont,
                onFontSelected = { viewModel.onFontSelected(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                "پشتیبان‌گیری و بازیابی",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedButton(
                onClick = {
                    val simpleDateFormat =
                        SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                    val fileName = "payment_backup_${simpleDateFormat.format(Date())}.json"
                    backupLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تهیه نسخه پشتیبان")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("بازیابی اطلاعات از فایل")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "خروج"
                )
                Spacer(Modifier.width(8.dp))
                Text("خروج از حساب کاربری")
            }
        }
    }

    backupJsonToRestore?.let { json ->
        AlertDialog(
            onDismissRequest = { backupJsonToRestore = null },
            title = { Text("هشدار جدی!") },
            text = {
                Text(
                    "آیا مطمئن هستید؟ با بازیابی اطلاعات، تمام داده‌های فعلی برنامه (اشخاص و پرداخت‌ها) برای همیشه حذف شده و اطلاعات فایل پشتیبان جایگزین آن خواهد شد. این عمل غیرقابل بازگشت است."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromBackupJson(json)
                        backupJsonToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("بله، بازیابی کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupJsonToRestore = null }) {
                    Text("لغو")
                }
            }
        )
    }
}

@Composable
fun DefaultAmountSetting(
    defaultAmount: Double,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var amountText by remember(defaultAmount, isEditing) {
        mutableStateOf(defaultAmount.toLong().toString())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        AnimatedContent(
            targetState = isEditing,
            label = "EditAmountAnimation",
            modifier = Modifier.padding(12.dp)
        ) { editing ->
            if (editing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { c -> c.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مبلغ پیش فرض پرداخت (تومان)") },
                        visualTransformation = PersianNumberVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("لغو")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onSave(amountText)
                            isEditing = false
                        }) {
                            Text("ذخیره")
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "مبلغ پیش فرض پرداخت",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${formatNumberAsPersian(defaultAmount)} تومان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Amount")
                    }
                }
            }
        }
    }
}

@Composable
fun FontSelectionDropdown(
    selectedFont: String,
    onFontSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fonts = listOf("Estedad", "Vazirmatn", "BYekan", "Sahel", "IranianSans")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("فونت برنامه: $selectedFont")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Font")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            fonts.forEach { fontName ->
                DropdownMenuItem(
                    text = { Text(fontName) },
                    onClick = {
                        onFontSelected(fontName)
                        expanded = false
                    }
                )
            }
        }
    }
}