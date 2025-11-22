@file:Suppress("unused")

package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.PersianNumberVisualTransformation

@Composable
fun EditPaymentDialog(
    paymentRecord: PaymentRecord,
    onConfirm: (amount: Double, description: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(paymentRecord.amount.toLong().toString()) }
    var description by remember { mutableStateOf(paymentRecord.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش پرداخت") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        amountText = value.filter { c -> c.isDigit() }
                    },
                    label = { Text("مبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PersianNumberVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        // تمام دکمه‌ها را خودمان داخل confirmButton می‌چینیم
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // در RTL: Start = راست، End = چپ

                // راست: ذخیره
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: paymentRecord.amount
                        onConfirm(amount, description)
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart)
                ) {
                    Text("ذخیره")
                }

                // وسط: لغو
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("لغو")
                }

                // چپ: حذف
                Button(
                    onClick = onDelete,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd),
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