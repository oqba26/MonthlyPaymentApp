package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oqba26.monthlypaymentapp.viewmodel.PersonListType
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun PaidListScreen(viewModel: PersonViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // شخصی که قرار است در این صفحه با تأیید کاربر به آرشیو منتقل شود
    val personToArchive = remember { mutableStateOf<PersonUiModel?>(null) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(
                    PersonScreenEvent.MovePersonNew(
                        fromIndex = from.index,
                        toIndex = to.index,
                        listType = PersonListType.PAID
                    )
                )
            }
        },
        onDragEnd = { _, _ ->
            if (searchQuery.isBlank()) {
                viewModel.onEvent(
                    PersonScreenEvent.CommitReorder(
                        listType = PersonListType.PAID
                    )
                )
            }
        }
    )

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

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

            LazyColumn(
                state = state.listState,
                modifier = Modifier.reorderable(state)
            ) {
                itemsIndexed(
                    uiState.paidPersons,
                    key = { _, person -> person.id }
                ) { index, person ->
                    ReorderableItem(state, key = person.id) { _ ->

                        val rowDragModifier =
                            if (searchQuery.isBlank()) {
                                Modifier.detectReorderAfterLongPress(state)
                            } else {
                                Modifier
                            }

                        Row(
                            modifier = rowDragModifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonListItem(
                                    person = person,
                                    index = index + 1,
                                    onPersonClick = {
                                        navController.navigate("person_detail/${person.id}")
                                    },
                                    // در لیست پرداخت شده‌ها، دکمه پرداخت سریع کاری نمی‌کند
                                    onQuickPayClick = { },
                                    onArchiveClick = { selectedPerson ->
                                        personToArchive.value = selectedPerson
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // دیالوگ تأیید انتقال به آرشیو برای لیست پرداخت‌شده‌ها
        personToArchive.value?.let { person ->
            AlertDialog(
                onDismissRequest = { personToArchive.value = null },
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
                        Button(onClick = { personToArchive.value = null }) {
                            Text("لغو")
                        }

                        // در RTL: این دکمه کاملاً به سمت چپ دیالوگ می‌چسبد
                        Button(
                            onClick = {
                                viewModel.onEvent(
                                    PersonScreenEvent.ArchivePerson(person.id)
                                )
                                personToArchive.value = null
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