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
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MapDefaults
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.ShuttleMember
import com.mikatechnology.BusTracker.services.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private var attendanceListener: ListenerRegistration? = null
    private var morningPickupsListener: ListenerRegistration? = null

    private var latestAttendanceResponses: Map<String, Map<String, Any>> = emptyMap()
    private var activeTripGroupID: String? = null
    private var activeTripDriverName: String? = null
    private var lastLocationUploadAt: Date? = null
    private var lastAppendedRoutePoint: LatLng? = null
    private var tripAutoStopJob: Job? = null

    private val _plannedTripEndAt = MutableStateFlow<Date?>(null)
    val plannedTripEndAt: StateFlow<Date?> = _plannedTripEndAt.asStateFlow()

    private val todayKey: String
        get() {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))
            formatter.calendar = java.util.Calendar.getInstance()
            return formatter.format(Date())
        }

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

        attendanceListener = db.collection("groups").document(groupID)
            .collection("attendance").document(todayKey)
            .addSnapshotListener { snapshot, _ ->
                applyAttendance(snapshot?.data)
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
        attendanceListener?.remove()
        morningPickupsListener?.remove()
        membersListener = null
        locationListener = null
        routeListener = null
        attendanceListener = null
        morningPickupsListener = null
        _morningPickups.value = emptyList()
        _driverRoute.value = emptyList()
        lastAppendedRoutePoint = null
        _isTripActive.value = false
        _currentActiveTripGroupID.value = null
        activeTripGroupID = null
        activeTripDriverName = null
        lastLocationUploadAt = null
        tripAutoStopJob?.cancel()
        tripAutoStopJob = null
        _plannedTripEndAt.value = null
        LocationTracker.setOnLocationUpdate(null)
        LocationTracker.stopTracking()
    }

    suspend fun startTrip(groupID: String, driverName: String, durationHours: Double) {
        if (_isTripActive.value) return
        require(durationHours > 0) { "Servis süresi seçin." }

        val endsAt = Date(System.currentTimeMillis() + (durationHours * 3_600_000).toLong())
        _plannedTripEndAt.value = endsAt

        db.collection("groups").document(groupID)
            .collection("attendance").document(todayKey)
            .delete()
            .await()

        latestAttendanceResponses = emptyMap()
        _members.value = _members.value.map { member ->
            if (member.role == MemberRole.Passenger) {
                member.copy(attendance = AttendanceStatus.Unknown)
            } else {
                member
            }
        }

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

        _isTripActive.value = true
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
            }
            lastLocationUploadAt = Date()
        }

        scheduleTripAutoStop(groupID, driverName, endsAt)
    }

    suspend fun stopTrip(groupID: String, driverName: String) {
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
        db.collection("groups").document(groupID).collection("live").document("current")
            .set(
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
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
        _members.value = fetched.map { member ->
            member.copy(attendance = attendanceByID[member.id] ?: member.attendance)
        }
        applyAttendance(mapOf("responses" to latestAttendanceResponses))
    }

    private fun applyAttendance(data: Map<String, Any>?) {
        if (data == null) {
            latestAttendanceResponses = emptyMap()
            resetPassengerAttendance()
            return
        }

        val parsed = parseAttendanceResponses(data)
        if (parsed.isNotEmpty()) {
            latestAttendanceResponses = parsed
        }

        if (latestAttendanceResponses.isEmpty()) return

        _members.value = _members.value.map { member ->
            if (member.role != MemberRole.Passenger) return@map member
            val response = latestAttendanceResponses[member.id]
            val status = response?.let { attendanceStatusFrom(it) } ?: AttendanceStatus.Unknown
            member.copy(attendance = status)
        }
    }

    private fun resetPassengerAttendance() {
        _members.value = _members.value.map { member ->
            if (member.role == MemberRole.Passenger) {
                member.copy(attendance = AttendanceStatus.Unknown)
            } else {
                member
            }
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
        return ShuttleMember(id = id, name = name, role = role)
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
        val driverName = data["driverName"] as? String ?: "Şoför"

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

    // MARK: - Passenger actions (ported from iOS)

    fun morningPickup(forMemberID: String): MorningPickup? {
        return _morningPickups.value.firstOrNull { it.memberID == forMemberID }
    }

    suspend fun setAttendance(
        groupID: String,
        memberID: String,
        name: String,
        status: AttendanceStatus
    ) {
        require(groupID.isNotBlank()) { "Servis bulunamadı. Çıkış yapıp tekrar katılın." }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException("Giriş yapmanız gerekiyor.")
        }

        val ref = db.collection("groups").document(groupID)
            .collection("attendance").document(todayKey)

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

        // Optimistic local update so UI reacts immediately
        val existing = latestAttendanceResponses[memberID]?.toMutableMap() ?: mutableMapOf()
        existing["status"] = status.rawValue
        existing["name"] = name
        latestAttendanceResponses = latestAttendanceResponses + (memberID to existing)

        applyAttendance(mapOf("responses" to latestAttendanceResponses))
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
        require(groupID.isNotBlank()) { "Servis bulunamadı. Çıkış yapıp tekrar katılın." }
        if (FirebaseAuth.getInstance().currentUser == null) {
            throw IllegalStateException("Giriş yapmanız gerekiyor.")
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

        db.collection("users").document(profile.userID).delete().await()

        for (groupID in groupIDs) {
            db.collection("groups").document(groupID)
                .collection("members").document(profile.memberID)
                .delete()
                .await()

            db.collection("groups").document(groupID)
                .collection("morningPickups").document(profile.memberID)
                .delete()
                .await()
        }
    }

    companion object {
        private const val MIN_ROUTE_POINT_DISTANCE_METERS = 20f
        val shared = ShuttleStore()
    }
}
