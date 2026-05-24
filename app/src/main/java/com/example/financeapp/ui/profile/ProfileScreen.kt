package com.example.financeapp.ui.profile

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.financeapp.util.AvatarManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    // Biometric capability dialog
    var biometricDialogMessage by remember { mutableStateOf<String?>(null) }
    val languageLabel = languageLabelFor(state.language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        ProfileHeader(
            displayName = state.displayName,
            memberSinceLabel = formatMemberSince(state.memberSince),
            avatarId = state.selectedAvatarId,
            onEditPhoto = {
                showAvatarPicker = true
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

    if (showAvatarPicker) {
        AvatarPickerDialog(
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { id ->
                viewModel.updateAvatar(id)
                showAvatarPicker = false
            },
            selectedAvatarId = state.selectedAvatarId
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
    avatarId: String,
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
                    ProfileAvatar(avatarId)
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
private fun ProfileAvatar(avatarId: String) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = AvatarManager.getAvatarResource(avatarId)),
            contentDescription = "Profile photo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    selectedAvatarId: String
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Avatar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AvatarManager.predefinedAvatars.keys.toList()) { id ->
                    val isSelected = id == selectedAvatarId
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAvatarSelected(id) },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = AvatarManager.getAvatarResource(id)),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun languageLabelFor(code: String): String {
    return when (code.lowercase()) {
        "ta" -> "Tamil"
        "si" -> "Sinhala"
        else -> "English"
    }
}
