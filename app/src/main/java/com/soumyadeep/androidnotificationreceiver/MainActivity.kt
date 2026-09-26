package com.soumyadeep.androidnotificationreceiver

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.soumyadeep.androidnotificationreceiver.ui.theme.AndroidNotificationReceiverTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Notification Channel for Android 8.0+
        NotificationHelper.createNotificationChannel(this)

        setContent {
            AndroidNotificationReceiverTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NotificationAppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fcmToken by NotificationRepository.fcmToken.collectAsState()
    val installationId by NotificationRepository.installationId.collectAsState()
    val isLoadingToken by NotificationRepository.isLoadingToken.collectAsState()
    val lastNotification by NotificationRepository.lastReceivedNotification.collectAsState()

    var isPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch initial token and FID on start
    LaunchedEffect(Unit) {
        NotificationRepository.fetchFcmToken()
        NotificationRepository.fetchInstallationId()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FCM Notification Receiver",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Receive & test push notifications from Spring Boot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Notification Permission Card (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPermissionGranted)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPermissionGranted) "Notification Permission Granted" else "Notification Permission Needed",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isPermissionGranted) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
        }

        // Registration Token Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FCM Registration Token",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isLoadingToken) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Token display box
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = fcmToken ?: "Fetching FCM Registration Token...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (fcmToken != null)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                Color.Gray
                        )
                    }
                }

                // Action buttons: Copy & Refresh Token
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            fcmToken?.let { token ->
                                copyToClipboard(context, "FCM Token", token)
                            } ?: Toast.makeText(context, "No token available", Toast.LENGTH_SHORT).show()
                        },
                        enabled = fcmToken != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Token",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Token")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = NotificationRepository.refreshFcmToken()
                                result.fold(
                                    onSuccess = { newToken ->
                                        Toast.makeText(context, "FCM Token Refreshed!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Failed to refresh token: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = !isLoadingToken,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Token",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Token")
                    }
                }

                // Firebase Installation ID (FID) section
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Firebase Installation ID (FID)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = installationId ?: "Fetching FID...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            installationId?.let { fid ->
                                copyToClipboard(context, "Firebase Installation ID", fid)
                            }
                        },
                        enabled = installationId != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy FID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Live Received Notification Log Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Received Notification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (lastNotification != null) {
                        TextButton(
                            onClick = { NotificationRepository.clearLastNotification() }
                        ) {
                            Text("Clear")
                        }
                    }
                }

                if (lastNotification != null) {
                    val notif = lastNotification!!
                    val formattedTime = remember(notif.timestamp) {
                        SimpleDateFormat("hh:mm:ss a, dd MMM yyyy", Locale.getDefault()).format(Date(notif.timestamp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = notif.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = notif.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (notif.data.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Data Payload:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            notif.data.forEach { (key, value) ->
                                Text(
                                    text = "• $key: $value",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Received at: $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notification received yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}
