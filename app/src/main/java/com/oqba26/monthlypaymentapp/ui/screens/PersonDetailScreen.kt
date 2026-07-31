package com.oqba26.monthlypaymentapp.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.oqba26.monthlypaymentapp.viewmodel.ContactViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.PersianDigitsTransformation
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianDateTime
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import com.oqba26.monthlypaymentapp.utils.getPersianMonthName
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.ContactMatch
import com.oqba26.monthlypaymentapp.viewmodel.MonthStatus
import com.oqba26.monthlypaymentapp.viewmodel.MonthUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

@Composable
fun PersonDetailScreen(
    personId: String,
    viewModel: PersonViewModel,
    contactViewModel: ContactViewModel = hiltViewModel(),
    navController: NavController,
) {
    LaunchedEffect(personId) {
        viewModel.onEvent(PersonScreenEvent.SelectPerson(personId))
    }

    val detailState by viewModel.personDetailState.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmountFlow.collectAsState(initial = 0.0)
    val context = LocalContext.current

    var expandedMonth by remember { mutableStateOf<Int?>(null) }
    var editingPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var addingPaymentMonth by remember { mutableStateOf<Int?>(null) }
    var editingPerson by remember { mutableStateOf(value = false) }
    var showBulkPaymentDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        PersonDetailHeader(
            name = detailState.person?.name ?: "...",
            onBack = { navController.popBackStack() }
        ) { editingPerson = true }

        if (detailState.person == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            YearSelector(
                year = detailState.selectedYear,
                onYearChange = { offset ->
                    viewModel.onEvent(PersonScreenEvent.ChangeYear(offset))
                }
            )

            val availableMonths = detailState.monthStates
                .filter { it.status == MonthStatus.AVAILABLE }
                .map { it.month }

            if (availableMonths.isNotEmpty()) {
                Button(
                    onClick = { showBulkPaymentDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("پرداخت دسته جمعی (${availableMonths.size.toString().toPersianDigits()} ماه)")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                items(detailState.monthStates) { monthModel ->
                    MonthListItem(
                        month = monthModel,
                        onHeaderClick = {
                            expandedMonth = if (expandedMonth == monthModel.month) null else monthModel.month
                        },
                        onEditClick = {
                            if (monthModel.status == MonthStatus.PAID) {
                                editingPayment = monthModel.payment
                            } else if (monthModel.status == MonthStatus.AVAILABLE) {
                                addingPaymentMonth = monthModel.month
                            }
                        }
                    )
                }
            }
        }
    }

    editingPayment?.let { payment ->
        EditPaymentDialog(
            paymentRecord = payment,
            onConfirm = { amount, desc ->
                viewModel.onEvent(PersonScreenEvent.UpdatePayment(payment, amount, desc))
                editingPayment = null
            },
            onDelete = {
                viewModel.onEvent(PersonScreenEvent.DeletePayment(payment))
                editingPayment = null
            },
            onShare = { p ->
                viewModel.shareReceipt(p, detailState.person?.name ?: "")
            },
            onDismiss = {
                editingPayment = null
            }
        )
    }

    // دیالوگ افزودن (اصلاح شده)
    addingPaymentMonth?.let { month ->
        val monthName = getPersianMonthName(month)
        val currentDateTime = formatTimestampToPersianDateTime(System.currentTimeMillis())
        
        val isCurrentMonth = (month == getCurrentShamsiMonth()) && (detailState.selectedYear == getCurrentShamsiYear())
        val initialDesc = if (isCurrentMonth) {
            "پرداخت برای ماه جاری در تاریخ $currentDateTime ثبت شد."
        } else {
            "پرداخت برای $monthName در تاریخ $currentDateTime ثبت شد."
        }

        AddPaymentDialog(
            personName = detailState.person?.name ?: "",
            defaultAmount = defaultPaymentAmount,
            initialDescription = initialDesc,
            onConfirm = { amount, description ->
                viewModel.onEvent(
                    PersonScreenEvent.AddPaymentForMonth(
                        personId = personId,
                        month = month,
                        year = detailState.selectedYear,
                        amount = amount,
                        description = description
                    )
                )
                addingPaymentMonth = null
            },
            onDismiss = {
                addingPaymentMonth = null
            }
        )
    }

    if (editingPerson) {
        UpdatePersonDialog(
            initialName = detailState.person?.name ?: "",
            initialPhone = detailState.person?.phoneNumber,
            onConfirm = { name, phone ->
                viewModel.onEvent(PersonScreenEvent.UpdatePerson(personId, name, phone))
                editingPerson = false
            },
            onDismiss = { editingPerson = false },
            onSearchContact = { name ->
                contactViewModel.findSimilarContacts(name)
            }
        )
    }

    if (showBulkPaymentDialog) {
        val availableMonths = detailState.monthStates
            .filter { it.status == MonthStatus.AVAILABLE }
            .map { it.month }
        
        BulkPaymentDialog(
            availableMonths = availableMonths,
            defaultAmount = defaultPaymentAmount,
            onConfirm = { selected, amount ->
                viewModel.onEvent(
                    PersonScreenEvent.AddBulkPayments(
                        personId = personId,
                        months = selected,
                        year = detailState.selectedYear,
                        amount = amount
                    )
                )
                showBulkPaymentDialog = false
            },
            onDismiss = { showBulkPaymentDialog = false }
        )
    }
}

@Composable
fun BulkPaymentDialog(
    availableMonths: List<Int>,
    defaultAmount: Double,
    onConfirm: (List<Int>, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedMonths = remember { mutableStateListOf<Int>().apply { addAll(availableMonths) } }
    var amountText by remember { mutableStateOf(defaultAmount.toInt().toString()) }

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
                        text = "پرداخت دسته جمعی",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text("ماه‌های مورد نظر را انتخاب کنید:")

                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(availableMonths) { month ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedMonths.contains(month)) selectedMonths.remove(month)
                                        else selectedMonths.add(month)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMonths.contains(month),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedMonths.add(month)
                                        else selectedMonths.remove(month)
                                    }
                                )
                                Text(getPersianMonthName(month))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("مبلغ برای هر ماه (تومان)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PersianDigitsTransformation()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val finalAmount = amountText.toDoubleOrNull() ?: defaultAmount
                                onConfirm(selectedMonths.toList(), finalAmount)
                            },
                            enabled = selectedMonths.isNotEmpty()
                        ) {
                            Text("ثبت پرداخت‌ها")
                        }

                        TextButton(onClick = onDismiss) {
                            Text("لغو")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonDetailHeader(
    name: String,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "ویرایش شخص")
            }
        }
    }
}

@Composable
fun UpdatePersonDialog(
    initialName: String,
    initialPhone: String?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSearchContact: (String) -> List<ContactMatch>
) {
    var name by remember { mutableStateOf(initialName) }
    var phoneNumber by remember { mutableStateOf(initialPhone ?: "") }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ویرایش اطلاعات شخص",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

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
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("شماره موبایل") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                visualTransformation = PersianDigitsTransformation(),
                                placeholder = { Text("۰۹---------", textAlign = androidx.compose.ui.text.style.TextAlign.Right) }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "ابتدا نام را وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                        val matches = onSearchContact(name)
                                        if (matches.isNotEmpty()) {
                                            similarContacts = matches
                                        } else {
                                            Toast.makeText(context, "مخاطبی با این نام پیدا نشد", Toast.LENGTH_SHORT).show()
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onConfirm(name, phoneNumber)
                                }
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text("ذخیره تغییرات")
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
fun YearSelector(year: Int, onYearChange: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { onYearChange(-1) }) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "سال قبل")
            }
            Text(
                text = "سال ${year.toString().toPersianDigits()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = { onYearChange(1) }) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "سال بعد")
            }
        }
    }
}

@Composable
fun MonthListItem(
    month: MonthUiModel,
    onHeaderClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val containerColor = when (month.status) {
        MonthStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
        MonthStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clickable(onClick = onHeaderClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = getPersianMonthName(month.month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (month.status == MonthStatus.PAID && month.payment != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${formatNumberAsPersian(month.payment.amount)} ت", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (month.status == MonthStatus.PAID || month.status == MonthStatus.AVAILABLE) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (month.status == MonthStatus.PAID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(if (month.status == MonthStatus.PAID) "ویرایش" else "پرداخت")
                    }
                }
            }
            if (month.status == MonthStatus.PAID && month.payment != null) {
                val desc = month.payment.description ?: ""
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
