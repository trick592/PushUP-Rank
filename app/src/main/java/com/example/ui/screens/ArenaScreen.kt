package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ArenaCatalog
import com.example.domain.model.ArenaChallenge
import com.example.domain.model.ArenaDuel
import com.example.domain.model.ArenaMode
import com.example.domain.model.ShopCatalog
import com.example.domain.model.UserProfile
import com.example.ui.components.PushCoinChip
import com.example.ui.theme.CoinAmber
import com.example.ui.theme.PostureGreen
import com.example.ui.viewmodel.UiState

private enum class ArenaTab(val label: String) {
  SOLO("Sfide & Boss"),
  DUELS("Duelli 1v1"),
}

@Composable
fun ArenaScreen(
  currentUserId: String,
  isGuestMode: Boolean,
  profileState: UiState<UserProfile>,
  duelsState: UiState<List<ArenaDuel>>,
  onStartSoloChallenge: (ArenaChallenge) -> Unit,
  onCreateDuel: (targetReps: Long, mode: ArenaMode) -> Unit,
  onJoinDuel: (duelId: String) -> Unit,
  onCancelDuel: (duelId: String) -> Unit,
  onStartDuelWorkout: (ArenaDuel) -> Unit,
  onUpgradeToGoogle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableStateOf(ArenaTab.SOLO) }
  var showCreateDuelDialog by remember { mutableStateOf(false) }

  if (showCreateDuelDialog) {
    CreateDuelDialog(
      onDismiss = { showCreateDuelDialog = false },
      onConfirm = { targetReps, mode ->
        showCreateDuelDialog = false
        onCreateDuel(targetReps, mode)
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
        val activeBoost =
          profile.activeBoostId
            .takeIf { it.isNotBlank() && it in profile.ownedBoosts }
            ?.let { ShopCatalog.findById(it) }

        Column(
          modifier = Modifier.fillMaxSize().widthIn(max = 640.dp).padding(horizontal = 16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Arena Competitiva",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
            PushCoinChip(coins = profile.pushCoins)
          }

          // Banner Vittorie Arena & Potenziamento attivo
          Card(
            modifier = Modifier.fillMaxWidth().testTag("arena_summary_card"),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = CoinAmber.copy(alpha = 0.16f),
                ) {
                  Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Vittorie Arena",
                    tint = CoinAmber,
                    modifier = Modifier.padding(8.dp).size(22.dp),
                  )
                }
                Column {
                  Text(
                    text = "${profile.arenaWins} Vittorie Arena",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (activeBoost != null) "Boost attivo: ${activeBoost.name}"
                      else "Nessun boost attivo (visita il Negozio)",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                      if (activeBoost != null) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ArenaTab.entries.forEach { tab ->
              FilterChip(
                selected = selectedTab == tab,
                onClick = { selectedTab = tab },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("arena_tab_${tab.name.lowercase()}"),
              )
            }
          }

          when (selectedTab) {
            ArenaTab.SOLO -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(ArenaCatalog.soloChallenges, key = { it.id }) { challenge ->
                  SoloChallengeCard(
                    challenge = challenge,
                    onStart = { onStartSoloChallenge(challenge) },
                  )
                }
              }
            }

            ArenaTab.DUELS -> {
              if (isGuestMode) {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(20.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Text(
                      text = "Duelli 1v1 Live su Cloud",
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Le sfide Solo e Boss sono già disponibili in modalità ospite. Accedi con Google per sfidare altri atleti in tempo reale nei Duelli 1v1.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                      onClick = onUpgradeToGoogle,
                      modifier =
                        Modifier.fillMaxWidth()
                          .heightIn(min = 50.dp)
                          .testTag("arena_guest_google_button"),
                      shape = RoundedCornerShape(14.dp),
                    ) {
                      Text("Accedi con Google per Duelli 1v1", fontWeight = FontWeight.Bold)
                    }
                  }
                }
              } else {
                Column(modifier = Modifier.fillMaxSize()) {
                  Button(
                    onClick = { showCreateDuelDialog = true },
                    modifier =
                      Modifier.fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(bottom = 12.dp)
                        .testTag("arena_create_duel_button"),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Crea Nuovo Duello 1v1 (+20 PC al vincitore)", fontWeight = FontWeight.Bold)
                  }

                  when (duelsState) {
                    is UiState.Loading -> {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                      }
                    }
                    is UiState.Error -> {
                      Text(
                        text = duelsState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                      )
                    }
                    is UiState.Success -> {
                      val duels = duelsState.data
                      if (duels.isEmpty()) {
                        Card(
                          modifier = Modifier.fillMaxWidth(),
                          shape = RoundedCornerShape(18.dp),
                          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                          Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Shield,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(32.dp),
                            )
                            Text(
                              text = "Nessun duello attivo",
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Bold,
                            )
                            Text(
                              text = "Apri una nuova sfida 1v1: altri atleti potranno accettarla e confrontare le proprie flessioni AI.",
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                          }
                        }
                      } else {
                        LazyColumn(
                          modifier = Modifier.fillMaxSize(),
                          contentPadding = PaddingValues(bottom = 24.dp),
                          verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                          items(duels, key = { it.id }) { duel ->
                            ArenaDuelCard(
                              duel = duel,
                              currentUserId = currentUserId,
                              onJoin = { onJoinDuel(duel.id) },
                              onCancel = { onCancelDuel(duel.id) },
                              onPlayTurn = { onStartDuelWorkout(duel) },
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SoloChallengeCard(
  challenge: ArenaChallenge,
  onStart: () -> Unit,
) {
  val badgeColor =
    when (challenge.difficultyLabel) {
      "BOSS" -> Color(0xFFFF4D6D)
      "TECNICA" -> Color(0xFF00E5FF)
      else -> CoinAmber
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = badgeColor.copy(alpha = 0.16f),
        ) {
          Text(
            text = challenge.difficultyLabel,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        PushCoinChip(coins = challenge.bonusCoins)
      }

      Text(
        text = challenge.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = challenge.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          Text("${challenge.targetReps} rep", style = MaterialTheme.typography.labelLarge)
        }
        if (challenge.timeLimitSeconds != null) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Text("${challenge.timeLimitSeconds}s", style = MaterialTheme.typography.labelLarge)
          }
        }
        Text(
          text = "Forma ≥ ${challenge.minFormScore}%",
          style = MaterialTheme.typography.labelLarge,
          color = PostureGreen,
        )
      }

      Button(
        onClick = onStart,
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("arena_challenge_${challenge.id}"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text("Sfida Ora (+${challenge.bonusCoins} PC Bonus)", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun ArenaDuelCard(
  duel: ArenaDuel,
  currentUserId: String,
  onJoin: () -> Unit,
  onCancel: () -> Unit,
  onPlayTurn: () -> Unit,
) {
  val isCreator = duel.creatorId == currentUserId
  val isOpponent = duel.opponentId == currentUserId
  val isParticipant = isCreator || isOpponent
  val userNeedsToPlay =
    duel.status != "FINISHED" &&
      ((isCreator && !duel.creatorFinished) || (isOpponent && !duel.opponentFinished))
  val modeLabel =
    if (duel.mode == ArenaMode.SPRINT_60S.firestoreKey) "Sprint 60s"
    else "Obiettivo ${duel.targetReps} rep"

  val statusColor =
    when (duel.status) {
      "OPEN" -> CoinAmber
      "ACTIVE" -> MaterialTheme.colorScheme.primary
      else -> PostureGreen
    }
  val statusLabel =
    when (duel.status) {
      "OPEN" -> "APERTO"
      "ACTIVE" -> "IN CORSO"
      else -> "COMPLETATO"
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("arena_duel_${duel.id}"),
    shape = RoundedCornerShape(20.dp),
    border =
      if (isParticipant && duel.status != "FINISHED") BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
      else null,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = modeLabel,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = statusColor.copy(alpha = 0.16f),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = duel.creatorName + if (isCreator) " (Tu)" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text =
              if (duel.creatorFinished) "${duel.creatorReps} rep • Forma ${duel.creatorFormScore}%"
              else "Turno da giocare",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
          Text(
            text =
              if (duel.opponentId.isEmpty()) "In attesa sfidante"
              else duel.opponentName + if (isOpponent) " (Tu)" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text =
              when {
                duel.opponentId.isEmpty() -> "Posto libero"
                duel.opponentFinished -> "${duel.opponentReps} rep • Forma ${duel.opponentFormScore}%"
                else -> "Turno da giocare"
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (duel.status == "FINISHED") {
        val resultBanner =
          when {
            duel.winnerId == currentUserId -> "Hai vinto il duello! (+20 PC bonus)"
            duel.winnerId.isEmpty() -> "Pareggio perfetto! (+10 PC bonus)"
            duel.winnerId == duel.creatorId -> "Vincitore: ${duel.creatorName}"
            else -> "Vincitore: ${duel.opponentName}"
          }
        Text(
          text = resultBanner,
          style = MaterialTheme.typography.labelLarge,
          color = if (duel.winnerId == currentUserId) PostureGreen else CoinAmber,
          fontWeight = FontWeight.Bold,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (duel.status == "OPEN" && !isCreator) {
            Button(
              onClick = onJoin,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("duel_join_${duel.id}"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text("Accetta Sfida 1v1", fontWeight = FontWeight.Bold)
            }
          }
          if (userNeedsToPlay) {
            Button(
              onClick = onPlayTurn,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("duel_play_${duel.id}"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.size(4.dp))
              Text("Gioca il tuo Turno", fontWeight = FontWeight.Bold)
            }
          }
          if (duel.status == "OPEN" && isCreator) {
            OutlinedButton(
              onClick = onCancel,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.heightIn(min = 48.dp).testTag("duel_cancel_${duel.id}"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "Annulla duello", modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CreateDuelDialog(
  onDismiss: () -> Unit,
  onConfirm: (targetReps: Long, mode: ArenaMode) -> Unit,
) {
  var selectedMode by remember { mutableStateOf(ArenaMode.TARGET) }
  var selectedTarget by remember { mutableLongStateOf(25L) }
  val repOptions = listOf(15L, 25L, 40L)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Nuovo Duello Arena 1v1") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Modalità sfida", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ArenaMode.entries.forEach { mode ->
            FilterChip(
              selected = selectedMode == mode,
              onClick = { selectedMode = mode },
              label = { Text(mode.label) },
            )
          }
        }
        Text("Obiettivo flessioni", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          repOptions.forEach { reps ->
            FilterChip(
              selected = selectedTarget == reps,
              onClick = { selectedTarget = reps },
              label = { Text("$reps rep") },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedTarget, selectedMode) },
        modifier = Modifier.testTag("confirm_create_duel_button"),
      ) {
        Text("Apri Duello")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Annulla")
      }
    },
  )
}
