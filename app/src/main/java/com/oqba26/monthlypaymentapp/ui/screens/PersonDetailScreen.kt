@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.monthlypaymentapp.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianDate
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianTime
import com.oqba26.monthlypaymentapp.utils.getPersianMonthName
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.MonthStatus
import com.oqba26.monthlypaymentapp.viewmodel.MonthUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

@Composable
fun PersonDetailScreen(
    personId: String,
    viewModel: PersonViewModel,
    navController: NavController
) {
    LaunchedEffect(personId) {
        viewModel.onEvent(PersonScreenEvent.SelectPerson(personId))
    }

    val detailState by viewModel.personDetailState.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmountFlow.collectAsState(initial = 0.0)

    var expandedMonth by remember { mutableStateOf<Int?>(null) }
    var paymentDialogState by remember { mutableStateOf<PaymentDialogState?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentRecord?>(null) }

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect { message ->
            infoDialogMessage = message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PersonDetailHeader(
            name = detailState.person?.name ?: "...",
            onBack = { navController.popBackStack() }
        )

        if (detailState.person == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            YearSelector(
                year = detailState.selectedYear,
                onYearChange = { offset ->
                    viewModel.onEvent(PersonScreenEvent.ChangeYear(offset))
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                items(detailState.monthStates) { monthModel ->
                    MonthListItem(
                        month = monthModel,
                        isExpanded = expandedMonth == monthModel.month,
                        onHeaderClick = {
                            expandedMonth =
                                if (expandedMonth == monthModel.month) null else monthModel.month
                        },
                        onEditClick = {
                            when (monthModel.status) {
                                MonthStatus.PAID -> {
                                    paymentDialogState =
                                        PaymentDialogState.Edit(monthModel.payment!!)
                                }

                                MonthStatus.AVAILABLE -> {
                                    paymentDialogState =
                                        PaymentDialogState.Add(monthModel.month)
                                }

                                else -> { /* Do nothing for other states */ }
                            }
                        }
                    )
                }
            }
        }
    }

    infoDialogMessage?.let {
        InfoDialog(message = it, onDismiss = { infoDialogMessage = null })
    }

    paymentToDelete?.let { payment ->
        DeleteConfirmationDialog(
            payment = payment,
            onConfirm = {
                viewModel.onEvent(PersonScreenEvent.DeletePayment(payment))
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    paymentDialogState?.let { state ->
        when (state) {
            is PaymentDialogState.Add -> {
                AddPaymentDialog(
                    personName = detailState.person!!.name,
                    defaultAmount = defaultPaymentAmount,
                    onConfirm = { amount, description ->
                        viewModel.onEvent(
                            PersonScreenEvent.AddPaymentForMonth(
                                personId = personId,
                                month = state.month,
                                year = detailState.selectedYear,
                                amount = amount,
                                description = description
                            )
                        )
                        paymentDialogState = null
                    },
                    onDismiss = { paymentDialogState = null }
                )
            }

            is PaymentDialogState.Edit -> {
                val payment = state.payment
                AddPaymentDialog(
                    personName = detailState.person!!.name,
                    defaultAmount = payment.amount,
                    initialDescription = payment.description,
                    onConfirm = { amount, description ->
                        viewModel.onEvent(
                            PersonScreenEvent.UpdatePayment(
                                payment = payment,
                                newAmount = amount,
                                newDescription = description
                            )
                        )
                        paymentDialogState = null
                    },
                    onDismiss = { paymentDialogState = null },
                    onDelete = {
                        paymentToDelete = payment
                        paymentDialogState = null
                    }
                )
            }
        }
    }
}

@Composable
private fun PersonDetailHeader(name: String, onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { onYearChange(-1) }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "سال قبل"
                )
            }

            Text(
                text = "سال ${year.toString().toPersianDigits()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            IconButton(onClick = { onYearChange(1) }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "سال بعد"
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MonthListItem(
    month: MonthUiModel,
    isExpanded: Boolean,          // فعلاً استفاده نمی‌شود
    onHeaderClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val containerColor = when (month.status) {
        MonthStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
        MonthStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
        MonthStatus.PAST_YEAR, MonthStatus.FUTURE_YEAR, MonthStatus.FUTURE_MONTH ->
            MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clickable(onClick = onHeaderClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // ردیف بالا: نام ماه + مبلغ + دکمه
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // نام ماه + مبلغ در صورت پرداخت
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getPersianMonthName(month.month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (month.status == MonthStatus.PAID && month.payment != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${formatNumberAsPersian(month.payment.amount)} تومان",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // دکمه سمت چپ
                when (month.status) {
                    MonthStatus.PAID -> {
                        Button(
                            onClick = onEditClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("ویرایش")
                        }
                    }

                    MonthStatus.AVAILABLE -> {
                        Button(
                            onClick = onEditClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("پرداخت")
                        }
                    }

                    else -> {
                        Text(
                            text = "غیرفعال",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // جزئیات پرداخت: تاریخ + (در صورت وجود) توضیحات
            month.payment?.let { payment ->
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ثبت شده در: " +
                            "${formatTimestampToPersianDate(payment.timestamp)} " +
                            "ساعت ${formatTimestampToPersianTime(payment.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                if (payment.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "توضیحات: ${payment.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    payment: PaymentRecord,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تایید حذف") },
        text = {
            Text(
                "آیا برای حذف پرداخت ماه ${
                    getPersianMonthName(payment.shamsiMonth)
                } به مبلغ ${
                    formatNumberAsPersian(payment.amount)
                } تومان مطمئن هستید؟"
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // در RTL: این دکمه می‌چسبد به سمت راست دیالوگ
                Button(onClick = onDismiss) {
                    Text("لغو")
                }

                // در RTL: این دکمه می‌چسبد به سمت چپ دیالوگ
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun InfoDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توجه") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("باشه")
            }
        }
    )
}

sealed class PaymentDialogState {
    data class Add(val month: Int) : PaymentDialogState()
    data class Edit(val payment: PaymentRecord) : PaymentDialogState()
}