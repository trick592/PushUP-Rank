package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.AuthManager
import com.example.ui.theme.ArenaBlack

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onContinueAsGuest: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    AuthManager.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = Modifier.fillMaxSize().background(ArenaBlack),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_dashboard_hero),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alpha = 0.35f,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                ArenaBlack.copy(alpha = 0.5f),
                ArenaBlack.copy(alpha = 0.92f),
                ArenaBlack,
              )
            )
          )
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .widthIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
          Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = "Logo PushUp Rank",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(18.dp).size(42.dp),
          )
        }

        Text(
          text = "PUSHUP RANK",
          style = MaterialTheme.typography.displayMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Text(
          text = "Trasforma ogni flessione in punti, grado competitivo e PushCoin con AI Vision 100% on-device.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureHighlightRow(
          icon = Icons.Filled.Psychology,
          title = "AI Pose Detection in Locale",
          subtitle = "Analisi gomiti e schiena in tempo reale con ML Kit senza salvare video.",
        )
        FeatureHighlightRow(
          icon = Icons.Filled.Lock,
          title = "Classifiche & Anti-Cheat Cloud",
          subtitle = "Sfida amici e scala i gradi da Bronzo a Maestro su Cloud Firestore.",
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage ?: "",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            AuthManager.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading,
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 56.dp)
              .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(16.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
          } else {
            Text(
              text = "Accedi con Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        OutlinedButton(
          onClick = onContinueAsGuest,
          enabled = !isLoading,
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 52.dp)
              .testTag("guest_access_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text(
            text = "Continua come Ospite",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Text(
          text = "Accedi con Google per classifiche e salvataggio cloud, oppure usa la modalità Ospite in locale.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(10.dp).size(24.dp),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun MedicalDisclaimerScreen(
  onAccepted: () -> Unit,
) {
  var checked by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .widthIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Icon(
            imageVector = Icons.Filled.HealthAndSafety,
            contentDescription = "Avvertenza sanitaria",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(14.dp).size(36.dp),
          )
        }

        Text(
          text = "Idoneità Fisica e Disclaimer Medico",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          shape = RoundedCornerShape(18.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "PushUp Rank è un'app di supporto sportivo e gamificazione e NON fornisce diagnosi, terapie o consulenze mediche.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "• Prima di iniziare attività fisica ad alta intensità consulta il tuo medico, soprattutto in caso di dolori articolari, patologie cardiovascolari o infortuni pregressi.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "• Interrompi immediatamente l'allenamento se avverti vertigini, affanno anomalo o dolore acuto a polsi, gomiti, spalle o zona lombare.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { checked = !checked }
              .padding(vertical = 8.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Checkbox(
            checked = checked,
            onCheckedChange = { checked = it },
            modifier = Modifier.testTag("medical_checkbox"),
          )
          Text(
            text = "Dichiaro di essere fisicamente idoneo all'attività sportiva e accetto che l'app non sostituisce un parere medico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      Button(
        onClick = onAccepted,
        enabled = checked,
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = 24.dp)
            .heightIn(min = 56.dp)
            .testTag("accept_medical_disclaimer_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(
          text = "Accetta e Continua",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun CameraPrivacyConsentDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.Videocam,
        contentDescription = "Privacy fotocamera",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
    },
    title = {
      Text(
        text = "Privacy della Fotocamera (100% On-Device)",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Per contare le tue flessioni e valutare la postura della schiena in tempo reale, PushUp Rank utilizza la fotocamera frontale con Google ML Kit.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "• Nessun video o immagine viene salvato nella memoria del telefono.\n• Nessun flusso video lascia il dispositivo o viene inviato ai server.\n• Ogni fotogramma viene analizzato nella RAM e scartato istantaneamente.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        modifier = Modifier.testTag("accept_camera_consent_button"),
      ) {
        Text("Accetta e Attiva Fotocamera")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("decline_camera_consent_button"),
      ) {
        Text("Annulla")
      }
    },
  )
}
