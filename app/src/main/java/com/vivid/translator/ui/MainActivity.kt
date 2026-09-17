package com.vivid.translator.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivid.translator.service.ScreenCaptureForegroundService
import com.vivid.translator.ui.screens.HomeScreen
import com.vivid.translator.ui.theme.VividTheme

class MainActivity : ComponentActivity() {
    private val translatorViewModel: TranslatorViewModel by viewModels()

    private val captureConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { consentResult ->
        if (consentResult.resultCode == Activity.RESULT_OK && consentResult.data != null) {
            launchCaptureService(consentResult.resultCode, consentResult.data!!)
        } else {
            Toast.makeText(this, "Screen capture declined", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            requestCaptureConsent()
        } else {
            Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestCaptureConsent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VividTheme {
                val sourceLanguage by translatorViewModel.sourceLanguage.collectAsStateWithLifecycle()
                val targetLanguage by translatorViewModel.targetLanguage.collectAsStateWithLifecycle()
                val sessionActive by translatorViewModel.sessionActive.collectAsStateWithLifecycle()
                val pipelineState by translatorViewModel.pipelineState.collectAsStateWithLifecycle()
                val recognizedText by translatorViewModel.recognizedText.collectAsStateWithLifecycle()
                val translatedText by translatorViewModel.translatedText.collectAsStateWithLifecycle()
                val history by translatorViewModel.history.collectAsStateWithLifecycle()

                LaunchedEffect(recognizedText, translatedText) {
                    if (recognizedText.isNotBlank() && translatedText.isNotBlank()) {
                        translatorViewModel.appendHistory(
                            TranslationRecord(
                                sourceText = recognizedText,
                                translatedText = translatedText,
                                targetCode = targetLanguage.code
                            )
                        )
                    }
                }

                HomeScreen(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    languageCatalog = translatorViewModel.languageCatalogView,
                    sessionActive = sessionActive,
                    pipelineState = pipelineState,
                    recognizedText = recognizedText,
                    translatedText = translatedText,
                    history = history,
                    onSelectSource = translatorViewModel::selectSourceLanguage,
                    onSelectTarget = translatorViewModel::selectTargetLanguage,
                    onSwapLanguages = translatorViewModel::swapLanguages,
                    onStartSession = { beginSessionFlow() },
                    onStopSession = { endSessionFlow() },
                    onRetranslate = { requestRetranslate() }
                )
            }
        }
    }

    private fun beginSessionFlow() {
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayAccessLauncher.launch(overlayIntent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        requestCaptureConsent()
    }

    private fun requestCaptureConsent() {
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        captureConsentLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun launchCaptureService(resultCode: Int, resultData: Intent) {
        val serviceIntent = ScreenCaptureForegroundService.startIntent(
            this,
            resultCode,
            resultData,
            translatorViewModel.targetLanguage.value.code,
            translatorViewModel.sourceLanguage.value.code
        )
        ContextCompat.startForegroundService(this, serviceIntent)
        translatorViewModel.markSessionActive(true)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun endSessionFlow() {
        val stopIntent = Intent(this, ScreenCaptureForegroundService::class.java).apply {
            action = ScreenCaptureForegroundService.ActionStop
        }
        startService(stopIntent)
        translatorViewModel.markSessionActive(false)
    }

    private fun requestRetranslate() {
        val captureIntent = Intent(this, ScreenCaptureForegroundService::class.java).apply {
            action = ScreenCaptureForegroundService.ActionCaptureNow
        }
        startService(captureIntent)
    }
}
