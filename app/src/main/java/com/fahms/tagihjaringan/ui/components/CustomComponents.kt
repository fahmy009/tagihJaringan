package com.fahms.tagihjaringan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fahms.tagihjaringan.ui.theme.*

/**
 * Top App Bar with gradient background from Pink Accent to Sky Blue
 * with 4 action icons on the right: Sync, Edit, Keuangan, Settings
 */
@Composable
fun GradientTopAppBar(
    onSyncClick: () -> Unit,
    onIsolirMassalClick: () -> Unit,
    onEditPriceClick: () -> Unit,
    onKeuanganClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(PinkPrimary, SkySecondary)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "tagihJaringan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "MikroTik & Sheets Billing",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync Cloud",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onIsolirMassalClick) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Isolir Massal",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onEditPriceClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Harga",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onKeuanganClick) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Keuangan",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Custom rounded circular Checkbox with smooth animated color transition to Pink
 */
@Composable
fun CustomRoundedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) PinkPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "CheckboxBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) PinkPrimary else Color(0xFFCBD5E1),
        animationSpec = tween(durationMillis = 250),
        label = "CheckboxBorderColor"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

enum class DialogType {
    SUCCESS,
    ERROR,
    WARNING,
    CONFIRMATION,
    LOADING
}

/**
 * Custom SweetAlert style Dialog
 */
@Composable
fun SweetAlertDialog(
    type: DialogType,
    title: String,
    message: String,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = "OK",
    dismissText: String = "Batal"
) {
    Dialog(onDismissRequest = { onDismiss?.invoke() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon according to type
                when (type) {
                    DialogType.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PriceGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✓", fontSize = 28.sp, color = PriceGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    DialogType.ERROR -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RedDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✕", fontSize = 28.sp, color = RedDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                    DialogType.WARNING -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WarningYellow.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "!", fontSize = 28.sp, color = WarningYellow, fontWeight = FontWeight.Bold)
                        }
                    }
                    DialogType.CONFIRMATION -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SkySecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "?", fontSize = 28.sp, color = SkySecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                    DialogType.LOADING -> {
                        CircularProgressIndicator(
                            color = PinkPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                // Message
                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Action Buttons (if not LOADING)
                if (type != DialogType.LOADING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onDismiss != null && type == DialogType.CONFIRMATION) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(dismissText)
                            }
                        }

                        Button(
                            onClick = {
                                onConfirm?.invoke() ?: onDismiss?.invoke()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (type) {
                                    DialogType.ERROR -> RedDanger
                                    DialogType.WARNING -> WarningYellow
                                    DialogType.SUCCESS -> PriceGreen
                                    else -> PinkPrimary
                                }
                            )
                        ) {
                            Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
