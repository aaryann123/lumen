package app.lumen

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ClockTest {
    private fun at(h: Int, m: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 28, h, m) } // a Wednesday

    @Test fun spellsTheHour() {
        assertEquals(listOf("TWELVE", "56", "PM", "Wednesday"), clockLines(at(12, 56)))
        assertEquals(listOf("TWELVE", "05", "AM", "Wednesday"), clockLines(at(0, 5)))
        assertEquals(listOf("THREE", "41", "PM", "Wednesday"), clockLines(at(15, 41)))
    }
}
