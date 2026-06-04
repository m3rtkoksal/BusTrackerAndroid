package com.mikatechnology.BusTracker.services

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.ActivityRecognitionResult
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Hareket sensörü — araçta mı? (konum paylaşımı olmadan özet, iOS ile aynı mantık). */
object MotionActivityService {
    private const val UPLOAD_INTERVAL_MS = 45_000L
    private const val SEGMENT_RETENTION_MS = 8 * 60 * 1000L
    private const val ACTIVITY_UPDATE_INTERVAL_MS = 30_000L
    private const val REQUEST_CODE_TRANSITION = 7101
    private const val REQUEST_CODE_UPDATES = 7102

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private var appContext: Context? = null
    private var role: MotionActivityRole? = null
    private var groupID: String? = null
    private var memberID: String? = null

    private var openAutomotiveStartedAt: Date? = null
    private val segments = mutableListOf<MotionActivitySegment>()
    private var uploadJob: Job? = null
    private var transitionPendingIntent: PendingIntent? = null
    private var updatesPendingIntent: PendingIntent? = null

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        refreshAuthorization(context)
    }

    fun refreshAuthorization(context: Context) {
        _isAuthorized.value = hasActivityRecognitionPermission(context)
    }

    fun hasActivityRecognitionPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAvailable(context: Context): Boolean {
        return runCatching {
            ActivityRecognition.getClient(context.applicationContext)
        }.isSuccess
    }

    fun updateMonitoring(
        context: Context,
        isEnabled: Boolean,
        role: MotionActivityRole,
        groupID: String,
        memberID: String
    ) {
        initialize(context)
        refreshAuthorization(context)

        if (!isEnabled || groupID.isBlank() || memberID.isBlank()) {
            stopMonitoring()
            return
        }

        if (_isMonitoring.value &&
            this.role == role &&
            this.groupID == groupID &&
            this.memberID == memberID
        ) {
            return
        }

        stopMonitoring()
        this.role = role
        this.groupID = groupID
        this.memberID = memberID
        startMonitoring(context)
    }

    fun stopMonitoring() {
        uploadJob?.cancel()
        uploadJob = null

        val context = appContext
        if (context != null) {
            val client = ActivityRecognition.getClient(context)
            transitionPendingIntent?.let { client.removeActivityTransitionUpdates(it) }
            updatesPendingIntent?.let { client.removeActivityUpdates(it) }
        }

        transitionPendingIntent = null
        updatesPendingIntent = null
        openAutomotiveStartedAt = null
        segments.clear()
        role = null
        groupID = null
        memberID = null
        _isMonitoring.value = false
    }

    private fun startMonitoring(context: Context) {
        val appCtx = context.applicationContext
        if (!isAvailable(appCtx)) return
        if (!hasActivityRecognitionPermission(appCtx)) return

        scope.launch {
            runCatching {
                val client = ActivityRecognition.getClient(appCtx)

                val transitionIntent = pendingIntent(appCtx, REQUEST_CODE_TRANSITION)
                transitionPendingIntent = transitionIntent
                val transitions = listOf(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build(),
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
                )
                client.requestActivityTransitionUpdates(
                    ActivityTransitionRequest(transitions),
                    transitionIntent
                ).await()

                val updatesIntent = pendingIntent(appCtx, REQUEST_CODE_UPDATES)
                updatesPendingIntent = updatesIntent
                client.requestActivityUpdates(ACTIVITY_UPDATE_INTERVAL_MS, updatesIntent).await()
            }.onFailure {
                stopMonitoring()
                return@launch
            }

            _isMonitoring.value = true
            startUploadLoop()
        }
    }

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MotionActivityReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    internal fun onTransitionResult(result: ActivityTransitionResult) {
        val now = Date()
        for (event in result.transitionEvents) {
            if (event.activityType != DetectedActivity.IN_VEHICLE) continue
            when (event.transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                    if (openAutomotiveStartedAt == null) {
                        openAutomotiveStartedAt = now
                    }
                }
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                    closeOpenSegment(now)
                }
            }
        }
        pruneSegments(now)
    }

    internal fun onActivityResult(result: ActivityRecognitionResult) {
        applyAutomotiveState(isAutomotive(result), Date())
    }

    private fun applyAutomotiveState(automotive: Boolean, now: Date) {
        if (automotive) {
            if (openAutomotiveStartedAt == null) {
                openAutomotiveStartedAt = now
            }
        } else {
            closeOpenSegment(now)
        }
        pruneSegments(now)
    }

    private fun closeOpenSegment(now: Date) {
        val started = openAutomotiveStartedAt ?: return
        segments.add(MotionActivitySegment(startedAt = started, endedAt = now, isAutomotive = true))
        openAutomotiveStartedAt = null
    }

    private fun isAutomotive(result: ActivityRecognitionResult): Boolean {
        val activities = result.probableActivities
        val vehicle = activities.firstOrNull { it.type == DetectedActivity.IN_VEHICLE } ?: return false
        val stationary = activities.firstOrNull { it.type == DetectedActivity.STILL }
        if (stationary != null && stationary.confidence > vehicle.confidence) return false
        return vehicle.confidence >= 50
    }

    private fun pruneSegments(now: Date) {
        val cutoff = Date(now.time - SEGMENT_RETENTION_MS)
        segments.removeAll { it.endedAt.before(cutoff) }
    }

    private fun startUploadLoop() {
        uploadJob?.cancel()
        uploadJob = scope.launch {
            uploadSnapshot()
            while (true) {
                delay(UPLOAD_INTERVAL_MS)
                uploadSnapshot()
            }
        }
    }

    private suspend fun uploadSnapshot() {
        val currentRole = role ?: return
        val currentGroupID = groupID ?: return
        val currentMemberID = memberID ?: return

        val now = Date()
        pruneSegments(now)

        val workingSegments = segments.toMutableList()
        openAutomotiveStartedAt?.let { openStart ->
            workingSegments.add(
                MotionActivitySegment(startedAt = openStart, endedAt = now, isAutomotive = true)
            )
        }

        val automotiveSeconds = workingSegments
            .filter { it.isAutomotive }
            .sumOf { (it.endedAt.time - it.startedAt.time) / 1000 }
            .toInt()

        runCatching {
            ShuttleStore.shared.uploadMotionActivitySnapshot(
                groupID = currentGroupID,
                role = currentRole,
                memberID = currentMemberID,
                automotiveSecondsInWindow = automotiveSeconds,
                segments = workingSegments
            )
        }
    }
}
