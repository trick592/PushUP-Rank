package com.example.ui.components

import com.example.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutChartAnalyzerTest {

  @Test
  fun analyze_emptyList_returnsZeros() {
    val summary = WorkoutChartAnalyzer.analyze(emptyList())
    assertEquals(0, summary.chronological.size)
    assertEquals(0, summary.averageReps)
    assertEquals(0L, summary.peakReps)
    assertNull(summary.deltaFromPrevious)
    assertEquals(0L, summary.overallDeltaReps)
  }

  @Test
  fun analyze_descendingWorkouts_reversesChronologicallyAndComputesMetrics() {
    val newest = WorkoutSession(id = "w3", reps = 120L, durationSeconds = 95L)
    val middle = WorkoutSession(id = "w2", reps = 110L, durationSeconds = 90L)
    val oldest = WorkoutSession(id = "w1", reps = 100L, durationSeconds = 85L)

    val summary = WorkoutChartAnalyzer.analyze(listOf(newest, middle, oldest))

    assertEquals(listOf("w1", "w2", "w3"), summary.chronological.map { it.id })
    assertEquals(110, summary.averageReps)
    assertEquals(120L, summary.peakReps)
    assertEquals(10L, summary.deltaFromPrevious)
    assertEquals(20L, summary.overallDeltaReps)
  }

  @Test
  fun nearestIndexForX_clampsAndSnapsProperly() {
    assertEquals(0, WorkoutChartAnalyzer.nearestIndexForX(touchX = 0f, plotStartX = 40f, plotWidth = 300f, itemCount = 4))
    assertEquals(1, WorkoutChartAnalyzer.nearestIndexForX(touchX = 140f, plotStartX = 40f, plotWidth = 300f, itemCount = 4))
    assertEquals(2, WorkoutChartAnalyzer.nearestIndexForX(touchX = 240f, plotStartX = 40f, plotWidth = 300f, itemCount = 4))
    assertEquals(3, WorkoutChartAnalyzer.nearestIndexForX(touchX = 500f, plotStartX = 40f, plotWidth = 300f, itemCount = 4))
  }
}
