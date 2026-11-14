package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.local.CompliancePreferences
import com.example.data.repository.PushUpRepository
import com.example.domain.model.ArenaChallenge
import com.example.domain.model.ArenaDuel
import com.example.domain.model.ArenaMode
import com.example.domain.model.BoostCalculator
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.ShopCatalog
import com.example.domain.model.ShopItem
import com.example.domain.model.UserProfile
import com.example.domain.model.WorkoutSession
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraPrivacyConsentDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MedicalDisclaimerScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.WorkoutCameraScreen
import com.example.ui.theme.PushUpRankTheme
import com.example.ui.viewmodel.GuestViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data object ArenaRoute
@Serializable data object LeaderboardRoute
@Serializable data object ShopRoute
@Serializable data object ProfileRoute
@Serializable data object WorkoutRoute

private sealed interface PendingWorkoutContext {
  data object Standard : PendingWorkoutContext
  data class SoloChallenge(val challenge: ArenaChallenge) : PendingWorkoutContext
  data class DuelTurn(val duel: ArenaDuel) : PendingWorkoutContext
}

private data class TopLevelTab(
  val label: String,
  val route: Any,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topLevelTabs =
  listOf(
    TopLevelTab("Home", DashboardRoute, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    TopLevelTab("Arena", ArenaRoute, Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "nav_arena"),
    TopLevelTab("Classifica", LeaderboardRoute, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_leaderboard"),
    TopLevelTab("Negozio", ShopRoute, Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_shop"),
    TopLevelTab("Profilo", ProfileRoute, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
  )

@Composable
fun PushUpRankRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val compliancePreferences = remember(context) { CompliancePreferences(context.applicationContext) }

  val currentUser by
    AuthManager.authStateFlow(Firebase.auth)
      .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  val isGuestMode by
    compliancePreferences.guestModeActive.collectAsStateWithLifecycle(initialValue = false)

  val user = currentUser
  LaunchedEffect(user?.uid) {
    if (user != null && isGuestMode) {
      compliancePreferences.setGuestModeActive(false)
    }
  }

  when {
    user != null -> {
      androidx.compose.runtime.key(user.uid) {
        AuthenticatedApp(currentUserId = user.uid)
      }
    }
    isGuestMode -> {
      GuestApp(compliancePreferences = compliancePreferences)
    }
    else -> {
      PushUpRankTheme {
        AuthScreen(
          onAuthSuccess = {},
          onContinueAsGuest = {
            scope.launch { compliancePreferences.setGuestModeActive(true) }
          },
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedApp(currentUserId: String) {
  val viewModel: MainViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) as Application
            val dbId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            val repository = PushUpRepository(db, Firebase.auth)
            val prefs = CompliancePreferences(app.applicationContext)
            MainViewModel(repository, prefs, currentUserId)
          }
        },
    )

  val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val workoutsState by viewModel.recentWorkoutsState.collectAsStateWithLifecycle()
  val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
  val arenaDuelsState by viewModel.arenaDuelsState.collectAsStateWithLifecycle()
  val medicalAccepted by viewModel.medicalDisclaimerAccepted.collectAsStateWithLifecycle()
  val cameraAccepted by viewModel.cameraConsentAccepted.collectAsStateWithLifecycle()
  val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  PushUpMainScaffold(
    currentUserId = currentUserId,
    isGuestMode = false,
    profileState = profileState,
    workoutsState = workoutsState,
    leaderboardState = leaderboardState,
    arenaDuelsState = arenaDuelsState,
    medicalAccepted = medicalAccepted,
    cameraAccepted = cameraAccepted,
    soundEnabled = soundEnabled,
    actionMessage = actionMessage,
    isDeletingAccount = isDeletingAccount,
    onClearActionMessage = viewModel::clearActionMessage,
    onAcceptMedical = viewModel::acceptMedicalDisclaimer,
    onAcceptCamera = viewModel::acceptCameraConsent,
    onToggleSound = viewModel::toggleSound,
    onRecordWorkout = viewModel::recordWorkout,
    onSubmitDuelWorkout = viewModel::submitDuelWorkout,
    onCreateDuel = viewModel::createArenaDuel,
    onJoinDuel = viewModel::joinArenaDuel,
    onCancelDuel = viewModel::cancelArenaDuel,
    onPurchaseOrEquip = viewModel::purchaseOrEquip,
    onEquipBoost = viewModel::equipBoost,
    onUpdateProfile = viewModel::updateProfile,
    onToggleFriend = viewModel::toggleFriend,
    onUpgradeToGoogle = {},
    onSignOut = { AuthManager.signOut(credentialManager, scope) },
    onDeleteAccount = {
      viewModel.deleteAccountAndData(
        onComplete = { AuthManager.signOut(credentialManager, scope) },
        onRequiresRecentLogin = { AuthManager.signOut(credentialManager, scope) },
      )
    },
  )
}

@Composable
private fun GuestApp(compliancePreferences: CompliancePreferences) {
  val viewModel: GuestViewModel =
    viewModel(
      key = GuestViewModel.GUEST_UID,
      factory =
        viewModelFactory {
          initializer { GuestViewModel(compliancePreferences) }
        },
    )

  val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val workoutsState by viewModel.recentWorkoutsState.collectAsStateWithLifecycle()
  val medicalAccepted by viewModel.medicalDisclaimerAccepted.collectAsStateWithLifecycle()
  val cameraAccepted by viewModel.cameraConsentAccepted.collectAsStateWithLifecycle()
  val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var upgradeError by remember { mutableStateOf<String?>(null) }

  PushUpMainScaffold(
    currentUserId = GuestViewModel.GUEST_UID,
    isGuestMode = true,
    profileState = profileState,
    workoutsState = workoutsState,
    leaderboardState = UiState.Success(emptyList()),
    arenaDuelsState = UiState.Success(emptyList()),
    medicalAccepted = medicalAccepted,
    cameraAccepted = cameraAccepted,
    soundEnabled = soundEnabled,
    actionMessage = upgradeError ?: actionMessage,
    isDeletingAccount = false,
    onClearActionMessage = {
      upgradeError = null
      viewModel.clearActionMessage()
    },
    onAcceptMedical = viewModel::acceptMedicalDisclaimer,
    onAcceptCamera = viewModel::acceptCameraConsent,
    onToggleSound = viewModel::toggleSound,
    onRecordWorkout = viewModel::recordWorkout,
    onSubmitDuelWorkout = { _, _, _, _, _ -> },
    onCreateDuel = { _, _ -> },
    onJoinDuel = {},
    onCancelDuel = {},
    onPurchaseOrEquip = viewModel::purchaseOrEquip,
    onEquipBoost = viewModel::equipBoost,
    onUpdateProfile = viewModel::updateProfile,
    onToggleFriend = {},
    onUpgradeToGoogle = {
      AuthManager.onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          scope.launch { compliancePreferences.setGuestModeActive(false) }
        },
        onAuthError = { msg -> upgradeError = msg },
        scope = scope,
      )
    },
    onSignOut = viewModel::exitGuestMode,
    onDeleteAccount = viewModel::deleteLocalData,
  )
}

@Composable
private fun PushUpMainScaffold(
  currentUserId: String,
  isGuestMode: Boolean,
  profileState: UiState<UserProfile>,
  workoutsState: UiState<List<WorkoutSession>>,
  leaderboardState: UiState<List<LeaderboardEntry>>,
  arenaDuelsState: UiState<List<ArenaDuel>>,
  medicalAccepted: Boolean?,
  cameraAccepted: Boolean,
  soundEnabled: Boolean,
  actionMessage: String?,
  isDeletingAccount: Boolean,
  onClearActionMessage: () -> Unit,
  onAcceptMedical: () -> Unit,
  onAcceptCamera: () -> Unit,
  onToggleSound: () -> Unit,
  onRecordWorkout: (
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
    arenaBonusCoins: Long,
    isArenaWin: Boolean,
    isArenaMode: Boolean,
    onSaved: () -> Unit,
  ) -> Unit,
  onSubmitDuelWorkout: (
    duelId: String,
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
    onSaved: () -> Unit,
  ) -> Unit,
  onCreateDuel: (targetReps: Long, mode: ArenaMode) -> Unit,
  onJoinDuel: (duelId: String) -> Unit,
  onCancelDuel: (duelId: String) -> Unit,
  onPurchaseOrEquip: (ShopItem) -> Unit,
  onEquipBoost: (String) -> Unit,
  onUpdateProfile: (username: String, avatarPreset: String) -> Unit,
  onToggleFriend: (String) -> Unit,
  onUpgradeToGoogle: () -> Unit,
  onSignOut: () -> Unit,
  onDeleteAccount: () -> Unit,
) {
  val activeThemeId =
    (profileState as? UiState.Success)?.data?.selectedThemeId ?: ShopCatalog.DEFAULT_THEME_ID

  PushUpRankTheme(selectedThemeId = activeThemeId) {
    if (medicalAccepted == false) {
      MedicalDisclaimerScreen(onAccepted = onAcceptMedical)
      return@PushUpRankTheme
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isInWorkout = currentDestination?.hasRoute<WorkoutRoute>() == true
    var showCameraConsentDialog by remember { mutableStateOf(false) }
    var pendingWorkoutContext by remember {
      mutableStateOf<PendingWorkoutContext>(PendingWorkoutContext.Standard)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
      if (actionMessage != null) {
        snackbarHostState.showSnackbar(actionMessage)
        onClearActionMessage()
      }
    }

    if (showCameraConsentDialog) {
      CameraPrivacyConsentDialog(
        onConfirm = {
          showCameraConsentDialog = false
          onAcceptCamera()
          navController.navigate(WorkoutRoute)
        },
        onDismiss = { showCameraConsentDialog = false },
      )
    }

    val launchWorkoutWithContext: (PendingWorkoutContext) -> Unit = { ctx ->
      pendingWorkoutContext = ctx
      if (cameraAccepted) {
        navController.navigate(WorkoutRoute)
      } else {
        showCameraConsentDialog = true
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp && !isInWorkout

      Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail) {
          NavigationRail {
            topLevelTabs.forEach { tab ->
              val selected = currentDestination?.hasRoute(tab.route::class) == true
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          contentWindowInsets =
            if (isInWorkout) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
          bottomBar = {
            if (!isInWorkout && !useNavigationRail) {
              NavigationBar(windowInsets = WindowInsets.navigationBars) {
                topLevelTabs.forEach { tab ->
                  val selected = currentDestination?.hasRoute(tab.route::class) == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                      )
                    },
                    label = { Text(tab.label, maxLines = 1) },
                    modifier = Modifier.testTag(tab.testTag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          ) {
            composable<DashboardRoute> {
              DashboardScreen(
                profileState = profileState,
                workoutsState = workoutsState,
                onStartWorkoutClick = { launchWorkoutWithContext(PendingWorkoutContext.Standard) },
              )
            }
            composable<ArenaRoute> {
              ArenaScreen(
                currentUserId = currentUserId,
                isGuestMode = isGuestMode,
                profileState = profileState,
                duelsState = arenaDuelsState,
                onStartSoloChallenge = { challenge ->
                  launchWorkoutWithContext(PendingWorkoutContext.SoloChallenge(challenge))
                },
                onCreateDuel = onCreateDuel,
                onJoinDuel = onJoinDuel,
                onCancelDuel = onCancelDuel,
                onStartDuelWorkout = { duel ->
                  launchWorkoutWithContext(PendingWorkoutContext.DuelTurn(duel))
                },
                onUpgradeToGoogle = onUpgradeToGoogle,
              )
            }
            composable<LeaderboardRoute> {
              LeaderboardScreen(
                currentUserId = currentUserId,
                profileState = profileState,
                leaderboardState = leaderboardState,
                onToggleFriend = onToggleFriend,
                isGuestMode = isGuestMode,
                onUpgradeToGoogle = onUpgradeToGoogle,
              )
            }
            composable<ShopRoute> {
              ShopScreen(
                profileState = profileState,
                onPurchaseOrEquip = onPurchaseOrEquip,
                onEquipBoost = onEquipBoost,
              )
            }
            composable<ProfileRoute> {
              ProfileSettingsScreen(
                profileState = profileState,
                soundEnabled = soundEnabled,
                isDeletingAccount = isDeletingAccount,
                onToggleSound = onToggleSound,
                onSaveProfile = onUpdateProfile,
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount,
                isGuestMode = isGuestMode,
                onUpgradeToGoogle = onUpgradeToGoogle,
              )
            }
            composable<WorkoutRoute> {
              val currentProfile = (profileState as? UiState.Success)?.data
              val skeletonEffect =
                currentProfile?.selectedSkeletonEffectId ?: ShopCatalog.DEFAULT_SKELETON_ID
              val activeBoostId =
                currentProfile?.activeBoostId
                  ?.takeIf { it.isNotBlank() && it in currentProfile.ownedBoosts }
                  ?: ""

              val activeContext = pendingWorkoutContext
              val challengeTitle =
                when (activeContext) {
                  is PendingWorkoutContext.Standard -> null
                  is PendingWorkoutContext.SoloChallenge -> activeContext.challenge.title
                  is PendingWorkoutContext.DuelTurn -> "Duello 1v1"
                }
              val targetReps =
                when (activeContext) {
                  is PendingWorkoutContext.Standard -> null
                  is PendingWorkoutContext.SoloChallenge -> activeContext.challenge.targetReps
                  is PendingWorkoutContext.DuelTurn -> activeContext.duel.targetReps
                }
              val timeLimitSeconds =
                when (activeContext) {
                  is PendingWorkoutContext.Standard -> null
                  is PendingWorkoutContext.SoloChallenge -> activeContext.challenge.timeLimitSeconds
                  is PendingWorkoutContext.DuelTurn ->
                    if (activeContext.duel.mode == ArenaMode.SPRINT_60S.firestoreKey) 60L else null
                }

              WorkoutCameraScreen(
                selectedSkeletonEffectId = skeletonEffect,
                activeBoostId = activeBoostId,
                challengeTitle = challengeTitle,
                targetReps = targetReps,
                timeLimitSeconds = timeLimitSeconds,
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound,
                onSaveSession = { reps, duration, formScore ->
                  when (activeContext) {
                    is PendingWorkoutContext.Standard -> {
                      onRecordWorkout(reps, duration, formScore, 0L, false, false) {
                        navController.popBackStack()
                      }
                    }
                    is PendingWorkoutContext.SoloChallenge -> {
                      val challenge = activeContext.challenge
                      val effectiveScore =
                        if (currentProfile != null) {
                          BoostCalculator.computeAndConsume(
                            profile = currentProfile,
                            reps = reps,
                            rawFormScore = formScore,
                            isArenaMode = true,
                          ).first.effectiveFormScore
                        } else {
                          formScore
                        }
                      val metTime =
                        challenge.timeLimitSeconds == null || duration <= (challenge.timeLimitSeconds + 2L)
                      val wonChallenge =
                        reps >= challenge.targetReps && effectiveScore >= challenge.minFormScore && metTime
                      onRecordWorkout(
                        reps,
                        duration,
                        formScore,
                        if (wonChallenge) challenge.bonusCoins else 0L,
                        wonChallenge,
                        true,
                      ) {
                        navController.popBackStack()
                      }
                    }
                    is PendingWorkoutContext.DuelTurn -> {
                      onSubmitDuelWorkout(activeContext.duel.id, reps, duration, formScore) {
                        navController.popBackStack()
                      }
                    }
                  }
                },
                onCancel = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }
  }
}
