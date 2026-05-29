package com.mikatechnology.BusTracker.ui.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun MyServicesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by UserSessionRepository.profile.collectAsStateWithLifecycle()
    val store = ShuttleStore.shared
    val isTripActive by store.isTripActive.collectAsStateWithLifecycle()
    val activeTripGroupID by store.currentActiveTripGroupID.collectAsStateWithLifecycle()

    // Build services from real profile data
    val allServices = buildServicesFromProfile(profile)

    // Active services:
    // - Explicitly marked in profile.activeGroupIDs while trip live, OR
    // - The group that is currently live in the store (covers legacy single-group + current trip state)
    val activeServices = allServices.filter { service ->
        val explicitlyActive = profile?.activeGroupIDs?.contains(service.id) == true
        val isCurrentLiveTrip = isTripActive && (service.id == activeTripGroupID)
        (explicitlyActive || isCurrentLiveTrip) && isTripActive
    }

    // Other registered services = user's services that are NOT currently active
    val otherServices = allServices.filter { service ->
        val belongsToUser = profile?.groupIDs?.contains(service.id) == true ||
                            profile?.groupID == service.id

        val explicitlyActive = profile?.activeGroupIDs?.contains(service.id) == true
        val isCurrentLiveTrip = isTripActive && (service.id == activeTripGroupID)

        val isCurrentlyActive = belongsToUser &&
                (explicitlyActive || isCurrentLiveTrip) &&
                isTripActive

        belongsToUser && !isCurrentlyActive
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MyServicesBackButton(onClick = onBack)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Big Title
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                Text(
                    text = "SERVİSLERİM",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonTheme.OnSurface,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(NeonTheme.Primary)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Active Service Card(s) - only when trip is live
            if (activeServices.isNotEmpty()) {
                activeServices.forEach { service ->
                    ActiveServiceCard(service = service)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Registered Routes Section
            if (otherServices.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(14.dp)
                            .background(NeonTheme.Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KAYITLI ROTALAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }

                otherServices.forEach { service ->
                    ServiceCard(service = service)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                // Empty state for no other routes
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(14.dp)
                            .background(NeonTheme.Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KAYITLI ROTALAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }

                EmptyRoutesCard()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Yeni Servis Ekle Button
            Button(
                onClick = {
                    // TODO: Servis ekleme akışı
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTheme.Background,
                    contentColor = NeonTheme.Primary
                ),
                border = BorderStroke(2.dp, NeonTheme.Primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "YENİ SERVİS EKLE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActiveServiceCard(service: ServiceDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NeonTheme.SurfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonTheme.Secondary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row {
                Text(
                    text = "SİSTEM AKTİF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = NeonTheme.Background,
                    modifier = Modifier
                        .background(NeonTheme.Secondary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonTheme.Secondary
                    )

                    if (service.departure != null) {
                        Text(
                            text = "Başlangıç: ${service.departure}",
                            fontSize = 12.sp,
                            color = NeonTheme.OnSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Sürücü servisi başlattı",
                            fontSize = 12.sp,
                            color = NeonTheme.OnSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "AKTİF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.Secondary,
                    modifier = Modifier
                        .background(NeonTheme.Secondary.copy(alpha = 0.15f))
                        .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NeonTheme.SurfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeonTheme.OnSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.OnSurface
                )
            }

            Button(
                onClick = {
                    // TODO: Switch to this service
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTheme.SurfaceContainerHighest,
                    contentColor = NeonTheme.Primary
                ),
                border = BorderStroke(1.dp, NeonTheme.Primary.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "GEÇİŞ YAP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyRoutesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NeonTheme.SurfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeonTheme.Outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Başka rota yok",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Yeni bir servis ekleyerek rotalarını genişletebilirsin.",
                fontSize = 13.sp,
                color = NeonTheme.OnSurfaceVariant
            )
        }
    }
}

private fun buildServicesFromProfile(profile: UserProfile?): List<ServiceDisplay> {
    if (profile == null) return emptyList()

    val groupIDs = if (profile.groupIDs.isNotEmpty()) {
        profile.groupIDs
    } else {
        listOfNotNull(profile.groupID).filter { it.isNotBlank() }
    }

    return groupIDs.map { gid ->
        val isActiveForUser = profile.activeGroupIDs.contains(gid)
        val name = if (gid == profile.groupID) {
            profile.groupName.takeIf { it.isNotBlank() } ?: "Servis"
        } else {
            "Servis ${gid.take(6)}"
        }

        ServiceDisplay(
            id = gid,
            name = name,
            isActive = isActiveForUser,
            departure = null
        )
    }
}

data class ServiceDisplay(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val departure: String?
)

@Composable
private fun MyServicesBackButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = "Geri",
            tint = NeonTheme.Secondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
