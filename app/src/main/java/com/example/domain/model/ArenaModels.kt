package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ArenaMode(val firestoreKey: String, val label: String) {
  TARGET("TARGET", "Obiettivo Rep"),
  SPRINT_60S("SPRINT_60S", "Sprint 60s"),
}

data class ArenaChallenge(
  val id: String,
  val title: String,
  val subtitle: String,
  val targetReps: Long,
  val timeLimitSeconds: Long?,
  val minFormScore: Long,
  val bonusCoins: Long,
  val difficultyLabel: String,
)

object ArenaCatalog {
  val soloChallenges: List<ArenaChallenge> =
    listOf(
      ArenaChallenge(
        id = "trial_sprint_60",
        title = "Sprint Lampo 60s",
        subtitle = "Completa almeno 15 flessioni valide in 60 secondi.",
        targetReps = 15L,
        timeLimitSeconds = 60L,
        minFormScore = 70L,
        bonusCoins = 15L,
        difficultyLabel = "VELOCE",
      ),
      ArenaChallenge(
        id = "trial_precision_25",
        title = "Prova di Precisione",
        subtitle = "Raggiungi 25 flessioni mantenendo almeno l'85% di forma.",
        targetReps = 25L,
        timeLimitSeconds = null,
        minFormScore = 85L,
        bonusCoins = 20L,
        difficultyLabel = "TECNICA",
      ),
      ArenaChallenge(
        id = "trial_spartan_boss",
        title = "Boss: Spartano d'Acciaio",
        subtitle = "Sfida d'élite: supera quota 40 flessioni in una singola serie.",
        targetReps = 40L,
        timeLimitSeconds = 150L,
        minFormScore = 80L,
        bonusCoins = 35L,
        difficultyLabel = "BOSS",
      ),
    )
}

@IgnoreExtraProperties
data class ArenaDuel(
  @DocumentId val id: String = "",
  val creatorId: String = "",
  val creatorName: String = "",
  val creatorReps: Long = 0L,
  val creatorFormScore: Long = 0L,
  val creatorFinished: Boolean = false,
  val opponentId: String = "",
  val opponentName: String = "",
  val opponentReps: Long = 0L,
  val opponentFormScore: Long = 0L,
  val opponentFinished: Boolean = false,
  val targetReps: Long = 20L,
  val mode: String = ArenaMode.TARGET.firestoreKey,
  val status: String = "OPEN",
  val winnerId: String = "",
  val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class WorkoutRewardSummary(
  val earnedCoins: Long,
  val effectiveFormScore: Long,
  val consumedBoostName: String? = null,
)

object BoostCalculator {
  fun computeAndConsume(
    profile: UserProfile,
    reps: Long,
    rawFormScore: Long,
    arenaBonusCoins: Long = 0L,
    isArenaMode: Boolean = false,
  ): Pair<WorkoutRewardSummary, List<String>> {
    val safeScore = rawFormScore.coerceIn(0L, 100L)
    val activeId = profile.activeBoostId.takeIf { it.isNotBlank() && it in profile.ownedBoosts }
    val activeBoostItem = activeId?.let { ShopCatalog.findById(it) }

    val effectiveScore =
      if (activeId == ShopCatalog.BOOST_PRECISION_FOCUS_ID) {
        (safeScore + 10L).coerceAtMost(100L)
      } else {
        safeScore
      }

    val baseCoins =
      if (activeId == ShopCatalog.BOOST_DOUBLE_COINS_ID) {
        reps * 2L
      } else {
        reps
      }

    val boostBonus =
      when (activeId) {
        ShopCatalog.BOOST_PRECISION_FOCUS_ID -> 5L
        ShopCatalog.BOOST_ARENA_SHIELD_ID -> if (isArenaMode) 15L else 0L
        else -> 0L
      }

    val maxAllowedCoins = (reps * 2L) + 40L
    val totalCoins = (baseCoins + arenaBonusCoins.coerceAtLeast(0L) + boostBonus).coerceIn(reps, maxAllowedCoins)

    val remainingBoosts =
      if (activeId != null) {
        val mutable = profile.ownedBoosts.toMutableList()
        val index = mutable.indexOf(activeId)
        if (index >= 0) mutable.removeAt(index)
        mutable.toList()
      } else {
        profile.ownedBoosts
      }

    val summary =
      WorkoutRewardSummary(
        earnedCoins = totalCoins,
        effectiveFormScore = effectiveScore,
        consumedBoostName = activeBoostItem?.name,
      )
    return summary to remainingBoosts
  }
}

