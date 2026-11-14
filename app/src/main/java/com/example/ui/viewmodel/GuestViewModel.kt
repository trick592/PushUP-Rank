package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CompliancePreferences
import com.example.domain.model.BoostCalculator
import com.example.domain.model.RankTier
import com.example.domain.model.ShopCatalog
import com.example.domain.model.ShopCategory
import com.example.domain.model.ShopItem
import com.example.domain.model.StreakCalculator
import com.example.domain.model.UserProfile
import com.example.domain.model.WeekKeyUtils
import com.example.domain.model.WorkoutSession
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID
import kotlin.math.max

@Serializable
data class LocalGuestProfile(
  val username: String = "Ospite",
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
  val selectedThemeId: String = ShopCatalog.DEFAULT_THEME_ID,
  val selectedSkeletonEffectId: String = ShopCatalog.DEFAULT_SKELETON_ID,
  val selectedFrameId: String = ShopCatalog.DEFAULT_FRAME_ID,
  val selectedTitleId: String = ShopCatalog.DEFAULT_TITLE_ID,
  val activeBoostId: String = "",
  val unlockedItems: List<String> = ShopCatalog.defaultUnlockedIds,
  val ownedBoosts: List<String> = emptyList(),
)

@Serializable
data class LocalGuestWorkout(
  val id: String,
  val reps: Long,
  val durationSeconds: Long,
  val averageFormScore: Long,
  val earnedCoins: Long,
  val repsPerMinute: Double,
  val timestampMillis: Long,
)

class GuestViewModel(
  private val compliancePreferences: CompliancePreferences,
) : ViewModel() {

  private val json = Json { ignoreUnknownKeys = true }

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  val medicalDisclaimerAccepted: StateFlow<Boolean?> =
    compliancePreferences.medicalDisclaimerAccepted
      .map<Boolean, Boolean?> { it }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val cameraConsentAccepted: StateFlow<Boolean> =
    compliancePreferences.cameraConsentAccepted
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

  val soundEnabled: StateFlow<Boolean> =
    compliancePreferences.soundEnabled
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

  val userProfileState: StateFlow<UiState<UserProfile>> =
    compliancePreferences.guestProfileJson
      .map<String?, UiState<UserProfile>> { raw ->
        val local = decodeProfile(raw)
        UiState.Success(local.toUserProfile())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val recentWorkoutsState: StateFlow<UiState<List<WorkoutSession>>> =
    compliancePreferences.guestWorkoutsJson
      .map<String?, UiState<List<WorkoutSession>>> { raw ->
        val list = decodeWorkouts(raw).map { it.toWorkoutSession() }
        UiState.Success(list)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  fun acceptMedicalDisclaimer() {
    viewModelScope.launch {
      compliancePreferences.setMedicalDisclaimerAccepted(true)
    }
  }

  fun acceptCameraConsent() {
    viewModelScope.launch {
      compliancePreferences.setCameraConsentAccepted(true)
    }
  }

  fun toggleSound() {
    viewModelScope.launch {
      val current = compliancePreferences.soundEnabled.first()
      compliancePreferences.setSoundEnabled(!current)
    }
  }

  fun recordWorkout(
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
    arenaBonusCoins: Long = 0L,
    isArenaWin: Boolean = false,
    isArenaMode: Boolean = false,
    onSaved: () -> Unit = {},
  ) {
    if (reps !in 1L..300L) {
      _actionMessage.value = "Numero di flessioni non valido per una singola sessione."
      return
    }
    val safeDuration = durationSeconds.coerceAtLeast(2L)
    val repsPerMinute = reps.toDouble() / (safeDuration.toDouble() / 60.0)
    if (repsPerMinute > 85.0) {
      _actionMessage.value = "Cadenza superiore al limite fisiologico anti-cheat."
      return
    }

    viewModelScope.launch {
      val current = decodeProfile(compliancePreferences.guestProfileJson.first())
      val existingWorkouts = decodeWorkouts(compliancePreferences.guestWorkoutsJson.first())
      val currentWeek = WeekKeyUtils.currentWeekKey()

      val (rewardSummary, remainingBoosts) =
        BoostCalculator.computeAndConsume(
          profile = current.toUserProfile(),
          reps = reps,
          rawFormScore = averageFormScore,
          arenaBonusCoins = arenaBonusCoins,
          isArenaMode = isArenaMode,
        )

      val streakUpdate =
        StreakCalculator.nextStreakOnWorkout(
          previousStreak = current.currentStreak,
          previousLongest = current.longestStreak,
          lastWorkoutDate = current.lastWorkoutDate,
        )

      val updatedProfile =
        current.copy(
          totalPushUps = current.totalPushUps + reps,
          weeklyPushUps = if (current.weekKey == currentWeek) current.weeklyPushUps + reps else reps,
          weekKey = currentWeek,
          pushCoins = current.pushCoins + rewardSummary.earnedCoins,
          bestSessionReps = max(current.bestSessionReps, reps),
          arenaWins = current.arenaWins + if (isArenaWin) 1L else 0L,
          currentStreak = streakUpdate.currentStreak,
          longestStreak = streakUpdate.longestStreak,
          lastWorkoutDate = streakUpdate.lastWorkoutDate,
          activeBoostId = "",
          ownedBoosts = remainingBoosts,
        )
      val newWorkout =
        LocalGuestWorkout(
          id = UUID.randomUUID().toString(),
          reps = reps,
          durationSeconds = safeDuration,
          averageFormScore = rewardSummary.effectiveFormScore,
          earnedCoins = rewardSummary.earnedCoins,
          repsPerMinute = repsPerMinute,
          timestampMillis = System.currentTimeMillis(),
        )
      val updatedWorkouts = (listOf(newWorkout) + existingWorkouts).take(50)

      compliancePreferences.saveGuestState(
        profileJson = json.encodeToString(updatedProfile),
        workoutsJson = json.encodeToString(updatedWorkouts),
      )
      val boostNote = rewardSummary.consumedBoostName?.let { " ($it usato)" } ?: ""
      _actionMessage.value = "Sessione salvata in locale! +${rewardSummary.earnedCoins} PushCoin$boostNote."
      onSaved()
    }
  }

  fun purchaseOrEquip(item: ShopItem) {
    viewModelScope.launch {
      val current = decodeProfile(compliancePreferences.guestProfileJson.first())
      val existingWorkoutsJson = compliancePreferences.guestWorkoutsJson.first() ?: "[]"

      if (item.category == ShopCategory.BOOST) {
        if (current.ownedBoosts.size >= 20) {
          _actionMessage.value = "Inventario potenziamenti pieno (massimo 20 cariche)."
          return@launch
        }
        if (current.pushCoins < item.priceCoins) {
          _actionMessage.value = "PushCoin insufficienti: servono ${item.priceCoins} PushCoin."
          return@launch
        }
        val updated =
          current.copy(
            pushCoins = current.pushCoins - item.priceCoins,
            ownedBoosts = current.ownedBoosts + item.id,
            activeBoostId = item.id,
          )
        compliancePreferences.saveGuestState(
          profileJson = json.encodeToString(updated),
          workoutsJson = existingWorkoutsJson,
        )
        _actionMessage.value = "${item.name} acquistato e attivato!"
        return@launch
      }

      val alreadyUnlocked = item.id in current.unlockedItems
      val newCoins =
        if (alreadyUnlocked) {
          current.pushCoins
        } else {
          if (current.pushCoins < item.priceCoins) {
            _actionMessage.value = "PushCoin insufficienti: servono ${item.priceCoins} PushCoin."
            return@launch
          }
          current.pushCoins - item.priceCoins
        }

      val updatedUnlocked =
        if (alreadyUnlocked) current.unlockedItems else (current.unlockedItems + item.id).distinct()

      val updated =
        current.copy(
          pushCoins = newCoins,
          unlockedItems = updatedUnlocked,
          selectedThemeId = if (item.category == ShopCategory.THEME) item.id else current.selectedThemeId,
          selectedSkeletonEffectId =
            if (item.category == ShopCategory.SKELETON) item.id else current.selectedSkeletonEffectId,
          selectedFrameId = if (item.category == ShopCategory.FRAME) item.id else current.selectedFrameId,
          selectedTitleId = if (item.category == ShopCategory.TITLE) item.id else current.selectedTitleId,
        )

      compliancePreferences.saveGuestState(
        profileJson = json.encodeToString(updated),
        workoutsJson = existingWorkoutsJson,
      )
      _actionMessage.value =
        if (alreadyUnlocked) "${item.name} equipaggiato!"
        else "${item.name} sbloccato ed equipaggiato!"
    }
  }

  fun equipBoost(boostId: String) {
    viewModelScope.launch {
      val current = decodeProfile(compliancePreferences.guestProfileJson.first())
      if (boostId.isNotEmpty() && boostId !in current.ownedBoosts) {
        _actionMessage.value = "Non possiedi cariche per questo potenziamento."
        return@launch
      }
      val existingWorkoutsJson = compliancePreferences.guestWorkoutsJson.first() ?: "[]"
      val updated = current.copy(activeBoostId = boostId)
      compliancePreferences.saveGuestState(
        profileJson = json.encodeToString(updated),
        workoutsJson = existingWorkoutsJson,
      )
      _actionMessage.value =
        if (boostId.isBlank()) "Potenziamento disattivato."
        else "Potenziamento attivato per la prossima sessione!"
    }
  }

  fun updateProfile(username: String, avatarPreset: String) {
    val cleanName = username.trim().take(24)
    if (cleanName.length < 2) {
      _actionMessage.value = "Il nome utente deve avere almeno 2 caratteri."
      return
    }
    viewModelScope.launch {
      val current = decodeProfile(compliancePreferences.guestProfileJson.first())
      val existingWorkoutsJson = compliancePreferences.guestWorkoutsJson.first() ?: "[]"
      val updated = current.copy(username = cleanName, avatarPreset = avatarPreset)
      compliancePreferences.saveGuestState(
        profileJson = json.encodeToString(updated),
        workoutsJson = existingWorkoutsJson,
      )
      _actionMessage.value = "Profilo ospite aggiornato."
    }
  }

  fun exitGuestMode() {
    viewModelScope.launch {
      compliancePreferences.setGuestModeActive(false)
    }
  }

  fun deleteLocalData() {
    viewModelScope.launch {
      compliancePreferences.clearAll()
    }
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  private fun decodeProfile(raw: String?): LocalGuestProfile {
    if (raw.isNullOrBlank()) return LocalGuestProfile()
    return runCatching { json.decodeFromString<LocalGuestProfile>(raw) }.getOrDefault(LocalGuestProfile())
  }

  private fun decodeWorkouts(raw: String?): List<LocalGuestWorkout> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<LocalGuestWorkout>>(raw) }.getOrDefault(emptyList())
  }

  private fun LocalGuestProfile.toUserProfile(): UserProfile =
    UserProfile(
      id = GUEST_UID,
      userId = GUEST_UID,
      username = username,
      email = "Modalità Ospite (Solo Locale)",
      avatarPreset = avatarPreset,
      totalPushUps = totalPushUps,
      weeklyPushUps = weeklyPushUps,
      weekKey = weekKey,
      pushCoins = pushCoins,
      bestSessionReps = bestSessionReps,
      arenaWins = arenaWins,
      currentStreak = currentStreak,
      longestStreak = longestStreak,
      lastWorkoutDate = lastWorkoutDate,
      rankTier = RankTier.fromTotalPushUps(totalPushUps).firestoreKey,
      selectedThemeId = selectedThemeId,
      selectedSkeletonEffectId = selectedSkeletonEffectId,
      selectedFrameId = selectedFrameId,
      selectedTitleId = selectedTitleId,
      activeBoostId = activeBoostId,
      unlockedItems = unlockedItems,
      ownedBoosts = ownedBoosts,
    )

  private fun LocalGuestWorkout.toWorkoutSession(): WorkoutSession =
    WorkoutSession(
      id = id,
      userId = GUEST_UID,
      reps = reps,
      durationSeconds = durationSeconds,
      averageFormScore = averageFormScore,
      earnedCoins = earnedCoins,
      repsPerMinute = repsPerMinute,
      createdAt = Timestamp(Date(timestampMillis)),
    )

  companion object {
    const val GUEST_UID = "local_guest"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
