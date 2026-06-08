package com.mikatechnology.BusTracker.data.repository

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.firebase.firestore.ListenerRegistration
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.effectiveAttendance
import com.mikatechnology.BusTracker.data.model.HolidayMode
import com.mikatechnology.BusTracker.data.model.ServiceSchedule
import com.mikatechnology.BusTracker.data.model.UpcomingService
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MapDefaults
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.ShuttleMember
import com.mikatechnology.BusTracker.data.model.TripTelemetry
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.MotionActivityRole
import com.mikatechnology.BusTracker.services.MotionActivitySegment
import com.mikatechnology.BusTracker.services.TripMotionAutoStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShuttleStore private constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _members = MutableStateFlow<List<ShuttleMember>>(emptyList())
    val members: StateFlow<List<ShuttleMember>> = _members.asStateFlow()

    private val _driverLocation = MutableStateFlow<DriverLocation?>(null)
    val driverLocation: StateFlow<DriverLocation?> = _driverLocation.asStateFlow()

    private val _driverRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val driverRoute: StateFlow<List<LatLng>> = _driverRoute.asStateFlow()

    private val _canonicalMorningRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val canonicalMorningRoute: StateFlow<List<LatLng>> = _canonicalMorningRoute.asStateFlow()

    private val _isCanonicalMorningRouteReady = MutableStateFlow(false)
    val isCanonicalMorningRouteReady: StateFlow<Boolean> = _isCanonicalMorningRouteReady.asStateFlow()

    private val _morningPickups = MutableStateFlow<List<MorningPickup>>(emptyList())
    val morningPickups: StateFlow<List<MorningPickup>> = _morningPickups.asStateFlow()

    private val _isTripActive = MutableStateFlow(false)
    val isTripActive: StateFlow<Boolean> = _isTripActive.asStateFlow()

    private val _currentActiveTripGroupID = MutableStateFlow<String?>(null)
    val currentActiveTripGroupID: StateFlow<String?> = _currentActiveTripGroupID.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var membersListener: ListenerRegistration? = null
    private var locationListener: ListenerRegistration? = null
    private var routeListener: ListenerRegistration? = null
    private var canonicalRouteListener: ListenerRegistration? = null
    private var attendanceListeners: MutableMap<String, ListenerRegistration> = mutableMapOf()
    private var attendanceResponsesByDate: Map<String, Map<String, Map<String, Any>>> = emptyMap()
    private var morningPickupsListener: ListenerRegistration? = null

    private var latestAttendanceResponses: Map<String, Map<String, Any>> = emptyMap()
    private var activeTripGroupID: String? = null
    private var activeTripDriverName: String? = null
    private var lastLocationUploadAt: Date? = null
    private var lastAppendedRoutePoint: LatLng? = null
    private var lastDriverTelemetryLocation: Location? = null
    private var tripAutoStopJob: Job? = null
    private var pickupReachLoggedMemberIDs: MutableSet<String> = mutableSetOf()
    private var activeTripStartedAt: Date? = null
    private var tripActivityListener: ListenerRegistration? = null
    private var motionAutoStopTriggered = false

    private val _plannedTripEndAt = MutableStateFlow<Date?>(null)
    val plannedTripEndAt: StateFlow<Date?> = _plannedTripEndAt.asStateFlow()

    private val _attendanceRevision = MutableStateFlow(0)
    val attendanceRevision: StateFlow<Int> = _attendanceRevision.asStateFlow()

    private val todayKey: String
        get() = HolidayMode.dateKey(Date())

    fun startListening(groupID: String) {
        stopListening()

        membersListener = db.collection("groups").document(groupID).collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                val fetched = snapshot?.documents?.mapNotNull { memberFrom(it.id, it.data) } ?: emptyList()
                mergeMembers(fetched)
            }

        locationListener = db.collection("groups").document(groupID).collection("live")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                val isActiveFromDoc = data?.get("isActive") as? Boolean ?: false
                _isTripActive.value = isActiveFromDoc
                _currentActiveTripGroupID.value = if (isActiveFromDoc) groupID else null
                _driverLocation.value = driverLocationFrom(snapshot)
            }

        routeListener = db.collection("groups").document(groupID).collection("live")
            .document("route")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                _driverRoute.value = routePointsFrom(snapshot?.data)
            }

        startAttendanceListeners(groupID)

        canonicalRouteListener = db.collection("groups").document(groupID)
            .collection("canonicalRoutes")
            .document("morning")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val data = snapshot?.data
                val ready = data?.get("status") as? String == "ready"
                _isCanonicalMorningRouteReady.value = ready
                _canonicalMorningRoute.value = if (ready) routePointsFrom(data) else emptyList()
            }

        morningPickupsListener = db.collection("groups").document(groupID)
            .collection("morningPickups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = error.localizedMessage
                    return@addSnapshotListener
                }
                _morningPickups.value = snapshot?.documents?.mapNotNull { doc ->
                    morningPickupFrom(doc.id, doc.data)
                } ?: emptyList()
            }
    }

    fun stopListening() {
        membersListener?.remove()
        locationListener?.remove()
        routeListener?.remove()
        canonicalRouteListener?.remove()
        stopAttendanceListeners()
        morningPickupsListener?.remove()
        membersListener = null
        locationListener = null
        routeListener = null
        canonicalRouteListener = null
        morningPickupsListener = null
        attendanceResponsesByDate = emptyMap()
        _morningPickups.value = emptyList()
        _driverRoute.value = emptyList()
        _canonicalMorningRoute.value = emptyList()
        _isCanonicalMorningRouteReady.value = false
        lastAppendedRoutePoint = null
        _isTripActive.value = false
        _currentActiveTripGroupID.value = null
        activeTripGroupID = null
        activeTripDriverName = null
        lastLocationUploadAt = null
        tripAutoStopJob?.cancel()
        tripAutoStopJob = null
        _plannedTripEndAt.value = null
        stopTripActivityListener()
        LocationTracker.setOnLocationUpdate(null)
        LocationTracker.stopTracking()
    }

    suspend fun startTrip(groupID: String, driverName: String, durationHours: Double) {
        if (_isTripActive.value) return
        require(durationHours > 0) { L10n.selectTripDuration }
        require(LocationTracker.canDriverStartTrip()) {
            L10n.alwaysLocationRequiredToStartTrip
        }

        val endsAt = Date(System.currentTimeMillis() + (durationHours * 3_600_000).toLong())
        _plannedTripEndAt.value = endsAt

        db.collection("groups").document(groupID).collection("tripEvents")
            .add(
                mapOf(
                    "type" to "started",
                    "date" to todayKey,
                    "driverName" to driverName,
                    "durationHours" to durationHours,
                    "plannedEndAt" to Timestamp(endsAt),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .await()

        db.collection("groups").document(groupID).collection("live").document("current")
            .set(
                mapOf(
                    "isActive" to true,
                    "driverName" to driverName,
                    "tripDate" to todayKey,
                    "approachSessionKey" to UUID.randomUUID().toString(),
                    "plannedEndAt" to Timestamp(endsAt),
                    "durationHours" to durationHours,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        resetDriverRoute(groupID)
        resetTripActivity(groupID)
        resetTripTelemetry(groupID)
        pickupReachLoggedMemberIDs = mutableSetOf()
        lastDriverTelemetryLocation = null

        _isTripActive.value = true
        activeTripStartedAt = Date()
        motionAutoStopTriggered = false
        startTripActivityListener(groupID)
        _currentActiveTripGroupID.value = groupID
        activeTripGroupID = groupID
        activeTripDriverName = driverName
        lastLocationUploadAt = null
        lastAppendedRoutePoint = null

        LocationTracker.setOnLocationUpdate { location ->
            scope.launch {
                handleLocationUpdate(location)
            }
        }
        LocationTracker.startTracking()

        LocationTracker.effectiveLocation?.let { location ->
            runCatching {
                uploadLocation(groupID, driverName, location, isActive = true)
                recordPickupReachIfNeeded(groupID, location.latitude, location.longitude)
                appendDriverTelemetrySample(groupID, location)
            }
            lastLocationUploadAt = Date()
        }

        scheduleTripAutoStop(groupID, driverName, endsAt)
    }

    suspend fun stopTrip(groupID: String, driverName: String) {
        val routeToArchive = _driverRoute.value.toList()
        val tripStartedAt = activeTripStartedAt
        activeTripStartedAt = null

        runCatching {
            archiveRouteHistory(
                groupID = groupID,
                driverName = driverName,
                points = routeToArchive,
                startedAt = tripStartedAt
            )
        }

        stopTripActivityListener()

        tripAutoStopJob?.cancel()
        tripAutoStopJob = null
        _plannedTripEndAt.value = null
        _isTripActive.value = false
        _currentActiveTripGroupID.value = null
        activeTripGroupID = null
        activeTripDriverName = null
        lastLocationUploadAt = null
        lastAppendedRoutePoint = null
        LocationTracker.setOnLocationUpdate(null)
        LocationTracker.stopTracking()

        db.collection("groups").document(groupID).collection("live").document("current")
            .set(
                mapOf(
                    "isActive" to false,
                    "driverName" to driverName,
                    "plannedEndAt" to FieldValue.delete(),
                    "durationHours" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        // Artık attendance reset yapılmıyor - her servis (sabah/akşam) ayrı dateKey'de tutulduğu için
        // yolcuların akşam seçimleri sabah servisi bittiğinde korunuyor
    }

    suspend fun reconcileActiveTripIfExpired(groupID: String, driverName: String) {
        if (!_isTripActive.value) return

        _plannedTripEndAt.value?.let { endsAt ->
            if (endsAt <= Date()) {
                stopTrip(groupID, driverName)
                return
            }
        }

        runCatching {
            val doc = db.collection("groups").document(groupID)
                .collection("live").document("current")
                .get()
                .await()
            val data = doc.data ?: return@runCatching
            val isActive = data["isActive"] as? Boolean ?: false
            if (!isActive) return@runCatching
            val timestamp = data["plannedEndAt"] as? Timestamp ?: return@runCatching
            val endsAt = timestamp.toDate()
            _plannedTripEndAt.value = endsAt
            if (endsAt <= Date()) {
                stopTrip(groupID, driverName)
            } else {
                scheduleTripAutoStop(groupID, driverName, endsAt)
            }
        }
    }

    private fun scheduleTripAutoStop(groupID: String, driverName: String, endsAt: Date) {
        tripAutoStopJob?.cancel()
        val delayMs = (endsAt.time - System.currentTimeMillis()).coerceAtLeast(0)
        tripAutoStopJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            if (_isTripActive.value && activeTripGroupID == groupID) {
                stopTrip(groupID, driverName)
            }
        }
    }

    private suspend fun handleLocationUpdate(location: Location) {
        if (!_isTripActive.value) return
        val groupID = activeTripGroupID ?: return
        val driverName = activeTripDriverName ?: return

        val now = Date()
        lastLocationUploadAt?.let { lastUpload ->
            if (now.time - lastUpload.time < 5_000) return
        }

        try {
            uploadLocation(groupID, driverName, location, isActive = true)
            recordPickupReachIfNeeded(groupID, location.latitude, location.longitude)
            appendDriverTelemetrySample(groupID, location)
            lastLocationUploadAt = now
        } catch (error: Exception) {
            _errorMessage.value = error.localizedMessage
        }
    }

    private suspend fun uploadLocation(
        groupID: String,
        driverName: String,
        location: Location,
        isActive: Boolean
    ) {
        val speedMps = TripTelemetry.speedMps(location, lastDriverTelemetryLocation)
        db.collection("groups").document(groupID).collection("live").document("current")
            .set(
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "speedMps" to speedMps,
                    "isActive" to isActive,
                    "driverName" to driverName,
                    "tripDate" to todayKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        if (isActive) {
            appendRoutePoint(groupID, LatLng(location.latitude, location.longitude))
        }
    }

    private suspend fun appendDriverTelemetrySample(groupID: String, location: Location) {
        val speedMps = TripTelemetry.speedMps(location, lastDriverTelemetryLocation)
        lastDriverTelemetryLocation = location
        val sample = TripTelemetry.Sample(
            speedMps = speedMps,
            latitude = location.latitude,
            longitude = location.longitude,
            sampledAt = Date(location.time)
        )
        writeTelemetrySamples(groupID, "driver", sample)
    }

    private suspend fun writeTelemetrySamples(
        groupID: String,
        documentID: String,
        newSample: TripTelemetry.Sample
    ) {
        val ref = db.collection("groups").document(groupID)
            .collection("tripTelemetry").document(documentID)

        val snapshot = ref.get().await()
        var samples = TripTelemetry.samplesFrom(snapshot.data)
        samples = (samples + newSample)
            .filter { it.sampledAt.time >= System.currentTimeMillis() - TripTelemetry.SAMPLE_WINDOW_MS }
            .takeLast(TripTelemetry.MAX_STORED_SAMPLES)

        val encoded = samples.map { TripTelemetry.firestorePayload(it) }
        ref.set(
            mapOf(
                "tripDate" to todayKey,
                "samples" to encoded,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    private suspend fun resetTripTelemetry(groupID: String) {
        val snapshot = db.collection("groups").document(groupID)
            .collection("tripTelemetry")
            .get()
            .await()
        for (document in snapshot.documents) {
            document.reference.delete().await()
        }
    }

    private suspend fun archiveRouteHistory(
        groupID: String,
        driverName: String,
        points: List<LatLng>,
        startedAt: Date?
    ) {
        if (points.size < MIN_ROUTE_HISTORY_POINT_COUNT) return

        val service = ServiceSchedule.currentDriverSession()
        val endedAt = Date()
        val pointsData = points.map { point ->
            mapOf(
                "latitude" to point.latitude,
                "longitude" to point.longitude
            )
        }

        db.collection("groups").document(groupID)
            .collection("routeHistory")
            .add(
                mapOf(
                    "session" to service.session.suffix,
                    "tripDate" to HolidayMode.dateKey(service.date),
                    "dateKey" to service.dateKey,
                    "driverName" to driverName,
                    "startedAt" to Timestamp(startedAt ?: endedAt),
                    "endedAt" to Timestamp(endedAt),
                    "pointCount" to points.size,
                    "points" to pointsData
                )
            )
            .await()
    }

    private suspend fun resetDriverRoute(groupID: String) {
        _driverRoute.value = emptyList()
        lastAppendedRoutePoint = null
        db.collection("groups").document(groupID).collection("live").document("route")
            .set(
                mapOf(
                    "tripDate" to todayKey,
                    "points" to emptyList<Map<String, Double>>()
                )
            )
            .await()
    }

    private suspend fun appendRoutePoint(groupID: String, coordinate: LatLng) {
        lastAppendedRoutePoint?.let { last ->
            val results = FloatArray(1)
            Location.distanceBetween(
                last.latitude,
                last.longitude,
                coordinate.latitude,
                coordinate.longitude,
                results
            )
            if (results[0] < MIN_ROUTE_POINT_DISTANCE_METERS) return
        }

        lastAppendedRoutePoint = coordinate
        _driverRoute.value = _driverRoute.value + coordinate

        db.collection("groups").document(groupID).collection("live").document("route")
            .set(
                mapOf(
                    "tripDate" to todayKey,
                    "points" to FieldValue.arrayUnion(
                        mapOf(
                            "latitude" to coordinate.latitude,
                            "longitude" to coordinate.longitude
                        )
                    )
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    private fun routePointsFrom(data: Map<String, Any>?): List<LatLng> {
        val rawPoints = data?.get("points") as? List<*> ?: return emptyList()
        return rawPoints.mapNotNull { entry ->
            val point = entry as? Map<*, *> ?: return@mapNotNull null
            val lat = (point["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            val lng = (point["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            LatLng(lat, lng)
        }
    }

    private fun mergeMembers(fetched: List<ShuttleMember>) {
        val attendanceByID = _members.value.associate { it.id to it.attendance }
        val boardedByID = _members.value.associate { it.id to it.boardedAt }
        _members.value = fetched.map { member ->
            member.copy(
                attendance = attendanceByID[member.id] ?: member.attendance,
                boardedAt = boardedByID[member.id] ?: member.boardedAt
            )
        }
        refreshMembersAttendanceForServiceDay()
    }

    private fun listenAttendance(groupID: String, dateKey: String): ListenerRegistration {
        return db.collection("groups").document(groupID)
            .collection("attendance").document(dateKey)
            .addSnapshotListener { snapshot, _ ->
                onAttendanceSnapshot(dateKey, snapshot?.data)
            }
    }

    private fun startAttendanceListeners(groupID: String) {
        stopAttendanceListeners()

        val dateKeysToListen = mutableSetOf<String>()

        // Yolcu için: sonraki 2 servis
        val nextServices = ServiceSchedule.nextTwoServices()
        for (service in nextServices) {
            dateKeysToListen.add(service.dateKey)
        }

        // Sürücü için: şu anki servis
        val driverService = ServiceSchedule.currentDriverSession()
        dateKeysToListen.add(driverService.dateKey)

        // Bugün sabah ve akşam (her zaman dinle)
        val (todayAm, todayPm) = ServiceSchedule.todayDateKeys()
        dateKeysToListen.add(todayAm)
        dateKeysToListen.add(todayPm)

        for (dateKey in dateKeysToListen) {
            attendanceListeners[dateKey] = listenAttendance(groupID, dateKey)
        }
    }

    private fun stopAttendanceListeners() {
        for ((_, listener) in attendanceListeners) {
            listener.remove()
        }
        attendanceListeners.clear()
    }

    private fun onAttendanceSnapshot(dateKey: String, data: Map<String, Any>?) {
        val updated = attendanceResponsesByDate.toMutableMap()
        if (data == null) {
            updated.remove(dateKey)
        } else {
            val parsed = parseAttendanceResponses(data)
            if (parsed.isNotEmpty()) {
                updated[dateKey] = parsed
            }
        }
        attendanceResponsesByDate = updated
        refreshMembersAttendanceForServiceDay()
        notifyAttendanceChanged()
    }

    private fun notifyAttendanceChanged() {
        _attendanceRevision.value += 1
    }

    private fun refreshMembersAttendanceForServiceDay() {
        latestAttendanceResponses = attendanceResponsesByDate[todayKey] ?: emptyMap()
        if (latestAttendanceResponses.isEmpty() && attendanceResponsesByDate.isEmpty()) {
            resetPassengerAttendance()
            return
        }

        _members.value = _members.value.map { member ->
            if (member.role != MemberRole.Passenger) return@map member
            val response = latestAttendanceResponses[member.id]
            val status = response?.let { attendanceStatusFrom(it) } ?: AttendanceStatus.Unknown
            val boardedAt = (response?.get("boardedAt") as? Timestamp)?.toDate()
            member.copy(attendance = status, boardedAt = boardedAt)
        }
    }

    fun planningAttendanceDateKey(holidayModeActive: Boolean): String =
        HolidayMode.attendancePlanningDateKey(holidayModeActive)

    fun rawAttendanceFor(memberID: String, dateKey: String): AttendanceStatus {
        val response = attendanceResponsesByDate[dateKey]?.get(memberID)
        return response?.let { attendanceStatusFrom(it) } ?: AttendanceStatus.Unknown
    }

    fun effectiveAttendanceFor(memberID: String, dateKey: String): AttendanceStatus {
        val member = _members.value.firstOrNull { it.id == memberID } ?: return AttendanceStatus.Unknown
        val raw = rawAttendanceFor(memberID, dateKey)
        return member.copy(attendance = raw).effectiveAttendance()
    }

    /** Sürücü: bugünün attendance belgesi (yolcuyla aynı anahtar) + tatil kuralı. */
    /** Sürücünün aktif servisi için attendance durumu */
    fun serviceDayAttendanceFor(member: ShuttleMember): AttendanceStatus {
        val currentService = ServiceSchedule.currentDriverSession()
        val raw = rawAttendanceFor(member.id, currentService.dateKey)
        return member.copy(attendance = raw).effectiveAttendance()
    }

    /** Sürücünün şu an hangi serviste olduğu */
    val currentDriverService: UpcomingService
        get() = ServiceSchedule.currentDriverSession()

    private fun resetPassengerAttendance() {
        _members.value = _members.value.map { member ->
            if (member.role == MemberRole.Passenger) {
                member.copy(attendance = AttendanceStatus.Unknown, boardedAt = null)
            } else {
                member
            }
        }
    }

    fun boardedAt(forMemberID: String): Date? {
        return (latestAttendanceResponses[forMemberID]?.get("boardedAt") as? Timestamp)?.toDate()
    }

    suspend fun uploadMotionActivitySnapshot(
        groupID: String,
        role: MotionActivityRole,
        memberID: String,
        automotiveSecondsInWindow: Int,
        segments: List<MotionActivitySegment>,
        currentActivity: String = "unknown",
        inVehicle: Boolean = false,
        lastVehicleExitAt: Date? = null,
        walkingSince: Date? = null,
        hasBeenInVehicle: Boolean = false
    ) {
        if (FirebaseAuth.getInstance().currentUser == null) return

        val documentID = when (role) {
            MotionActivityRole.Driver -> "activity_driver"
            MotionActivityRole.Passenger -> "activity_passenger_$memberID"
        }

        val encodedSegments = segments.map { segment ->
            mapOf(
                "startedAt" to Timestamp(segment.startedAt),
                "endedAt" to Timestamp(segment.endedAt),
                "isAutomotive" to segment.isAutomotive
            )
        }

        val payload = mutableMapOf<String, Any>(
            "tripDate" to todayKey,
            "role" to role.rawValue,
            "memberID" to memberID,
            "automotiveSecondsInWindow" to automotiveSecondsInWindow,
            "segments" to encodedSegments,
            "currentActivity" to currentActivity,
            "inVehicle" to inVehicle,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (hasBeenInVehicle) {
            payload["hasBeenInVehicle"] = true
        }
        lastVehicleExitAt?.let { payload["lastVehicleExitAt"] = Timestamp(it) }
        walkingSince?.let { payload["walkingSince"] = Timestamp(it) }

        db.collection("groups").document(groupID)
            .collection("tripActivity").document(documentID)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    private fun startTripActivityListener(groupID: String) {
        stopTripActivityListener()
        tripActivityListener = db.collection("groups").document(groupID)
            .collection("tripActivity")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                evaluatePassengerMotionAutoStop(snapshot)
            }
    }

    private fun stopTripActivityListener() {
        tripActivityListener?.remove()
        tripActivityListener = null
    }

    private fun evaluatePassengerMotionAutoStop(
        snapshot: com.google.firebase.firestore.QuerySnapshot?
    ) {
        if (!_isTripActive.value || motionAutoStopTriggered) return
        val groupID = activeTripGroupID ?: return
        val driverName = activeTripDriverName ?: return
        val tripStartedAt = activeTripStartedAt ?: return

        val documents = snapshot?.documents.orEmpty()
        val pickupReachMemberIDs = mutableSetOf<String>()
        val passengerSnapshots = mutableListOf<TripMotionAutoStop.PassengerMotionSnapshot>()

        for (document in documents) {
            val documentID = document.id
            val data = document.data ?: continue

            if (documentID.startsWith("pickupReach_")) {
                (data["memberID"] as? String)?.let { pickupReachMemberIDs.add(it) }
                continue
            }

            if (!documentID.startsWith("activity_passenger_")) continue
            if (data["role"] != MotionActivityRole.Passenger.rawValue) continue
            val memberID = data["memberID"] as? String ?: continue

            passengerSnapshots.add(
                TripMotionAutoStop.PassengerMotionSnapshot(
                    memberID = memberID,
                    inVehicle = data["inVehicle"] as? Boolean ?: false,
                    currentActivity = data["currentActivity"] as? String ?: "unknown",
                    hasBeenInVehicle = data["hasBeenInVehicle"] as? Boolean ?: false,
                    lastVehicleExitAt = (data["lastVehicleExitAt"] as? Timestamp)?.toDate(),
                    walkingSince = (data["walkingSince"] as? Timestamp)?.toDate(),
                    hadPickupReach = pickupReachMemberIDs.contains(memberID)
                )
            )
        }

        val mergedPassengers = passengerSnapshots.map { state ->
            state.copy(hadPickupReach = state.hadPickupReach || pickupReachMemberIDs.contains(state.memberID))
        }

        val comingMemberIDs = _members.value
            .filter { it.role == MemberRole.Passenger && serviceDayAttendanceFor(it) == AttendanceStatus.Coming }
            .map { it.id }
            .toSet()

        if (!TripMotionAutoStop.shouldAutoStopTrip(tripStartedAt, comingMemberIDs, mergedPassengers)) {
            return
        }

        motionAutoStopTriggered = true
        scope.launch {
            stopTrip(groupID, driverName)
        }
    }

    private suspend fun recordPickupReachIfNeeded(
        groupID: String,
        driverLatitude: Double,
        driverLongitude: Double
    ) {
        if (!_isTripActive.value) return

        val driverLocation = Location("").apply {
            latitude = driverLatitude
            longitude = driverLongitude
        }

        for (pickup in _morningPickups.value) {
            if (pickupReachLoggedMemberIDs.contains(pickup.memberID)) continue

            val member = _members.value.firstOrNull { it.id == pickup.memberID }
            if (member != null && serviceDayAttendanceFor(member) == AttendanceStatus.NotComing) continue

            val pickupLocation = Location("").apply {
                latitude = pickup.latitude
                longitude = pickup.longitude
            }
            if (driverLocation.distanceTo(pickupLocation) > PICKUP_REACH_RADIUS_METERS) continue

            pickupReachLoggedMemberIDs.add(pickup.memberID)
            db.collection("groups").document(groupID)
                .collection("tripActivity").document("pickupReach_${pickup.memberID}")
                .set(
                    mapOf(
                        "tripDate" to todayKey,
                        "memberID" to pickup.memberID,
                        "latitude" to pickup.latitude,
                        "longitude" to pickup.longitude,
                        "reachedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        }
    }

    private suspend fun resetTripActivity(groupID: String) {
        val snapshot = db.collection("groups").document(groupID)
            .collection("tripActivity")
            .get()
            .await()
        for (document in snapshot.documents) {
            document.reference.delete().await()
        }
    }

    private fun parseAttendanceResponses(data: Map<String, Any>): Map<String, Map<String, Any>> {
        val raw = data["responses"] ?: return emptyMap()
        @Suppress("UNCHECKED_CAST")
        if (raw is Map<*, *>) {
            return raw.mapNotNull { (key, value) ->
                val memberID = key as? String ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val response = value as? Map<String, Any> ?: return@mapNotNull null
                memberID to response
            }.toMap()
        }
        return emptyMap()
    }

    private fun attendanceStatusFrom(response: Map<String, Any>): AttendanceStatus? {
        val statusRaw = stringValue(response["status"]) ?: return null
        return AttendanceStatus.fromRaw(statusRaw)
    }

    private fun stringValue(value: Any?): String? {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            else -> null
        }
    }

    private fun memberFrom(id: String, data: Map<String, Any>?): ShuttleMember? {
        if (data == null) return null
        val name = data["name"] as? String ?: return null
        val roleRaw = data["role"] as? String ?: return null
        val role = MemberRole.entries.firstOrNull { it.rawValue == roleRaw } ?: return null
        val holidayEnd = stringValue(data["holidayModeEndDate"])
        return ShuttleMember(
            id = id,
            name = name,
            role = role,
            holidayModeEndDate = holidayEnd
        )
    }

    private fun morningPickupFrom(documentId: String, data: Map<String, Any>?): MorningPickup? {
        if (data == null) return null
        val memberID = data["memberID"] as? String ?: documentId
        val name = data["name"] as? String ?: return null
        val latitude = (data["latitude"] as? Number)?.toDouble() ?: return null
        val longitude = (data["longitude"] as? Number)?.toDouble() ?: return null
        val updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        return MorningPickup(
            memberID = memberID,
            name = name,
            latitude = latitude,
            longitude = longitude,
            updatedAt = updatedAt
        )
    }

    private fun driverLocationFrom(
        snapshot: com.google.firebase.firestore.DocumentSnapshot?
    ): DriverLocation? {
        val data = snapshot?.data ?: return null
        val latitude = (data["latitude"] as? Number)?.toDouble() ?: return null
        val longitude = (data["longitude"] as? Number)?.toDouble() ?: return null
        val isActive = data["isActive"] as? Boolean ?: return null
        if (!isActive) return null

        val updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
        val driverName = data["driverName"] as? String ?: L10n.driverDefaultName

        var resolvedLatitude = latitude
        var resolvedLongitude = longitude
        if (BuildConfig.DEBUG && !MapDefaults.isNearHome(latitude, longitude)) {
            resolvedLatitude = MapDefaults.homeLatLng.latitude
            resolvedLongitude = MapDefaults.homeLatLng.longitude
        }

        return DriverLocation(
            latitude = resolvedLatitude,
            longitude = resolvedLongitude,
            updatedAt = updatedAt,
            isActive = isActive,
            driverName = driverName
        )
    }

    suspend fun setHolidayMode(groupID: String, memberID: String, endDate: Date) {
        require(groupID.isNotBlank()) { L10n.shuttleNotFoundRejoin }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException(L10n.signInRequired)
        }

        val endKey = HolidayMode.dateKey(endDate)
        db.collection("groups").document(groupID)
            .collection("members").document(memberID)
            .set(
                mapOf(
                    "holidayModeEndDate" to endKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        _members.value = _members.value.map { member ->
            if (member.id == memberID) member.copy(holidayModeEndDate = endKey) else member
        }
        clearMemberAttendanceStatus(groupID, memberID)
    }

    suspend fun clearHolidayMode(groupID: String, memberID: String) {
        require(groupID.isNotBlank()) { L10n.shuttleNotFoundRejoin }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException(L10n.signInRequired)
        }

        db.collection("groups").document(groupID)
            .collection("members").document(memberID)
            .update(
                mapOf(
                    "holidayModeEndDate" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()

        _members.value = _members.value.map { member ->
            if (member.id == memberID) member.copy(holidayModeEndDate = null) else member
        }
        clearMemberAttendanceStatus(groupID, memberID)
    }

    suspend fun updateMemberName(groupID: String, memberID: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return

        require(groupID.isNotBlank()) { L10n.shuttleNotFoundRejoin }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException(L10n.signInRequired)
        }

        db.collection("groups").document(groupID)
            .collection("members").document(memberID)
            .update(
                mapOf(
                    "name" to trimmedName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()

        _members.value = _members.value.map { member ->
            if (member.id == memberID) member.copy(name = trimmedName) else member
        }
    }

    private suspend fun clearMemberAttendanceStatus(groupID: String, memberID: String) {
        val dateKeys = listOf(todayKey, HolidayMode.tomorrowDateKey())
        for (dateKey in dateKeys) {
            val ref = db.collection("groups").document(groupID)
                .collection("attendance").document(dateKey)
            try {
                ref.update(
                    FieldPath.of("responses", memberID, "status"),
                    FieldValue.delete(),
                    FieldPath.of("responses", memberID, "updatedAt"),
                    FieldValue.delete()
                ).await()
            } catch (_: Exception) {
                // Belge veya alan yoksa yoksay
            }

            val updated = attendanceResponsesByDate.toMutableMap()
            val dayMap = (updated[dateKey] ?: emptyMap()).toMutableMap()
            val memberResp = (dayMap[memberID] ?: emptyMap()).toMutableMap()
            memberResp.remove("status")
            memberResp.remove("updatedAt")
            if (memberResp.isEmpty()) {
                dayMap.remove(memberID)
            } else {
                dayMap[memberID] = memberResp
            }
            if (dayMap.isEmpty()) {
                updated.remove(dateKey)
            } else {
                updated[dateKey] = dayMap
            }
            attendanceResponsesByDate = updated
        }
        latestAttendanceResponses = attendanceResponsesByDate[todayKey] ?: emptyMap()
        refreshMembersAttendanceForServiceDay()
        notifyAttendanceChanged()
    }

    // MARK: - Passenger actions (ported from iOS)

    fun morningPickup(forMemberID: String): MorningPickup? {
        return _morningPickups.value.firstOrNull { it.memberID == forMemberID }
    }

    suspend fun setAttendance(
        groupID: String,
        memberID: String,
        name: String,
        status: AttendanceStatus,
        dateKey: String = todayKey
    ) {
        require(groupID.isNotBlank()) { L10n.shuttleNotFoundRejoin }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException(L10n.signInRequired)
        }

        val ref = db.collection("groups").document(groupID)
            .collection("attendance").document(dateKey)

        // Ensure parent doc exists
        ref.set(
            mapOf("updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        // FieldPath — iOS ile aynı; diğer yolcuların yanıtlarını silmez
        ref.update(
            FieldPath.of("responses", memberID, "status"),
            status.rawValue,
            FieldPath.of("responses", memberID, "name"),
            name,
            FieldPath.of("responses", memberID, "updatedAt"),
            FieldValue.serverTimestamp()
        ).await()

        // Optimistic local update — ilgili günün belgesi (tatilde yarın ayrı)
        val existing = (attendanceResponsesByDate[dateKey]?.get(memberID) ?: emptyMap()).toMutableMap()
        existing["status"] = status.rawValue
        existing["name"] = name
        val dateResponses = (attendanceResponsesByDate[dateKey] ?: emptyMap()).toMutableMap()
        dateResponses[memberID] = existing
        attendanceResponsesByDate = attendanceResponsesByDate + (dateKey to dateResponses)
        if (dateKey == todayKey) {
            latestAttendanceResponses = dateResponses
        }
        refreshMembersAttendanceForServiceDay()
        notifyAttendanceChanged()
        // Gelmiyorum: biniş noktası Firestore'da kalır; sürücü haritasında attendance ile gizlenir.
        // Geliyorum tekrar seçilince aynı pin sürücüde yeniden görünür.
    }

    suspend fun setMorningPickup(
        groupID: String,
        memberID: String,
        name: String,
        latitude: Double,
        longitude: Double
    ) {
        require(groupID.isNotBlank()) { L10n.shuttleNotFoundRejoin }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException(L10n.signInRequired)
        }

        db.collection("groups").document(groupID)
            .collection("morningPickups").document(memberID)
            .set(
                mapOf(
                    "memberID" to memberID,
                    "name" to name,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    suspend fun deleteUserData(profile: UserProfile) {
        val groupIDs = buildSet {
            addAll(profile.groupIDs)
            addAll(profile.activeGroupIDs)
            if (profile.groupID.isNotBlank()) add(profile.groupID)
        }

        try {
            db.collection("users").document(profile.userID).delete().await()
        } catch (_: Exception) {
            // Kısmi silme: diğer adımlara devam et.
        }

        for (groupID in groupIDs) {
            try {
                db.collection("groups").document(groupID)
                    .collection("members").document(profile.memberID)
                    .delete()
                    .await()
            } catch (_: Exception) {
            }

            try {
                db.collection("groups").document(groupID)
                    .collection("morningPickups").document(profile.memberID)
                    .delete()
                    .await()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun isUserProfileDeleted(userID: String): Boolean {
        return try {
            fetchUserProfileDocument(userID) == null
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun fetchUserProfileDocument(userID: String): Map<String, Any>? {
        val doc = db.collection("users").document(userID).get().await()
        return doc.data
    }

    companion object {
        private const val MIN_ROUTE_POINT_DISTANCE_METERS = 20f
        private const val MIN_ROUTE_HISTORY_POINT_COUNT = 5
        private const val PICKUP_REACH_RADIUS_METERS = 120f
        val shared = ShuttleStore()
    }
}
