package com.example.pose

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executor

/**
 * CameraX ImageAnalysis.Analyzer che esegue Google ML Kit Pose Detection 100% on-device in RAM.
 * Nessun fotogramma o video viene mai salvato su disco o trasmesso in rete.
 */
class PoseImageAnalyzer(
  private val detector: PushUpDetector,
  private val mainExecutor: Executor,
  @Volatile var isFrontCamera: Boolean = true,
  private val onFrameAnalyzed: (PoseSkeletonFrame) -> Unit,
) : ImageAnalysis.Analyzer {

  private val options =
    PoseDetectorOptions.Builder()
      .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
      .build()

  private val poseClient: PoseDetector = PoseDetection.getClient(options)

  @Volatile
  private var isClosed: Boolean = false

  @SuppressLint("UnsafeOptInUsageError")
  override fun analyze(imageProxy: ImageProxy) {
    if (isClosed) {
      imageProxy.close()
      return
    }
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
      imageProxy.close()
      return
    }

    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val inputImage =
      try {
        InputImage.fromMediaImage(mediaImage, rotationDegrees)
      } catch (e: Exception) {
        Log.w(TAG, "Impossibile creare InputImage dal frame", e)
        imageProxy.close()
        return
      }

    // Calcola le dimensioni effettive dopo la rotazione per mappare correttamente l'overlay
    val isRotated90Or270 = rotationDegrees == 90 || rotationDegrees == 270
    val effectiveWidth = if (isRotated90Or270) imageProxy.height else imageProxy.width
    val effectiveHeight = if (isRotated90Or270) imageProxy.width else imageProxy.height

    poseClient
      .process(inputImage)
      .addOnSuccessListener { pose ->
        if (isClosed) return@addOnSuccessListener
        val frame =
          detector.processPose(
            pose = pose,
            imageWidth = effectiveWidth,
            imageHeight = effectiveHeight,
            isFrontCamera = isFrontCamera,
          )
        mainExecutor.execute {
          if (!isClosed) {
            onFrameAnalyzed(frame)
          }
        }
      }
      .addOnFailureListener { error ->
        Log.w(TAG, "Errore ML Kit Pose frame", error)
      }
      .addOnCompleteListener {
        // Rilascia immediatamente il buffer RAM del fotogramma
        imageProxy.close()
      }
  }

  fun close() {
    isClosed = true
    runCatching { poseClient.close() }
  }

  private companion object {
    const val TAG = "PoseImageAnalyzer"
  }
}
