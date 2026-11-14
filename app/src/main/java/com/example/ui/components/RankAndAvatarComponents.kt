package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.RankTier
import com.example.domain.model.ShopCatalog
import com.example.ui.theme.ArenaSurfaceHighest
import com.example.ui.theme.CoinAmber

enum class AvatarPreset(val id: String, val label: String, val icon: ImageVector) {
  BOLT("avatar_bolt", "Fulmine", Icons.Filled.Bolt),
  FLAME("avatar_flame", "Fiamma", Icons.Filled.LocalFireDepartment),
  FITNESS("avatar_fitness", "Forza", Icons.Filled.FitnessCenter),
  SHIELD("avatar_shield", "Scudo", Icons.Filled.Shield),
  STAR("avatar_star", "Stella", Icons.Filled.Star),
  TROPHY("avatar_trophy", "Trofeo", Icons.Filled.EmojiEvents);

  companion object {
    fun fromId(id: String): AvatarPreset = entries.firstOrNull { it.id == id } ?: BOLT
  }
}

@Composable
fun AthleteAvatar(
  avatarPreset: String,
  frameId: String,
  size: Dp = 52.dp,
  modifier: Modifier = Modifier,
) {
  val preset = AvatarPreset.fromId(avatarPreset)
  val frameItem = ShopCatalog.findById(frameId)
  val brush =
    if (frameItem != null && frameItem.id != ShopCatalog.DEFAULT_FRAME_ID) {
      Brush.linearGradient(listOf(frameItem.primaryColor, frameItem.secondaryColor))
    } else {
      Brush.linearGradient(
        listOf(
          MaterialTheme.colorScheme.outline,
          MaterialTheme.colorScheme.outline,
        )
      )
    }
  val borderWidth = if (frameId == ShopCatalog.DEFAULT_FRAME_ID) 2.dp else 3.5.dp

  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(ArenaSurfaceHighest)
        .border(width = borderWidth, brush = brush, shape = CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = preset.icon,
      contentDescription = "Avatar ${preset.label}",
      tint = frameItem?.primaryColor ?: MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(size * 0.54f),
    )
  }
}

@Composable
fun RankBadge(
  rankTier: RankTier,
  size: Dp = 48.dp,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = this.size.width
      val h = this.size.height
      val shieldPath =
        Path().apply {
          moveTo(w * 0.5f, h * 0.05f)
          lineTo(w * 0.92f, h * 0.22f)
          lineTo(w * 0.85f, h * 0.68f)
          lineTo(w * 0.5f, h * 0.96f)
          lineTo(w * 0.15f, h * 0.68f)
          lineTo(w * 0.08f, h * 0.22f)
          close()
        }
      drawPath(
        path = shieldPath,
        brush = Brush.verticalGradient(listOf(rankTier.badgeColor, rankTier.badgeSecondaryColor)),
      )
      drawPath(
        path = shieldPath,
        color = Color.White.copy(alpha = 0.45f),
        style = Stroke(width = w * 0.05f),
      )
    }
    Icon(
      imageVector = Icons.Filled.MilitaryTech,
      contentDescription = "Badge grado ${rankTier.displayName}",
      tint = Color(0xFF0A0D12),
      modifier = Modifier.size(size * 0.48f),
    )
  }
}

@Composable
fun PushCoinChip(
  coins: Long,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = CoinAmber.copy(alpha = 0.14f),
    border = androidx.compose.foundation.BorderStroke(1.dp, CoinAmber.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.MonetizationOn,
        contentDescription = "PushCoin",
        tint = CoinAmber,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "$coins",
        style = MaterialTheme.typography.labelLarge,
        color = CoinAmber,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
