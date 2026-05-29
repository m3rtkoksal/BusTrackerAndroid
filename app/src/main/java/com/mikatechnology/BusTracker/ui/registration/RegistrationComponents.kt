package com.mikatechnology.BusTracker.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun NeonRoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonGlassCard(accent = accent, modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.25f), CircleShape)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEÇ VE DEVAM ET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = accent
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun NeonRegistrationHero(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }

        if (title.isNotBlank()) {
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonTheme.OnSurface,
                textAlign = TextAlign.Center
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(width = 32.dp, height = 1.dp)
                    .background(NeonTheme.OnSurfaceVariant.copy(alpha = 0.3f))
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
            Box(
                Modifier
                    .size(width = 32.dp, height = 1.dp)
                    .background(NeonTheme.OnSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun RegistrationBackButton(
    text: String = "ROL SEÇİMİNE DÖN",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = NeonTheme.OnSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = NeonTheme.OnSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun NeonGlassCard(
    accent: Color? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
fun NeonFormField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.OnSurface
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = NeonTheme.OnSurfaceVariant) },
            singleLine = true,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = NeonTheme.OnSurface,
                unfocusedTextColor = NeonTheme.OnSurface,
                focusedBorderColor = if (errorText != null) {
                    NeonTheme.Error.copy(alpha = 0.8f)
                } else {
                    NeonTheme.Secondary.copy(alpha = 0.5f)
                },
                unfocusedBorderColor = if (errorText != null) {
                    NeonTheme.Error.copy(alpha = 0.6f)
                } else {
                    NeonTheme.Outline.copy(alpha = 0.5f)
                },
                focusedContainerColor = NeonTheme.SurfaceBright.copy(alpha = 0.8f),
                unfocusedContainerColor = NeonTheme.SurfaceBright.copy(alpha = 0.8f)
            )
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = NeonTheme.Error
            )
        }
    }
}

@Composable
fun NeonPhoneFormField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.OnSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NeonTheme.SurfaceBright.copy(alpha = 0.8f))
                .border(1.dp, NeonTheme.Outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "+90",
                fontWeight = FontWeight.Medium,
                color = NeonTheme.Secondary
            )
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(normalizeLocalDigits(it)) },
                placeholder = { Text("5321234567", color = NeonTheme.OnSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonTheme.OnSurface,
                    unfocusedTextColor = NeonTheme.OnSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

private fun normalizeLocalDigits(raw: String): String {
    var digits = raw.filter { it.isDigit() }
    if (digits.startsWith("90") && digits.length > 10) {
        digits = digits.drop(2)
    } else if (digits.startsWith("0") && digits.length > 10) {
        digits = digits.drop(1)
    }
    return digits.take(10)
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.5f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Text("...", color = NeonTheme.OnSurface)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = NeonTheme.OnSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeonTheme.OnSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val isInteractive = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isInteractive) NeonTheme.SurfaceBright
                else NeonTheme.SurfaceBright.copy(alpha = 0.35f)
            )
            .border(
                1.dp,
                NeonTheme.Outline.copy(alpha = if (isInteractive) 0.4f else 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isInteractive, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Text("...", color = NeonTheme.OnSurface)
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = if (isInteractive) {
                    NeonTheme.OnSurface
                } else {
                    NeonTheme.OnSurface.copy(alpha = 0.45f)
                }
            )
        }
    }
}

val DriverHeroIcon = Icons.Default.DirectionsCar
val PassengerHeroIcon = Icons.Default.Person
