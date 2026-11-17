package com.example

import com.example.location.VenueLocationManager
import com.example.model.KisiiGradingScale
import com.example.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun kisiiGradingScale_mapsPercentagesAccurately() {
        assertEquals("A", KisiiGradingScale.letterForScore(74.0))
        assertEquals("B", KisiiGradingScale.letterForScore(65.0))
        assertEquals("C", KisiiGradingScale.letterForScore(52.0))
        assertEquals("D", KisiiGradingScale.letterForScore(43.0))
        assertEquals("E", KisiiGradingScale.letterForScore(32.0))
        assertEquals(4.0, KisiiGradingScale.gradePointsForLetter("A"), 0.001)
    }

    @Test
    fun venueProximity_classifiesGeofenceCorrectly() {
        assertEquals(
            VerificationStatus.VERIFIED_IN_VENUE,
            VenueLocationManager.classifyVenueProximity(distanceMeters = 45.0, geofenceRadiusMeters = 120L)
        )
        assertEquals(
            VerificationStatus.NEAR_CAMPUS,
            VenueLocationManager.classifyVenueProximity(distanceMeters = 350.0, geofenceRadiusMeters = 120L)
        )
        assertEquals(
            VerificationStatus.REMOTE_LOGGED,
            VenueLocationManager.classifyVenueProximity(distanceMeters = 1500.0, geofenceRadiusMeters = 120L)
        )
    }
}
