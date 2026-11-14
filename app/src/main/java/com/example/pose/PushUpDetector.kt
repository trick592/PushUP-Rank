package com.example.pose

import android.graphics.PointF
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Fasi biomeccaniche della flessione (macchina a stati finiti).
 */
enum class PushUpPhase(val label: String) {
  WAITING_FOR_POSE("Inquadra tutto il busto"),
  UP_READY("Posizione alta pronta"),
  DESCENDING("Scendi controllato"),
  BOTTOM_REACHED("Profondità raggiunta, spingi!"),
  POSTURE_ERROR("Correggi la schiena!"),
}

data class NormalizedJoint(
  val x: Float,
  val y: Float,
  val likelihood: Float,
)

data class PoseSkeletonFrame(
  val joints: Map<Int, NormalizedJoint> = emptyMap(),
  val connections: List<Pair<Int, Int>> = emptyList(),
  val sourceWidth: Int = 0,
  val sourceHeight: Int = 0,
  val isPostureValid: Boolean = false,
  val elbowAngleDegrees: Float = 180f,
  val backAngleDegrees: Float = 180f,
  val phase: PushUpPhase = PushUpPhase.WAITING_FOR_POSE,
  val repCount: Int = 0,
  val averageFormScore: Int = 100,
  val feedbackMessage: String = "Posizionati in plank davanti alla fotocamera",
  val justCountedRep: Boolean = false,
)

/**
 * Motore matematico on-device per il rilevamento e la validazione biomeccanica delle flessioni.
 *
 * Regole di validazione:
 * 1. Visibilità minima dei landmark chiave (spalla, gomito, polso, anca >= 0.55f).
 * 2. Allineamento schiena/plank: angolo Spalla-Anca-Caviglia >= 150° (o allineamento torso-plank).
 * 3. Profondità valida: angolo del gomito < 90° nella fase bassa.
 * 4. Estensione completa: risalita fino a gomito > 160° prima di assegnare +1 rep e +1 PushCoin.
 * 5. Anti-cheat temporale: intervallo minimo 650ms tra due ripetizioni consecutive.
 */
class PushUpDetector(
  private val minConfidence: Float = 0.55f,
  private val bottomAngleThreshold: Float = 90f,
  private val topAngleThreshold: Float = 160f,
  private val descendingTriggerAngle: Float = 148f,
  private val minBackAlignmentDegrees: Float = 150f,
  private val minRepIntervalMs: Long = 650L,
) {
  private var smoothedElbowAngle: Float = 180f
  private var smoothedBackAngle: Float = 180f
  private var hasBottomBeenReached: Boolean = false
  private var formBrokenDuringRep: Boolean = false
  private var repCount: Int = 0
  private var lastRepTimestampMs: Long = 0L
  private val formScores: MutableList<Int> = mutableListOf()
  private var minElbowInCurrentRep: Float = 180f

  fun reset() {
    smoothedElbowAngle = 180f
    smoothedBackAngle = 180f
    hasBottomBeenReached = false
    formBrokenDuringRep = false
    repCount = 0
    lastRepTimestampMs = 0L
    formScores.clear()
    minElbowInCurrentRep = 180f
  }

  /**
   * Elabora un singolo frame di ML Kit Pose e aggiorna lo stato della flessione.
   * Tutte le coordinate vengono normalizzate in [0f..1f] per l'overlay Jetpack Compose.
   */
  fun processPose(
    pose: Pose,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true,
    timestampMs: Long = System.currentTimeMillis(),
  ): PoseSkeletonFrame {
    if (imageWidth <= 0 || imageHeight <= 0) {
      return PoseSkeletonFrame(repCount = repCount)
    }

    // Selezione automatica del lato corporeo con confidenza media più alta (destro o sinistro)
    val leftScore = averageLikelihood(
      pose,
      PoseLandmark.LEFT_SHOULDER,
      PoseLandmark.LEFT_ELBOW,
      PoseLandmark.LEFT_WRIST,
      PoseLandmark.LEFT_HIP,
    )
    val rightScore = averageLikelihood(
      pose,
      PoseLandmark.RIGHT_SHOULDER,
      PoseLandmark.RIGHT_ELBOW,
      PoseLandmark.RIGHT_WRIST,
      PoseLandmark.RIGHT_HIP,
    )

    val useLeft = leftScore >= rightScore
    val shoulder = pose.getPoseLandmark(if (useLeft) PoseLandmark.LEFT_SHOULDER else PoseLandmark.RIGHT_SHOULDER)
    val elbow = pose.getPoseLandmark(if (useLeft) PoseLandmark.LEFT_ELBOW else PoseLandmark.RIGHT_ELBOW)
    val wrist = pose.getPoseLandmark(if (useLeft) PoseLandmark.LEFT_WRIST else PoseLandmark.RIGHT_WRIST)
    val hip = pose.getPoseLandmark(if (useLeft) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP)
    val ankle = pose.getPoseLandmark(if (useLeft) PoseLandmark.LEFT_ANKLE else PoseLandmark.RIGHT_ANKLE)

    val joints = buildNormalizedJoints(pose, imageWidth.toFloat(), imageHeight.toFloat(), isFrontCamera)

    if (shoulder == null || elbow == null || wrist == null || hip == null ||
      shoulder.inFrameLikelihood < minConfidence ||
      elbow.inFrameLikelihood < minConfidence ||
      wrist.inFrameLikelihood < minConfidence ||
      hip.inFrameLikelihood < minConfidence
    ) {
      return PoseSkeletonFrame(
        joints = joints,
        connections = SKELETON_CONNECTIONS,
        sourceWidth = imageWidth,
        sourceHeight = imageHeight,
        isPostureValid = false,
        phase = PushUpPhase.WAITING_FOR_POSE,
        repCount = repCount,
        averageFormScore = currentAverageScore(),
        feedbackMessage = "Inquadra spalle, gomiti, polsi e fianchi nel riquadro",
      )
    }

    // 1. Calcolo dell'angolo del gomito: Spalla -> Gomito -> Polso
    val rawElbowAngle = calculateAngle(shoulder.position, elbow.position, wrist.position)
    smoothedElbowAngle = exponentialSmooth(previous = smoothedElbowAngle, current = rawElbowAngle, alpha = 0.45f)

    // 2. Verifica allineamento schiena/plank
    val (rawBackAngle, isBackAligned) = evaluateBackAlignment(shoulder, hip, ankle)
    smoothedBackAngle = exponentialSmooth(previous = smoothedBackAngle, current = rawBackAngle, alpha = 0.35f)

    var justCounted = false
    var phase: PushUpPhase
    var feedback: String

    if (!isBackAligned) {
      if (hasBottomBeenReached || smoothedElbowAngle < descendingTriggerAngle) {
        formBrokenDuringRep = true
      }
      phase = PushUpPhase.POSTURE_ERROR
      feedback = "Schiena non allineata (${smoothedBackAngle.toInt()}°): contrai il core!"
    } else {
      minElbowInCurrentRep = minOf(minElbowInCurrentRep, smoothedElbowAngle)

      if (smoothedElbowAngle < bottomAngleThreshold) {
        hasBottomBeenReached = true
        phase = PushUpPhase.BOTTOM_REACHED
        feedback = "Ottima profondità (${smoothedElbowAngle.toInt()}°)! Spingi su"
      } else if (smoothedElbowAngle > topAngleThreshold) {
        if (hasBottomBeenReached && !formBrokenDuringRep) {
          val elapsed = timestampMs - lastRepTimestampMs
          if (elapsed >= minRepIntervalMs) {
            repCount += 1
            lastRepTimestampMs = timestampMs
            justCounted = true
            val depthBonus = ((90f - minElbowInCurrentRep).coerceIn(0f, 35f) / 35f * 15f).toInt()
            val alignmentScore = ((smoothedBackAngle - 150f).coerceIn(0f, 30f) / 30f * 15f).toInt()
            formScores.add((70 + depthBonus + alignmentScore).coerceIn(60, 100))
            feedback = "Valida! +1 PushCoin (${repCount} rep)"
          } else {
            feedback = "Rallenta la cadenza per la validazione"
          }
        } else if (formBrokenDuringRep) {
          feedback = "Ripetizione annullata: mantieni la schiena dritta"
        } else {
          feedback = "Pronto! Scendi sotto i 90° al gomito"
        }
        hasBottomBeenReached = false
        formBrokenDuringRep = false
        minElbowInCurrentRep = 180f
        phase = PushUpPhase.UP_READY
      } else if (smoothedElbowAngle < descendingTriggerAngle) {
        phase = PushUpPhase.DESCENDING
        feedback =
          if (hasBottomBeenReached) "Completa la risalita oltre 160°"
          else "Scendi ancora (${smoothedElbowAngle.toInt()}° -> <90°)"
      } else {
        phase = PushUpPhase.UP_READY
        feedback = "Pronto! Scendi controllato"
      }
    }

    return PoseSkeletonFrame(
      joints = joints,
      connections = SKELETON_CONNECTIONS,
      sourceWidth = imageWidth,
      sourceHeight = imageHeight,
      isPostureValid = isBackAligned,
      elbowAngleDegrees = smoothedElbowAngle,
      backAngleDegrees = smoothedBackAngle,
      phase = phase,
      repCount = repCount,
      averageFormScore = currentAverageScore(),
      feedbackMessage = feedback,
      justCountedRep = justCounted,
    )
  }

  private fun evaluateBackAlignment(
    shoulder: PoseLandmark,
    hip: PoseLandmark,
    ankle: PoseLandmark?,
  ): Pair<Float, Boolean> {
    if (ankle != null && ankle.inFrameLikelihood >= 0.45f) {
      val angle = calculateAngle(shoulder.position, hip.position, ankle.position)
      return angle to (angle >= minBackAlignmentDegrees)
    }
    // Fallback geometrico se le caviglie sono fuori inquadratura (tipico della camera frontale a terra):
    // valuta l'inclinazione del tronco spalla-anca rispetto all'orizzontale del pavimento.
    val dx = abs(hip.position.x - shoulder.position.x).coerceAtLeast(1f)
    val dy = abs(hip.position.y - shoulder.position.y)
    val tiltFromHorizontalDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val estimatedPlankAngle = (180f - (tiltFromHorizontalDeg - 22f).coerceAtLeast(0f)).coerceIn(90f, 180f)
    return estimatedPlankAngle to (tiltFromHorizontalDeg <= 50f)
  }

  private fun currentAverageScore(): Int {
    return if (formScores.isEmpty()) 100 else formScores.average().toInt()
  }

  private fun averageLikelihood(pose: Pose, vararg types: Int): Float {
    val scores = types.toList().mapNotNull { pose.getPoseLandmark(it)?.inFrameLikelihood }
    return if (scores.size == types.size) scores.average().toFloat() else 0f
  }

  private fun buildNormalizedJoints(
    pose: Pose,
    width: Float,
    height: Float,
    mirrorX: Boolean,
  ): Map<Int, NormalizedJoint> {
    val map = mutableMapOf<Int, NormalizedJoint>()
    for (landmark in pose.allPoseLandmarks) {
      val nx = (landmark.position.x / width)
      val ny = (landmark.position.y / height)
      map[landmark.landmarkType] = NormalizedJoint(
        x = if (mirrorX) 1f - nx else nx,
        y = ny,
        likelihood = landmark.inFrameLikelihood,
      )
    }
    return map
  }

  companion object {
    /**
     * Calcola l'angolo interno in gradi [0°, 180°] formato da tre punti 2D
     * centrato su [midPoint] tramite atan2.
     */
    fun calculateAngle(
      firstPoint: PointF,
      midPoint: PointF,
      lastPoint: PointF,
    ): Float {
      val radians =
        atan2((lastPoint.y - midPoint.y).toDouble(), (lastPoint.x - midPoint.x).toDouble()) -
          atan2((firstPoint.y - midPoint.y).toDouble(), (firstPoint.x - midPoint.x).toDouble())
      var degrees = abs(Math.toDegrees(radians)).toFloat()
      if (degrees > 180f) {
        degrees = 360f - degrees
      }
      return degrees
    }

    fun exponentialSmooth(previous: Float, current: Float, alpha: Float): Float {
      return previous * (1f - alpha) + current * alpha
    }

    val SKELETON_CONNECTIONS: List<Pair<Int, Int>> = listOf(
      PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
      PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
      PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
      PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
      PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
      PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
      PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
      PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
      PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
      PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
      PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
      PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
    )
  }
}
