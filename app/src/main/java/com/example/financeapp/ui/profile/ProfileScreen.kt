package com.example.financeapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.fragment.app.FragmentActivity
import com.example.financeapp.util.BiometricHelper
import com.example.financeapp.util.BiometricStatus
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    // Biometric capability dialog
    var biometricDialogMessage by remember { mutableStateOf<String?>(null) }
    val languageLabel = languageLabelFor(state.language)
    val currencyLabel = state.currency.ifBlank { "LKR" }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                viewModel.onProfilePhotoSelected(context, uri)
            }
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProfileHeader(
            displayName = state.displayName,
            memberSinceLabel = formatMemberSince(state.memberSince),
            localPhotoPath = state.localProfilePhotoPath,
            onEditPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        SettingsSection(
            title = "General",
            items = listOf(
                SettingItem.Toggle(
                    label = "Dark Mode",
                    icon = Icons.Outlined.DarkMode,
                    checked = state.isDarkMode,
                    onToggle = viewModel::setDarkMode
                ),
                SettingItem.Navigation(
                    label = "Language",
                    icon = Icons.Outlined.Language,
                    value = languageLabel,
                    onClick = { showLanguagePicker = true }
                ),
                SettingItem.Navigation(
                    label = "Currency",
                    icon = Icons.Outlined.Payments,
                    value = currencyLabel,
                    onClick = { showCurrencyPicker = true }
                )
            )
        )

        SettingsSection(
            title = "Security",
            items = listOf(
                SettingItem.Navigation(
                    label = "Change Password",
                    icon = Icons.Outlined.Lock,
                    value = null,
                    onClick = { }
                ),
                SettingItem.Toggle(
                    label = "Biometrics",
                    icon = Icons.Outlined.Fingerprint,
                    checked = state.biometricsEnabled,
                    onToggle = { enabling ->
                        if (!enabling) {
                            // Turning off — no check needed.
                            viewModel.setBiometricsEnabled(false)
                        } else {
                            // Turning on — verify device capability first, then confirm
                            // with a real fingerprint scan before persisting the setting.
                            when (BiometricHelper.checkStatus(context)) {
                                BiometricStatus.Available -> {
                                    BiometricHelper.authenticate(
                                        activity = activity,
                                        title = "Enable Fingerprint Login",
                                        subtitle = "Confirm your fingerprint to enable biometric unlock",
                                        negativeButtonText = "Cancel",
                                        onSuccess = { viewModel.setBiometricsEnabled(true) },
                                    )
                                }
                                BiometricStatus.NotEnrolled -> {
                                    biometricDialogMessage =
                                        "No fingerprints are set up on this device.\n\n" +
                                        "Go to Settings → Security → Fingerprint to add one, then try again."
                                }
                                BiometricStatus.NoHardware,
                                BiometricStatus.Unavailable,
                                -> {
                                    biometricDialogMessage =
                                        "This device does not support biometric authentication."
                                }
                            }
                        }
                    }
                )
            )
        )

        SettingsSection(
            title = "Legal",
            items = listOf(
                SettingItem.Navigation(
                    label = "Privacy Policy",
                    icon = Icons.Outlined.Policy,
                    value = null,
                    onClick = { }
                ),
                SettingItem.Navigation(
                    label = "Terms of Service",
                    icon = Icons.Outlined.Description,
                    value = null,
                    onClick = { }
                )
            )
        )

        LogoutButton(
            onLogout = {
                viewModel.logout()
                onLogout()
            }
        )
    }

    if (showLanguagePicker) {
        OptionPickerDialog(
            title = "Select language",
            options = listOf(
                "English" to "en",
                "Tamil" to "ta",
                "Sinhala" to "si",
            ),
            selectedValue = state.language,
            onDismiss = { showLanguagePicker = false },
            onSelect = { value ->
                viewModel.setLanguage(value)
                showLanguagePicker = false
            }
        )
    }

    if (showCurrencyPicker) {
        OptionPickerDialog(
            title = "Select currency",
            options = listOf(
                "USD" to "USD",
                "LKR" to "LKR",
            ),
            selectedValue = state.currency,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { value ->
                viewModel.setCurrency(value)
                showCurrencyPicker = false
            }
        )
    }

    // Biometric unavailable info dialog
    biometricDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { biometricDialogMessage = null },
            title = { Text("Biometrics Unavailable") },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { biometricDialogMessage = null }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    memberSinceLabel: String,
    localPhotoPath: String,
    onEditPhoto: () -> Unit,
) {
    GlassCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    val editBadgeColor = Color(0xFF2ECC71)
                    ProfileAvatar(localPhotoPath = localPhotoPath)
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit photo",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(editBadgeColor)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { onEditPhoto() }
                            .padding(4.dp),
                        tint = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayName.ifBlank { "User" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = memberSinceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(localPhotoPath: String) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (localPhotoPath.isNotBlank()) {
            AsyncImage(
                model = File(localPhotoPath),
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile placeholder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingItem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
            )
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingRow(item)
                    if (index < items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item is SettingItem.Navigation) {
                if (item is SettingItem.Navigation) {
                    item.onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (item is SettingItem.Toggle) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        when (item) {
            is SettingItem.Navigation -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.value.isNullOrBlank()) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is SettingItem.Toggle -> {
                Switch(
                    checked = item.checked,
                    onCheckedChange = item.onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.secondary,
                        checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    val logoutContainer = Color(0xFF3B1212)
    Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(
            containerColor = logoutContainer,
            contentColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Log Out",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            content()
        }
    }
}

private fun formatMemberSince(memberSince: Long): String {
    if (memberSince <= 0L) {
        return "MEMBER SINCE --"
    }
    val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
    val date = Instant.ofEpochMilli(memberSince)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return "MEMBER SINCE ${formatter.format(date)}"
}

private sealed class SettingItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    class Navigation(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        val value: String?,
        val onClick: () -> Unit,
    ) : SettingItem(label, icon)

    class Toggle(
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        val checked: Boolean,
        val onToggle: (Boolean) -> Unit,
    ) : SettingItem(label, icon)
}

@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (label, value) ->
                    TextButton(onClick = { onSelect(value) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (value == selectedValue) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

private fun languageLabelFor(code: String): String {
    return when (code.lowercase()) {
        "ta" -> "Tamil"
        "si" -> "Sinhala"
        else -> "English"
    }
}
