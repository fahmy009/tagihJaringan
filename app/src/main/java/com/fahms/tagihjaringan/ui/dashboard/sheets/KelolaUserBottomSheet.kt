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
import com.fahms.tagihjaringan.data.local.UserSession
import com.fahms.tagihjaringan.data.model.UserAccount
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.PriceGreen
import com.fahms.tagihjaringan.ui.theme.RedDanger
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelolaUserBottomSheet(
    users: List<UserAccount>,
    userSession: UserSession,
    existingProfiles: List<String> = emptyList(),
    onSaveUser: (username: String, pass: String, role: String, profile: String, name: String, oldUsername: String) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onDismiss: () -> Unit
) {
    val role = userSession.role.uppercase()
    val assignedProfile = userSession.assignedProfile.trim()
    val allowedProfiles = remember(assignedProfile) {
        assignedProfile.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val filteredUsers = remember(users, role, allowedProfiles) {
        if (role != "ADMIN" && allowedProfiles.isNotEmpty()) {
            users.filter { user ->
                allowedProfiles.any { prof -> user.assignedProfile.equals(prof, ignoreCase = true) }
            }
        } else {
            users
        }
    }

    var selectedUserForEdit by remember { mutableStateOf<UserAccount?>(null) }
    var showUserFormDialog by remember { mutableStateOf(false) }

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
                    text = "👥 Kelola User & Akun",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Button(
                    onClick = {
                        selectedUserForEdit = null
                        showUserFormDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah User")
                }
            }

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada user registered.",
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
                    items(filteredUsers, key = { it.username }) { userItem ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = userItem.name.ifBlank { userItem.username },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (userItem.role.uppercase()) {
                                                "ADMIN" -> PinkPrimary
                                                "MITRA" -> SkySecondary
                                                else -> PriceGreen
                                            }
                                        ) {
                                            Text(
                                                text = userItem.role,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "User: ${userItem.username} • Profil: ${userItem.assignedProfile.ifBlank { "Semua" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            selectedUserForEdit = userItem
                                            showUserFormDialog = true
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = SkySecondary)
                                    }

                                    if (userItem.username != "admin") {
                                        IconButton(
                                            onClick = { onDeleteUser(userItem.username) }
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
    }

    if (showUserFormDialog) {
        UserFormDialog(
            userForEdit = selectedUserForEdit,
            currentUserRole = role,
            allowedProfiles = if (role == "ADMIN") existingProfiles else allowedProfiles,
            onSave = { u, p, r, prof, n, oldUser ->
                showUserFormDialog = false
                onSaveUser(u, p, r, prof, n, oldUser)
            },
            onDismiss = { showUserFormDialog = false }
        )
    }
}

@Composable
fun UserFormDialog(
    userForEdit: UserAccount?,
    currentUserRole: String,
    allowedProfiles: List<String>,
    onSave: (username: String, pass: String, role: String, profile: String, name: String, oldUsername: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(userForEdit?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(userForEdit?.name ?: "") }
    var selectedRole by remember {
        mutableStateOf(if (currentUserRole != "ADMIN") "COLLECTOR" else (userForEdit?.role ?: "COLLECTOR"))
    }

    var selectedProfileInput by remember {
        mutableStateOf(
            userForEdit?.assignedProfile?.ifBlank { allowedProfiles.firstOrNull() ?: "" }
                ?: (allowedProfiles.firstOrNull() ?: "")
        )
    }

    val isEditing = userForEdit != null

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
                    text = if (isEditing) "📝 Edit Akun User" else "➕ Tambah User Baru",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "Password Baru (Opsional)" else "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Role selection (Only Admin can change Role)
                if (currentUserRole == "ADMIN") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Role:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        listOf("COLLECTOR", "MITRA", "ADMIN").forEach { r ->
                            FilterChip(
                                selected = selectedRole.equals(r, ignoreCase = true),
                                onClick = { selectedRole = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProfileInput,
                            onValueChange = { selectedProfileInput = it },
                            label = { Text("Profil Wilayah Tugas (Pisahkan Koma jika Multi)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (allowedProfiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Klik untuk memilih profil:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allowedProfiles.forEach { prof ->
                                    FilterChip(
                                        selected = selectedProfileInput.contains(prof, ignoreCase = true),
                                        onClick = {
                                            selectedProfileInput = if (selectedProfileInput.isBlank()) prof else if (!selectedProfileInput.contains(prof, ignoreCase = true)) "$selectedProfileInput, $prof" else selectedProfileInput
                                        },
                                        label = { Text("+ $prof", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = "COLLECTOR (Tukang Tagih)",
                        onValueChange = {},
                        label = { Text("Role") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Profile selection selector for Mitra with single or multiple allowed profiles
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Profil Wilayah Tugas Penagih:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allowedProfiles.forEach { prof ->
                                FilterChip(
                                    selected = selectedProfileInput.equals(prof, ignoreCase = true),
                                    onClick = { selectedProfileInput = prof },
                                    label = { Text(prof, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
                            if (username.isNotBlank()) {
                                onSave(username.trim(), password.trim(), selectedRole.trim(), selectedProfileInput.trim(), name.trim(), userForEdit?.username ?: "")
                            }
                        },
                        enabled = username.isNotBlank() && (isEditing || password.isNotBlank()),
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
