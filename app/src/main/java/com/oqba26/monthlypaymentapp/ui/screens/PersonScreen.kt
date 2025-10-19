package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.monthlypaymentapp.core.PaymentApplication
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import com.oqba26.monthlypaymentapp.utils.toPersianDate
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.DashboardUiModel
import com.oqba26.monthlypaymentapp.viewmodel.MonthStatus
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonScreen(
    onSettingsClick: () -> Unit,
    //onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PaymentApplication
    val viewModel: PersonViewModel = viewModel(factory = app.personViewModelFactory)

    val uiState by viewModel.uiState.collectAsState()
    val persons = uiState.persons
    val allPayments = uiState.payments

    val searchQuery by viewModel.searchQuery.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmountFlow.collectAsState(initial = 0.0)
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var personToDelete by remember { mutableStateOf<PersonUiModel?>(null) }

    var showDetailsSheet by remember { mutableStateOf(false) }
    var selectedPerson by remember { mutableStateOf<PersonUiModel?>(null) }

    var personForQuickPay by remember { mutableStateOf<PersonUiModel?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.onEvent(PersonScreenEvent.RefreshData)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
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
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                    /*IconButton(onClick = onExitApp) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "خروج")
                    }*/
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPersonDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن شخص")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            Column {
                DashboardCard(dashboardData)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("جستجوی نام...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true
                )

                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(persons, key = { _, person -> person.id }) { index, person ->
                        PersonListItem(
                            person = person,
                            index = index + 1,
                            onPersonClick = {
                                selectedPerson = person
                                showDetailsSheet = true
                            },
                            onQuickPayClick = { personForQuickPay = person },
                            onDeleteClick = { personToDelete = it }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    }
                }
            }
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
            )
        }
    }

    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = sheetState
        ) {
            selectedPerson?.let {
                DetailsSheetContent(
                    person = it,
                    allPayments = allPayments,
                    defaultPaymentAmount = defaultPaymentAmount,
                    viewModel = viewModel
                )
            }
        }
    }

    personForQuickPay?.let { person ->
        var amount by remember { mutableStateOf(defaultPaymentAmount.toLong().toString()) }
        QuickPayDialog(
            personName = person.name,
            amount = amount,
            onAmountChange = { amount = it.filter { c -> c.isDigit() } },
            onConfirm = {
                val finalAmount = amount.toDoubleOrNull() ?: defaultPaymentAmount
                viewModel.onEvent(PersonScreenEvent.AddQuickPayment(person.id, finalAmount))
                personForQuickPay = null
            },
            onDismiss = { personForQuickPay = null }
        )
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("افزودن شخص جدید") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.onEvent(PersonScreenEvent.AddPerson(name))
                            showAddPersonDialog = false
                        }
                    }) { Text("افزودن") }

                    Button(
                        onClick = { showAddPersonDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("لغو") }
                }
            }
        )
    }

    personToDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("حذف ${person.name}") },
            text = { Text("آیا از حذف این شخص مطمئن هستید؟ تمام داده‌های پرداخت او نیز حذف خواهد شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(PersonScreenEvent.DeletePerson(person.id))
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("لغو") }
            }
        )
    }
}

@Composable
fun QuickPayDialog(
    personName: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پرداخت سریع برای $personName") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("مبلغ (تومان)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PersianNumberVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onConfirm) { Text("ثبت") }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("لغو") }
            }
        }
    )
}

@Composable
fun DetailsSheetContent(
    person: PersonUiModel,
    allPayments: List<PaymentRecord>,
    defaultPaymentAmount: Double,
    viewModel: PersonViewModel
) {
    val selectedYear by viewModel.selectedYear.collectAsState()

    var paymentToEdit by remember { mutableStateOf<PaymentRecord?>(null) }
    var showPaymentInfoDialog by remember { mutableStateOf(false) }
    var showAddPaymentInSheetDialog by remember { mutableStateOf(false) }
    var showEditPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    var paymentAmount by remember { mutableStateOf("") }
    var selectedMonthForPayment by remember { mutableIntStateOf(1) }

    val currentYear = getCurrentShamsiYear()
    val currentMonth = getCurrentShamsiMonth()
    val persianMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

    val personPaymentHistory = remember(allPayments, person.id) {
        allPayments.filter { it.personId == person.id }
    }

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Text(person.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        YearNavigator(
            modifier = Modifier.padding(top = 8.dp),
            selectedYear = selectedYear,
            onYearChange = { offset -> viewModel.onEvent(PersonScreenEvent.ChangeYear(offset)) },
            showMessage = { msg ->
                dialogMessage = msg
                showMessageDialog = true
            }
        )
        Spacer(Modifier.height(16.dp))
        MonthsList(
            paymentHistory = personPaymentHistory,
            selectedYear = selectedYear,
            currentYear = currentYear,
            currentMonth = currentMonth,
            persianMonths = persianMonths,
            onAvailableClick = { month, _ ->
                selectedMonthForPayment = month
                paymentAmount = defaultPaymentAmount.toLong().toString()
                showAddPaymentInSheetDialog = true
            },
            onPaidClick = { payment ->
                paymentToEdit = payment
                paymentAmount = payment.amount.toLong().toString()
                showPaymentInfoDialog = true
            },
            onDisabledClick = { message ->
                dialogMessage = message
                showMessageDialog = true
            }
        )
    }

    if (showAddPaymentInSheetDialog) {
        AddPaymentDialog(
            monthName = persianMonths[selectedMonthForPayment - 1],
            year = selectedYear,
            amount = paymentAmount,
            onAmountChange = { paymentAmount = it.filter { c -> c.isDigit() } },
            onConfirm = {
                val amount = paymentAmount.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.onEvent(PersonScreenEvent.AddPaymentForMonth(person.id, selectedMonthForPayment, selectedYear, amount))
                    showAddPaymentInSheetDialog = false
                }
            },
            onDismiss = { showAddPaymentInSheetDialog = false }
        )
    }

    paymentToEdit?.let { payment ->
        if (showPaymentInfoDialog) {
            PaymentInfoDialog(
                payment = payment,
                onEdit = {
                    showPaymentInfoDialog = false
                    showEditPaymentDialog = true
                },
                onDismiss = {
                    showPaymentInfoDialog = false
                    paymentToEdit = null
                }
            )
        }

        if (showEditPaymentDialog) {
            EditPaymentDialog(
                amount = paymentAmount,
                onAmountChange = { paymentAmount = it.filter { c -> c.isDigit() } },
                onSave = {
                    val amount = paymentAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.onEvent(PersonScreenEvent.UpdatePayment(payment, amount))
                        showEditPaymentDialog = false
                        paymentToEdit = null
                    }
                },
                onDelete = {
                    showEditPaymentDialog = false
                    showDeleteConfirmDialog = true
                },
                onDismiss = {
                    showEditPaymentDialog = false
                    paymentToEdit = null
                }
            )
        }

        if (showDeleteConfirmDialog) {
            DeleteConfirmDialog(
                onConfirm = {
                    viewModel.onEvent(PersonScreenEvent.DeletePayment(payment))
                    showDeleteConfirmDialog = false
                    paymentToEdit = null
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    paymentToEdit = null
                }
            )
        }
    }

    if (showMessageDialog) {
        MessageDialog(message = dialogMessage) { showMessageDialog = false }
    }
}

@Composable
private fun PaymentInfoDialog(
    payment: PaymentRecord,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentMonth = getCurrentShamsiMonth()
    val persianMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    val monthName = persianMonths[payment.shamsiMonth - 1]

    val persianDate = payment.timestamp.toPersianDate("Y/n/j").toPersianDigits()
    val persianTime = payment.timestamp.toPersianDate("H:i:s").toPersianDigits()

    val timeInfo = if (payment.shamsiMonth == currentMonth) {
        "پرداخت برای $monthName در ساعت $persianTime ثبت شد"
    } else {
        "پرداخت برای $monthName در تاریخ $persianDate ساعت $persianTime ثبت شد"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جزئیات پرداخت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = timeInfo,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "مبلغ: ${formatNumberAsPersian(payment.amount)} تومان",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("ویرایش") }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("بستن") }
            }
        }
    )
}

@Composable
private fun YearNavigator(
    modifier: Modifier = Modifier,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    showMessage: (String) -> Unit
) {
    val currentYear = getCurrentShamsiYear()
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedYear > currentYear) {
                    onYearChange(-1)
                } else {
                    showMessage("زمان ثبت پرداخت حقوق برای سال‌های گذشته تمام شده است")
                }
            }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Year") }
            Text("سال ${selectedYear.toString().toPersianDigits()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                if (selectedYear < currentYear) {
                    onYearChange(1)
                } else {
                    showMessage("زمان ثبت پرداخت حقوق برای سال‌های آینده فرا نرسیده است")
                }
            }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Year") }
        }
    }
}

@Composable
private fun MonthsList(
    paymentHistory: List<PaymentRecord>,
    selectedYear: Int,
    currentYear: Int,
    currentMonth: Int,
    persianMonths: List<String>,
    onAvailableClick: (month: Int, year: Int) -> Unit,
    onPaidClick: (payment: PaymentRecord) -> Unit,
    onDisabledClick: (message: String) -> Unit
) {
    LazyColumn(modifier= Modifier.height(320.dp)) {
        itemsIndexed(persianMonths, key = { index, _ -> "$selectedYear-$index" }) { index, monthName ->
            val monthNumber = index + 1
            val payment = paymentHistory.find { p -> p.shamsiMonth == monthNumber && p.shamsiYear == selectedYear }

            val monthStatus = when {
                payment != null -> MonthStatus.PAID
                selectedYear > currentYear -> MonthStatus.FUTURE_YEAR
                selectedYear < currentYear -> MonthStatus.PAST_YEAR
                monthNumber > currentMonth -> MonthStatus.FUTURE_MONTH
                else -> MonthStatus.AVAILABLE
            }

            MonthListItem(
                monthName = monthName,
                payment = payment,
                status = monthStatus,
                onItemClick = {
                    when (monthStatus) {
                        MonthStatus.AVAILABLE -> onAvailableClick(monthNumber, selectedYear)
                        MonthStatus.PAID -> onPaidClick(payment!!)
                        else -> onDisabledClick(getMessageForStatus(monthStatus, monthName))
                    }
                }
            )
            if(index < persianMonths.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
private fun MonthListItem(
    monthName: String,
    payment: PaymentRecord?,
    status: MonthStatus,
    onItemClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when(status) {
            MonthStatus.PAID -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            MonthStatus.AVAILABLE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "MonthListItemColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onItemClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (status == MonthStatus.PAID) 2.dp else 0.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconColor = when(status) {
                    MonthStatus.PAID -> MaterialTheme.colorScheme.primary
                    MonthStatus.AVAILABLE -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = iconColor.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = when(status) {
                            MonthStatus.PAID -> Icons.Filled.Check
                            MonthStatus.AVAILABLE -> Icons.Filled.Add
                            else -> Icons.Filled.Close
                        },
                        contentDescription = status.name,
                        tint = iconColor,
                        modifier = Modifier.width(20.dp).height(20.dp)
                    )
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (status == MonthStatus.PAID) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            if (payment != null) {
                Text(
                    text = "${formatNumberAsPersian(payment.amount)} تومان",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getMessageForStatus(status: MonthStatus, monthName: String): String {
    return when (status) {
        MonthStatus.FUTURE_YEAR -> "این سال هنوز نرسیده است."
        MonthStatus.PAST_YEAR -> "شما در سال گذشته برای ماه $monthName پرداختی ثبت نکرده‌اید."
        MonthStatus.FUTURE_MONTH -> "پرداخت حقوق برای این ماه امکان‌پذیر نیست (زمانش که برسه این ماه باز می‌شه)"
        else -> ""
    }
}

@Composable
private fun AddPaymentDialog(
    monthName: String,
    year: Int,
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداخت برای $monthName ${year.toString().toPersianDigits()}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("مبلغ (تومان)") },
                visualTransformation = PersianNumberVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("تومان") }
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onConfirm) { Text("ثبت") }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("لغو") }
            }
        }
    )
}

@Composable
private fun EditPaymentDialog(
    amount: String,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش یا حذف پرداخت") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("مبلغ جدید (تومان)") },
                visualTransformation = PersianNumberVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("تومان") }
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSave) { Text("ذخیره") }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) { Text("لغو") }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("حذف") }
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تایید حذف") },
        text = { Text("آیا از حذف این پرداخت مطمئن هستید؟") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف کن") }
                TextButton(onClick = onDismiss) { Text("لغو") }
            }
        }
    )
}

@Composable
private fun MessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توجه") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("باشه") } }
    )
}

@Composable
fun DashboardCard(data: DashboardUiModel) {
    val animatedProgress by animateFloatAsState(
        targetValue = data.progress,
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "پرداختی های این ماه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "افراد باقیمانده: ${(data.totalCount - data.paidCount).toString().toPersianDigits()} نفر",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "مجموع درآمد ماه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${formatNumberAsPersian(data.totalIncome)} تومان",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PersonListItem(
    person: PersonUiModel,
    index: Int,
    onPersonClick: () -> Unit,
    onQuickPayClick: (PersonUiModel) -> Unit,
    onDeleteClick: (PersonUiModel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPersonClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index.toString().toPersianDigits()} - ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal
        )
        Text(
            person.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))

        if (person.hasPaidThisMonth) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "پرداخت شده",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            IconButton(onClick = { onQuickPayClick(person) }) {
                Icon(
                    Icons.Filled.AddCard,
                    contentDescription = "پرداخت سریع",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        IconButton(onClick = { onDeleteClick(person) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "حذف",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
