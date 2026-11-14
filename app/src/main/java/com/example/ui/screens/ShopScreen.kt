package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ShopCatalog
import com.example.domain.model.ShopCategory
import com.example.domain.model.ShopItem
import com.example.domain.model.UserProfile
import com.example.ui.components.PushCoinChip
import com.example.ui.theme.CoinAmber
import com.example.ui.viewmodel.UiState

@Composable
fun ShopScreen(
  profileState: UiState<UserProfile>,
  onPurchaseOrEquip: (ShopItem) -> Unit,
  onEquipBoost: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var selectedCategory by remember { mutableStateOf(ShopCategory.THEME) }

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
        val items = ShopCatalog.items.filter { it.category == selectedCategory }

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
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Negozio PushCoin",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
            PushCoinChip(coins = profile.pushCoins)
          }

          Text(
            text = "Sblocca titoli, cosmetici e potenziamenti consumabili usando solo i PushCoin guadagnati con le tue flessioni.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          LazyRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(ShopCategory.entries) { category ->
              FilterChip(
                selected = selectedCategory == category,
                onClick = { selectedCategory = category },
                label = { Text(category.label) },
                modifier = Modifier.testTag("shop_category_${category.name.lowercase()}"),
              )
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(items, key = { it.id }) { item ->
              val isBoost = item.category == ShopCategory.BOOST
              val ownedCharges = if (isBoost) profile.ownedBoosts.count { it == item.id } else 0
              val isUnlocked = !isBoost && item.id in profile.unlockedItems
              val isEquipped =
                when (item.category) {
                  ShopCategory.THEME -> profile.selectedThemeId == item.id
                  ShopCategory.SKELETON -> profile.selectedSkeletonEffectId == item.id
                  ShopCategory.FRAME -> profile.selectedFrameId == item.id
                  ShopCategory.TITLE -> profile.selectedTitleId == item.id
                  ShopCategory.BOOST -> profile.activeBoostId == item.id && ownedCharges > 0
                }
              val canAfford = profile.pushCoins >= item.priceCoins
              val canBuyBoost = canAfford && profile.ownedBoosts.size < 20

              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border =
                  if (isEquipped) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                  else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(52.dp)
                        .clip(CircleShape)
                        .background(
                          Brush.linearGradient(listOf(item.primaryColor, item.secondaryColor))
                        )
                  )

                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = item.name,
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = item.description,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isBoost) {
                      Text(
                        text =
                          if (isEquipped) "Attivo per la prossima sessione • Cariche: $ownedCharges"
                          else "Cariche possedute: $ownedCharges (max inventario 20)",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (isEquipped) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                      )
                    } else if (item.category == ShopCategory.TITLE && item.titleText != null) {
                      Text(
                        text = "Titolo profilo: «${item.titleText}»",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }

                  if (isBoost) {
                    Column(
                      horizontalAlignment = Alignment.End,
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      if (ownedCharges > 0) {
                        if (isEquipped) {
                          OutlinedButton(
                            onClick = { onEquipBoost("") },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("shop_boost_unequip_${item.id}"),
                          ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Attivo")
                          }
                        } else {
                          OutlinedButton(
                            onClick = { onEquipBoost(item.id) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("shop_boost_equip_${item.id}"),
                          ) {
                            Text("Attiva")
                          }
                        }
                      }
                      Button(
                        onClick = { onPurchaseOrEquip(item) },
                        enabled = canBuyBoost,
                        colors =
                          ButtonDefaults.buttonColors(
                            containerColor = CoinAmber,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                          ),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("shop_item_${item.id}"),
                      ) {
                        if (!canAfford) {
                          Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                          )
                          Spacer(modifier = Modifier.size(4.dp))
                        }
                        Text("+1 (${item.priceCoins} PC)", fontWeight = FontWeight.Bold)
                      }
                    }
                  } else {
                    when {
                      isEquipped -> {
                        OutlinedButton(
                          onClick = {},
                          enabled = false,
                          modifier = Modifier.heightIn(min = 48.dp).testTag("shop_item_${item.id}"),
                        ) {
                          Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.size(4.dp))
                          Text("Attivo")
                        }
                      }
                      isUnlocked -> {
                        Button(
                          onClick = { onPurchaseOrEquip(item) },
                          modifier = Modifier.heightIn(min = 48.dp).testTag("shop_item_${item.id}"),
                        ) {
                          Text("Equipaggia")
                        }
                      }
                      else -> {
                        Button(
                          onClick = { onPurchaseOrEquip(item) },
                          enabled = canAfford,
                          colors =
                            ButtonDefaults.buttonColors(
                              containerColor = CoinAmber,
                              contentColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                          modifier = Modifier.heightIn(min = 48.dp).testTag("shop_item_${item.id}"),
                        ) {
                          if (!canAfford) {
                            Icon(
                              Icons.Filled.Lock,
                              contentDescription = null,
                              modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                          }
                          Text("${item.priceCoins} PC", fontWeight = FontWeight.Bold)
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

