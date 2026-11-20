package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualLogTest {
    @Test
    fun entriesAddSetOrKeepTheMax() {
        fun e(v: Double, mode: ManualLog.Mode) = ManualLog.Entry("x", v, "count", mode)
        assertEquals(750.0, ManualLog.apply(500.0, e(250.0, ManualLog.Mode.ADD)), 0.0)
        assertEquals(250.0, ManualLog.apply(null, e(250.0, ManualLog.Mode.ADD)), 0.0)
        assertEquals(1.0, ManualLog.apply(1.0, e(1.0, ManualLog.Mode.SET)), 0.0)
        assertEquals(20.0, ManualLog.apply(20.0, e(15.0, ManualLog.Mode.MAX)), 0.0)
        assertEquals(0.0, ManualLog.apply(1.0, e(-5.0, ManualLog.Mode.ADD)), 0.0)
    }
}
