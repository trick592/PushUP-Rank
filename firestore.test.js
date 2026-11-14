const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const testTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, ts) {
  return {
    userId: uid,
    username: "AliceAthlete",
    email: "alice@example.com",
    avatarPreset: "avatar_bolt",
    totalPushUps: 0,
    weeklyPushUps: 0,
    weekKey: "2026-W46",
    pushCoins: 0,
    bestSessionReps: 0,
    arenaWins: 0,
    currentStreak: 0,
    longestStreak: 0,
    lastWorkoutDate: "",
    rankTier: "BRONZE",
    selectedThemeId: "theme_volt",
    selectedSkeletonEffectId: "skel_classic",
    selectedFrameId: "frame_none",
    selectedTitleId: "title_rookie",
    activeBoostId: "",
    unlockedItems: ["theme_volt", "skel_classic", "frame_none", "title_rookie"],
    ownedBoosts: [],
    friendUids: [],
    medicalDisclaimerAccepted: true,
    cameraConsentAccepted: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validLeaderboardEntry(uid, ts) {
  return {
    userId: uid,
    username: "AliceAthlete",
    avatarPreset: "avatar_bolt",
    selectedFrameId: "frame_none",
    selectedTitleId: "title_rookie",
    totalPushUps: 0,
    weeklyPushUps: 0,
    weekKey: "2026-W46",
    rankTier: "BRONZE",
    isPublic: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validArenaDuel(creatorId, ts) {
  return {
    creatorId,
    creatorName: "AliceAthlete",
    creatorReps: 0,
    creatorFormScore: 0,
    creatorFinished: false,
    opponentId: "",
    opponentName: "",
    opponentReps: 0,
    opponentFormScore: 0,
    opponentFinished: false,
    targetReps: 25,
    mode: "TARGET",
    status: "OPEN",
    winnerId: "",
    isPublic: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list users, leaderboard, or arena_duels", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("leaderboard").where("isPublic", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("arena_duels").where("isPublic", "==", true).get()
  );
});

test("Owner can create and read own profile, but cannot create with pre-minted PushCoins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, testTimestamp()))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const cheatedProfile = validUserProfile(BOB_UID, testTimestamp());
  cheatedProfile.pushCoins = 9999;
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(cheatedProfile));
});

test("Cross-user PII isolation and shadow update rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, testTimestamp()))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ username: "Hacked" })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "AlicePro",
      isAdmin: true,
      updatedAt: testTimestamp(),
    })
  );
});

test("Anti-cheat: impossible cadence or excessive PushCoin reward is rejected, 2x Boost + Arena bonus accepted", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, testTimestamp()))
  );

  // Cadence > 85 rpm rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w1").set({
      userId: ALICE_UID,
      reps: 50,
      durationSeconds: 10,
      averageFormScore: 95,
      earnedCoins: 50,
      repsPerMinute: 300.0,
      createdAt: testTimestamp(),
    })
  );

  // Valid workout with 2x boost accepted
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w2").set({
      userId: ALICE_UID,
      reps: 25,
      durationSeconds: 45,
      averageFormScore: 94,
      earnedCoins: 50,
      repsPerMinute: 33.3,
      createdAt: testTimestamp(),
    })
  );

  // Excessive coin jump (> 2x + 40) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPushUps: 25,
      weeklyPushUps: 25,
      weekKey: "2026-W46",
      pushCoins: 500,
      bestSessionReps: 25,
      arenaWins: 1,
      rankTier: "BRONZE",
      activeBoostId: "",
      ownedBoosts: [],
      updatedAt: testTimestamp(),
    })
  );

  // Valid reward and streak increment accepted
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPushUps: 25,
      weeklyPushUps: 25,
      weekKey: "2026-W46",
      pushCoins: 50,
      bestSessionReps: 25,
      arenaWins: 1,
      currentStreak: 1,
      longestStreak: 1,
      lastWorkoutDate: "2026-11-14",
      rankTier: "BRONZE",
      activeBoostId: "",
      ownedBoosts: [],
      updatedAt: testTimestamp(),
    })
  );

  // Impossible streak jump (> +1 in one workout) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPushUps: 40,
      weeklyPushUps: 40,
      weekKey: "2026-W46",
      pushCoins: 65,
      bestSessionReps: 25,
      arenaWins: 1,
      currentStreak: 99,
      longestStreak: 99,
      lastWorkoutDate: "2026-11-15",
      rankTier: "BRONZE",
      activeBoostId: "",
      ownedBoosts: [],
      updatedAt: testTimestamp(),
    })
  );
});

test("Leaderboard enforces isPublic filter and prevents cross-user overwrite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardEntry(ALICE_UID, testTimestamp()))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("leaderboard").get());
  await assertSucceeds(
    bobDb.collection("leaderboard").where("isPublic", "==", true).get()
  );
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalPushUps: 999,
      updatedAt: testTimestamp(),
    })
  );
});

test("Arena Duel lifecycle enforces join, score submission, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const duelRef = aliceDb.collection("arena_duels").doc("duel_001");
  await assertSucceeds(duelRef.set(validArenaDuel(ALICE_UID, testTimestamp())));

  // Creator cannot join own duel as opponent
  await assertFails(
    duelRef.update({
      opponentId: ALICE_UID,
      opponentName: "AliceAthlete",
      status: "ACTIVE",
      updatedAt: testTimestamp(),
    })
  );

  // Bob joins duel
  const bobDuelRef = bobDb.collection("arena_duels").doc("duel_001");
  await assertSucceeds(
    bobDuelRef.update({
      opponentId: BOB_UID,
      opponentName: "BobFit",
      status: "ACTIVE",
      updatedAt: testTimestamp(),
    })
  );

  // Charlie cannot hijack active duel
  await assertFails(
    charlieDb.collection("arena_duels").doc("duel_001").update({
      opponentId: CHARLIE_UID,
      opponentName: "Charlie",
      status: "ACTIVE",
      updatedAt: testTimestamp(),
    })
  );

  // Alice and Bob submit scores
  await assertSucceeds(
    duelRef.update({
      creatorReps: 25,
      creatorFormScore: 93,
      creatorFinished: true,
      status: "ACTIVE",
      winnerId: "",
      updatedAt: testTimestamp(),
    })
  );

  await assertSucceeds(
    bobDuelRef.update({
      opponentReps: 28,
      opponentFormScore: 95,
      opponentFinished: true,
      status: "FINISHED",
      winnerId: BOB_UID,
      updatedAt: testTimestamp(),
    })
  );

  // Terminal state lock: finished duel cannot be modified
  await assertFails(
    duelRef.update({
      creatorReps: 99,
      creatorFormScore: 100,
      creatorFinished: true,
      status: "FINISHED",
      winnerId: ALICE_UID,
      updatedAt: testTimestamp(),
    })
  );
});
