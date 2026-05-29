package com.mikatechnology.BusTracker.ui.driver

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun DriverAlwaysLocationGuideSheet(
    waitingForSettingsReturn: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(NeonTheme.SurfaceContainer)
            .padding(horizontal = 28.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 48.dp, height = 4.dp)
                .clip(CircleShape)
                .background(NeonTheme.SurfaceContainerHigh)
        )

        Text(
            text = "Servis için konum izni",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (waitingForSettingsReturn) {
                "Ayarlar açıldıysa aşağıdaki adımları uygulayın, sonra bu ekrana dönün."
            } else {
                "Yolcular sizi haritada görebilsin diye tek seferlik izin gerekir."
            },
            fontSize = 14.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        GuideStep(number = 1, text = "Açılan pencerede Konum veya İzinler'e dokunun.")
        GuideStep(number = 2, text = "\"Her zaman izin ver\" seçeneğini işaretleyin.")
        GuideStep(number = 3, text = "Geri gelip Servisi başlat'a tekrar basın.")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonTheme.Primary.copy(alpha = 0.15f))
                .border(1.dp, NeonTheme.Primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable(onClick = onRequestPermission)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "İZİN VER",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = NeonTheme.Primary
            )
        }

        Text(
            text = "Pencere açılmadıysa",
            fontSize = 12.sp,
            color = NeonTheme.OnSurfaceVariant
        )

        Text(
            text = "Ayarlara git",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.Secondary,
            modifier = Modifier.clickable(onClick = onOpenSettings)
        )

        Text(
            text = "Vazgeç",
            fontSize = 13.sp,
            color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onDismiss)
        )
    }
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NeonTheme.Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.Primary
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.OnSurface,
            modifier = Modifier.weight(1f),
            lineHeight = 21.sp
        )
    }
}
