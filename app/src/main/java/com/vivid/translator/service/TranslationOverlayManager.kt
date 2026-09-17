package com.vivid.translator.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vivid.translator.ui.screens.TranslationOverlayCard
import com.vivid.translator.ui.theme.VividTheme

class TranslationOverlayManager(private val hostContext: Context) {
    private val windowManager = hostContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var recognizedSnapshot = ""
    private var translatedSnapshot = ""
    private var attached = false

    fun show(onRetranslate: () -> Unit) {
        if (attached) {
            render(recognizedSnapshot, translatedSnapshot)
            return
        }
        val overlayOwner = OverlayLifecycleOwner()
        overlayOwner.handleCreate()
        val composeView = ComposeView(hostContext).apply {
            setViewTreeLifecycleOwner(overlayOwner)
            setViewTreeSavedStateRegistryOwner(overlayOwner)
            setContent {
                VividTheme {
                    TranslationOverlayCard(
                        recognizedText = recognizedSnapshot,
                        translatedText = translatedSnapshot,
                        onRetranslate = onRetranslate,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        windowManager.addView(composeView, overlayParams)
        overlayView = composeView
        attached = true
    }

    fun render(recognizedText: String, translatedText: String) {
        recognizedSnapshot = recognizedText
        translatedSnapshot = translatedText
        overlayView?.setContent {
            VividTheme {
                TranslationOverlayCard(
                    recognizedText = recognizedSnapshot,
                    translatedText = translatedSnapshot,
                    onRetranslate = null,
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    fun dismiss() {
        val activeView = overlayView ?: return
        if (attached) {
            windowManager.removeView(activeView)
        }
        overlayView = null
        attached = false
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performAttach()
        }

        fun handleCreate() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry
    }
}
