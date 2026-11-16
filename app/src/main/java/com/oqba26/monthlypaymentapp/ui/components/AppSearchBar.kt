package com.oqba26.monthlypaymentapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oqba26.monthlypaymentapp.utils.AudioRecorder
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel
import java.io.File

@Composable
fun AppSearchBar(
    viewModel: PersonViewModel,
    modifier: Modifier = Modifier,
    placeholderText: String = "جستجوی نام...",
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val isProcessingVoice by viewModel.isProcessingVoice.collectAsState()

    val audioRecorder = remember { AudioRecorder(context) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            if (audioRecorder.isRecording) {
                audioRecorder.stop()
                viewModel.setRecordingVoice(isRecording = false)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val file = File.createTempFile("voice_search_", ".m4a", context.cacheDir)
            currentAudioFile = file
            if (audioRecorder.start(file)) {
                viewModel.setRecordingVoice(isRecording = true)
            } else {
                Toast.makeText(context, "خطا در شروع ضبط صدا", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "برای جستجوی صوتی نیاز به دسترسی میکروفون است", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.onSearchQueryChange(it) },
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            if (isRecordingVoice) {
                Text(
                    text = "در حال ضبط صدا... صحبت کنید",
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(placeholderText)
            }
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (searchQuery.isNotBlank() && !isRecordingVoice && !isProcessingVoice) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "پاک کردن")
                    }
                }

                if (isProcessingVoice) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (isRecordingVoice) {
                    IconButton(
                        onClick = {
                            val stopped = audioRecorder.stop()
                            viewModel.setRecordingVoice(isRecording = false)
                            val file = currentAudioFile
                            if (stopped && (file != null) && file.exists() && (file.length() > 0)) {
                                viewModel.processVoiceSearch(file)
                            } else {
                                Toast.makeText(context, "صدایی ضبط نشد یا زمان بسیار کوتاه بود", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseScale)
                                .background(Color.Red.copy(alpha = 0.2f), shape = CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "توقف ضبط و جستجو",
                                tint = Color.Red,
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val file = File.createTempFile("voice_search_", ".m4a", context.cacheDir)
                                currentAudioFile = file
                                if (audioRecorder.start(file)) {
                                    viewModel.setRecordingVoice(isRecording = true)
                                } else {
                                    Toast.makeText(context, "خطا در شروع ضبط صدا", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "جستجوی صوتی",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}
