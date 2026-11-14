package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class ShopCategory(val label: String) {
  THEME("Temi App"),
  SKELETON("Effetti AI"),
  FRAME("Cornici"),
  TITLE("Titoli"),
  BOOST("Potenziamenti"),
}

data class ShopItem(
  val id: String,
  val name: String,
  val description: String,
  val category: ShopCategory,
  val priceCoins: Long,
  val primaryColor: Color,
  val secondaryColor: Color,
  val titleText: String? = null,
)

object ShopCatalog {
  const val DEFAULT_THEME_ID = "theme_volt"
  const val DEFAULT_SKELETON_ID = "skel_classic"
  const val DEFAULT_FRAME_ID = "frame_none"
  const val DEFAULT_TITLE_ID = "title_rookie"

  const val BOOST_DOUBLE_COINS_ID = "boost_double_coins"
  const val BOOST_ARENA_SHIELD_ID = "boost_arena_shield"
  const val BOOST_PRECISION_FOCUS_ID = "boost_precision_focus"

  val defaultUnlockedIds: List<String> =
    listOf(DEFAULT_THEME_ID, DEFAULT_SKELETON_ID, DEFAULT_FRAME_ID, DEFAULT_TITLE_ID)

  val items: List<ShopItem> =
    listOf(
      // App Themes
      ShopItem(
        id = DEFAULT_THEME_ID,
        name = "Volt Electric",
        description = "Palette ad alto contrasto lime e ossidiana.",
        category = ShopCategory.THEME,
        priceCoins = 0L,
        primaryColor = Color(0xFFC6FF00),
        secondaryColor = Color(0xFF00E5FF),
      ),
      ShopItem(
        id = "theme_crimson",
        name = "Crimson Apex",
        description = "Accenti rosso gara e carbonio opaco.",
        category = ShopCategory.THEME,
        priceCoins = 30L,
        primaryColor = Color(0xFFFF4D6D),
        secondaryColor = Color(0xFFFF9E00),
      ),
      ShopItem(
        id = "theme_cyber",
        name = "Cyber Azure",
        description = "Blu elettrico e ciano olografico.",
        category = ShopCategory.THEME,
        priceCoins = 50L,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF7C4DFF),
      ),
      ShopItem(
        id = "theme_gold",
        name = "Obsidian Gold",
        description = "Oro campione su nero profondo.",
        category = ShopCategory.THEME,
        priceCoins = 100L,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFF8F00),
      ),

      // Camera Skeleton Effects
      ShopItem(
        id = DEFAULT_SKELETON_ID,
        name = "Biometric Lime",
        description = "Scheletro classico verde/rosso ad alta leggibilità.",
        category = ShopCategory.SKELETON,
        priceCoins = 0L,
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFFB9F6CA),
      ),
      ShopItem(
        id = "skel_laser",
        name = "Laser Cyan",
        description = "Linee laser ciano con nodi luminosi.",
        category = ShopCategory.SKELETON,
        priceCoins = 25L,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF84FFFF),
      ),
      ShopItem(
        id = "skel_plasma",
        name = "Plasma Glow",
        description = "Effetto alone viola-neon sui giunti.",
        category = ShopCategory.SKELETON,
        priceCoins = 60L,
        primaryColor = Color(0xFFD500F9),
        secondaryColor = Color(0xFF00E5FF),
      ),
      ShopItem(
        id = "skel_gold",
        name = "Champion Aura",
        description = "Tracciamento dorato con doppia aura.",
        category = ShopCategory.SKELETON,
        priceCoins = 120L,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFFAB00),
      ),

      // Avatar Frames
      ShopItem(
        id = DEFAULT_FRAME_ID,
        name = "Standard",
        description = "Bordo essenziale da allenamento.",
        category = ShopCategory.FRAME,
        priceCoins = 0L,
        primaryColor = Color(0xFF4B5563),
        secondaryColor = Color(0xFF9CA3AF),
      ),
      ShopItem(
        id = "frame_steel",
        name = "Steel Ring",
        description = "Anello metallico bicolore titanio.",
        category = ShopCategory.FRAME,
        priceCoins = 20L,
        primaryColor = Color(0xFF90CAF9),
        secondaryColor = Color(0xFFE3F2FD),
      ),
      ShopItem(
        id = "frame_neon",
        name = "Neon Pulse",
        description = "Cornice luminosa lime e ciano.",
        category = ShopCategory.FRAME,
        priceCoins = 45L,
        primaryColor = Color(0xFFC6FF00),
        secondaryColor = Color(0xFF00E5FF),
      ),
      ShopItem(
        id = "frame_apex",
        name = "Apex Halo",
        description = "Aura dorata dei primi in classifica.",
        category = ShopCategory.FRAME,
        priceCoins = 90L,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFF6D00),
      ),

      // Profile Titles (ampliati per Shop & Classifica)
      ShopItem(
        id = DEFAULT_TITLE_ID,
        name = "Sfida Principiante",
        description = "Il primo passo verso la vetta.",
        category = ShopCategory.TITLE,
        priceCoins = 0L,
        primaryColor = Color(0xFF9CA3AF),
        secondaryColor = Color(0xFF6B7280),
        titleText = "Sfida Principiante",
      ),
      ShopItem(
        id = "title_iron",
        name = "Braccia d'Acciaio",
        description = "Titolo forgiato nella costanza.",
        category = ShopCategory.TITLE,
        priceCoins = 25L,
        primaryColor = Color(0xFF4DD0E1),
        secondaryColor = Color(0xFF0097A7),
        titleText = "Braccia d'Acciaio",
      ),
      ShopItem(
        id = "title_spartan",
        name = "Cuore Spartano",
        description = "Non arretra mai davanti alle ultime ripetizioni.",
        category = ShopCategory.TITLE,
        priceCoins = 40L,
        primaryColor = Color(0xFFFF5252),
        secondaryColor = Color(0xFFFF9100),
        titleText = "Cuore Spartano",
      ),
      ShopItem(
        id = "title_machine",
        name = "Macchina da Push-Up",
        description = "Per chi non salta mai una serie.",
        category = ShopCategory.TITLE,
        priceCoins = 75L,
        primaryColor = Color(0xFFC6FF00),
        secondaryColor = Color(0xFF76FF03),
        titleText = "Macchina da Push-Up",
      ),
      ShopItem(
        id = "title_titan",
        name = "Titano della Panca",
        description = "Forza esplosiva e postura impeccabile.",
        category = ShopCategory.TITLE,
        priceCoins = 95L,
        primaryColor = Color(0xFF7C4DFF),
        secondaryColor = Color(0xFF00E5FF),
        titleText = "Titano della Panca",
      ),
      ShopItem(
        id = "title_legend",
        name = "Leggenda On-Device",
        description = "Titolo d'élite della community.",
        category = ShopCategory.TITLE,
        priceCoins = 150L,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFF6D00),
        titleText = "Leggenda On-Device",
      ),
      ShopItem(
        id = "title_arena_king",
        name = "Gladiatore dell'Arena",
        description = "Temuto in ogni duello competitivo 1v1.",
        category = ShopCategory.TITLE,
        priceCoins = 180L,
        primaryColor = Color(0xFFFF1744),
        secondaryColor = Color(0xFFFFD54F),
        titleText = "Gladiatore dell'Arena",
      ),
      ShopItem(
        id = "title_cyber_monk",
        name = "Monaco Biomeccanico",
        description = "Controllo totale di angoli e respirazione.",
        category = ShopCategory.TITLE,
        priceCoins = 240L,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFC6FF00),
        titleText = "Monaco Biomeccanico",
      ),

      // Consumable Power-Ups (Potenziamenti)
      ShopItem(
        id = BOOST_DOUBLE_COINS_ID,
        name = "Furia x2 PushCoin",
        description = "Raddoppia i PushCoin guadagnati nella tua prossima sessione (1 carica).",
        category = ShopCategory.BOOST,
        priceCoins = 35L,
        primaryColor = Color(0xFFFFD54F),
        secondaryColor = Color(0xFFFF6D00),
      ),
      ShopItem(
        id = BOOST_ARENA_SHIELD_ID,
        name = "Carica Arena +15 PC",
        description = "Assegna +15 PushCoin bonus se completi una sfida o duello in Arena (1 carica).",
        category = ShopCategory.BOOST,
        priceCoins = 25L,
        primaryColor = Color(0xFFFF4D6D),
        secondaryColor = Color(0xFFD500F9),
      ),
      ShopItem(
        id = BOOST_PRECISION_FOCUS_ID,
        name = "Focus Biomeccanico",
        description = "Aumenta il punteggio Forma finale di +10% (max 100%) e +5 PC bonus (1 carica).",
        category = ShopCategory.BOOST,
        priceCoins = 20L,
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF00E676),
      ),
    )

  fun findById(id: String): ShopItem? = items.firstOrNull { it.id == id }
}
