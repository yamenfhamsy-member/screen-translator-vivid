package com.vivid.translator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vivid.translator.core.capture.ScreenFrameCaptor
import com.vivid.translator.core.network.GtxTranslationClient
import com.vivid.translator.core.ocr.TextRecognitionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScreenCaptureForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val translationClient = GtxTranslationClient()
    private val recognitionEngine = TextRecognitionEngine()
    private var frameCaptor: ScreenFrameCaptor? = null
    private var overlayManager: TranslationOverlayManager? = null
    private var activeTargetLanguage = "en"
    private var activeSourceLanguage = "auto"

    override fun onCreate() {
        super.onCreate()
        overlayManager = TranslationOverlayManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStart -> {
                activeTargetLanguage = intent.getStringExtra(ExtraTargetLanguage) ?: "en"
                activeSourceLanguage = intent.getStringExtra(ExtraSourceLanguage) ?: "auto"
                val captureResultCode = intent.getIntExtra(ExtraResultCode, 0)
                val captureResultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(ExtraResultData, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(ExtraResultData)
                }
                startCaptureSession(captureResultCode, captureResultData)
            }
            ActionCaptureNow -> {
                runTranslateCycle()
            }
            ActionStop -> {
                stopCaptureSession()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCaptureSession()
        recognitionEngine.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCaptureSession(resultCode: Int, resultData: Intent?) {
        if (resultData == null) {
            updatePipelineState(PipelineState.ConsentMissing)
            return
        }
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
        val displayMetrics = resources.displayMetrics
        val captor = ScreenFrameCaptor(
            mediaProjection,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi
        )
        captor.start()
        frameCaptor = captor
        val sessionNotification = buildSessionNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SessionNotificationId, sessionNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(SessionNotificationId, sessionNotification)
        }
        overlayManager?.show { runTranslateCycle() }
        updatePipelineState(PipelineState.Running)
        runTranslateCycle()
    }

    private fun runTranslateCycle() {
        serviceScope.launch(Dispatchers.IO) {
            updatePipelineState(PipelineState.Working)
            val screenBitmap = frameCaptor?.captureFrame()
            if (screenBitmap == null) {
                updatePipelineState(PipelineState.Running)
                return@launch
            }
            val recognizedText = try {
                recognitionEngine.extractText(screenBitmap)
            } catch (failure: Exception) {
                updatePipelineState(PipelineState.OcrFailed)
                return@launch
            } finally {
                screenBitmap.recycle()
            }
            if (recognizedText.isBlank()) {
                TranslationBus.publishRecognized("")
                TranslationBus.publishTranslated("")
                overlayManager?.render("", "")
                updatePipelineState(PipelineState.Running)
                return@launch
            }
            TranslationBus.publishRecognized(recognizedText)
            val translationResult = translationClient.translate(recognizedText, activeTargetLanguage, activeSourceLanguage)
            translationResult
                .onSuccess { translatedText ->
                    TranslationBus.publishTranslated(translatedText)
                    overlayManager?.render(recognizedText, translatedText)
                    updatePipelineState(PipelineState.Running)
                }
                .onFailure {
                    TranslationBus.publishTranslated("")
                    overlayManager?.render(recognizedText, "")
                    updatePipelineState(PipelineState.NetworkFailed)
                }
        }
    }

    private fun stopCaptureSession() {
        frameCaptor?.release()
        frameCaptor = null
        overlayManager?.dismiss()
        stopForeground(STOP_FOREGROUND_REMOVE)
        updatePipelineState(PipelineState.Idle)
    }

    private fun buildSessionNotification(): Notification {
        return NotificationCompat.Builder(this, SessionChannelId)
            .setContentTitle("Vivid Translate")
            .setContentText("Screen translation running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sessionChannel = NotificationChannel(
            SessionChannelId,
            "Screen translation",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(sessionChannel)
    }

    private fun updatePipelineState(state: PipelineState) {
        TranslationBus.publishState(state)
    }

    enum class PipelineState {
        Idle,
        Running,
        Working,
        ConsentMissing,
        OcrFailed,
        NetworkFailed
    }

    object TranslationBus {
        private val recognizedTextFlow = MutableStateFlow("")
        private val translatedTextFlow = MutableStateFlow("")
        private val pipelineStateFlow = MutableStateFlow(PipelineState.Idle)

        val recognizedText: StateFlow<String> = recognizedTextFlow.asStateFlow()
        val translatedText: StateFlow<String> = translatedTextFlow.asStateFlow()
        val pipelineState: StateFlow<PipelineState> = pipelineStateFlow.asStateFlow()

        fun publishRecognized(value: String) {
            recognizedTextFlow.value = value
        }

        fun publishTranslated(value: String) {
            translatedTextFlow.value = value
        }

        fun publishState(value: PipelineState) {
            pipelineStateFlow.value = value
        }
    }

    companion object {
        const val ActionStart = "com.vivid.translator.action.START"
        const val ActionCaptureNow = "com.vivid.translator.action.CAPTURE_NOW"
        const val ActionStop = "com.vivid.translator.action.STOP"
        const val ExtraResultCode = "extra_result_code"
        const val ExtraResultData = "extra_result_data"
        const val ExtraTargetLanguage = "extra_target_language"
        const val ExtraSourceLanguage = "extra_source_language"
        const val SessionChannelId = "vivid_capture"
        const val SessionNotificationId = 1001

        fun startIntent(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            targetLanguage: String,
            sourceLanguage: String
        ): Intent {
            return Intent(context, ScreenCaptureForegroundService::class.java).apply {
                action = ActionStart
                putExtra(ExtraResultCode, resultCode)
                putExtra(ExtraResultData, resultData)
                putExtra(ExtraTargetLanguage, targetLanguage)
                putExtra(ExtraSourceLanguage, sourceLanguage)
            }
        }
    }
}
