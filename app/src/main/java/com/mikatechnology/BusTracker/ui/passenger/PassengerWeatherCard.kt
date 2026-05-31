package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.services.PassengerWeatherCardModel
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PassengerClothingAdviceCard(
    model: PassengerWeatherCardModel?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                color = NeonTheme.Secondary.copy(alpha = 0.22f),
                shape = RectangleShape
            )
            .padding(16.dp)
    ) {
        Text(
            text = "GİYİM ÖNERİSİ",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        when {
            model != null -> {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = model.emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = model.advice,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonTheme.OnSurface,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = model.contextLine,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonTheme.OnSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            isLoading -> {
                Text(
                    text = "Biniş noktana göre öneri hazırlanıyor…",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant
                )
            }
            else -> {
                Text(
                    text = "Öneri şu an alınamadı.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant
                )
            }
        }
    }
}
