package com.mikatechnology.BusTracker.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class BaseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BaseUiState())
    val uiState: StateFlow<BaseUiState> = _uiState.asStateFlow()

    private var confirmHandler: (() -> Unit)? = null

    protected fun updateState(transform: (BaseUiState) -> BaseUiState) {
        _uiState.update(transform)
    }

    fun configureScreen(
        title: String,
        subtitle: String = "",
        navSubtitleStyle: NavSubtitleStyle = NavSubtitleStyle.Hidden,
        navigationBarStyle: NavigationBarStyle = NavigationBarStyle.Primary,
        showsBackButton: Boolean = false,
        usesLargeTitle: Boolean = true,
        hidesNavigationBar: Boolean = false,
        embedsInNavigationStack: Boolean = true,
        usesCustomNavHeader: Boolean? = null,
        contentScrollEnabled: Boolean = true
    ) {
        updateState {
            it.copy(
                title = title,
                subtitle = subtitle,
                navSubtitleStyle = navSubtitleStyle,
                navigationBarStyle = navigationBarStyle,
                showsBackButton = showsBackButton,
                usesLargeTitle = usesLargeTitle,
                hidesNavigationBar = hidesNavigationBar,
                embedsInNavigationStack = embedsInNavigationStack,
                usesCustomNavHeader = usesCustomNavHeader,
                contentScrollEnabled = contentScrollEnabled
            )
        }
    }

    fun showError(message: String, title: String = "Hata") {
        updateState {
            it.copy(
                alert = PopupPresentation(
                    style = PopupStyle.Error,
                    title = title,
                    message = message
                )
            )
        }
    }

    fun showSuccess(message: String, title: String = "Başarılı") {
        updateState {
            it.copy(
                toast = PopupPresentation(
                    style = PopupStyle.Success,
                    title = title,
                    message = message
                )
            )
        }
    }

    fun showInfo(message: String, title: String = "Bilgi") {
        updateState {
            it.copy(
                alert = PopupPresentation(
                    style = PopupStyle.Info,
                    title = title,
                    message = message
                )
            )
        }
    }

    fun showConfirm(
        title: String,
        message: String,
        confirmTitle: String = "Onayla",
        cancelTitle: String = "Vazgeç",
        destructive: Boolean = false,
        onConfirm: () -> Unit
    ) {
        confirmHandler = onConfirm
        updateState {
            it.copy(
                confirmDialog = ConfirmPresentation(
                    title = title,
                    message = message,
                    confirmTitle = confirmTitle,
                    cancelTitle = cancelTitle,
                    isDestructive = destructive
                )
            )
        }
    }

    fun confirmDialogAction() {
        confirmHandler?.invoke()
        clearConfirmDialog()
    }

    fun clearConfirmDialog() {
        confirmHandler = null
        updateState { it.copy(confirmDialog = null) }
    }

    fun clearAlert() {
        updateState { it.copy(alert = null) }
    }

    fun clearToast() {
        updateState { it.copy(toast = null) }
    }

    fun setLoading(loading: Boolean, message: String = "Yükleniyor...") {
        updateState {
            it.copy(
                isLoading = loading,
                loadingMessage = message
            )
        }
    }
}
