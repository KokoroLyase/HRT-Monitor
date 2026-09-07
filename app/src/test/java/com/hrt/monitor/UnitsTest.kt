package com.hrt.monitor

import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    private fun assertNear(expected: Double, actual: Double, tol: Double = 0.02) {
        assertEquals(expected, actual, tol)
    }

    @Test
    fun e2Conversions() {
        assertNear(367.1, Units.toBase(Hormone.E2, 100.0, "pg/mL"))
        assertNear(100.0, Units.fromBase(Hormone.E2, 367.1, "pg/mL"), 0.05)
        assertEquals("367.1", Units.format(367.1))
    }

    @Test
    fun tConversions() {
        assertNear(9.16, Units.toBase(Hormone.T, 916.0, "ng/dL"))
        assertNear(0.28846, Units.toBase(Hormone.T, 1.0, "nmol/L"))
        assertNear(55.0, Units.fromBase(Hormone.T, 0.55, "ng/dL"), 0.01)
    }

    @Test
    fun prlConversions() {
        assertNear(23.585, Units.toBase(Hormone.PRL, 500.0, "mIU/L"))
        assertNear(23.3, Units.toBase(Hormone.PRL, 23.3, "ng/mL"))
    }

    @Test
    fun p4Conversions() {
        assertNear(3.18, Units.toBase(Hormone.P4, 1.0, "ng/mL"))
        assertNear(1.0, Units.fromBase(Hormone.P4, 3.18, "ng/mL"), 0.01)
    }

    @Test
    fun lhFshSameUnit() {
        assertEquals(1.0, Units.factor(Hormone.LH, "IU/L"), 0.0)
        assertEquals(1.0, Units.factor(Hormone.LH, "mIU/mL"), 0.0)
    }

    @Test
    fun format() {
        assertEquals("367.1", Units.format(367.1))
        assertEquals("0.55", Units.format(0.55))
        assertEquals("1284", Units.format(1284.4))
        assertEquals("0.001", Units.format(0.001))
    }
}
