package app.lumen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuotesTest {
    @Test fun filters() {
        assertEquals(QUOTES.size, pool("all").size)
        assertTrue(pool("villains").all { QUOTES[it].villain })
        assertTrue(pool("heroes").isNotEmpty()) // no heroes left: falls back to everything instead of crashing
    }

    @Test fun everyQuoteOncePerRound() {
        val r = Random(1)
        val pool = pool("all")
        var bag = emptyList<Int>()
        var last = -1
        repeat(3) {
            val seen = mutableListOf<Int>()
            repeat(pool.size) { val (i, rest) = deal(bag, pool, last, r); assertNotEquals(last, i); seen += i; bag = rest; last = i }
            assertEquals(pool.sorted(), seen.sorted()) // each one exactly once, then a new round
        }
    }

    @Test fun shortSlotsGetShortLines() {
        for (i in QUOTES.indices) assertTrue(QUOTES[fitting(i, 40)].line.length <= 40)
        val short = QUOTES.indices.first { QUOTES[it].line.length <= 40 }
        assertEquals(short, fitting(short, 40)) // already fits: unchanged
    }
}
