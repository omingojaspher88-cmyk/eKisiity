package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.model.VerificationStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LiveLocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAtMillis: Long,
)

class VenueLocationManager(context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): Result<LiveLocationReading> {
        return try {
            val freshLocation = requestCurrentFix()
            val resolved = freshLocation ?: requestLastKnownFix()
            if (resolved != null) {
                Result.success(
                    LiveLocationReading(
                        latitude = resolved.latitude,
                        longitude = resolved.longitude,
                        accuracyMeters = resolved.accuracy.toDouble().coerceAtLeast(0.0),
                        capturedAtMillis = System.currentTimeMillis(),
                    )
                )
            } else {
                Result.failure(
                    IllegalStateException(
                        "Unable to obtain GPS fix. Ensure device location is enabled."
                    )
                )
            }
        } catch (se: SecurityException) {
            Result.failure(se)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentFix(): Location? =
        suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { tokenSource.cancel() }
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastKnownFix(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }

    companion object {
        fun distanceBetweenMeters(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double,
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0].toDouble()
        }

        fun classifyVenueProximity(
            distanceMeters: Double,
            geofenceRadiusMeters: Long,
        ): VerificationStatus = when {
            distanceMeters <= geofenceRadiusMeters.toDouble() -> VerificationStatus.VERIFIED_IN_VENUE
            distanceMeters <= 800.0 -> VerificationStatus.NEAR_CAMPUS
            else -> VerificationStatus.REMOTE_LOGGED
        }
    }
}
