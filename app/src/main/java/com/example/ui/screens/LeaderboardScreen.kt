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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.RankTier
import com.example.domain.model.ShopCatalog
import com.example.domain.model.UserProfile
import com.example.domain.model.WeekKeyUtils
import com.example.ui.components.AthleteAvatar
import com.example.ui.components.RankBadge
import com.example.ui.theme.CoinAmber
import com.example.ui.viewmodel.UiState

@Composable
fun LeaderboardScreen(
  currentUserId: String,
  profileState: UiState<UserProfile>,
  leaderboardState: UiState<List<LeaderboardEntry>>,
  onToggleFriend: (String) -> Unit,
  isGuestMode: Boolean = false,
  onUpgradeToGoogle: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (isGuestMode) {
    Box(
      modifier = modifier.fillMaxSize().padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = CoinAmber,
            modifier = Modifier.size(48.dp),
          )
          Text(
            text = "Classifica Cloud Riservata",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "In modalità Ospite i tuoi allenamenti e PushCoin sono salvati in locale. Accedi con Google per competere nella classifica globale e tra amici.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = onUpgradeToGoogle,
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("guest_upgrade_google_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Accedi con Google", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    return
  }
  var showFriendsOnly by remember { mutableStateOf(false) }
  var showWeekly by remember { mutableStateOf(true) }

  val currentProfile = (profileState as? UiState.Success)?.data
  val friendUids = currentProfile?.friendUids.orEmpty()
  val currentWeek = remember { WeekKeyUtils.currentWeekKey() }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier.fillMaxSize().widthIn(max = 640.dp).padding(horizontal = 16.dp),
    ) {
      Column(
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Classifica",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Anti-Cheat Attivo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        // Filtri Ambito (Globale / Amici) e Periodo (Settimanale / Di sempre)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = !showFriendsOnly,
            onClick = { showFriendsOnly = false },
            label = { Text("Globale") },
            leadingIcon = {
              Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("filter_global"),
          )
          FilterChip(
            selected = showFriendsOnly,
            onClick = { showFriendsOnly = true },
            label = { Text("Amici (${friendUids.size})") },
            leadingIcon = {
              Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("filter_friends"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = showWeekly,
            onClick = { showWeekly = true },
            label = { Text("Settimanale") },
            modifier = Modifier.testTag("filter_weekly"),
          )
          FilterChip(
            selected = !showWeekly,
            onClick = { showWeekly = false },
            label = { Text("Di sempre") },
            modifier = Modifier.testTag("filter_all_time"),
          )
        }
      }

      when (leaderboardState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
              text = leaderboardState.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }
        is UiState.Success -> {
          val filtered =
            leaderboardState.data
              .filter { entry ->
                !showFriendsOnly || entry.userId == currentUserId || entry.userId in friendUids
              }
              .sortedByDescending { entry ->
                if (showWeekly) {
                  if (entry.weekKey == currentWeek) entry.weeklyPushUps else 0L
                } else {
                  entry.totalPushUps
                }
              }

          if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.EmojiEvents,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(40.dp),
                )
                Text(
                  text =
                    if (showFriendsOnly) "Nessun amico ancora aggiunto"
                    else "Nessun atleta in classifica",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text =
                    if (showFriendsOnly) "Passa a Globale e tocca l'icona + accanto a un atleta per sfidarlo!"
                    else "Completa la tua prima sessione per entrare in classifica.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              itemsIndexed(filtered, key = { _, entry -> entry.userId }) { index, entry ->
                val isMe = entry.userId == currentUserId
                val isFriend = entry.userId in friendUids
                val score =
                  if (showWeekly) {
                    if (entry.weekKey == currentWeek) entry.weeklyPushUps else 0L
                  } else {
                    entry.totalPushUps
                  }
                val rank = RankTier.fromKey(entry.rankTier)
                val titleText =
                  ShopCatalog.findById(entry.selectedTitleId)?.titleText ?: "Sfida Principiante"

                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(18.dp),
                  border =
                    if (isMe) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null,
                  colors =
                    CardDefaults.cardColors(
                      containerColor =
                        if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Text(
                      text = "#${index + 1}",
                      style = MaterialTheme.typography.titleLarge,
                      color =
                        when (index) {
                          0 -> CoinAmber
                          1 -> ColorSilver
                          2 -> RankTier.BRONZE.badgeColor
                          else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                      fontWeight = FontWeight.Bold,
                    )
                    AthleteAvatar(
                      avatarPreset = entry.avatarPreset,
                      frameId = entry.selectedFrameId,
                      size = 44.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Text(
                          text = if (isMe) "${entry.username} (Tu)" else entry.username,
                          style = MaterialTheme.typography.titleMedium,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        RankBadge(rankTier = rank, size = 22.dp)
                      }
                      Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = if (showWeekly) "sett." else "tot.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }

                    if (!isMe) {
                      IconButton(
                        onClick = { onToggleFriend(entry.userId) },
                        modifier = Modifier.size(48.dp).testTag("toggle_friend_${entry.userId}"),
                      ) {
                        Icon(
                          imageVector =
                            if (isFriend) Icons.Filled.PersonRemove else Icons.Filled.PersonAdd,
                          contentDescription =
                            if (isFriend) "Rimuovi dagli amici" else "Aggiungi agli amici",
                          tint =
                            if (isFriend) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
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

private val ColorSilver = androidx.compose.ui.graphics.Color(0xFFE0E5EC)
