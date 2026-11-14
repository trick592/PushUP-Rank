package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.domain.model.ArenaDuel
import com.example.domain.model.ArenaMode
import com.example.domain.model.BoostCalculator
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.RankTier
import com.example.domain.model.ShopCatalog
import com.example.domain.model.ShopCategory
import com.example.domain.model.ShopItem
import com.example.domain.model.StreakCalculator
import com.example.domain.model.UserProfile
import com.example.domain.model.WeekKeyUtils
import com.example.domain.model.WorkoutRewardSummary
import com.example.domain.model.WorkoutSession
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.max

class PushUpRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("L'utente deve essere autenticato con Google.")
  }

  suspend fun ensureProfileInitialized(
    displayName: String?,
    email: String?,
    medicalAccepted: Boolean,
    cameraAccepted: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val leaderboardRef = db.collection("leaderboard").document(uid)

    val existingUser = try {
      userRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, userRef.path)
      throw e
    }

    val safeUsername = (displayName?.trim().takeIf { !it.isNullOrBlank() } ?: "Atleta_${uid.take(4)}")
      .take(24)
      .let { if (it.length < 2) "Atleta" else it }
    val weekKey = WeekKeyUtils.currentWeekKey()

    if (!existingUser.exists()) {
      val batch = db.batch()
      val userPayload = mapOf(
        "userId" to uid,
        "username" to safeUsername,
        "email" to (email ?: ""),
        "avatarPreset" to "avatar_bolt",
        "totalPushUps" to 0L,
        "weeklyPushUps" to 0L,
        "weekKey" to weekKey,
        "pushCoins" to 0L,
        "bestSessionReps" to 0L,
        "arenaWins" to 0L,
        "currentStreak" to 0L,
        "longestStreak" to 0L,
        "lastWorkoutDate" to "",
        "rankTier" to RankTier.BRONZE.firestoreKey,
        "selectedThemeId" to ShopCatalog.DEFAULT_THEME_ID,
        "selectedSkeletonEffectId" to ShopCatalog.DEFAULT_SKELETON_ID,
        "selectedFrameId" to ShopCatalog.DEFAULT_FRAME_ID,
        "selectedTitleId" to ShopCatalog.DEFAULT_TITLE_ID,
        "activeBoostId" to "",
        "unlockedItems" to ShopCatalog.defaultUnlockedIds,
        "ownedBoosts" to emptyList<String>(),
        "friendUids" to emptyList<String>(),
        "medicalDisclaimerAccepted" to medicalAccepted,
        "cameraConsentAccepted" to cameraAccepted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      val leaderboardPayload = mapOf(
        "userId" to uid,
        "username" to safeUsername,
        "avatarPreset" to "avatar_bolt",
        "selectedFrameId" to ShopCatalog.DEFAULT_FRAME_ID,
        "selectedTitleId" to ShopCatalog.DEFAULT_TITLE_ID,
        "totalPushUps" to 0L,
        "weeklyPushUps" to 0L,
        "weekKey" to weekKey,
        "rankTier" to RankTier.BRONZE.firestoreKey,
        "isPublic" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      batch.set(userRef, userPayload)
      batch.set(leaderboardRef, leaderboardPayload)
      try {
        batch.commit().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, userRef.path)
        throw e
      }
    }
  }

  fun observeUserProfile(): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  fun observeRecentWorkouts(limit: Long = 30L): Flow<List<WorkoutSession>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/workouts"
    emitAll(
      db.collection("users").document(uid).collection("workouts")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(WorkoutSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
    val uid = requireUserId()
    val path = "leaderboard"
    emitAll(
      db.collection(path)
        .whereEqualTo("isPublic", true)
        .limit(100)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeArenaDuels(): Flow<List<ArenaDuel>> = flow {
    val uid = requireUserId()
    val path = "arena_duels"
    emitAll(
      db.collection(path)
        .whereEqualTo("isPublic", true)
        .limit(50)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(ArenaDuel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(
              compareBy<ArenaDuel>(
                {
                  when (it.status) {
                    "ACTIVE" -> 0
                    "OPEN" -> 1
                    else -> 2
                  }
                },
                { -(it.updatedAt?.toDate()?.time ?: 0L) },
              )
            )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createArenaDuel(
    currentProfile: UserProfile,
    targetReps: Long,
    mode: ArenaMode,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(targetReps in 10L..100L) { "Obiettivo del duello deve essere tra 10 e 100 rep." }
    val safeName = currentProfile.username.trim().take(24).let { if (it.length < 2) "Atleta" else it }
    val duelId = UUID.randomUUID().toString()
    val duelRef = db.collection("arena_duels").document(duelId)
    val payload = mapOf(
      "creatorId" to uid,
      "creatorName" to safeName,
      "creatorReps" to 0L,
      "creatorFormScore" to 0L,
      "creatorFinished" to false,
      "opponentId" to "",
      "opponentName" to "",
      "opponentReps" to 0L,
      "opponentFormScore" to 0L,
      "opponentFinished" to false,
      "targetReps" to targetReps,
      "mode" to mode.firestoreKey,
      "status" to "OPEN",
      "winnerId" to "",
      "isPublic" to true,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      duelRef.set(payload).await()
      duelId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, duelRef.path)
      throw e
    }
  }

  suspend fun joinArenaDuel(duelId: String, currentProfile: UserProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeName = currentProfile.username.trim().take(24).let { if (it.length < 2) "Atleta" else it }
    val duelRef = db.collection("arena_duels").document(duelId)
    try {
      duelRef.update(
        mapOf(
          "opponentId" to uid,
          "opponentName" to safeName,
          "status" to "ACTIVE",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, duelRef.path)
      throw e
    }
  }

  suspend fun cancelOpenDuel(duelId: String): Result<Unit> = runCatching {
    requireUserId()
    val duelRef = db.collection("arena_duels").document(duelId)
    try {
      duelRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, duelRef.path)
      throw e
    }
  }

  suspend fun submitArenaDuelScore(
    duelId: String,
    currentProfile: UserProfile,
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
  ): Result<WorkoutRewardSummary> = runCatching {
    val uid = requireUserId()
    require(reps in 1L..300L) { "Numero di flessioni non valido per una singola sessione." }
    val safeDuration = durationSeconds.coerceAtLeast(2L)
    val repsPerMinute = (reps.toDouble() / (safeDuration.toDouble() / 60.0))
    require(repsPerMinute <= 85.0) {
      "Cadenza superiore al limite fisiologico anti-cheat (${String.format("%.1f", repsPerMinute)} rep/min)."
    }

    val duelRef = db.collection("arena_duels").document(duelId)
    val snapshot = try {
      duelRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, duelRef.path)
      throw e
    }
    val duel = snapshot.toObject(ArenaDuel::class.java)
      ?: throw IllegalStateException("Duello non trovato o annullato.")

    val isCreator = duel.creatorId == uid
    val isOpponent = duel.opponentId == uid
    require(isCreator || isOpponent) { "Non partecipi a questo duello." }

    val (rewardPreview, _) =
      BoostCalculator.computeAndConsume(
        profile = currentProfile,
        reps = reps,
        rawFormScore = averageFormScore,
        isArenaMode = true,
      )
    val effectiveScore = rewardPreview.effectiveFormScore

    val bothFinished = if (isCreator) duel.opponentFinished else duel.creatorFinished
    val newStatus = if (bothFinished) "FINISHED" else duel.status
    val winnerId =
      if (!bothFinished) {
        ""
      } else {
        val otherReps = if (isCreator) duel.opponentReps else duel.creatorReps
        val otherScore = if (isCreator) duel.opponentFormScore else duel.creatorFormScore
        val otherUid = if (isCreator) duel.opponentId else duel.creatorId
        when {
          reps > otherReps -> uid
          reps < otherReps -> otherUid
          effectiveScore > otherScore -> uid
          effectiveScore < otherScore -> otherUid
          else -> ""
        }
      }

    val duelUpdates =
      if (isCreator) {
        mapOf(
          "creatorReps" to reps,
          "creatorFormScore" to effectiveScore,
          "creatorFinished" to true,
          "status" to newStatus,
          "winnerId" to winnerId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      } else {
        mapOf(
          "opponentReps" to reps,
          "opponentFormScore" to effectiveScore,
          "opponentFinished" to true,
          "status" to newStatus,
          "winnerId" to winnerId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      }

    try {
      duelRef.update(duelUpdates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, duelRef.path)
      throw e
    }

    val didWin = bothFinished && winnerId == uid
    val duelBonus =
      when {
        didWin -> 20L
        bothFinished && winnerId.isEmpty() -> 10L
        else -> 5L
      }

    recordWorkoutSession(
      currentProfile = currentProfile,
      reps = reps,
      durationSeconds = safeDuration,
      averageFormScore = averageFormScore,
      arenaBonusCoins = duelBonus,
      isArenaWin = didWin,
      isArenaMode = true,
    ).getOrThrow()
  }

  suspend fun recordWorkoutSession(
    currentProfile: UserProfile,
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
    arenaBonusCoins: Long = 0L,
    isArenaWin: Boolean = false,
    isArenaMode: Boolean = false,
  ): Result<WorkoutRewardSummary> = runCatching {
    val uid = requireUserId()
    require(reps in 1L..300L) { "Numero di flessioni non valido per una singola sessione." }
    val safeDuration = durationSeconds.coerceAtLeast(2L)
    val repsPerMinute = (reps.toDouble() / (safeDuration.toDouble() / 60.0))
    require(repsPerMinute <= 85.0) {
      "Cadenza superiore al limite fisiologico anti-cheat (${String.format("%.1f", repsPerMinute)} rep/min)."
    }

    val (rewardSummary, remainingBoosts) =
      BoostCalculator.computeAndConsume(
        profile = currentProfile,
        reps = reps,
        rawFormScore = averageFormScore,
        arenaBonusCoins = arenaBonusCoins,
        isArenaMode = isArenaMode,
      )

    val currentWeek = WeekKeyUtils.currentWeekKey()
    val newTotal = currentProfile.totalPushUps + reps
    val newWeekly =
      if (currentProfile.weekKey == currentWeek) currentProfile.weeklyPushUps + reps else reps
    val newCoins = currentProfile.pushCoins + rewardSummary.earnedCoins
    val newBest = max(currentProfile.bestSessionReps, reps)
    val newArenaWins = currentProfile.arenaWins + if (isArenaWin) 1L else 0L
    val streakUpdate =
      StreakCalculator.nextStreakOnWorkout(
        previousStreak = currentProfile.currentStreak,
        previousLongest = currentProfile.longestStreak,
        lastWorkoutDate = currentProfile.lastWorkoutDate,
      )
    val newRank = RankTier.fromTotalPushUps(newTotal).firestoreKey

    val workoutId = UUID.randomUUID().toString()
    val workoutRef = db.collection("users").document(uid).collection("workouts").document(workoutId)
    val userRef = db.collection("users").document(uid)
    val leaderboardRef = db.collection("leaderboard").document(uid)

    val workoutPayload = mapOf(
      "userId" to uid,
      "reps" to reps,
      "durationSeconds" to safeDuration,
      "averageFormScore" to rewardSummary.effectiveFormScore,
      "earnedCoins" to rewardSummary.earnedCoins,
      "repsPerMinute" to repsPerMinute,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    val userUpdates = mapOf(
      "totalPushUps" to newTotal,
      "weeklyPushUps" to newWeekly,
      "weekKey" to currentWeek,
      "pushCoins" to newCoins,
      "bestSessionReps" to newBest,
      "arenaWins" to newArenaWins,
      "currentStreak" to streakUpdate.currentStreak,
      "longestStreak" to streakUpdate.longestStreak,
      "lastWorkoutDate" to streakUpdate.lastWorkoutDate,
      "rankTier" to newRank,
      "activeBoostId" to "",
      "ownedBoosts" to remainingBoosts,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val leaderboardUpdates = mapOf(
      "username" to currentProfile.username,
      "avatarPreset" to currentProfile.avatarPreset,
      "selectedFrameId" to currentProfile.selectedFrameId,
      "selectedTitleId" to currentProfile.selectedTitleId,
      "totalPushUps" to newTotal,
      "weeklyPushUps" to newWeekly,
      "weekKey" to currentWeek,
      "rankTier" to newRank,
      "isPublic" to true,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val batch = db.batch()
    batch.set(workoutRef, workoutPayload)
    batch.update(userRef, userUpdates)
    batch.update(leaderboardRef, leaderboardUpdates)
    try {
      batch.commit().await()
      rewardSummary
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, workoutRef.path)
      throw e
    }
  }

  suspend fun purchaseOrEquipItem(
    currentProfile: UserProfile,
    item: ShopItem,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val leaderboardRef = db.collection("leaderboard").document(uid)

    if (item.category == ShopCategory.BOOST) {
      require(currentProfile.ownedBoosts.size < 20) {
        "Inventario potenziamenti pieno (massimo 20 cariche)."
      }
      require(currentProfile.pushCoins >= item.priceCoins) {
        "PushCoin insufficienti: servono ${item.priceCoins} PushCoin."
      }
      val newBalance = currentProfile.pushCoins - item.priceCoins
      val updatedBoosts = currentProfile.ownedBoosts + item.id
      try {
        userRef.update(
          mapOf(
            "pushCoins" to newBalance,
            "ownedBoosts" to updatedBoosts,
            "activeBoostId" to item.id,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, userRef.path)
        throw e
      }
      return@runCatching
    }

    val isUnlocked = item.id in currentProfile.unlockedItems
    val newBalance =
      if (isUnlocked) {
        currentProfile.pushCoins
      } else {
        require(currentProfile.pushCoins >= item.priceCoins) {
          "PushCoin insufficienti: servono ${item.priceCoins} PushCoin."
        }
        currentProfile.pushCoins - item.priceCoins
      }

    val updatedUnlocked =
      if (isUnlocked) currentProfile.unlockedItems else (currentProfile.unlockedItems + item.id).distinct()

    val themeId = if (item.category == ShopCategory.THEME) item.id else currentProfile.selectedThemeId
    val skeletonId =
      if (item.category == ShopCategory.SKELETON) item.id else currentProfile.selectedSkeletonEffectId
    val frameId = if (item.category == ShopCategory.FRAME) item.id else currentProfile.selectedFrameId
    val titleId = if (item.category == ShopCategory.TITLE) item.id else currentProfile.selectedTitleId

    val batch = db.batch()
    batch.update(
      userRef,
      mapOf(
        "pushCoins" to newBalance,
        "unlockedItems" to updatedUnlocked,
        "selectedThemeId" to themeId,
        "selectedSkeletonEffectId" to skeletonId,
        "selectedFrameId" to frameId,
        "selectedTitleId" to titleId,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    if (item.category == ShopCategory.FRAME || item.category == ShopCategory.TITLE) {
      batch.update(
        leaderboardRef,
        mapOf(
          "selectedFrameId" to frameId,
          "selectedTitleId" to titleId,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun equipOwnedBoost(currentProfile: UserProfile, boostId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    if (boostId.isNotEmpty()) {
      require(boostId in currentProfile.ownedBoosts) {
        "Non possiedi cariche per questo potenziamento."
      }
    }
    val userRef = db.collection("users").document(uid)
    try {
      userRef.update(
        mapOf(
          "activeBoostId" to boostId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun updateProfileDisplay(
    username: String,
    avatarPreset: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanName = username.trim().take(24)
    require(cleanName.length >= 2) { "Il nome utente deve avere almeno 2 caratteri." }
    val userRef = db.collection("users").document(uid)
    val leaderboardRef = db.collection("leaderboard").document(uid)
    val batch = db.batch()
    batch.update(
      userRef,
      mapOf(
        "username" to cleanName,
        "avatarPreset" to avatarPreset,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.update(
      leaderboardRef,
      mapOf(
        "username" to cleanName,
        "avatarPreset" to avatarPreset,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun toggleFriend(currentProfile: UserProfile, targetUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(targetUid != uid) { "Non puoi aggiungere te stesso come amico." }
    val updated =
      if (targetUid in currentProfile.friendUids) {
        currentProfile.friendUids - targetUid
      } else {
        require(currentProfile.friendUids.size < 50) { "Limite massimo di 50 amici raggiunto." }
        currentProfile.friendUids + targetUid
      }
    val userRef = db.collection("users").document(uid)
    try {
      userRef.update(
        mapOf(
          "friendUids" to updated,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun syncComplianceFlags(
    medicalAccepted: Boolean,
    cameraAccepted: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      userRef.update(
        mapOf(
          "medicalDisclaimerAccepted" to medicalAccepted,
          "cameraConsentAccepted" to cameraAccepted,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun deleteAccountAndAllData(): Result<Unit> = runCatching {
    val user = auth.currentUser ?: throw IllegalStateException("Nessun utente autenticato.")
    val uid = user.uid
    val workoutsCollection = db.collection("users").document(uid).collection("workouts")
    try {
      val workouts = workoutsCollection.get().await()
      val openDuels = db.collection("arena_duels")
        .whereEqualTo("creatorId", uid)
        .whereEqualTo("status", "OPEN")
        .get()
        .await()
      val batch = db.batch()
      workouts.documents.forEach { doc -> batch.delete(doc.reference) }
      openDuels.documents.forEach { doc -> batch.delete(doc.reference) }
      batch.delete(db.collection("leaderboard").document(uid))
      batch.delete(db.collection("users").document(uid))
      batch.commit().await()
      user.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$uid")
      throw e
    }
  }
}
