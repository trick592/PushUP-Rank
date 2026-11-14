package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {
  private const val TAG = "PushUpAuth"

  fun authStateFlow(auth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return
    }
    val clientId =
      try {
        context.getString(R.string.default_web_client_id)
      } catch (e: Exception) {
        onUnauthenticated()
        return
      }

    val googleIdOption =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
  ) {
    val clientId =
      try {
        context.getString(R.string.default_web_client_id)
      } catch (e: Exception) {
        onAuthError("Configurazione Google Sign-In mancante: default_web_client_id non trovato.")
        return
      }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
      try {
        val activity = context as? Activity
        if (activity == null) {
          onAuthError("Contesto Activity non disponibile per l'accesso.")
          return@launch
        }
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError("Tipo di credenziale non supportato.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Accesso con Google non riuscito.")
      }
    }
  }

  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
          onSignOutComplete()
        }
      }
    }
  }
}
