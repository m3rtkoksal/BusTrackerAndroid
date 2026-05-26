package com.mikatechnology.BusTracker.data.repository

import android.location.Location
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MapDefaults
import com.mikatechnology.BusTracker.data.model.MemberRole
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

class ShuttleStore private constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _members = MutableStateFlow<List<ShuttleMember>>(emptyList())
    val members: StateFlow<List<ShuttleMember>> = _members.asStateFlow()

    private val _driverLocation = MutableStateFlow<DriverLocation?>(null)
    val driverLocation: StateFlow<DriverLocation?> = _driverLocation.asStateFlow()

    private val _morningPickups = MutableStateFlow<List<MorningPickup>>(emptyList())
    val morningPickups: StateFlow<List<MorningPickup>> = _morningPickups.asStateFlow()

    private val _isTripActive = MutableStateFlow(false)
    val isTripActive: StateFlow<Boolean> = _isTripActive.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var membersListener: ListenerRegistration? = null
    private var locationListener: ListenerRegistration? = null
    private var attendanceListener: ListenerRegistration? = null
    private var morningPickupsListener: ListenerRegistration? = null

    private var latestAttendanceResponses: Map<String, Map<String, Any>> = emptyMap()
    private var activeTripGroupID: String? = null
    private var activeTripDriverName: String? = null
    private var lastLocationUploadAt: Date? = null

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
                _isTripActive.value = data?.get("isActive") as? Boolean ?: false
                _driverLocation.value = driverLocationFrom(snapshot)
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
                _morningPickups.value = snapshot?.documents?.mapNotNull { morningPickupFrom(it.data) }
                    ?: emptyList()
            }
    }

    fun stopListening() {
        membersListener?.remove()
        locationListener?.remove()
        attendanceListener?.remove()
        morningPickupsListener?.remove()
        membersListener = null
        locationListener = null
        attendanceListener = null
        morningPickupsListener = null
        _morningPickups.value = emptyList()
        _isTripActive.value = false
        activeTripGroupID = null
        activeTripDriverName = null
        lastLocationUploadAt = null
        LocationTracker.setOnLocationUpdate(null)
        LocationTracker.stopTracking()
    }

    suspend fun startTrip(groupID: String, driverName: String) {
        if (_isTripActive.value) return

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
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        _isTripActive.value = true
        activeTripGroupID = groupID
        activeTripDriverName = driverName
        lastLocationUploadAt = null

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
    }

    suspend fun stopTrip(groupID: String, driverName: String) {
        _isTripActive.value = false
        activeTripGroupID = null
        activeTripDriverName = null
        lastLocationUploadAt = null
        LocationTracker.setOnLocationUpdate(null)
        LocationTracker.stopTracking()

        db.collection("groups").document(groupID).collection("live").document("current")
            .set(
                mapOf(
                    "isActive" to false,
                    "driverName" to driverName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
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

    private fun morningPickupFrom(data: Map<String, Any>?): MorningPickup? {
        if (data == null) return null
        val memberID = data["memberID"] as? String ?: return null
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

    companion object {
        val shared = ShuttleStore()
    }
}
