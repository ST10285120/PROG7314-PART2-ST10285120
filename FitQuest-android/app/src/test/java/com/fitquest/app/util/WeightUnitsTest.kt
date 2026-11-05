package com.fitquest.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightUnitsTest {

    @Test
    fun `kilograms pass through unchanged`() {
        assertEquals(60.0, WeightUnits.fromKg(60.0, "KG"), 0.0)
        assertEquals(60.0, WeightUnits.toKg(60.0, "KG"), 0.0)
    }

    @Test
    fun `pounds convert both ways`() {
        assertEquals(220.46, WeightUnits.fromKg(100.0, "LB"), 0.01)
        assertEquals(100.0, WeightUnits.toKg(220.46226218, "LB"), 0.001)
    }

    @Test
    fun `whole numbers display without a decimal`() {
        assertEquals("60", WeightUnits.number(60.0, "KG"))
        assertEquals("60 kg", WeightUnits.format(60.0, "KG"))
    }

    @Test
    fun `the unit label follows the setting`() {
        assertEquals("lb", WeightUnits.label("LB"))
        assertEquals("kg", WeightUnits.label("KG"))
    }

    @Test
    fun `parse accepts a comma decimal and rejects text`() {
        assertEquals(12.5, WeightUnits.parse("12,5")!!, 0.0)
        assertEquals(12.5, WeightUnits.parse(" 12.5 ")!!, 0.0)
        assertNull(WeightUnits.parse("heavy"))
    }
}
