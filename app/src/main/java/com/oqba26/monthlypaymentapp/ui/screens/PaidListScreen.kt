package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.oqba26.monthlypaymentapp.viewmodel.ContactViewModel
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
fun PaidListScreen(
    viewModel: PersonViewModel,
    contactViewModel: ContactViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val defaultPaymentAmount by viewModel.defaultPaymentAmountFlow.collectAsState(initial = 0.0)
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val personToArchive = remember { mutableStateOf<PersonUiModel?>(null) }
    val personForBulkPayment = remember { mutableStateOf<PersonUiModel?>(null) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(PersonScreenEvent.MovePersonNew(from.index, to.index, PersonListType.PAID))
            }
        },
        onDragEnd = { _, _ ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(PersonScreenEvent.CommitReorder(PersonListType.PAID))
            }
        }
    )

    @OptIn(ExperimentalMaterialApi::class)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.onEvent(PersonScreenEvent.RefreshData) }
    )

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("جستجوی نام...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )

                LazyColumn(
                    state = state.listState,
                    modifier = Modifier.reorderable(state)
                ) {
                    itemsIndexed(uiState.paidPersons, key = { _, person -> person.id }) { index, person ->
                        ReorderableItem(state, key = person.id) { _ ->
                            val rowDragModifier = if (searchQuery.isBlank()) Modifier.detectReorderAfterLongPress(state) else Modifier
                            Row(modifier = rowDragModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PersonListItem(
                                        person = person,
                                        index = index + 1,
                                        onPersonClick = { navController.navigate("person_detail/${person.id}") },
                                        onQuickPayClick = { },
                                        onArchiveClick = { personToArchive.value = it },
                                        onSmsClick = { contactViewModel.sendSmsReminder(it) },
                                        onDebtClick = { personForBulkPayment.value = it }
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        personForBulkPayment.value?.let { person: PersonUiModel ->
            BulkPaymentDialog(
                availableMonths = person.unpaidMonths,
                defaultAmount = if (person.monthlyCommitment > 0) person.monthlyCommitment else defaultPaymentAmount,
                onConfirm = { selected: List<Int>, amount: Double ->
                    viewModel.onEvent(
                        PersonScreenEvent.AddBulkPayments(
                            personId = person.id,
                            months = selected,
                            year = com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear(),
                            amount = amount
                        )
                    )
                    personForBulkPayment.value = null
                },
                onDismiss = { personForBulkPayment.value = null }
            )
        }

        personToArchive.value?.let { person ->
            Dialog(onDismissRequest = { personToArchive.value = null }) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(text = "انتقال به آرشیو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = "آیا از انتقال ${person.name} به آرشیو مطمئن هستید؟", style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { personToArchive.value = null }) { Text("لغو") }
                                Button(onClick = {
                                    viewModel.onEvent(PersonScreenEvent.ArchivePerson(person.id))
                                    personToArchive.value = null
                                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                    Text("آرشیو")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
