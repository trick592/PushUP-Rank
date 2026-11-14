package com.example.pose

import android.graphics.PointF
import com.example.domain.model.RankTier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PushUpDetectorTest {

  @Test
  fun calculateAngle_rightAngle_returns90Degrees() {
    val shoulder = PointF(0f, 0f)
    val elbow = PointF(0f, 100f)
    val wrist = PointF(100f, 100f)
    val angle = PushUpDetector.calculateAngle(shoulder, elbow, wrist)
    assertEquals(90f, angle, 0.01f)
  }

  @Test
  fun calculateAngle_straightLine_returns180Degrees() {
    val shoulder = PointF(0f, 0f)
    val elbow = PointF(50f, 0f)
    val wrist = PointF(100f, 0f)
    val angle = PushUpDetector.calculateAngle(shoulder, elbow, wrist)
    assertEquals(180f, angle, 0.01f)
  }

  @Test
  fun rankTier_thresholdsMapCorrectly() {
    assertEquals(RankTier.BRONZE, RankTier.fromTotalPushUps(0L))
    assertEquals(RankTier.SILVER, RankTier.fromTotalPushUps(100L))
    assertEquals(RankTier.GOLD, RankTier.fromTotalPushUps(500L))
    assertEquals(RankTier.PLATINUM, RankTier.fromTotalPushUps(1500L))
    assertEquals(RankTier.DIAMOND, RankTier.fromTotalPushUps(4000L))
    assertEquals(RankTier.MASTER, RankTier.fromTotalPushUps(10000L))
  }
}
