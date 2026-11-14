package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

  @Test
  fun nextStreakOnWorkout_firstWorkout_startsAtOne() {
    val result =
      StreakCalculator.nextStreakOnWorkout(
        previousStreak = 0L,
        previousLongest = 0L,
        lastWorkoutDate = "",
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      )
    assertEquals(1L, result.currentStreak)
    assertEquals(1L, result.longestStreak)
    assertEquals("2026-11-14", result.lastWorkoutDate)
  }

  @Test
  fun nextStreakOnWorkout_consecutiveDay_incrementsStreakAndLongest() {
    val result =
      StreakCalculator.nextStreakOnWorkout(
        previousStreak = 3L,
        previousLongest = 3L,
        lastWorkoutDate = "2026-11-13",
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      )
    assertEquals(4L, result.currentStreak)
    assertEquals(4L, result.longestStreak)
    assertEquals("2026-11-14", result.lastWorkoutDate)
  }

  @Test
  fun nextStreakOnWorkout_sameDay_preservesStreak() {
    val result =
      StreakCalculator.nextStreakOnWorkout(
        previousStreak = 5L,
        previousLongest = 7L,
        lastWorkoutDate = "2026-11-14",
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      )
    assertEquals(5L, result.currentStreak)
    assertEquals(7L, result.longestStreak)
    assertEquals("2026-11-14", result.lastWorkoutDate)
  }

  @Test
  fun nextStreakOnWorkout_missedDay_resetsToOneButKeepsLongest() {
    val result =
      StreakCalculator.nextStreakOnWorkout(
        previousStreak = 6L,
        previousLongest = 10L,
        lastWorkoutDate = "2026-11-10",
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      )
    assertEquals(1L, result.currentStreak)
    assertEquals(10L, result.longestStreak)
    assertEquals("2026-11-14", result.lastWorkoutDate)
  }

  @Test
  fun effectiveDisplayStreak_returnsZeroWhenMissedYesterdayAndToday() {
    assertEquals(
      4L,
      StreakCalculator.effectiveDisplayStreak(
        lastWorkoutDate = "2026-11-14",
        storedStreak = 4L,
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      ),
    )
    assertEquals(
      4L,
      StreakCalculator.effectiveDisplayStreak(
        lastWorkoutDate = "2026-11-13",
        storedStreak = 4L,
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      ),
    )
    assertEquals(
      0L,
      StreakCalculator.effectiveDisplayStreak(
        lastWorkoutDate = "2026-11-12",
        storedStreak = 4L,
        todayDate = "2026-11-14",
        yesterdayDate = "2026-11-13",
      ),
    )
  }
}
