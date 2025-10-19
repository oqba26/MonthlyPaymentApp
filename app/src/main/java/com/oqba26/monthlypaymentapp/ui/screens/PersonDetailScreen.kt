package com.oqba26.monthlypaymentapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.MonthStatus
import com.oqba26.monthlypaymentapp.viewmodel.PersonDetailViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onBackClick: () -> Unit
) {
    val viewModel: PersonDetailViewModel = viewModel(factory = PersonDetailViewModel.Factory)
    val context = LocalContext.current
    val person by viewModel.person.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val paymentHistory by viewModel.paymentHistory.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmount.collectAsState()

    var paymentToEdit by remember { mutableStateOf<PaymentRecord?>(null) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showEditPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    var paymentAmount by remember { mutableStateOf("") }
    var selectedMonthForPayment by remember { mutableIntStateOf(1) }

    val currentYear = getCurrentShamsiYear()
    val currentMonth = getCurrentShamsiMonth()
    val persianMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

    LaunchedEffect(key1 = true) {
        viewModel.toastMessage.collectLatest {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "...") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(top = 16.dp)) {
            YearNavigator(
                modifier = Modifier.padding(horizontal = 16.dp),
                viewModel = viewModel,
                selectedYear = selectedYear,
                paymentHistory = paymentHistory,
                currentYear = currentYear,
                showMessage = { msg ->
                    dialogMessage = msg
                    showMessageDialog = true
                }
            )
            Spacer(Modifier.height(20.dp))
            MonthsList(
                paymentHistory = paymentHistory,
                selectedYear = selectedYear,
                currentYear = currentYear,
                currentMonth = currentMonth,
                persianMonths = persianMonths,
                onAvailableClick = { month, _ ->
                    selectedMonthForPayment = month
                    paymentAmount = defaultPaymentAmount.toLong().toString()
                    showAddPaymentDialog = true
                },
                onPaidClick = { payment ->
                    paymentToEdit = payment
                    paymentAmount = payment.amount.toLong().toString()
                    showEditPaymentDialog = true
                },
                onDisabledClick = { message ->
                    dialogMessage = message
                    showMessageDialog = true
                }
            )
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            monthName = persianMonths[selectedMonthForPayment - 1],
            year = selectedYear,
            amount = paymentAmount,
            onAmountChange = { paymentAmount = it.filter { c -> c.isDigit() } },
            onConfirm = {
                val amount = paymentAmount.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.addPaymentForMonth(selectedMonthForPayment, selectedYear, amount)
                    showAddPaymentDialog = false
                }
            },
            onDismiss = { showAddPaymentDialog = false }
        )
    }

    paymentToEdit?.let { payment ->
        if (showEditPaymentDialog) {
            EditPaymentDialog(
                amount = paymentAmount,
                onAmountChange = { paymentAmount = it.filter { c -> c.isDigit() } },
                onSave = {
                    val amount = paymentAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.updatePayment(payment, amount)
                        showEditPaymentDialog = false
                    }
                },
                onDelete = {
                    showEditPaymentDialog = false
                    showDeleteConfirmDialog = true
                },
                onDismiss = { showEditPaymentDialog = false }
            )
        }

        if (showDeleteConfirmDialog) {
            DeleteConfirmDialog(
                onConfirm = {
                    viewModel.deletePayment(payment)
                    showDeleteConfirmDialog = false
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }
    }

    if (showMessageDialog) {
        MessageDialog(message = dialogMessage) { showMessageDialog = false }
    }
}

@Composable
private fun YearNavigator(
    modifier: Modifier = Modifier,
    viewModel: PersonDetailViewModel,
    selectedYear: Int,
    paymentHistory: List<PaymentRecord>,
    currentYear: Int,
    showMessage: (String) -> Unit
) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val hasData = paymentHistory.any { it.shamsiYear == selectedYear - 1 }
                if (hasData || selectedYear - 1 >= currentYear - 5) { // Allow going back 5 years
                    viewModel.changeYear(-1)
                } else {
                    showMessage("هیچ داده‌ای برای سال قبل وجود ندارد")
                }
            }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Year") }
            Text("سال ${selectedYear.toString().toPersianDigits()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            IconButton(onClick = {
                if (selectedYear < currentYear) {
                    viewModel.changeYear(1)
                } else {
                    showMessage("نمی‌توان به سال آینده رفت")
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
    LazyColumn {
        itemsIndexed(persianMonths, key = { index, _ -> index }) { index, monthName ->
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
    val isClickable = status == MonthStatus.AVAILABLE || status == MonthStatus.PAID
    val backgroundColor by animateColorAsState(
        targetValue = if (status == MonthStatus.PAID) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(400),
        label = "MonthListItemColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onItemClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        AnimatedContent(
            targetState = status,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "MonthListItemContent"
        ) { targetStatus ->
            when (targetStatus) {
                MonthStatus.PAID -> {
                    payment?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${formatNumberAsPersian(it.amount)} تومان",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Check, contentDescription = "Paid", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                MonthStatus.AVAILABLE -> {
                    Icon(Icons.Filled.Add, contentDescription = "Add Payment", tint = MaterialTheme.colorScheme.secondary)
                }
                else -> {
                    Icon(Icons.Filled.Close, contentDescription = "Disabled", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        }
    }
}

private fun getMessageForStatus(status: MonthStatus, monthName: String): String {
    return when (status) {
        MonthStatus.FUTURE_YEAR -> "این سال هنوز نرسیده است."
        MonthStatus.PAST_YEAR -> "شما در سال گذشته برای ماه $monthName پرداختی ثبت نکرده‌اید."
        MonthStatus.FUTURE_MONTH -> "این ماه هنوز نرسیده است."
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
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
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
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSave) { Text("ذخیره") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("لغو") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("حذف") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تایید حذف") },
        text = { Text("آیا از حذف این پرداخت مطمئن هستید؟ این عمل غیرقابل بازگشت است.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("حذف کن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
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
