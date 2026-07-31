package com.oqba26.monthlypaymentapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PermissionsDialog(
    onRequestPermissions: () -> Unit,
    onExitClick: () -> Unit
) {
    Dialog(onDismissRequest = { }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "دسترسی‌های الزامی",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "کاربر گرامی، برای استفاده از برنامه باید دسترسی‌های زیر را تایید کنید:\n\n" +
                                "۱. مخاطبین: برای مدیریت پرداخت‌های افراد\n" +
                                "۲. حافظه: برای ذخیره فایل‌های پشتیبان\n\n" +
                                "توجه: در صورت عدم تایید، برنامه بسته خواهد شد.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("متوجه شدم؛ اعطای دسترسی", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onExitClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
