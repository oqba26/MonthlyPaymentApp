package com.oqba26.monthlypaymentapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.oqba26.monthlypaymentapp.utils.PersianDigitsTransformation
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import com.oqba26.monthlypaymentapp.utils.getPersianMonthName
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.ContactMatch
import com.oqba26.monthlypaymentapp.viewmodel.ContactSuggestion
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel

@Composable
fun ArchiveConfirmationDialog(personName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "انتقال به آرشیو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = "آیا از انتقال $personName به آرشیو مطمئن هستید؟", style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onDismiss) { Text("لغو") }
                        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Text("آرشیو")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactSuggestionDialog(
    suggestion: ContactSuggestion,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedNumber by remember { mutableStateOf(suggestion.matches.firstOrNull()?.phoneNumber ?: "") }

    Dialog(onDismissRequest = onDismiss) {
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
                        text = "شماره تماس پیدا شد",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        Text("برای «${suggestion.personName}» موارد زیر در مخاطبین پیدا شد:")
                        Spacer(Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestion.matches) { match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedNumber = match.phoneNumber }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedNumber == match.phoneNumber,
                                        onClick = { selectedNumber = match.phoneNumber }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = match.nameInContacts,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = match.phoneNumber.toPersianDigits(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("آیا می‌خواهید شماره انتخاب شده به اطلاعات شخص اضافه شود؟")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onConfirm(selectedNumber) },
                            enabled = selectedNumber.isNotEmpty()
                        ) {
                            Text("بله، اضافه کن")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("خیر")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddNewPersonDialog(
    isMosqueCategory: Boolean = false,
    onConfirm: (String, String, Boolean, Double, Int, Int, Double) -> Unit,
    onDismiss: () -> Unit,
    onSearchContact: (String) -> List<ContactMatch>
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(value = false) }
    var monthlyCommitment by remember { mutableStateOf("") }
    var initialPaymentAmount by remember { mutableStateOf("") }

    val currentShamsiYear = getCurrentShamsiYear()
    val currentShamsiMonth = getCurrentShamsiMonth()

    var startMonth by remember { mutableIntStateOf(if (isMosqueCategory) currentShamsiMonth else 1) }
    var startYear by remember { mutableIntStateOf(currentShamsiYear) }

    var similarContacts by remember { mutableStateOf<List<ContactMatch>>(emptyList()) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val matches = onSearchContact(name)
            if (matches.isNotEmpty()) {
                similarContacts = matches
            } else {
                Toast.makeText(context, "مخاطبی با این نام پیدا نشد", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "دسترسی به مخاطبین داده نشد", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                        text = if (isAnonymous) "ثبت کمک آنی" else "افزودن شخص جدید",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (isMosqueCategory) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAnonymous = !isAnonymous }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isAnonymous,
                                onCheckedChange = { isAnonymous = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ثبت به صورت خیر ناشناس")
                        }
                    }

                    if (!isAnonymous) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("نام شخص") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("شماره موبایل (اختیاری)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        visualTransformation = PersianDigitsTransformation(),
                                        placeholder = {
                                            Text(
                                                "۰۹---------",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (name.isBlank()) {
                                            Toast.makeText(context, "ابتدا نام را وارد کنید", Toast.LENGTH_SHORT)
                                                .show()
                                            return@IconButton
                                        }
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_CONTACTS
                                            ) -> {
                                                val matches = onSearchContact(name)
                                                if (matches.isNotEmpty()) {
                                                    similarContacts = matches
                                                } else {
                                                    Toast.makeText(context, "مخاطبی با این نام پیدا نشد", Toast.LENGTH_SHORT)
                                                        .show()
                                                }
                                            }

                                            else -> {
                                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "جستجو در مخاطبین")
                                }
                            }

                            if (similarContacts.isNotEmpty()) {
                                Text(
                                    "موارد مشابه پیدا شده:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    items(similarContacts) { match ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    phoneNumber = match.phoneNumber
                                                    similarContacts = emptyList()
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = phoneNumber == match.phoneNumber,
                                                onClick = {
                                                    phoneNumber = match.phoneNumber
                                                    similarContacts = emptyList()
                                                }
                                            )
                                            Column {
                                                Text(match.nameInContacts, style = MaterialTheme.typography.bodySmall)
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        match.phoneNumber.toPersianDigits(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isMosqueCategory) {
                        if (isAnonymous) {
                            OutlinedTextField(
                                value = initialPaymentAmount,
                                onValueChange = { value ->
                                    if (value.length <= 12) {
                                        val digitsOnly = value.filter { it.isDigit() || (it in '\u0660'..'\u0669') || (it in '\u06f0'..'\u06f9') }
                                        initialPaymentAmount = digitsOnly.map {
                                            when (it) {
                                                in '\u0660'..'\u0669' -> (it.code - '\u0660'.code + '0'.code).toChar()
                                                in '\u06f0'..'\u06f9' -> (it.code - '\u06f0'.code + '0'.code).toChar()
                                                else -> it
                                            }
                                        }.joinToString("")
                                    }
                                },
                                label = { Text("مبلغ پرداختی (تومان)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PersianNumberVisualTransformation()
                            )
                        } else {
                            OutlinedTextField(
                                value = monthlyCommitment,
                                onValueChange = { value ->
                                    if (value.length <= 12) {
                                        val digitsOnly = value.filter { it.isDigit() || (it in '\u0660'..'\u0669') || (it in '\u06f0'..'\u06f9') }
                                        monthlyCommitment = digitsOnly.map {
                                            when (it) {
                                                in '\u0660'..'\u0669' -> (it.code - '\u0660'.code + '0'.code).toChar()
                                                in '\u06f0'..'\u06f9' -> (it.code - '\u06f0'.code + '0'.code).toChar()
                                                else -> it
                                            }
                                        }.joinToString("")
                                    }
                                },
                                label = { Text("مبلغ تعهد ماهانه (تومان) - اختیاری") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PersianNumberVisualTransformation(),
                                placeholder = { Text("اگر مبلغ ثابتی پرداخت می‌کند وارد کنید") }
                            )
                        }
                    }

                    if (!isAnonymous) {
                        // انتخاب ماه و سال شروع
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تاریخ شروع تعهد:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // سال
                                Box(modifier = Modifier.weight(1f)) {
                                    var expandedYear by remember { mutableStateOf(false) }
                                    OutlinedButton(
                                        onClick = { expandedYear = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("سال ${startYear.toString().toPersianDigits()}")
                                    }
                                    DropdownMenu(
                                        expanded = expandedYear,
                                        onDismissRequest = { expandedYear = false }) {
                                        (currentShamsiYear - 1..currentShamsiYear + 1).forEach { y ->
                                            DropdownMenuItem(
                                                text = { Text(y.toString().toPersianDigits()) },
                                                onClick = { startYear = y; expandedYear = false }
                                            )
                                        }
                                    }
                                }

                                // ماه
                                Box(modifier = Modifier.weight(1f)) {
                                    var expandedMonth by remember { mutableStateOf(false) }
                                    OutlinedButton(
                                        onClick = { expandedMonth = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(getPersianMonthName(startMonth))
                                    }
                                    DropdownMenu(
                                        expanded = expandedMonth,
                                        onDismissRequest = { expandedMonth = false }) {
                                        (1..12).forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(getPersianMonthName(m)) },
                                                onClick = { startMonth = m; expandedMonth = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isAnonymous || name.isNotBlank()) {
                                    val commitment = monthlyCommitment.toDoubleOrNull() ?: 0.0
                                    val initial = initialPaymentAmount.toDoubleOrNull() ?: 0.0
                                    onConfirm(name, phoneNumber, isAnonymous, commitment, startMonth, startYear, initial)
                                }
                            },
                            enabled = isAnonymous || name.isNotBlank()
                        ) {
                            Text(if (isAnonymous) "ثبت کمک" else "افزودن")
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
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

@Composable
fun BulkSmsSelectionDialog(
    debtors: List<PersonUiModel>,
    onStart: (List<PersonUiModel>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(debtors.map { it.id }) } }

    Dialog(onDismissRequest = onDismiss) {
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
                        text = "انتخاب افراد برای یادآور گروهی",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        itemsIndexed(debtors) { _, person ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedIds.contains(person.id)) selectedIds.remove(person.id)
                                        else selectedIds.add(person.id)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(person.id),
                                    onCheckedChange = null
                                )
                                Text(person.name, modifier = Modifier.padding(start = 8.dp))
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${person.debtCount.toString().toPersianDigits()} بدهی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = {
                            val selectedList = debtors.filter { selectedIds.contains(it.id) }
                            onStart(selectedList)
                        }) {
                            Text("شروع ارسال (${selectedIds.size.toString().toPersianDigits()})")
                        }
                        TextButton(onClick = onDismiss) { Text("لغو") }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkSmsProgressDialog(
    currentName: String,
    currentIndex: Int,
    total: Int,
    onSend: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ارسال گروهی پیامک",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("در حال آماده‌سازی پیام برای:")
                        Text(
                            currentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "نفر ${(currentIndex + 1).toString().toPersianDigits()} از ${total.toString().toPersianDigits()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex.toFloat() / total.toFloat()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onSend) { Text("ارسال پیام") }
                        Row {
                            TextButton(onClick = onSkip) { Text("رد کردن") }
                            TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                                Text("توقف")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectCardDialog(
    cardNumbers: List<String>,
    onCardSelected: (String?) -> Unit,
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "انتخاب شماره کارت",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        Text("کدام شماره کارت در متن پیامک درج شود؟")
                        Spacer(Modifier.height(16.dp))

                        if (cardNumbers.isEmpty()) {
                            Text("هیچ شماره کارتی در تنظیمات ثبت نشده است.", color = MaterialTheme.colorScheme.error)
                        }

                        cardNumbers.forEach { card ->
                            OutlinedButton(
                                onClick = { onCardSelected(card) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(card.toPersianDigits())
                            }
                        }

                        TextButton(
                            onClick = { onCardSelected(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ارسال بدون شماره کارت")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("لغو")
                        }
                    }
                }
            }
        }
    }
}
