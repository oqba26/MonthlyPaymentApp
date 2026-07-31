@file:Suppress("unused")

package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oqba26.monthlypaymentapp.R
import androidx.compose.ui.window.Dialog
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation

@Composable
fun EditPaymentDialog(
    paymentRecord: PaymentRecord,
    onConfirm: (amount: Double, description: String) -> Unit,
    onDelete: () -> Unit,
    onShare: (PaymentRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(paymentRecord.amount.toLong().toString()) }
    var description by remember { mutableStateOf(paymentRecord.description ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ویرایش پرداخت",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { value ->
                            // اجازه ورود فقط به اعداد (چه انگلیسی چه فارسی)
                            val digitsOnly = value.filter { it.isDigit() || it in '\u0660'..'\u0669' || it in '\u06f0'..'\u06f9' }
                            // تبدیل اعداد فارسی به انگلیسی برای ذخیره در استیت
                            amountText = digitsOnly.map { 
                                when (it) {
                                    in '\u0660'..'\u0669' -> (it.code - '\u0660'.code + '0'.code).toChar()
                                    in '\u06f0'..'\u06f9' -> (it.code - '\u06f0'.code + '0'.code).toChar()
                                    else -> it
                                }
                            }.joinToString("")
                        },
                        label = { Text("مبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PersianNumberVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onShare(paymentRecord) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("اشتراک‌گذاری رسید")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: paymentRecord.amount
                                onConfirm(amount, description)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ذخیره")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("لغو")
                        }

                        IconButton(
                            onClick = onDelete,
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
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

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = rawAmount,
                        onValueChange = { value ->
                            // اجازه ورود فقط به اعداد (چه انگلیسی چه فارسی)
                            val digitsOnly = value.filter { it.isDigit() || it in '\u0660'..'\u0669' || it in '\u06f0'..'\u06f9' }
                            // تبدیل اعداد فارسی به انگلیسی برای ذخیره در استیت
                            rawAmount = digitsOnly.map { 
                                when (it) {
                                    in '\u0660'..'\u0669' -> (it.code - '\u0660'.code + '0'.code).toChar()
                                    in '\u06f0'..'\u06f9' -> (it.code - '\u06f0'.code + '0'.code).toChar()
                                    else -> it
                                }
                            }.joinToString("")
                        },
                        label = { Text("مبلغ (به تومان)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PersianNumberVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            val label = if (onDelete != null) "ویرایش" else "ثبت"
                            Text(label)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onDelete != null) {
                                Button(
                                    onClick = onDelete,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("حذف")
                                }
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
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
}
