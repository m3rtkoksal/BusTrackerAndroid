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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.delay

@Composable
fun OTPVerificationSheetContent(
    formattedPhone: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    buttonText: String = "ONAYLA VE OLUŞTUR",
    modifier: Modifier = Modifier
) {
    var resendCooldown by remember { mutableIntStateOf(34) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    LaunchedEffect(otpCode) {
        if (otpCode.length == 6 && !isLoading) {
            onSubmit()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            if (resendCooldown > 0) resendCooldown -= 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NeonTheme.SurfaceContainerHigh)
                    .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = NeonTheme.Secondary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = "Kod Doğrulama",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = NeonTheme.Secondary.copy(alpha = 0.9f),
                            fontFamily = FontFamily.Monospace
                        )
                    ) {
                        append(formattedPhone)
                    }
                    append(" numarasına tek kullanımlık kod gönderdik.")
                },
                fontSize = 14.sp,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 28.dp)
            )

            NeonOtpInput(
                code = otpCode,
                onCodeChange = onOtpChange,
                focusRequester = focusRequester,
                onSubmit = onSubmit,
                isLoading = isLoading,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonTheme.SurfaceContainerHigh)
                    .border(1.dp, NeonTheme.Primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = otpCode.length >= 6 && !isLoading,
                        onClick = onSubmit
                    )
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NeonTheme.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = buttonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = NeonTheme.Primary.copy(alpha = if (otpCode.length >= 6) 1f else 0.5f)
                    )
                }
            }

            Text(
                text = if (resendCooldown > 0) {
                    "KODU TEKRAR GÖNDER (${resendCooldown}S)"
                } else {
                    "KODU TEKRAR GÖNDER"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = if (resendCooldown > 0) NeonTheme.OnSurfaceVariant else NeonTheme.Secondary,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable(enabled = resendCooldown == 0) {
                        resendCooldown = 34
                        onResend()
                    }
            )
    }
}

@Composable
private fun NeonOtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val length = 6

    BasicTextField(
        value = code,
        onValueChange = { newValue ->
            val filtered = newValue.filter { char -> char.isDigit() }.take(length)
            onCodeChange(filtered)

            // Auto-submit when 6 digits are entered (matching iOS behavior)
            if (filtered.length == length && !isLoading) {
                onSubmit?.invoke()
            }
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val digit = code.getOrNull(index)?.toString() ?: ""
                    val isActive = index == code.length
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonTheme.SurfaceBright.copy(alpha = 0.8f))
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) {
                                    NeonTheme.Secondary
                                } else {
                                    NeonTheme.Outline.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTheme.Primary
                        )
                    }
                }
            }
        }
    )
}
