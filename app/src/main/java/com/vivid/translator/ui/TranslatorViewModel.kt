package com.vivid.translator.ui

import androidx.lifecycle.ViewModel
import com.vivid.translator.service.ScreenCaptureForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SupportedLanguage(val code: String, val displayName: String)

data class TranslationRecord(val sourceText: String, val translatedText: String, val targetCode: String)

class TranslatorViewModel : ViewModel() {
    private val languageCatalog = listOf(
        SupportedLanguage("auto", "Detect"),
        SupportedLanguage("en", "English"),
        SupportedLanguage("ar", "Arabic"),
        SupportedLanguage("fr", "French"),
        SupportedLanguage("de", "German"),
        SupportedLanguage("es", "Spanish"),
        SupportedLanguage("tr", "Turkish"),
        SupportedLanguage("ur", "Urdu")
    )

    private val sourceLanguageFlow = MutableStateFlow(languageCatalog.first())
    private val targetLanguageFlow = MutableStateFlow(languageCatalog[1])
    private val sessionActiveFlow = MutableStateFlow(false)
    private val historyFlow = MutableStateFlow<List<TranslationRecord>>(emptyList())

    val languageCatalogView: List<SupportedLanguage> = languageCatalog
    val sourceLanguage: StateFlow<SupportedLanguage> = sourceLanguageFlow.asStateFlow()
    val targetLanguage: StateFlow<SupportedLanguage> = targetLanguageFlow.asStateFlow()
    val sessionActive: StateFlow<Boolean> = sessionActiveFlow.asStateFlow()
    val history: StateFlow<List<TranslationRecord>> = historyFlow.asStateFlow()

    val recognizedText: StateFlow<String> = ScreenCaptureForegroundService.TranslationBus.recognizedText
    val translatedText: StateFlow<String> = ScreenCaptureForegroundService.TranslationBus.translatedText
    val pipelineState: StateFlow<ScreenCaptureForegroundService.PipelineState> =
        ScreenCaptureForegroundService.TranslationBus.pipelineState

    fun selectSourceLanguage(language: SupportedLanguage) {
        sourceLanguageFlow.value = language
    }

    fun selectTargetLanguage(language: SupportedLanguage) {
        if (language.code == "auto") return
        targetLanguageFlow.value = language
    }

    fun markSessionActive(active: Boolean) {
        sessionActiveFlow.value = active
    }

    fun swapLanguages() {
        val currentSource = sourceLanguageFlow.value
        val currentTarget = targetLanguageFlow.value
        if (currentSource.code == "auto") return
        sourceLanguageFlow.value = currentTarget
        targetLanguageFlow.value = currentSource
    }

    fun appendHistory(record: TranslationRecord) {
        historyFlow.value = listOf(record) + historyFlow.value.take(19)
    }
}
