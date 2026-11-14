package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.RankTier
import com.example.domain.model.ShopCatalog
import com.example.domain.model.UserProfile
import com.example.domain.model.WorkoutSession
import com.example.ui.components.AthleteAvatar
import com.example.ui.components.PushCoinChip
import com.example.ui.components.RankBadge
import com.example.ui.components.WorkoutProgressChartCard
import com.example.ui.theme.ArenaBlack
import com.example.ui.theme.CoinAmber
import com.example.ui.theme.PostureGreen
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  profileState: UiState<UserProfile>,
  workoutsState: UiState<List<WorkoutSession>>,
  onStartWorkoutClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    when (profileState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(
            text = profileState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
          )
        }
      }
      is UiState.Success -> {
        val profile = profileState.data
        val rank = RankTier.fromTotalPushUps(profile.totalPushUps)
        val progress = RankTier.progressToNext(profile.totalPushUps)
        val equippedTitle = ShopCatalog.findById(profile.selectedTitleId)?.titleText ?: "Sfida Principiante"
        val workouts = (workoutsState as? UiState.Success)?.data.orEmpty()

        LazyColumn(
          modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Header utente + PushCoin Wallet
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                AthleteAvatar(
                  avatarPreset = profile.avatarPreset,
                  frameId = profile.selectedFrameId,
                  size = 52.dp,
                )
                Column {
                  Text(
                    text = profile.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Text(
                    text = equippedTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              PushCoinChip(
                coins = profile.pushCoins,
                modifier = Modifier.testTag("dashboard_pushcoin_chip"),
              )
            }
          }

          // Hero Rank Card con banner illustrato e progressione livello
          item {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("dashboard_rank_card"),
              shape = RoundedCornerShape(24.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
              Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                  painter = painterResource(id = R.drawable.img_dashboard_hero),
                  contentDescription = "Banner competitivo PushUp Rank",
                  contentScale = ContentScale.Crop,
                  alpha = 0.28f,
                  modifier = Modifier.matchParentSize(),
                )
                Box(
                  modifier =
                    Modifier.matchParentSize()
                      .background(
                        Brush.horizontalGradient(
                          listOf(
                            ArenaBlack.copy(alpha = 0.90f),
                            ArenaBlack.copy(alpha = 0.65f),
                          )
                        )
                      )
                )

                Column(
                  modifier = Modifier.padding(20.dp),
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text(
                        text = "GRADO ATTUALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = rank.displayName.uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = rank.badgeColor,
                      )
                      Text(
                        text = rank.motto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    RankBadge(rankTier = rank, size = 68.dp)
                  }

                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = "${profile.totalPushUps} flessioni totali",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text =
                          if (rank.nextTierThreshold != null) {
                            "Prossimo: ${rank.nextTierThreshold}"
                          } else {
                            "Grado Massimo!"
                          },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    LinearProgressIndicator(
                      progress = { progress },
                      modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                      color = MaterialTheme.colorScheme.primary,
                      trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                  }
                }
              }
            }
          }

          // Pulsante CTA Avvia Allenamento AI
          item {
            Button(
              onClick = onStartWorkoutClick,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 64.dp)
                  .testTag("start_workout_button"),
              shape = RoundedCornerShape(20.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.size(10.dp))
              Column {
                Text(
                  text = "AVVIA ALLENAMENTO AI",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Rilevamento ML Kit on-device • +1 PushCoin / rep",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
              }
            }
          }

          // Griglia Metriche Rapide
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              MetricTile(
                icon = Icons.Filled.CalendarToday,
                label = "Settimana",
                value = "${profile.weeklyPushUps}",
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
              )
              MetricTile(
                icon = Icons.Filled.EmojiEvents,
                label = "Record Serie",
                value = "${profile.bestSessionReps}",
                accent = CoinAmber,
                modifier = Modifier.weight(1f),
              )
              MetricTile(
                icon = Icons.Filled.Bolt,
                label = "Totali",
                value = "${profile.totalPushUps}",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
            }
          }

          // Grafico Interattivo Andamento Allenamenti
          item {
            WorkoutProgressChartCard(workouts = workouts)
          }

          // Storico Allenamenti Recenti
          item {
            Text(
              text = "Sessioni Recenti",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.padding(top = 4.dp),
            )
          }

          if (workouts.isEmpty()) {
            item {
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
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                  )
                  Text(
                    text = "Nessuna sessione registrata",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "Appoggia il telefono a terra con la fotocamera frontale rivolta verso di te e avvia il tuo primo set!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            val totalCount = workouts.size
            items(
              count = totalCount,
              key = { index -> workouts[index].id },
            ) { index ->
              val session = workouts[index]
              val sessionNumber = totalCount - index
              WorkoutHistoryRow(sessionNumber = sessionNumber, session = session)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun WorkoutHistoryRow(
  sessionNumber: Int,
  session: WorkoutSession,
) {
  val dateStr =
    session.createdAt?.toDate()?.let { date: Date ->
      SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN).format(date)
    } ?: "Oggi"

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PostureGreen.copy(alpha = 0.14f),
        ) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Sessione valida",
            tint = PostureGreen,
            modifier = Modifier.padding(10.dp).size(22.dp),
          )
        }
        Column {
          Text(
            text = "#$sessionNumber • ${session.reps} flessioni valide",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "$dateStr • ${session.durationSeconds}s • Forma ${session.averageFormScore}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      PushCoinChip(coins = session.earnedCoins)
    }
  }
}
