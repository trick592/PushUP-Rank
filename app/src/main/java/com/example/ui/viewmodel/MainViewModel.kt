package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CompliancePreferences
import com.example.data.repository.PushUpRepository
import com.example.domain.model.ArenaDuel
import com.example.domain.model.ArenaMode
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.ShopCategory
import com.example.domain.model.ShopItem
import com.example.domain.model.UserProfile
import com.example.domain.model.WorkoutSession
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
  private val repository: PushUpRepository,
  private val compliancePreferences: CompliancePreferences,
  private val currentUserId: String,
) : ViewModel() {

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isDeletingAccount = MutableStateFlow(false)
  val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

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
    repository.observeUserProfile()
      .map<UserProfile?, UiState<UserProfile>> { profile ->
        if (profile != null) {
          UiState.Success(profile)
        } else {
          UiState.Loading
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Errore osservazione profilo", rootError)
        emit(UiState.Error(rootError.message ?: "Impossibile caricare il profilo."))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val recentWorkoutsState: StateFlow<UiState<List<WorkoutSession>>> =
    repository.observeRecentWorkouts()
      .map<List<WorkoutSession>, UiState<List<WorkoutSession>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Errore osservazione allenamenti", rootError)
        emit(UiState.Error(rootError.message ?: "Impossibile caricare lo storico."))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
    repository.observeLeaderboard()
      .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Errore osservazione classifica", rootError)
        emit(UiState.Error(rootError.message ?: "Impossibile caricare la classifica."))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val arenaDuelsState: StateFlow<UiState<List<ArenaDuel>>> =
    repository.observeArenaDuels()
      .map<List<ArenaDuel>, UiState<List<ArenaDuel>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Errore osservazione duelli arena", rootError)
        emit(UiState.Error(rootError.message ?: "Impossibile caricare i duelli Arena."))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  init {
    viewModelScope.launch {
      val user = Firebase.auth.currentUser
      val medical = compliancePreferences.medicalDisclaimerAccepted.first()
      val camera = compliancePreferences.cameraConsentAccepted.first()
      repository.ensureProfileInitialized(
        displayName = user?.displayName,
        email = user?.email,
        medicalAccepted = medical,
        cameraAccepted = camera,
      ).onFailure { error ->
        Log.e(TAG, "Inizializzazione profilo fallita", error)
        _actionMessage.value = error.localizedMessage ?: "Errore di sincronizzazione iniziale."
      }
    }
  }

  fun acceptMedicalDisclaimer() {
    viewModelScope.launch {
      compliancePreferences.setMedicalDisclaimerAccepted(true)
      val cameraAccepted = compliancePreferences.cameraConsentAccepted.first()
      repository.syncComplianceFlags(medicalAccepted = true, cameraAccepted = cameraAccepted)
    }
  }

  fun acceptCameraConsent() {
    viewModelScope.launch {
      compliancePreferences.setCameraConsentAccepted(true)
      val medicalAccepted = compliancePreferences.medicalDisclaimerAccepted.first()
      repository.syncComplianceFlags(medicalAccepted = medicalAccepted, cameraAccepted = true)
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
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.recordWorkoutSession(
        currentProfile = profile,
        reps = reps,
        durationSeconds = durationSeconds,
        averageFormScore = averageFormScore,
        arenaBonusCoins = arenaBonusCoins,
        isArenaWin = isArenaWin,
        isArenaMode = isArenaMode,
      )
        .onSuccess { summary ->
          val boostNote = summary.consumedBoostName?.let { " ($it usato)" } ?: ""
          _actionMessage.value = "Sessione salvata! +${summary.earnedCoins} PushCoin$boostNote."
          onSaved()
        }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Errore nel salvataggio della sessione."
        }
    }
  }

  fun submitDuelWorkout(
    duelId: String,
    reps: Long,
    durationSeconds: Long,
    averageFormScore: Long,
    onSaved: () -> Unit = {},
  ) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.submitArenaDuelScore(
        duelId = duelId,
        currentProfile = profile,
        reps = reps,
        durationSeconds = durationSeconds,
        averageFormScore = averageFormScore,
      )
        .onSuccess { summary ->
          _actionMessage.value = "Risultato Duello inviato! +${summary.earnedCoins} PushCoin accreditati."
          onSaved()
        }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Errore durante l'invio del risultato."
        }
    }
  }

  fun createArenaDuel(targetReps: Long, mode: ArenaMode) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.createArenaDuel(profile, targetReps, mode)
        .onSuccess { _actionMessage.value = "Duello Arena 1v1 aperto! Attendi uno sfidante o gioca il tuo turno." }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Impossibile creare il duello."
        }
    }
  }

  fun joinArenaDuel(duelId: String) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.joinArenaDuel(duelId, profile)
        .onSuccess { _actionMessage.value = "Sfida accettata! Tocca a te completare il turno in Arena." }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Impossibile partecipare al duello."
        }
    }
  }

  fun cancelArenaDuel(duelId: String) {
    viewModelScope.launch {
      repository.cancelOpenDuel(duelId)
        .onSuccess { _actionMessage.value = "Duello aperto annullato." }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Impossibile annullare il duello."
        }
    }
  }

  fun purchaseOrEquip(item: ShopItem) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    val alreadyUnlocked = item.id in profile.unlockedItems
    viewModelScope.launch {
      repository.purchaseOrEquipItem(profile, item)
        .onSuccess {
          _actionMessage.value =
            when {
              item.category == ShopCategory.BOOST -> "${item.name} acquistato e attivato per la prossima sessione!"
              alreadyUnlocked -> "${item.name} equipaggiato!"
              else -> "${item.name} sbloccato ed equipaggiato!"
            }
        }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Operazione negozio fallita."
        }
    }
  }

  fun equipBoost(boostId: String) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.equipOwnedBoost(profile, boostId)
        .onSuccess {
          _actionMessage.value =
            if (boostId.isBlank()) "Potenziamento disattivato."
            else "Potenziamento attivato per la prossima sessione!"
        }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Impossibile attivare il potenziamento."
        }
    }
  }

  fun updateProfile(username: String, avatarPreset: String) {
    viewModelScope.launch {
      repository.updateProfileDisplay(username, avatarPreset)
        .onSuccess { _actionMessage.value = "Profilo aggiornato." }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Impossibile aggiornare il profilo."
        }
    }
  }

  fun toggleFriend(targetUid: String) {
    val profile = (userProfileState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      val wasFriend = targetUid in profile.friendUids
      repository.toggleFriend(profile, targetUid)
        .onSuccess {
          _actionMessage.value = if (wasFriend) "Rimosso dagli amici." else "Aggiunto agli amici!"
        }
        .onFailure { error ->
          _actionMessage.value = error.localizedMessage ?: "Errore gestione amici."
        }
    }
  }

  fun deleteAccountAndData(onComplete: () -> Unit, onRequiresRecentLogin: () -> Unit) {
    viewModelScope.launch {
      _isDeletingAccount.value = true
      repository.deleteAccountAndAllData()
        .onSuccess {
          compliancePreferences.clearAll()
          _isDeletingAccount.value = false
          onComplete()
        }
        .onFailure { error ->
          _isDeletingAccount.value = false
          val msg = error.localizedMessage ?: "Errore durante l'eliminazione dell'account."
          if (msg.contains("recent", ignoreCase = true)) {
            onRequiresRecentLogin()
          } else {
            _actionMessage.value = msg
          }
        }
    }
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  private companion object {
    const val TAG = "MainViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
