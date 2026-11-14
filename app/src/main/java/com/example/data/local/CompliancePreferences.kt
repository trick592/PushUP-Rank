package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.complianceDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "pushup_rank_compliance")

class CompliancePreferences(private val context: Context) {
  private object Keys {
    val MEDICAL_DISCLAIMER_ACCEPTED = booleanPreferencesKey("medical_disclaimer_accepted")
    val CAMERA_CONSENT_ACCEPTED = booleanPreferencesKey("camera_consent_accepted")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val GUEST_MODE_ACTIVE = booleanPreferencesKey("guest_mode_active")
    val GUEST_PROFILE_JSON = stringPreferencesKey("guest_profile_json")
    val GUEST_WORKOUTS_JSON = stringPreferencesKey("guest_workouts_json")
  }

  val medicalDisclaimerAccepted: Flow<Boolean> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.MEDICAL_DISCLAIMER_ACCEPTED] ?: false }

  val cameraConsentAccepted: Flow<Boolean> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.CAMERA_CONSENT_ACCEPTED] ?: false }

  val soundEnabled: Flow<Boolean> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.SOUND_ENABLED] ?: true }

  val guestModeActive: Flow<Boolean> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.GUEST_MODE_ACTIVE] ?: false }

  val guestProfileJson: Flow<String?> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.GUEST_PROFILE_JSON] }

  val guestWorkoutsJson: Flow<String?> =
    context.complianceDataStore.data.map { prefs -> prefs[Keys.GUEST_WORKOUTS_JSON] }

  suspend fun setMedicalDisclaimerAccepted(accepted: Boolean) {
    context.complianceDataStore.edit { prefs -> prefs[Keys.MEDICAL_DISCLAIMER_ACCEPTED] = accepted }
  }

  suspend fun setCameraConsentAccepted(accepted: Boolean) {
    context.complianceDataStore.edit { prefs -> prefs[Keys.CAMERA_CONSENT_ACCEPTED] = accepted }
  }

  suspend fun setSoundEnabled(enabled: Boolean) {
    context.complianceDataStore.edit { prefs -> prefs[Keys.SOUND_ENABLED] = enabled }
  }

  suspend fun setGuestModeActive(active: Boolean) {
    context.complianceDataStore.edit { prefs -> prefs[Keys.GUEST_MODE_ACTIVE] = active }
  }

  suspend fun saveGuestState(profileJson: String, workoutsJson: String) {
    context.complianceDataStore.edit { prefs ->
      prefs[Keys.GUEST_PROFILE_JSON] = profileJson
      prefs[Keys.GUEST_WORKOUTS_JSON] = workoutsJson
    }
  }

  suspend fun clearGuestData() {
    context.complianceDataStore.edit { prefs ->
      prefs.remove(Keys.GUEST_MODE_ACTIVE)
      prefs.remove(Keys.GUEST_PROFILE_JSON)
      prefs.remove(Keys.GUEST_WORKOUTS_JSON)
    }
  }

  suspend fun clearAll() {
    context.complianceDataStore.edit { it.clear() }
  }
}
