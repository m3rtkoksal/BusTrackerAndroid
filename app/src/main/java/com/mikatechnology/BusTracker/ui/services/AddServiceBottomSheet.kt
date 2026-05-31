package com.mikatechnology.BusTracker.ui.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.registration.NeonFormField
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun AddServiceBottomSheet(
    serviceCode: String,
    onServiceCodeChange: (String) -> Unit,
    isLoading: Boolean,
    errorText: String?,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer)
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(NeonTheme.SurfaceContainerHighest)
        )

        Text(
            text = "Yeni servis ekle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface
        )

        Text(
            text = "Sürücünün verdiği servis kodunu girin. Ekledikten sonra bu servis aktif olur.",
            fontSize = 14.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        NeonFormField(
            title = "Servis kodu",
            value = serviceCode,
            onValueChange = onServiceCodeChange,
            placeholder = "6 haneli kod",
            errorText = errorText
        )

        Button(
            onClick = onJoin,
            enabled = !isLoading && serviceCode.trim().length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonTheme.Secondary.copy(alpha = 0.15f),
                contentColor = NeonTheme.Secondary,
                disabledContainerColor = NeonTheme.Secondary.copy(alpha = 0.08f),
                disabledContentColor = NeonTheme.OnSurfaceVariant
            ),
            shape = RectangleShape,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NeonTheme.Secondary.copy(alpha = 0.45f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = NeonTheme.Secondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp)
                )
            } else {
                Text(
                    text = "SERVİSE KATIL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        TextButton(onClick = onDismiss) {
            Text(
                text = "Vazgeç",
                color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
