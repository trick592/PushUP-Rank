package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class RankTier(
  val firestoreKey: String,
  val displayName: String,
  val minPushUps: Long,
  val nextTierThreshold: Long?,
  val badgeColor: Color,
  val badgeSecondaryColor: Color,
  val motto: String,
) {
  BRONZE(
    firestoreKey = "BRONZE",
    displayName = "Bronzo",
    minPushUps = 0L,
    nextTierThreshold = 100L,
    badgeColor = Color(0xFFCD7F32),
    badgeSecondaryColor = Color(0xFF8B4513),
    motto = "Prime fondamenta di forza",
  ),
  SILVER(
    firestoreKey = "SILVER",
    displayName = "Argento",
    minPushUps = 100L,
    nextTierThreshold = 500L,
    badgeColor = Color(0xFFE0E5EC),
    badgeSecondaryColor = Color(0xFF8A95A5),
    motto = "Costanza e controllo",
  ),
  GOLD(
    firestoreKey = "GOLD",
    displayName = "Oro",
    minPushUps = 500L,
    nextTierThreshold = 1500L,
    badgeColor = Color(0xFFFFD54F),
    badgeSecondaryColor = Color(0xFFB28900),
    motto = "Potenza agonistica",
  ),
  PLATINUM(
    firestoreKey = "PLATINUM",
    displayName = "Platino",
    minPushUps = 1500L,
    nextTierThreshold = 4000L,
    badgeColor = Color(0xFF4DD0E1),
    badgeSecondaryColor = Color(0xFF00838F),
    motto = "Precisione biomeccanica",
  ),
  DIAMOND(
    firestoreKey = "DIAMOND",
    displayName = "Diamante",
    minPushUps = 4000L,
    nextTierThreshold = 10000L,
    badgeColor = Color(0xFF82B1FF),
    badgeSecondaryColor = Color(0xFF2962FF),
    motto = "Resistenza d'élite",
  ),
  MASTER(
    firestoreKey = "MASTER",
    displayName = "Maestro",
    minPushUps = 10000L,
    nextTierThreshold = null,
    badgeColor = Color(0xFFC6FF00),
    badgeSecondaryColor = Color(0xFF00E5FF),
    motto = "Vertice assoluto PushUp Rank",
  );

  companion object {
    fun fromTotalPushUps(total: Long): RankTier {
      return entries.lastOrNull { total >= it.minPushUps } ?: BRONZE
    }

    fun fromKey(key: String): RankTier {
      return entries.firstOrNull { it.firestoreKey == key } ?: BRONZE
    }

    fun progressToNext(total: Long): Float {
      val current = fromTotalPushUps(total)
      val next = current.nextTierThreshold ?: return 1f
      val span = (next - current.minPushUps).coerceAtLeast(1L)
      return ((total - current.minPushUps).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }
  }
}
