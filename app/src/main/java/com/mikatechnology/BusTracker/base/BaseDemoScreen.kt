package com.mikatechnology.BusTracker.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BaseDemoViewModel : BaseViewModel() {
    init {
        configureScreen(
            title = "ServisTakip",
            subtitle = "Base katman demo",
            navSubtitleStyle = NavSubtitleStyle.NeonCaps,
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            hidesNavigationBar = true
        )
    }

    fun demoLoading() {
        setLoading(true, "Bağlanıyor...")
        viewModelScope.launch {
            delay(1_500)
            setLoading(false)
        }
    }

    fun stopLoading() {
        setLoading(false)
    }
}

@Composable
fun BaseDemoScreen(
    viewModel: BaseDemoViewModel = viewModel()
) {
    BaseViewShell(viewModel = viewModel) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BaseView + BaseViewModel hazır",
                color = NeonTheme.OnSurface
            )

            Button(
                onClick = { viewModel.showSuccess("Toast mesajı") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonTheme.Secondary)
            ) {
                Text("Toast göster")
            }

            OutlinedButton(
                onClick = { viewModel.showError("Örnek hata mesajı") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hata dialog")
            }

            OutlinedButton(
                onClick = {
                    viewModel.showConfirm(
                        title = "Çıkış Yap",
                        message = "Emin misiniz?",
                        confirmTitle = "Çıkış",
                        destructive = true,
                        onConfirm = { viewModel.showSuccess("Onaylandı") }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Onay dialog")
            }

            OutlinedButton(
                onClick = { viewModel.demoLoading() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Loading aç")
            }

            OutlinedButton(
                onClick = { viewModel.stopLoading() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Loading kapat")
            }
        }
    }
}
