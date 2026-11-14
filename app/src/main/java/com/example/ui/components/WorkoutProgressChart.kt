package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.domain.model.WorkoutSession
import com.example.ui.theme.CoinAmber
import com.example.ui.theme.PostureGreen
import com.example.ui.theme.PostureRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ChartDisplayMode {
  LINE,
  BAR,
}

data class WorkoutChartSummary(
  val chronological: List<WorkoutSession>,
  val averageReps: Int,
  val peakReps: Long,
  val deltaFromPrevious: Long?,
  val overallDeltaReps: Long,
)

object WorkoutChartAnalyzer {
  fun analyze(workoutsDescending: List<WorkoutSession>): WorkoutChartSummary {
    val chronological = workoutsDescending.reversed()
    if (chronological.isEmpty()) {
      return WorkoutChartSummary(
        chronological = emptyList(),
        averageReps = 0,
        peakReps = 0L,
        deltaFromPrevious = null,
        overallDeltaReps = 0L,
      )
    }
    val average = chronological.map { it.reps }.average().roundToInt()
    val peak = chronological.maxOf { it.reps }
    val latest = chronological.last().reps
    val prev = if (chronological.size >= 2) chronological[chronological.lastIndex - 1].reps else null
    val first = chronological.first().reps
    return WorkoutChartSummary(
      chronological = chronological,
      averageReps = average,
      peakReps = peak,
      deltaFromPrevious = prev?.let { latest - it },
      overallDeltaReps = latest - first,
    )
  }

  fun nearestIndexForX(
    touchX: Float,
    plotStartX: Float,
    plotWidth: Float,
    itemCount: Int,
  ): Int {
    if (itemCount <= 1 || plotWidth <= 0f) return 0
    val clamped = ((touchX - plotStartX) / plotWidth).coerceIn(0f, 1f)
    return (clamped * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
  }
}

@Composable
fun WorkoutProgressChartCard(
  workouts: List<WorkoutSession>,
  modifier: Modifier = Modifier,
) {
  val summary = remember(workouts) { WorkoutChartAnalyzer.analyze(workouts) }
  val chronological = summary.chronological
  var chartMode by remember { mutableStateOf(ChartDisplayMode.LINE) }
  var selectedIndex by remember(chronological.size) {
    mutableIntStateOf((chronological.size - 1).coerceAtLeast(0))
  }

  Card(
    modifier = modifier.fillMaxWidth().testTag("progress_chart_card"),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Header + Toggle Linee / Barre
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.TrendingUp,
              contentDescription = "Andamento flessioni",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(8.dp).size(22.dp),
            )
          }
          Column {
            Text(
              text = "Andamento Progressi",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (chronological.isEmpty()) "Flessioni per allenamento"
                else "Media ${summary.averageReps} rep • Picco ${summary.peakReps} rep",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (chronological.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Row(
              modifier = Modifier.padding(3.dp),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ChartModeButton(
                selected = chartMode == ChartDisplayMode.LINE,
                icon = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = "Grafico a linee",
                onClick = { chartMode = ChartDisplayMode.LINE },
                modifier = Modifier.testTag("progress_chart_mode_line"),
              )
              ChartModeButton(
                selected = chartMode == ChartDisplayMode.BAR,
                icon = Icons.Filled.BarChart,
                contentDescription = "Grafico a barre",
                onClick = { chartMode = ChartDisplayMode.BAR },
                modifier = Modifier.testTag("progress_chart_mode_bar"),
              )
            }
          }
        }
      }

      if (chronological.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Timeline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp),
            )
            Text(
              text = "Il tuo grafico è pronto a partire",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Completa almeno una sessione per visualizzare l'evoluzione delle flessioni nel tempo e confrontare ogni allenamento.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        val safeSelected = selectedIndex.coerceIn(0, chronological.lastIndex)
        val selectedSession = chronological[safeSelected]
        val previousSession = if (safeSelected > 0) chronological[safeSelected - 1] else null
        val selectedDelta = previousSession?.let { selectedSession.reps - it.reps }

        // Pannello tooltip interattivo per la sessione selezionata
        SelectedSessionReadout(
          sessionNumber = safeSelected + 1,
          session = selectedSession,
          deltaFromPrevious = selectedDelta,
          modifier = Modifier.testTag("progress_chart_tooltip"),
        )

        // Canvas interattivo (Linee o Barre)
        InteractiveWorkoutCanvas(
          sessions = chronological,
          selectedIndex = safeSelected,
          mode = chartMode,
          onSelectIndex = { idx -> selectedIndex = idx },
          modifier = Modifier.testTag("progress_chart_canvas"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Tocca o trascina sul grafico per esplorare ogni sessione",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (chronological.size >= 2) {
            val overall = summary.overallDeltaReps
            val trendColor =
              when {
                overall > 0L -> PostureGreen
                overall < 0L -> PostureRed
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            val trendPrefix = if (overall > 0L) "+" else ""
            Text(
              text = "$trendPrefix$overall rep dal #1",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = trendColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChartModeButton(
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .background(
          if (selected) MaterialTheme.colorScheme.primary
          else Color.Transparent
        )
        .clickable(onClick = onClick)
        .padding(8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}

@Composable
private fun SelectedSessionReadout(
  sessionNumber: Int,
  session: WorkoutSession,
  deltaFromPrevious: Long?,
  modifier: Modifier = Modifier,
) {
  val dateStr =
    remember(session.id, session.createdAt) {
      session.createdAt?.toDate()?.let { date: Date ->
        SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN).format(date)
      } ?: "Sessione recente"
    }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
          )
          Text(
            text = "Allenamento $sessionNumber • $dateStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "${session.reps} flessioni",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${session.durationSeconds}s • Forma ${session.averageFormScore}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 1.dp),
          )
        }
      }

      if (deltaFromPrevious != null) {
        val deltaColor =
          when {
            deltaFromPrevious > 0L -> PostureGreen
            deltaFromPrevious < 0L -> PostureRed
            else -> CoinAmber
          }
        val sign = if (deltaFromPrevious > 0L) "+" else ""
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = deltaColor.copy(alpha = 0.14f),
        ) {
          Text(
            text = "$sign$deltaFromPrevious rep",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = deltaColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        ) {
          Text(
            text = "Baseline",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun InteractiveWorkoutCanvas(
  sessions: List<WorkoutSession>,
  selectedIndex: Int,
  mode: ChartDisplayMode,
  onSelectIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
  val axisTextStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
  val textMeasurer = rememberTextMeasurer()

  val animProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 450),
    label = "chart_reveal",
  )

  val rawMax = remember(sessions) { sessions.maxOfOrNull { it.reps } ?: 10L }
  val scaleMax = remember(rawMax) { ((rawMax.coerceAtLeast(5L) * 1.15f).roundToInt()).coerceAtLeast(10) }

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(210.dp)
        .pointerInput(sessions.size) {
          val leftPad = 36.dp.toPx()
          val rightPad = 14.dp.toPx()
          val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
          detectTapGestures { offset ->
            val idx =
              if (mode == ChartDisplayMode.BAR && sessions.size > 1) {
                val step = plotW / sessions.size
                ((offset.x - leftPad) / step).toInt().coerceIn(0, sessions.lastIndex)
              } else {
                WorkoutChartAnalyzer.nearestIndexForX(
                  touchX = offset.x,
                  plotStartX = leftPad,
                  plotWidth = plotW,
                  itemCount = sessions.size,
                )
              }
            onSelectIndex(idx)
          }
        }
        .pointerInput(sessions.size, mode) {
          val leftPad = 36.dp.toPx()
          val rightPad = 14.dp.toPx()
          val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
          detectHorizontalDragGestures { change, _ ->
            change.consume()
            val idx =
              if (mode == ChartDisplayMode.BAR && sessions.size > 1) {
                val step = plotW / sessions.size
                ((change.position.x - leftPad) / step).toInt().coerceIn(0, sessions.lastIndex)
              } else {
                WorkoutChartAnalyzer.nearestIndexForX(
                  touchX = change.position.x,
                  plotStartX = leftPad,
                  plotWidth = plotW,
                  itemCount = sessions.size,
                )
              }
            onSelectIndex(idx)
          }
        },
  ) {
    val leftPad = 36.dp.toPx()
    val rightPad = 14.dp.toPx()
    val topPad = 12.dp.toPx()
    val bottomPad = 26.dp.toPx()

    val plotWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
    val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
    val baselineY = topPad + plotHeight

    // Griglia orizzontale + etichette asse Y (0, 50%, 100%)
    val gridRatios = listOf(0f, 0.5f, 1f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    for (ratio in gridRatios) {
      val y = baselineY - (plotHeight * ratio)
      drawLine(
        color = gridColor.copy(alpha = if (ratio == 0f) 0.8f else 0.45f),
        start = Offset(leftPad, y),
        end = Offset(leftPad + plotWidth, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = if (ratio == 0f) null else dashEffect,
      )
      val tickValue = (scaleMax * ratio).roundToInt().toString()
      val measured = textMeasurer.measure(tickValue, style = axisTextStyle)
      drawText(
        textLayoutResult = measured,
        topLeft =
          Offset(
            x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
            y = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height),
          ),
      )
    }

    if (sessions.isEmpty()) return@Canvas

    val count = sessions.size
    val xCenters =
      if (mode == ChartDisplayMode.BAR) {
        val slotWidth = plotWidth / count.toFloat()
        List(count) { i -> leftPad + slotWidth * (i + 0.5f) }
      } else {
        if (count == 1) {
          listOf(leftPad + plotWidth / 2f)
        } else {
          val step = plotWidth / (count - 1).toFloat()
          List(count) { i -> leftPad + step * i }
        }
      }

    val points =
      sessions.mapIndexed { idx, session ->
        val normY = (session.reps.toFloat() / scaleMax.toFloat()).coerceIn(0f, 1f)
        val y = baselineY - (plotHeight * normY * animProgress)
        Offset(xCenters[idx], y)
      }

    val safeSelected = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeSelected]

    if (mode == ChartDisplayMode.BAR) {
      val slotWidth = plotWidth / count.toFloat()
      val barWidth = (slotWidth * 0.58f).coerceIn(10.dp.toPx(), 34.dp.toPx())
      points.forEachIndexed { idx, pt ->
        val isSelected = idx == safeSelected
        val barTop = pt.y
        val barHeight = (baselineY - barTop).coerceAtLeast(3.dp.toPx())
        val barLeft = pt.x - barWidth / 2f
        val barAlpha = if (isSelected) 1f else 0.45f
        drawRoundRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(primaryColor.copy(alpha = barAlpha), secondaryColor.copy(alpha = barAlpha * 0.85f)),
              startY = barTop,
              endY = baselineY,
            ),
          topLeft = Offset(barLeft, barTop),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        if (isSelected) {
          drawRoundRect(
            color = primaryColor,
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
          )
        }
      }
    } else {
      // Linea verticale guida sull'elemento selezionato
      drawLine(
        color = primaryColor.copy(alpha = 0.45f),
        start = Offset(selectedPoint.x, topPad),
        end = Offset(selectedPoint.x, baselineY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = dashEffect,
      )

      if (points.size >= 2) {
        val linePath = Path().apply {
          moveTo(points.first().x, points.first().y)
          for (i in 0 until points.lastIndex) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
          }
        }
        val fillPath =
          Path().apply {
            addPath(linePath)
            lineTo(points.last().x, baselineY)
            lineTo(points.first().x, baselineY)
            close()
          }
        drawPath(
          path = fillPath,
          brush =
            Brush.verticalGradient(
              colors = listOf(primaryColor.copy(alpha = 0.30f), primaryColor.copy(alpha = 0.0f)),
              startY = topPad,
              endY = baselineY,
            ),
        )
        drawPath(
          path = linePath,
          brush = Brush.horizontalGradient(listOf(secondaryColor, primaryColor)),
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }

      // Nodi
      points.forEachIndexed { idx, pt ->
        val isSelected = idx == safeSelected
        if (isSelected) {
          drawCircle(
            color = primaryColor.copy(alpha = 0.24f),
            radius = 12.dp.toPx(),
            center = pt,
          )
          drawCircle(
            color = surfaceColor,
            radius = 6.dp.toPx(),
            center = pt,
          )
          drawCircle(
            color = primaryColor,
            radius = 6.dp.toPx(),
            center = pt,
            style = Stroke(width = 3.dp.toPx()),
          )
        } else {
          drawCircle(
            color = primaryColor,
            radius = 3.5.dp.toPx(),
            center = pt,
          )
        }
      }
    }

    // Etichette asse X ("#1", "#2", ...) con passo adattivo anti-sovrapposizione
    val labelStride =
      when {
        count <= 6 -> 1
        count <= 12 -> 2
        count <= 20 -> 3
        else -> 5
      }
    points.forEachIndexed { idx, pt ->
      val shouldShow = idx == 0 || idx == points.lastIndex || idx == safeSelected || idx % labelStride == 0
      if (shouldShow) {
        val label = "#${idx + 1}"
        val style =
          if (idx == safeSelected) axisTextStyle.copy(color = primaryColor, fontWeight = FontWeight.Bold)
          else axisTextStyle
        val measured = textMeasurer.measure(label, style = style)
        val xPos = (pt.x - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width)
        drawText(
          textLayoutResult = measured,
          topLeft = Offset(x = xPos, y = baselineY + 6.dp.toPx()),
        )
      }
    }
  }
}
