package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DayKeyUtils
import com.example.domain.model.RankTier
import com.example.domain.model.StreakCalculator
import com.example.domain.model.UserProfile
import com.example.ui.components.AthleteAvatar
import com.example.ui.components.AvatarPreset
import com.example.ui.components.RankBadge
import com.example.ui.theme.CoinAmber
import com.example.ui.theme.PostureGreen
import com.example.ui.viewmodel.UiState

@Composable
fun ProfileSettingsScreen(
  profileState: UiState<UserProfile>,
  soundEnabled: Boolean,
  isDeletingAccount: Boolean,
  onToggleSound: () -> Unit,
  onSaveProfile: (username: String, avatarPreset: String) -> Unit,
  onSignOut: () -> Unit,
  onDeleteAccount: () -> Unit,
  isGuestMode: Boolean = false,
  onUpgradeToGoogle: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { if (!isDeletingAccount) showDeleteDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.DeleteForever,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(32.dp),
        )
      },
      title = {
        Text(
          if (isGuestMode) "Eliminare tutti i dati locali?"
          else "Eliminare definitivamente l'account?"
        )
      },
      text = {
        Text(
          if (isGuestMode) {
            "Questa azione è irreversibile. Verranno cancellati il tuo profilo ospite, i PushCoin e lo storico locale delle flessioni."
          } else {
            "Questa azione è irreversibile. Verranno cancellati il tuo account Firebase, il profilo, lo storico degli allenamenti, i PushCoin e la tua voce in classifica."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDeleteAccount()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_account_button"),
        ) {
          Text("Elimina Tutto")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Annulla")
        }
      },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    when (profileState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(profileState.message, color = MaterialTheme.colorScheme.error)
        }
      }
      is UiState.Success -> {
        val profile = profileState.data
        val rank = RankTier.fromTotalPushUps(profile.totalPushUps)
        var usernameInput by remember(profile.username) { mutableStateOf(profile.username) }
        var selectedAvatar by remember(profile.avatarPreset) { mutableStateOf(profile.avatarPreset) }

        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 640.dp)
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = "Profilo & Impostazioni",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )

          // Card Profilo & Modifica Avatar/Nickname
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                AthleteAvatar(
                  avatarPreset = selectedAvatar,
                  frameId = profile.selectedFrameId,
                  size = 64.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = profile.email.ifBlank { "Account Google collegato" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                RankBadge(rankTier = rank, size = 44.dp)
              }

              val todayDate = DayKeyUtils.dateKey(0)
              val activeStreak =
                StreakCalculator.effectiveDisplayStreak(
                  lastWorkoutDate = profile.lastWorkoutDate,
                  storedStreak = profile.currentStreak,
                  todayDate = todayDate,
                )
              val bestStreak = maxOf(profile.longestStreak, activeStreak)
              val trainedToday = profile.lastWorkoutDate == todayDate

              // Card Serie Giornaliera (Daily Streak)
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border =
                  BorderStroke(
                    1.dp,
                    if (activeStreak > 0L) CoinAmber.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier = Modifier.fillMaxWidth().testTag("profile_streak_card"),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color =
                          if (activeStreak > 0L) CoinAmber.copy(alpha = 0.18f)
                          else MaterialTheme.colorScheme.surfaceContainer,
                      ) {
                        Icon(
                          imageVector = Icons.Filled.LocalFireDepartment,
                          contentDescription = "Serie giornaliera",
                          tint =
                            if (activeStreak > 0L) CoinAmber
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.padding(8.dp).size(24.dp),
                        )
                      }
                      Column {
                        Text(
                          text =
                            if (activeStreak == 1L) "1 giorno consecutivo"
                            else "$activeStreak giorni consecutivi",
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.testTag("profile_streak_count"),
                        )
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                          )
                          Text(
                            text = "Record migliore: $bestStreak gg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color =
                        if (trainedToday) PostureGreen.copy(alpha = 0.16f)
                        else CoinAmber.copy(alpha = 0.16f),
                    ) {
                      Text(
                        text = if (trainedToday) "Oggi attivo" else "Da completare",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (trainedToday) PostureGreen else CoinAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                      )
                    }
                  }

                  Text(
                    text =
                      when {
                        trainedToday -> "Serie giornaliera al sicuro! Torna domani per continuarla."
                        activeStreak > 0L -> "Completa almeno una sessione oggi per non perdere la tua serie!"
                        else -> "Completa una sessione oggi per avviare la tua serie giornaliera!"
                      },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              OutlinedTextField(
                value = usernameInput,
                onValueChange = { if (it.length <= 24) usernameInput = it },
                label = { Text("Nickname Atleta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("username_input"),
              )

              Text(
                text = "Scegli Avatar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AvatarPreset.entries) { preset ->
                  val isSelected = selectedAvatar == preset.id
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color =
                      if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border =
                      if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                      else null,
                    modifier =
                      Modifier.clickable { selectedAvatar = preset.id }
                        .testTag("avatar_preset_${preset.id}"),
                  ) {
                    Column(
                      modifier = Modifier.padding(10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.label,
                        tint =
                          if (isSelected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                      )
                      Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }

              Button(
                onClick = { onSaveProfile(usernameInput, selectedAvatar) },
                enabled = usernameInput.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_profile_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Salva Profilo", fontWeight = FontWeight.Bold)
              }
            }
          }

          // Preferenze Allenamento & Compliance Google Play
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Column {
                    Text("Feedback Sonoro AI", style = MaterialTheme.typography.titleMedium)
                    Text(
                      "Segnale acustico ad ogni flessione valida",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Switch(
                  checked = soundEnabled,
                  onCheckedChange = { onToggleSound() },
                  modifier = Modifier.testTag("settings_sound_switch"),
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                  text = "Fotocamera 100% locale in RAM: nessuna registrazione video salvata o caricata.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                  text = "Disclaimer medico accettato: attività fisica svolta sotto la propria responsabilità.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Azioni Account (Google Upgrade per Ospiti, Logout + Eliminazione Definitiva Google Play)
          if (isGuestMode) {
            Button(
              onClick = onUpgradeToGoogle,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .testTag("guest_profile_google_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Accedi con Google (Classifica Cloud)", fontWeight = FontWeight.Bold)
            }
          }

          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("sign_out_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (isGuestMode) "Esci da Modalità Ospite" else "Disconnetti Account")
          }

          Button(
            onClick = { showDeleteDialog = true },
            enabled = !isDeletingAccount,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.error,
              ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("delete_account_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            if (isDeletingAccount) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Filled.DeleteForever, contentDescription = null)
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                if (isGuestMode) "Elimina Dati Locali Ospite"
                else "Elimina Account e Tutti i Dati",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}
