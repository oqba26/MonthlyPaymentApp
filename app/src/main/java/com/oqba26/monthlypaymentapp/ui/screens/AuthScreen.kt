package com.oqba26.monthlypaymentapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.monthlypaymentapp.core.PaymentApplication
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.viewmodel.AuthState
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel

@Composable
fun AuthScreen(
    viewModel: PersonViewModel = viewModel(
        // استفاده از Factory که در PaymentApplication تعریف شده است
        factory = (LocalContext.current.applicationContext as PaymentApplication).personViewModelFactory
    )
) {
    // مشاهده وضعیت لودینگ از ViewModel
    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    // مدیریت وضعیت نمایش فرم (ورود یا ثبت نام)
    var isLoginMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "ورود به حساب کاربری" else "ایجاد حساب جدید",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        AuthForm(
            isLoginMode = isLoginMode,
            isLoading = isLoading,
            onAuthenticate = { username, password ->
                val request = AuthRequest(username, password)
                if (isLoginMode) {
                    viewModel.login(request)
                } else {
                    viewModel.register(request)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // دکمه تغییر وضعیت: رفتن به ثبت نام یا ورود
        TextButton(onClick = { isLoginMode = !isLoginMode }, enabled = !isLoading) {
            Text(
                text = if (isLoginMode) "حساب کاربری ندارید؟ ثبت نام کنید." else "قبلاً ثبت نام کرده‌اید؟ وارد شوید."
            )
        }
    }
}

@Composable
private fun AuthForm(
    isLoginMode: Boolean,
    isLoading: Boolean,
    onAuthenticate: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val buttonText = if (isLoginMode) "ورود" else "ثبت نام"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // فیلد نام کاربری
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("نام کاربری") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // فیلد رمز عبور
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("رمز عبور") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // دکمه ورود/ثبت نام
        Button(
            onClick = { onAuthenticate(username, password) },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            enabled = username.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Text(buttonText, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}