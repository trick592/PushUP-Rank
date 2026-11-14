package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.ArenaMode
import com.example.domain.model.DayKeyUtils
import com.example.domain.model.ShopCatalog
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class PushUpRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureProfileInitialized_andRecordWorkout_awardsExactCoinsAndUpdatesLeaderboard() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val repository = PushUpRepository(firestore, auth)

    val initResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.ensureProfileInitialized(
          displayName = "AliceFit",
          email = "alice@test.com",
          medicalAccepted = true,
          cameraAccepted = true,
        )
      }
    assertThat(initResult.isSuccess).isTrue()

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserProfile().first { it != null }!!
      }
    assertThat(profile.userId).isEqualTo(uid)
    assertThat(profile.pushCoins).isEqualTo(0L)
    assertThat(profile.currentStreak).isEqualTo(0L)
    assertThat(profile.longestStreak).isEqualTo(0L)
    assertThat(profile.lastWorkoutDate).isEmpty()

    val recordResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.recordWorkoutSession(
          currentProfile = profile,
          reps = 30L,
          durationSeconds = 45L,
          averageFormScore = 92L,
        )
      }
    assertThat(recordResult.isSuccess).isTrue()

    val updatedProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserProfile().first { (it?.totalPushUps ?: 0L) == 30L }!!
      }
    assertThat(updatedProfile.pushCoins).isEqualTo(30L)
    assertThat(updatedProfile.weeklyPushUps).isEqualTo(30L)
    assertThat(updatedProfile.currentStreak).isEqualTo(1L)
    assertThat(updatedProfile.longestStreak).isEqualTo(1L)
    assertThat(updatedProfile.lastWorkoutDate).isEqualTo(DayKeyUtils.dateKey(0))

    // Second workout on the same day should preserve streak at 1
    val secondResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.recordWorkoutSession(
          currentProfile = updatedProfile,
          reps = 10L,
          durationSeconds = 20L,
          averageFormScore = 90L,
        )
      }
    assertThat(secondResult.isSuccess).isTrue()

    val sameDayProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserProfile().first { (it?.totalPushUps ?: 0L) == 40L }!!
      }
    assertThat(sameDayProfile.pushCoins).isEqualTo(40L)
    assertThat(sameDayProfile.currentStreak).isEqualTo(1L)
    assertThat(sameDayProfile.longestStreak).isEqualTo(1L)

    val shopItem = ShopCatalog.findById("skel_laser")!!
    val purchaseResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.purchaseOrEquipItem(sameDayProfile, shopItem)
      }
    assertThat(purchaseResult.isSuccess).isTrue()

    val afterPurchase =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserProfile().first { it?.selectedSkeletonEffectId == "skel_laser" }!!
      }
    assertThat(afterPurchase.pushCoins).isEqualTo(15L)

    val leaderboard =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeLeaderboard().first { list -> list.any { it.userId == uid && it.totalPushUps == 40L } }
      }
    assertThat(leaderboard.any { it.userId == uid }).isTrue()
  }

  @Test
  fun shopBoostAndTitlePurchase_consumesDoubleCoinsBoostOnNextWorkout() = runBlocking {
    signInTestUser("boost_${UUID.randomUUID().toString().take(6)}@test.com")
    val repository = PushUpRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureProfileInitialized("BoostTester", "boost@test.com", true, true).getOrThrow()
    }
    val initial = withTimeout(FLOW_TIMEOUT_MS) { repository.observeUserProfile().first { it != null }!! }

    // Earn 80 PushCoins (80 reps in 90s = 53.3 rpm <= 85)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordWorkoutSession(initial, reps = 80L, durationSeconds = 90L, averageFormScore = 90L).getOrThrow()
    }
    val funded = withTimeout(FLOW_TIMEOUT_MS) { repository.observeUserProfile().first { (it?.pushCoins ?: 0L) == 80L }!! }

    // Buy new title "Cuore Spartano" (40 PC)
    val spartanTitle = ShopCatalog.findById("title_spartan")!!
    withTimeout(DEFAULT_TIMEOUT_MS) { repository.purchaseOrEquipItem(funded, spartanTitle).getOrThrow() }
    val withTitle = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserProfile().first { it?.selectedTitleId == "title_spartan" }!!
    }
    assertThat(withTitle.pushCoins).isEqualTo(40L)

    // Buy 2x coins Boost (35 PC) -> 5 PC remaining, boost active
    val doubleBoost = ShopCatalog.findById(ShopCatalog.BOOST_DOUBLE_COINS_ID)!!
    withTimeout(DEFAULT_TIMEOUT_MS) { repository.purchaseOrEquipItem(withTitle, doubleBoost).getOrThrow() }
    val withBoost = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserProfile().first { it?.activeBoostId == ShopCatalog.BOOST_DOUBLE_COINS_ID }!!
    }
    assertThat(withBoost.pushCoins).isEqualTo(5L)
    assertThat(withBoost.ownedBoosts).containsExactly(ShopCatalog.BOOST_DOUBLE_COINS_ID)

    // Record 20 reps -> earns 40 PC (2x), consumes boost
    val reward = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordWorkoutSession(withBoost, reps = 20L, durationSeconds = 30L, averageFormScore = 88L).getOrThrow()
    }
    assertThat(reward.earnedCoins).isEqualTo(40L)

    val afterBoost = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserProfile().first { (it?.totalPushUps ?: 0L) == 100L }!!
    }
    assertThat(afterBoost.pushCoins).isEqualTo(45L)
    assertThat(afterBoost.activeBoostId).isEmpty()
    assertThat(afterBoost.ownedBoosts).isEmpty()
  }

  @Test
  fun arenaDuel_full1v1Lifecycle_declaresWinnerAndUpdatesArenaWins() = runBlocking {
    val aliceEmail = "duel_alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val repo = PushUpRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileInitialized("AliceDuel", aliceEmail, true, true).getOrThrow()
    }
    val aliceProfile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile().first { it != null }!! }

    val duelId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createArenaDuel(aliceProfile, targetReps = 25L, mode = ArenaMode.TARGET).getOrThrow()
    }

    // Bob joins and submits 25 reps
    val bobEmail = "duel_bob_${UUID.randomUUID().toString().take(6)}@test.com"
    val bobUid = signInTestUser(bobEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileInitialized("BobDuel", bobEmail, true, true).getOrThrow()
    }
    val bobProfile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile().first { it?.userId == bobUid }!! }
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.joinArenaDuel(duelId, bobProfile).getOrThrow() }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitArenaDuelScore(duelId, bobProfile, reps = 25L, durationSeconds = 35L, averageFormScore = 85L).getOrThrow()
    }

    // Alice signs back in and submits 30 reps -> Alice wins!
    val aliceBackUid = signInTestUser(aliceEmail)
    assertThat(aliceBackUid).isEqualTo(aliceUid)
    val aliceBackProfile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile().first { it?.userId == aliceUid }!! }
    val reward = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitArenaDuelScore(duelId, aliceBackProfile, reps = 30L, durationSeconds = 40L, averageFormScore = 92L).getOrThrow()
    }
    // 30 reps + 20 winner bonus = 50 PushCoins
    assertThat(reward.earnedCoins).isEqualTo(50L)

    val finishedDuel = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeArenaDuels().first { list -> list.any { it.id == duelId && it.status == "FINISHED" } }
        .first { it.id == duelId }
    }
    assertThat(finishedDuel.winnerId).isEqualTo(aliceUid)
    assertThat(finishedDuel.creatorReps).isEqualTo(30L)
    assertThat(finishedDuel.opponentReps).isEqualTo(25L)

    val aliceUpdated = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile().first { (it?.arenaWins ?: 0L) == 1L }!!
    }
    assertThat(aliceUpdated.pushCoins).isEqualTo(50L)
  }

  @Test
  fun crossUserProfileRead_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID().toString().take(6)}@test.com")
    val aliceRepo = PushUpRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureProfileInitialized("AliceIso", "alice@test.com", true, false).getOrThrow()
    }

    signInTestUser("bob_iso_${UUID.randomUUID().toString().take(6)}@test.com")
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
      fail("Expected PERMISSION_DENIED when reading another user's private profile")
    } catch (e: FirebaseFirestoreException) {
      assertThat(e.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }
  }

  @Test
  fun observeLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = PushUpRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeLeaderboard().first()
      }
      fail("Expected PERMISSION_DENIED when unauthenticated")
    } catch (e: Exception) {
      // Unauthenticated requireUserId() or PERMISSION_DENIED both enforce strict auth gating
      val firestoreException = e.unwrapFirestoreException()
      if (firestoreException != null) {
        assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
      } else {
        assertThat(e).isInstanceOf(IllegalStateException::class.java)
      }
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}

