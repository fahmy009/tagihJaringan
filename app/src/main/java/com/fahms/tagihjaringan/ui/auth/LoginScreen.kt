package com.fahms.tagihjaringan.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted
import com.google.gson.JsonParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    currentProjectId: String = "",
    currentApiKey: String = "",
    currentAppId: String = "",
    onSaveFirebaseConfig: ((projectId: String, apiKey: String, appId: String) -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEC4899), // Vibrant Pink
            Color(0xFF8B5CF6), // Rich Purple
            Color(0xFF0EA5E9)  // Deep Sky Blue
        )
    )

    val badgeGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEC4899),
            Color(0xFF0EA5E9)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // Center Login Card Form Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glassmorphism Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Elevated Circular WiFi Branding Badge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(brush = badgeGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "tagihJaringan",
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PinkPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Sistem Kasir & Billing WiFi Standalone",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PinkPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Username Input Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("admin / mitra / collector") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "User", tint = PinkPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary,
                            cursorColor = PinkPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Password Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Pass", tint = PinkPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password",
                                    tint = TextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary,
                            cursorColor = PinkPrimary
                        ),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    onLoginClick(username, password)
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Animated Error Message Banner
                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Login Action Button
                    Button(
                        onClick = { onLoginClick(username, password) },
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            disabledContainerColor = PinkPrimary.copy(alpha = 0.5f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "MASUK KE SISTEM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Credit Pill Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "Developed with ❤️ by Muhammad Fahmy",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Top-Right Glassmorphism Firebase Config Icon Button Layer (Rendered LAST to capture click events directly)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { showConfigDialog = true },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                shadowElevation = 8.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Konfigurasi Firebase",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        FirebaseConfigDialog(
            initialProjectId = currentProjectId,
            initialApiKey = currentApiKey,
            initialAppId = currentAppId,
            onSaveConfig = { pId, key, app ->
                showConfigDialog = false
                onSaveFirebaseConfig?.invoke(pId, key, app)
            },
            onDismiss = { showConfigDialog = false }
        )
    }
}

@Composable
fun FirebaseConfigDialog(
    initialProjectId: String = "",
    initialApiKey: String = "",
    initialAppId: String = "",
    onSaveConfig: (projectId: String, apiKey: String, appId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var projectId by remember { mutableStateOf(initialProjectId) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var appId by remember { mutableStateOf(initialAppId) }
    var parseStatusMessage by remember { mutableStateOf("") }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isNotBlank()) {
                    val root = JsonParser.parseString(jsonString).asJsonObject
                    val projInfo = root.getAsJsonObject("project_info")
                    val parsedProjectId = projInfo.get("project_id")?.asString ?: ""

                    val clients = root.getAsJsonArray("client")
                    var parsedApiKey = ""
                    var parsedAppId = ""
                    if (clients != null && clients.size() > 0) {
                        val clientObj = clients.get(0).asJsonObject
                        val clientInfo = clientObj.getAsJsonObject("client_info")
                        parsedAppId = clientInfo.get("mobilesdk_app_id")?.asString ?: ""

                        val apiKeys = clientObj.getAsJsonArray("api_key")
                        if (apiKeys != null && apiKeys.size() > 0) {
                            parsedApiKey = apiKeys.get(0).asJsonObject.get("current_key")?.asString ?: ""
                        }
                    }

                    if (parsedProjectId.isNotBlank() && parsedApiKey.isNotBlank() && parsedAppId.isNotBlank()) {
                        projectId = parsedProjectId
                        apiKey = parsedApiKey
                        appId = parsedAppId
                        parseStatusMessage = "✓ File google-services.json berhasil dibaca!"
                    } else {
                        parseStatusMessage = "⚠️ Format JSON kurang lengkap."
                    }
                }
            } catch (e: Exception) {
                parseStatusMessage = "⚠️ Gagal membaca file JSON: ${e.message}"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "⚙️ Konfigurasi Firebase Database",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Upload Button
                Button(
                    onClick = { jsonPickerLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkySecondary)
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = "Upload")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📄 Upload google-services.json", fontWeight = FontWeight.Bold)
                }

                if (parseStatusMessage.isNotBlank()) {
                    Text(
                        text = parseStatusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (parseStatusMessage.startsWith("✓")) PinkPrimary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Project ID Firebase") },
                    placeholder = { Text("tagihjaringan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key Firebase") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = { Text("App ID Firebase") },
                    placeholder = { Text("1:475246362649:android:...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (projectId.isNotBlank() && apiKey.isNotBlank() && appId.isNotBlank()) {
                                onSaveConfig(projectId.trim(), apiKey.trim(), appId.trim())
                            }
                        },
                        enabled = projectId.isNotBlank() && apiKey.isNotBlank() && appId.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
