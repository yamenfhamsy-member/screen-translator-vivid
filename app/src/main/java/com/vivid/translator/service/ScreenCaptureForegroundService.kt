package com.vivid.translator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vivid.translator.core.capture.ScreenFrameCaptor
import com.vivid.translator.core.network.GtxTranslationClient
import com.vivid.translator.core.ocr.TextRecognitionEngine
import com.vivid.translator.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
            ActionDemoOverlay -> {
                startDemoSession()
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
        updatePipelineState(PipelineState.Starting)
        try {
            if (resultData == null) {
                updatePipelineState(PipelineState.ConsentMissing)
                return
            }
            val sessionNotification = buildSessionNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(SessionNotificationId, sessionNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(SessionNotificationId, sessionNotification)
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
            overlayManager?.show { runTranslateCycle() }
            updatePipelineState(PipelineState.Running)
            runTranslateCycle()
        } catch (failure: Exception) {
            stopCaptureSession()
            updatePipelineState(PipelineState.StartFailed)
            stopSelf()
        }
    }

    private fun runTranslateCycle() {
        serviceScope.launch(Dispatchers.IO) {
            updatePipelineState(PipelineState.Working)
            var screenBitmap: Bitmap? = null
            var captureAttempt = 0
            while (screenBitmap == null && captureAttempt < 6) {
                if (captureAttempt > 0) {
                    delay(400)
                }
                screenBitmap = frameCaptor?.captureFrame()
                captureAttempt += 1
            }
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
                renderOverlay("", "")
                updatePipelineState(PipelineState.Running)
                return@launch
            }
            TranslationBus.publishRecognized(recognizedText)
            val translationResult = translationClient.translate(recognizedText, activeTargetLanguage, activeSourceLanguage)
            translationResult
                .onSuccess { translatedText ->
                    TranslationBus.publishTranslated(translatedText)
                    renderOverlay(recognizedText, translatedText)
                    updatePipelineState(PipelineState.Running)
                }
                .onFailure {
                    TranslationBus.publishTranslated("")
                    renderOverlay(recognizedText, "")
                    updatePipelineState(PipelineState.NetworkFailed)
                }
        }
    }

    private fun renderOverlay(recognizedText: String, translatedText: String) {
        serviceScope.launch(Dispatchers.Main) {
            val manager = overlayManager ?: return@launch
            if (!manager.isAttached()) {
                manager.show { runTranslateCycle() }
            }
            manager.render(recognizedText, translatedText)
        }
    }

    private fun stopCaptureSession() {
        frameCaptor?.release()
        frameCaptor = null
        overlayManager?.dismiss()
        stopForeground(STOP_FOREGROUND_REMOVE)
        updatePipelineState(PipelineState.Idle)
    }

    private fun startDemoSession() {
        updatePipelineState(PipelineState.Starting)
        try {
            val sessionNotification = buildSessionNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(SessionNotificationId, sessionNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(SessionNotificationId, sessionNotification)
            }
            overlayManager?.show { runTranslateCycle() }
            overlayManager?.render("Floating overlay check", "بطاقة عائمة تجريبية")
            TranslationBus.publishRecognized("Floating overlay check")
            TranslationBus.publishTranslated("بطاقة عائمة تجريبية")
            updatePipelineState(PipelineState.Running)
        } catch (failure: Exception) {
            stopCaptureSession()
            updatePipelineState(PipelineState.StartFailed)
            stopSelf()
        }
    }

    private fun buildSessionNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, SessionChannelId)
            .setContentTitle("Vivid Translate")
            .setContentText("Screen translation running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openPending)
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
        Starting,
        Running,
        Working,
        ConsentMissing,
        StartFailed,
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
        const val ActionDemoOverlay = "com.vivid.translator.action.DEMO_OVERLAY"
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
