package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
import java.util.Locale

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "Atleta",
  val email: String = "",
  val avatarPreset: String = "avatar_bolt",
  val totalPushUps: Long = 0L,
  val weeklyPushUps: Long = 0L,
  val weekKey: String = WeekKeyUtils.currentWeekKey(),
  val pushCoins: Long = 0L,
  val bestSessionReps: Long = 0L,
  val arenaWins: Long = 0L,
  val currentStreak: Long = 0L,
  val longestStreak: Long = 0L,
  val lastWorkoutDate: String = "",
  val rankTier: String = RankTier.BRONZE.firestoreKey,
  val selectedThemeId: String = ShopCatalog.DEFAULT_THEME_ID,
  val selectedSkeletonEffectId: String = ShopCatalog.DEFAULT_SKELETON_ID,
  val selectedFrameId: String = ShopCatalog.DEFAULT_FRAME_ID,
  val selectedTitleId: String = ShopCatalog.DEFAULT_TITLE_ID,
  val activeBoostId: String = "",
  val unlockedItems: List<String> = ShopCatalog.defaultUnlockedIds,
  val ownedBoosts: List<String> = emptyList(),
  val friendUids: List<String> = emptyList(),
  val medicalDisclaimerAccepted: Boolean = false,
  val cameraConsentAccepted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WorkoutSession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val reps: Long = 0L,
  val durationSeconds: Long = 0L,
  val averageFormScore: Long = 0L,
  val earnedCoins: Long = 0L,
  val repsPerMinute: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LeaderboardEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "Atleta",
  val avatarPreset: String = "avatar_bolt",
  val selectedFrameId: String = ShopCatalog.DEFAULT_FRAME_ID,
  val selectedTitleId: String = ShopCatalog.DEFAULT_TITLE_ID,
  val totalPushUps: Long = 0L,
  val weeklyPushUps: Long = 0L,
  val weekKey: String = WeekKeyUtils.currentWeekKey(),
  val rankTier: String = RankTier.BRONZE.firestoreKey,
  val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

object WeekKeyUtils {
  fun currentWeekKey(): String {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.minimalDaysInFirstWeek = 4
    val year = calendar.get(Calendar.YEAR)
    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    return String.format(Locale.US, "%04d-W%02d", year, week)
  }
}

object DayKeyUtils {
  fun dateKey(daysOffset: Int = 0): String {
    val calendar = Calendar.getInstance()
    if (daysOffset != 0) {
      calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
  }
}

data class StreakUpdate(
  val currentStreak: Long,
  val longestStreak: Long,
  val lastWorkoutDate: String,
)

object StreakCalculator {
  fun nextStreakOnWorkout(
    previousStreak: Long,
    previousLongest: Long,
    lastWorkoutDate: String,
    todayDate: String = DayKeyUtils.dateKey(0),
    yesterdayDate: String = DayKeyUtils.dateKey(-1),
  ): StreakUpdate {
    val newStreak =
      when (lastWorkoutDate) {
        todayDate -> previousStreak.coerceAtLeast(1L)
        yesterdayDate -> (previousStreak + 1L).coerceAtMost(100000L)
        else -> 1L
      }
    val newLongest = maxOf(previousLongest, newStreak).coerceAtMost(100000L)
    return StreakUpdate(
      currentStreak = newStreak,
      longestStreak = newLongest,
      lastWorkoutDate = todayDate,
    )
  }

  fun effectiveDisplayStreak(
    lastWorkoutDate: String,
    storedStreak: Long,
    todayDate: String = DayKeyUtils.dateKey(0),
    yesterdayDate: String = DayKeyUtils.dateKey(-1),
  ): Long {
    if (storedStreak <= 0L) return 0L
    return if (lastWorkoutDate == todayDate || lastWorkoutDate == yesterdayDate) {
      storedStreak
    } else {
      0L
    }
  }
}

