@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import com.oqba26.monthlypaymentapp.viewmodel.DashboardUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonListType
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel,
    navController: NavController
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmountFlow.collectAsState(initial = 0.0)
    val personForPaymentDialog by viewModel.personForPaymentDialog.collectAsState()
    val isAddPersonDialogShown by viewModel.showAddPersonDialog.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // شخصی که قرار است با تأیید کاربر به آرشیو منتقل شود
    var personToArchive by remember { mutableStateOf<PersonUiModel?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(
                    PersonScreenEvent.MovePersonNew(
                        fromIndex = from.index,
                        toIndex = to.index,
                        listType = PersonListType.UNPAID
                    )
                )
            }
        },
        onDragEnd = { _, _ ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(PersonScreenEvent.CommitReorder(PersonListType.UNPAID))
            }
        }
    )

    // Pull-to-refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.onEvent(PersonScreenEvent.RefreshData) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(data = dashboardData)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("جستجوی نام...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = reorderableState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderableState)
            ) {
                itemsIndexed(uiState.unpaidPersons, key = { _, person -> person.id }) { index, person ->
                    ReorderableItem(reorderableState, key = person.id) { _ ->
                        val dragModifier =
                            if (searchQuery.isBlank()) Modifier.detectReorderAfterLongPress(
                                reorderableState
                            ) else Modifier

                        Box(modifier = dragModifier) {
                            PersonListItem(
                                person = person,
                                index = index + 1,
                                onPersonClick = {
                                    navController.navigate("person_detail/${person.id}")
                                },
                                onQuickPayClick = { viewModel.onQuickPayClicked(it) },
                                onArchiveClick = { selectedPerson ->
                                    personToArchive = selectedPerson
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // اندیکاتور رفرش
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // دیالوگ پرداخت سریع
        personForPaymentDialog?.let { person ->
            AddPaymentDialog(
                personName = person.name,
                defaultAmount = defaultPaymentAmount,
                onConfirm = { amount, description ->
                    viewModel.onEvent(
                        PersonScreenEvent.AddQuickPayment(
                            person.id,
                            amount,
                            description
                        )
                    )
                    viewModel.onDismissPaymentDialog()
                },
                onDismiss = { viewModel.onDismissPaymentDialog() }
            )
        }

        // دیالوگ افزودن شخص جدید
        if (isAddPersonDialogShown) {
            AddNewPersonDialog(
                onConfirm = { name ->
                    viewModel.onEvent(PersonScreenEvent.AddPerson(name))
                    viewModel.onDismissAddPersonDialog()
                },
                onDismiss = { viewModel.onDismissAddPersonDialog() }
            )
        }

        // دیالوگ تایید انتقال به آرشیو
        personToArchive?.let { person ->
            AlertDialog(
                onDismissRequest = { personToArchive = null },
                title = { Text("انتقال به آرشیو") },
                text = { Text("آیا از انتقال ${person.name} به آرشیو مطمئن هستید؟") },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // در RTL: این دکمه کاملاً به سمت راست دیالوگ می‌چسبد
                        Button(onClick = { personToArchive = null }) {
                            Text("لغو")
                        }

                        // در RTL: این دکمه کاملاً به سمت چپ دیالوگ می‌چسبد
                        Button(
                            onClick = {
                                viewModel.onEvent(
                                    PersonScreenEvent.ArchivePerson(person.id)
                                )
                                personToArchive = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("آرشیو")
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@Composable
fun AddNewPersonDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن شخص جدید") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام شخص") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name)
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("افزودن")
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
        },
        dismissButton = {}
    )
}

@Composable
fun AddPaymentDialog(
    personName: String,
    defaultAmount: Double,
    initialDescription: String = "",
    onConfirm: (amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var rawAmount by remember { mutableStateOf(defaultAmount.toLong().toString()) }
    var description by remember { mutableStateOf(initialDescription) }

    val title =
        if (onDelete != null) "ویرایش پرداخت برای $personName" else "ثبت پرداخت برای $personName"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = formatNumberAsPersian(rawAmount.toDoubleOrNull() ?: 0.0)
                        .removeSuffix(".00"),
                    onValueChange = { value ->
                        rawAmount = value.filter { it.isDigit() }
                    },
                    label = { Text("مبلغ (به تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (onDelete != null) {
                // حالت ویرایش: ویرایش راست، لغو وسط، حذف چپ (در RTL)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val amountAsDouble = rawAmount.toDoubleOrNull() ?: defaultAmount
                            onConfirm(amountAsDouble, description)
                        },
                        enabled = rawAmount.isNotBlank()
                    ) {
                        Text("ویرایش")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("لغو")
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("حذف")
                    }
                }
            } else {
                // حالت افزودن: ثبت و لغو
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val amountAsDouble = rawAmount.toDoubleOrNull() ?: defaultAmount
                            onConfirm(amountAsDouble, description)
                        },
                        enabled = rawAmount.isNotBlank()
                    ) {
                        Text("ثبت")
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
        },
        dismissButton = {}
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
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.height(4.dp))

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
    onArchiveClick: (PersonUiModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPersonClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (person.hasPaidThisMonth) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "پرداخت شده",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { onArchiveClick(person) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("آرشیو", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { onQuickPayClick(person) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("پرداخت", color = Color.White)
                    }

                    Button(
                        onClick = { onArchiveClick(person) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("آرشیو", color = Color.White)
                    }
                }
            }
        }
    }
}