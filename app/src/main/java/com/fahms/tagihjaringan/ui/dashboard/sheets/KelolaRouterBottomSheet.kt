package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fahms.tagihjaringan.data.model.MikrotikRouter
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.RedDanger
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelolaRouterBottomSheet(
    routers: List<MikrotikRouter>,
    existingProfiles: List<String> = emptyList(),
    onSaveRouter: (MikrotikRouter) -> Unit,
    onDeleteRouter: (routerId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRouterForEdit by remember { mutableStateOf<MikrotikRouter?>(null) }
    var showRouterFormDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐 Kelola Router MikroTik",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Button(
                    onClick = {
                        selectedRouterForEdit = null
                        showRouterFormDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Router")
                }
            }

            if (routers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada router terdaftar. Silakan tambahkan router di bawah.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(routers, key = { it.id }) { routerItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Router,
                                        contentDescription = "Router",
                                        tint = SkySecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = routerItem.name.ifBlank { "Router MikroTik" },
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (routerItem.version == "v6") Color(0xFFE11D48) else SkySecondary
                                            ) {
                                                Text(
                                                    text = routerItem.version.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "IP: ${routerItem.ip} • Port: ${routerItem.apiPort} • Profil: ${routerItem.targetProfile.ifBlank { "Semua" }}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            selectedRouterForEdit = routerItem
                                            showRouterFormDialog = true
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = SkySecondary)
                                    }

                                    IconButton(
                                        onClick = { onDeleteRouter(routerItem.id) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = RedDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRouterFormDialog) {
        RouterFormDialog(
            routerForEdit = selectedRouterForEdit,
            existingProfiles = existingProfiles,
            onSave = { router ->
                showRouterFormDialog = false
                onSaveRouter(router)
            },
            onDismiss = { showRouterFormDialog = false }
        )
    }
}

@Composable
fun RouterFormDialog(
    routerForEdit: MikrotikRouter?,
    existingProfiles: List<String>,
    onSave: (MikrotikRouter) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(routerForEdit?.name ?: "") }
    var ip by remember { mutableStateOf(routerForEdit?.ip ?: "") }
    var user by remember { mutableStateOf(routerForEdit?.user ?: "") }
    var pass by remember { mutableStateOf(routerForEdit?.pass ?: "") }
    var targetProfile by remember { mutableStateOf(routerForEdit?.targetProfile ?: "") }
    var version by remember { mutableStateOf(routerForEdit?.version ?: "v7") }
    var apiPortText by remember { mutableStateOf(routerForEdit?.apiPort?.toString() ?: if (version == "v6") "8728" else "443") }

    val isEditing = routerForEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isEditing) "📝 Edit Router MikroTik" else "🌐 Tambah Router MikroTik",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Version Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tipe RouterOS:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    FilterChip(
                        selected = version == "v7",
                        onClick = {
                            version = "v7"
                            if (apiPortText == "8728") apiPortText = "443"
                        },
                        label = { Text("v7 (REST API)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = version == "v6",
                        onClick = {
                            version = "v6"
                            if (apiPortText == "443") apiPortText = "8728"
                        },
                        label = { Text("v6 (Socket 8728)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Router (misal: Router Sumbergadung)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP / Host API Router") },
                        placeholder = { Text("192.168.1.1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = apiPortText,
                        onValueChange = { apiPortText = it.filter { c -> c.isDigit() } },
                        label = { Text("Port API") },
                        placeholder = { Text("8089") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username Router") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password Router") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = targetProfile,
                        onValueChange = { targetProfile = it },
                        label = { Text("Target Profil Wilayah (Kosongkan jika Semua)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (existingProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Klik untuk memilih profil:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingProfiles.forEach { prof ->
                                FilterChip(
                                    selected = targetProfile.contains(prof, ignoreCase = true),
                                    onClick = {
                                        targetProfile = if (targetProfile.isBlank()) prof else if (!targetProfile.contains(prof, ignoreCase = true)) "$targetProfile, $prof" else targetProfile
                                    },
                                    label = { Text("+ $prof", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

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
                            if (name.isNotBlank() && ip.isNotBlank()) {
                                var formattedIp = ip.trim()
                                var extractedPort: Int? = null

                                val scheme = if (formattedIp.startsWith("https://")) "https://" else if (formattedIp.startsWith("http://")) "http://" else ""
                                val noScheme = formattedIp.replace("https://", "").replace("http://", "")

                                if (noScheme.contains(":")) {
                                    val parts = noScheme.split(":")
                                    val hostOnly = parts[0].split("/")[0].trim()
                                    extractedPort = parts[1].filter { c -> c.isDigit() }.toIntOrNull()
                                    formattedIp = if (scheme.isNotBlank()) "$scheme$hostOnly" else hostOnly
                                }

                                val parsedPort = apiPortText.toIntOrNull() ?: extractedPort ?: (if (version == "v6") 8728 else 443)

                                val item = routerForEdit?.copy(
                                    name = name.trim(),
                                    ip = formattedIp,
                                    user = user.trim(),
                                    pass = pass.trim(),
                                    targetProfile = targetProfile.trim(),
                                    version = version,
                                    apiPort = parsedPort
                                ) ?: MikrotikRouter(
                                    name = name.trim(),
                                    ip = formattedIp,
                                    user = user.trim(),
                                    pass = pass.trim(),
                                    targetProfile = targetProfile.trim(),
                                    version = version,
                                    apiPort = parsedPort
                                )
                                onSave(item)
                            }
                        },
                        enabled = name.isNotBlank() && ip.isNotBlank(),
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
