package com.nexus.playtime

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaytimeCalcTest {
    private val h = 3_600_000L
    private fun s(state: SessionState, end: Long? = null) = GameSessionEntity("s", "g", SessionMethod.MANUAL, state, 0, end)

    @Test fun finishedSessionSubtractsPauses() =
        assertEquals(2 * h, PlaytimeCalc.activeMillis(s(SessionState.FINISHED, 3 * h), listOf(SessionPauseEntity(1, "s", h, 2 * h)), 10 * h))

    @Test fun runningSessionUsesNow() = assertEquals(h, PlaytimeCalc.activeMillis(s(SessionState.ACTIVE), emptyList(), h))

    @Test fun openPauseFreezesTime() =
        assertEquals(h, PlaytimeCalc.activeMillis(s(SessionState.PAUSED), listOf(SessionPauseEntity(1, "s", h, null)), 5 * h))

    @Test fun clockMovedBackwardsNeverNegative() = assertEquals(0, PlaytimeCalc.activeMillis(s(SessionState.ACTIVE), emptyList(), -h))
}
