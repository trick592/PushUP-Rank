package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.ShopCatalog
import com.example.pose.NormalizedJoint
import com.example.pose.PoseImageAnalyzer
import com.example.pose.PoseSkeletonFrame
import com.example.pose.PushUpDetector
import com.example.pose.PushUpPhase
import com.example.ui.components.PushCoinChip
import com.example.ui.theme.ArenaBlack
import com.example.ui.theme.PostureGreen
import com.example.ui.theme.PostureRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "WorkoutCameraScreen"

@Composable
fun WorkoutCameraScreen(
  selectedSkeletonEffectId: String,
  activeBoostId: String = "",
  challengeTitle: String? = null,
  targetReps: Long? = null,
  timeLimitSeconds: Long? = null,
  soundEnabled: Boolean,
  onToggleSound: () -> Unit,
  onSaveSession: (reps: Long, durationSeconds: Long, averageFormScore: Long) -> Unit,
  onCancel: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  // Aggiorna lo stato del permesso se l'utente rientra dalle Impostazioni di sistema
  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasCameraPermission =
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val detector = remember { PushUpDetector() }
  var poseFrame by remember { mutableStateOf(PoseSkeletonFrame()) }
  var elapsedSeconds by remember { mutableLongStateOf(0L) }
  var showExitConfirm by remember { mutableStateOf(false) }
  var timerAutoSaved by remember { mutableStateOf(false) }
  var timeExpiredZeroReps by remember { mutableStateOf(false) }
  var preferFrontCamera by remember { mutableStateOf(true) }
  var hasMultipleCameras by remember { mutableStateOf(false) }
  var cameraError by remember { mutableStateOf<String?>(null) }
  var retryNonce by remember { mutableIntStateOf(0) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

  val activeBoostItem = remember(activeBoostId) {
    activeBoostId.takeIf { it.isNotBlank() }?.let { ShopCatalog.findById(it) }
  }

  val toneGenerator = remember {
    runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()
  }
  DisposableEffect(Unit) {
    onDispose { toneGenerator?.release() }
  }

  LaunchedEffect(hasCameraPermission, cameraError, timeLimitSeconds) {
    if (hasCameraPermission && cameraError == null) {
      while (true) {
        delay(1000L)
        elapsedSeconds += 1L
        if (timeLimitSeconds != null && elapsedSeconds >= timeLimitSeconds && !timerAutoSaved) {
          timerAutoSaved = true
          if (poseFrame.repCount > 0) {
            onSaveSession(
              poseFrame.repCount.toLong(),
              elapsedSeconds.coerceAtLeast(2L),
              poseFrame.averageFormScore.toLong(),
            )
          } else {
            timeExpiredZeroReps = true
          }
          break
        }
      }
    }
  }

  LaunchedEffect(poseFrame.repCount, poseFrame.phase) {
    if (!soundEnabled || toneGenerator == null) return@LaunchedEffect
    if (poseFrame.justCountedRep) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    } else if (poseFrame.phase == PushUpPhase.POSTURE_ERROR) {
      toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 90)
    }
  }

  BackHandler {
    if (poseFrame.repCount > 0) {
      showExitConfirm = true
    } else {
      onCancel()
    }
  }

  if (showExitConfirm) {
    AlertDialog(
      onDismissRequest = { showExitConfirm = false },
      title = { Text("Terminare la sessione?") },
      text = {
        Text("Hai completato ${poseFrame.repCount} flessioni valide. Vuoi salvarle e ottenere ${poseFrame.repCount} PushCoin?")
      },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirm = false
            onSaveSession(
              poseFrame.repCount.toLong(),
              elapsedSeconds.coerceAtLeast(2L),
              poseFrame.averageFormScore.toLong(),
            )
          }
        ) {
          Text("Salva Sessione")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExitConfirm = false; onCancel() }) {
          Text("Scarta")
        }
      },
    )
  }

  Box(modifier = Modifier.fillMaxSize().background(ArenaBlack)) {
    if (!hasCameraPermission) {
      Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.VideocamOff,
          contentDescription = "Permesso fotocamera mancante",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Permesso Fotocamera Necessario",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Consenti l'accesso alla fotocamera per il tracciamento AI on-device in tempo reale.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("request_camera_permission_button"),
        ) {
          Text("Consenti Fotocamera")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
          onClick = {
            val intent =
              Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
              }
            context.startActivity(intent)
          },
          modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
          Text("Apri Impostazioni App")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
          onClick = onCancel,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
          Text("Torna Indietro")
        }
      }
      return@Box
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val poseAnalyzer = remember(detector, mainExecutor) {
      PoseImageAnalyzer(
        detector = detector,
        mainExecutor = mainExecutor,
        isFrontCamera = preferFrontCamera,
        onFrameAnalyzed = { frame -> poseFrame = frame },
      )
    }

    // Rilascio ordinato: prima unbindAll() di CameraX, poi chiusura di ML Kit e dell'executor
    DisposableEffect(lifecycleOwner) {
      onDispose {
        runCatching { cameraProviderRef?.unbindAll() }
        poseAnalyzer.close()
        analyzerExecutor.shutdown()
      }
    }

    // Binding reattivo CameraX legato a PreviewView, permesso, switch camera e retry
    LaunchedEffect(previewViewRef, hasCameraPermission, preferFrontCamera, retryNonce) {
      val previewView = previewViewRef ?: return@LaunchedEffect
      cameraError = null
      try {
        val provider = awaitCameraProvider(context)
        cameraProviderRef = provider

        val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        hasMultipleCameras = hasFront && hasBack

        val primaryChoice: Pair<CameraSelector, Boolean>? =
          when {
            preferFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA to true
            !preferFrontCamera && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA to false
            hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA to true
            hasBack -> CameraSelector.DEFAULT_BACK_CAMERA to false
            provider.availableCameraInfos.isNotEmpty() -> {
              provider.availableCameraInfos.first().cameraSelector to false
            }
            else -> null
          }

        if (primaryChoice == null) {
          cameraError = "Nessuna fotocamera rilevata su questo dispositivo o emulatore."
          return@LaunchedEffect
        }

        val previewResolutionSelector =
          ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

        val analysisResolutionSelector =
          ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
              ResolutionStrategy(
                Size(640, 480),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
              )
            )
            .build()

        fun bindUseCases(selector: CameraSelector, isFront: Boolean) {
          poseAnalyzer.isFrontCamera = isFront
          val preview =
            Preview.Builder()
              .setResolutionSelector(previewResolutionSelector)
              .build()
              .also { it.surfaceProvider = previewView.surfaceProvider }

          val imageAnalysis =
            ImageAnalysis.Builder()
              .setResolutionSelector(analysisResolutionSelector)
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
              .also { it.setAnalyzer(analyzerExecutor, poseAnalyzer) }

          provider.unbindAll()
          provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageAnalysis,
          )
        }

        try {
          bindUseCases(primaryChoice.first, primaryChoice.second)
        } catch (primaryError: Exception) {
          Log.w(TAG, "Binding fotocamera primaria fallito, provo fallback", primaryError)
          val fallback =
            when {
              primaryChoice.second && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA to false
              !primaryChoice.second && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA to true
              else -> null
            }
          if (fallback != null) {
            preferFrontCamera = fallback.second
            bindUseCases(fallback.first, fallback.second)
          } else {
            throw primaryError
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Impossibile avviare la fotocamera CameraX", e)
        cameraError = e.localizedMessage ?: "Errore imprevisto durante l'avvio della fotocamera."
      }
    }

    // Anteprima CameraX (COMPATIBLE TextureView garantisce composizione corretta con l'overlay Canvas)
    AndroidView(
      factory = { ctx ->
        PreviewView(ctx).apply {
          keepScreenOn = true
          scaleType = PreviewView.ScaleType.FILL_CENTER
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          previewViewRef = this
        }
      },
      update = { view ->
        view.keepScreenOn = true
        if (previewViewRef !== view) {
          previewViewRef = view
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Overlay Scheletro AI allineato esattamente al crop FILL_CENTER della PreviewView
    val shopEffect = ShopCatalog.findById(selectedSkeletonEffectId)
    val validLineColor = shopEffect?.primaryColor ?: PostureGreen
    val validJointColor = shopEffect?.secondaryColor ?: Color.White
    val activeLineColor = if (poseFrame.isPostureValid) validLineColor else PostureRed

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      val srcW = poseFrame.sourceWidth.takeIf { it > 0 }?.toFloat() ?: w
      val srcH = poseFrame.sourceHeight.takeIf { it > 0 }?.toFloat() ?: h
      val scale = maxOf(w / srcW, h / srcH)
      val scaledW = srcW * scale
      val scaledH = srcH * scale
      val offsetX = (w - scaledW) / 2f
      val offsetY = (h - scaledH) / 2f

      fun mapToCanvas(joint: NormalizedJoint): Offset {
        return Offset(
          x = joint.x * scaledW + offsetX,
          y = joint.y * scaledH + offsetY,
        )
      }

      val joints = poseFrame.joints

      for ((startType, endType) in poseFrame.connections) {
        val start = joints[startType]
        val end = joints[endType]
        if (start != null && end != null && start.likelihood >= 0.45f && end.likelihood >= 0.45f) {
          val p1 = mapToCanvas(start)
          val p2 = mapToCanvas(end)
          if (selectedSkeletonEffectId == "skel_plasma" || selectedSkeletonEffectId == "skel_gold") {
            drawLine(
              color = activeLineColor.copy(alpha = 0.28f),
              start = p1,
              end = p2,
              strokeWidth = 22f,
              cap = StrokeCap.Round,
            )
          }
          drawLine(
            color = activeLineColor,
            start = p1,
            end = p2,
            strokeWidth = 9f,
            cap = StrokeCap.Round,
          )
        }
      }

      joints.values
        .filter { it.likelihood >= 0.45f }
        .forEach { joint ->
          val center = mapToCanvas(joint)
          drawCircle(
            color = if (poseFrame.isPostureValid) validJointColor else PostureRed,
            radius = 12f,
            center = center,
          )
          drawCircle(
            color = ArenaBlack,
            radius = 5f,
            center = center,
          )
        }
    }

    if (cameraError != null) {
      Box(
        modifier = Modifier.fillMaxSize().background(ArenaBlack.copy(alpha = 0.85f)).padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.VideocamOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = "Fotocamera non disponibile",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
            )
            Text(
              text = cameraError.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Button(
              onClick = { retryNonce += 1 },
              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("retry_camera_button"),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null)
              Spacer(modifier = Modifier.size(8.dp))
              Text("Riprova Fotocamera")
            }
            if (hasMultipleCameras) {
              OutlinedButton(
                onClick = { preferFrontCamera = !preferFrontCamera },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
              ) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Prova Altra Fotocamera")
              }
            }
            TextButton(
              onClick = onCancel,
              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
              Text("Torna Indietro")
            }
          }
        }
      }
      return@Box
    }

    // HUD Superiore e Controlli Inferiori
    Column(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = ArenaBlack.copy(alpha = 0.72f),
            shape = RoundedCornerShape(16.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "REP ${poseFrame.repCount}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("live_rep_counter"),
              )
              PushCoinChip(coins = poseFrame.repCount.toLong())
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasMultipleCameras) {
              Surface(
                color = ArenaBlack.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp),
              ) {
                IconButton(
                  onClick = { preferFrontCamera = !preferFrontCamera },
                  modifier = Modifier.size(48.dp).testTag("switch_camera_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Cambia fotocamera",
                    tint = Color.White,
                  )
                }
              }
            }
            Surface(
              color = ArenaBlack.copy(alpha = 0.72f),
              shape = RoundedCornerShape(14.dp),
            ) {
              IconButton(
                onClick = onToggleSound,
                modifier = Modifier.size(48.dp).testTag("toggle_sound_button"),
              ) {
                Icon(
                  imageVector =
                    if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp
                    else Icons.AutoMirrored.Filled.VolumeOff,
                  contentDescription = if (soundEnabled) "Disattiva audio" else "Attiva audio",
                  tint = Color.White,
                )
              }
            }
            Surface(
              color = ArenaBlack.copy(alpha = 0.72f),
              shape = RoundedCornerShape(14.dp),
            ) {
              IconButton(
                onClick = {
                  if (poseFrame.repCount > 0) showExitConfirm = true else onCancel()
                },
                modifier = Modifier.size(48.dp).testTag("cancel_workout_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Chiudi allenamento",
                  tint = Color.White,
                )
              }
            }
          }
        }

        // Banner Sfida Arena / Boost Attivo (se presenti)
        if (challengeTitle != null || activeBoostItem != null) {
          Surface(
            color = ArenaBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (challengeTitle != null) {
                val targetLabel = if (targetReps != null) " • ${poseFrame.repCount}/$targetReps rep" else ""
                Text(
                  text = "$challengeTitle$targetLabel",
                  style = MaterialTheme.typography.labelLarge,
                  color =
                    if (targetReps != null && poseFrame.repCount >= targetReps) PostureGreen
                    else MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
              if (activeBoostItem != null) {
                Text(
                  text = "Boost: ${activeBoostItem.name}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFFD54F),
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        // Telemetria angoli e feedback postura in tempo reale
        Surface(
          color =
            if (poseFrame.phase == PushUpPhase.POSTURE_ERROR || timeExpiredZeroReps) {
              PostureRed.copy(alpha = 0.85f)
            } else {
              ArenaBlack.copy(alpha = 0.72f)
            },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Gomito: ${poseFrame.elbowAngleDegrees.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
              Text(
                text = "Schiena: ${poseFrame.backAngleDegrees.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
              val shownSeconds =
                if (timeLimitSeconds != null) (timeLimitSeconds - elapsedSeconds).coerceAtLeast(0L)
                else elapsedSeconds
              val timerPrefix = if (timeLimitSeconds != null) "Resta " else ""
              Text(
                text = "$timerPrefix${shownSeconds / 60}:${(shownSeconds % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text =
                if (timeExpiredZeroReps) "Tempo scaduto con 0 flessioni. Riprova la sfida!"
                else poseFrame.feedbackMessage,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // Barra inferiore Salva Sessione
      Button(
        onClick = {
          if (poseFrame.repCount > 0) {
            onSaveSession(
              poseFrame.repCount.toLong(),
              elapsedSeconds.coerceAtLeast(2L),
              poseFrame.averageFormScore.toLong(),
            )
          } else {
            onCancel()
          }
        },
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 58.dp)
            .testTag("finish_workout_button"),
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor =
              if (poseFrame.repCount > 0) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor =
              if (poseFrame.repCount > 0) MaterialTheme.colorScheme.onPrimary
              else Color.White,
          ),
      ) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text =
            if (poseFrame.repCount > 0) {
              "Termina e Salva (+${poseFrame.repCount} PushCoin)"
            } else {
              "Termina Allenamento"
            },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

private suspend fun awaitCameraProvider(context: android.content.Context): ProcessCameraProvider {
  val future = ProcessCameraProvider.getInstance(context)
  return suspendCancellableCoroutine { continuation ->
    future.addListener(
      {
        try {
          continuation.resume(future.get())
        } catch (e: Exception) {
          continuation.resumeWithException(e)
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }
}
