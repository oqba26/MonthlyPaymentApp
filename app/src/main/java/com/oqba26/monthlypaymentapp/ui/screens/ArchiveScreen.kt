package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.oqba26.monthlypaymentapp.viewmodel.PersonScreenEvent
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

@Composable
fun ArchiveScreen(viewModel: PersonViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var personToDelete by remember { mutableStateOf<PersonUiModel?>(null) }
    var personToRestore by remember { mutableStateOf<PersonUiModel?>(null) }

    if (uiState.archivedPersons.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("لیست آرشیو خالی است.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.archivedPersons, key = { it.id }) { person ->
                ArchivedPersonListItem(
                    person = person,
                    onRestore = {
                        personToRestore = person
                    },
                    onDelete = {
                        personToDelete = person
                    }
                )
                HorizontalDivider()
            }
        }
    }

    // دیالوگ تایید حذف قطعی
    personToDelete?.let { person ->
        ConfirmDeleteDialog(
            title = "حذف قطعی شخص",
            message = "آیا از حذف قطعی ${person.name} و تمامی سوابق پرداخت او مطمئن هستید؟ این عمل غیرقابل بازگشت است.",
            onConfirm = {
                viewModel.onEvent(PersonScreenEvent.DeletePerson(person.id))
                personToDelete = null
            },
            onDismiss = { personToDelete = null }
        )
    }

    // دیالوگ تایید بازیابی
    personToRestore?.let { person ->
        Dialog(
            onDismissRequest = { personToRestore = null }
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "بازیابی شخص",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "آیا از بازیابی ${person.name} به لیست اصلی مطمئن هستید؟",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { personToRestore = null }
                            ) {
                                Text("لغو")
                            }

                            Button(
                                onClick = {
                                    viewModel.onEvent(PersonScreenEvent.RestorePerson(person.id))
                                    personToRestore = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("بازیابی")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedPersonListItem(
    person: PersonUiModel,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = person.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف قطعی",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Restore, contentDescription = "بازیابی")
                Spacer(Modifier.width(4.dp))
                Text("بازیابی")
            }
        }
    }
}