package com.mikatechnology.BusTracker.services

/** Process başına sürücü/kayıt bildirimi en fazla bir kez; yolcu aksiyonları ayrı akışta sorulur. */
object PermissionPromptSession {
    @Volatile
    var locationPromptHandledThisLaunch: Boolean = false

    @Volatile
    var motionPromptHandledThisLaunch: Boolean = false

    @Volatile
    var notificationPromptHandledThisLaunch: Boolean = false

    val mayPromptLocation: Boolean
        get() = !locationPromptHandledThisLaunch

    val mayPromptMotion: Boolean
        get() = !motionPromptHandledThisLaunch

    val mayPromptNotifications: Boolean
        get() = !notificationPromptHandledThisLaunch

    fun markLocationPromptHandled() {
        locationPromptHandledThisLaunch = true
    }

    fun markMotionPromptHandled() {
        motionPromptHandledThisLaunch = true
    }

    fun markNotificationPromptHandled() {
        notificationPromptHandledThisLaunch = true
    }
}
