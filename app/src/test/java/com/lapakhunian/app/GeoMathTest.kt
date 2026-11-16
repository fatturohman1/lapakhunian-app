package com.lapakhunian.app

import com.lapakhunian.app.core.util.distanceKm
import com.lapakhunian.app.core.util.formatDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    @Test fun `haversine jakarta bandung is about 120 km`() {
        val km = distanceKm(-6.2, 106.816666, -6.917464, 107.619123)
        assertTrue("jarak=$km", km in 110.0..130.0)
    }

    @Test fun `same point is zero distance`() {
        assertEquals(0.0, distanceKm(-6.2, 106.8, -6.2, 106.8), 0.0001)
    }

    @Test fun `distance labels use meters below one km and indonesian decimal comma`() {
        assertEquals("400 m", formatDistance(0.4))
        assertEquals("3,2 km", formatDistance(3.24))
        assertEquals("12 km", formatDistance(12.0))
    }
}