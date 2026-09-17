package com.vivid.translator.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vivid.translator.service.ScreenCaptureForegroundService
import com.vivid.translator.ui.SupportedLanguage
import com.vivid.translator.ui.TranslationRecord
import com.vivid.translator.ui.theme.AshBorder
import com.vivid.translator.ui.theme.BoneWhite
import com.vivid.translator.ui.theme.Obsidian

@Composable
fun HomeScreen(
    sourceLanguage: SupportedLanguage,
    targetLanguage: SupportedLanguage,
    languageCatalog: List<SupportedLanguage>,
    sessionActive: Boolean,
    pipelineState: ScreenCaptureForegroundService.PipelineState,
    recognizedText: String,
    translatedText: String,
    history: List<TranslationRecord>,
    onSelectSource: (SupportedLanguage) -> Unit,
    onSelectTarget: (SupportedLanguage) -> Unit,
    onSwapLanguages: () -> Unit,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onRetranslate: () -> Unit,
    onOpenHome: () -> Unit
) {
    var contactSheetOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TopBarRow(onContact = { contactSheetOpen = true })
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "BUILD V1.2", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(24.dp))
        HeroSection()
        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = AshBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))
        SessionControls(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            languageCatalog = languageCatalog,
            sessionActive = sessionActive,
            pipelineState = pipelineState,
            onSelectSource = onSelectSource,
            onSelectTarget = onSelectTarget,
            onSwapLanguages = onSwapLanguages,
            onStartSession = onStartSession,
            onStopSession = onStopSession,
            onRetranslate = onRetranslate,
            onOpenHome = onOpenHome
        )
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = AshBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))
        TranslationPreview(recognizedText = recognizedText, translatedText = translatedText)
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = AshBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        HistorySection(history = history)
    }
    if (contactSheetOpen) {
        ContactDialog(onClose = { contactSheetOpen = false })
    }
}

@Composable
private fun TopBarRow(onContact: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "VIVID+CO", style = MaterialTheme.typography.labelSmall)
        OutlinedButton(
            onClick = onContact,
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Obsidian,
                contentColor = BoneWhite
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Text(text = "CONTACT", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrismArtifact()
        Text(
            text = "Translate any screen instantly",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Capture on-screen text and read it in your language, floating above every app.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SessionControls(
    sourceLanguage: SupportedLanguage,
    targetLanguage: SupportedLanguage,
    languageCatalog: List<SupportedLanguage>,
    sessionActive: Boolean,
    pipelineState: ScreenCaptureForegroundService.PipelineState,
    onSelectSource: (SupportedLanguage) -> Unit,
    onSelectTarget: (SupportedLanguage) -> Unit,
    onSwapLanguages: () -> Unit,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onRetranslate: () -> Unit,
    onOpenHome: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguagePicker(
                label = sourceLanguage.displayName,
                options = languageCatalog,
                onPick = onSelectSource,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onSwapLanguages,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Obsidian,
                    contentColor = BoneWhite
                )
            ) {
                Text(text = "SWAP", style = MaterialTheme.typography.labelSmall)
            }
            LanguagePicker(
                label = targetLanguage.displayName,
                options = languageCatalog.filter { candidate -> candidate.code != "auto" },
                onPick = onSelectTarget,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = pipelineStatusLine(sessionActive, pipelineState),
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sessionActive) {
                VividOutlineAction(label = "STOP", onClick = onStopSession, modifier = Modifier.weight(1f))
                VividOutlineAction(label = "RETRANSLATE", onClick = onRetranslate, modifier = Modifier.weight(1f))
            } else {
                VividOutlineAction(label = "START TRANSLATING", onClick = onStartSession, modifier = Modifier.weight(1f))
            }
        }
        if (sessionActive) {
            VividOutlineAction(label = "OPEN HOME — FLOAT OVER APPS", onClick = onOpenHome, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LanguagePicker(
    label: String,
    options: List<SupportedLanguage>,
    onPick: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Obsidian,
                contentColor = BoneWhite
            )
        ) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        onPick(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VividOutlineAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Obsidian,
            contentColor = BoneWhite
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TranslationPreview(recognizedText: String, translatedText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "LIVE RESULT", style = MaterialTheme.typography.labelSmall)
        Text(
            text = recognizedText.ifBlank { "Nothing captured yet" },
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = translatedText.ifBlank { "Translation appears here" },
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun HistorySection(history: List<TranslationRecord>) {
    Column {
        Text(text = "RECENT TRANSLATIONS", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text(
                text = "No translations yet",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        history.forEach { record ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(text = record.translatedText, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.sourceText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.targetCode.uppercase(),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            HorizontalDivider(color = AshBorder, thickness = 1.dp)
        }
    }
}

private fun pipelineStatusLine(
    sessionActive: Boolean,
    pipelineState: ScreenCaptureForegroundService.PipelineState
): String {
    if (!sessionActive) return "Overlay idle — start a session to float translations."
    return when (pipelineState) {
        ScreenCaptureForegroundService.PipelineState.Idle -> "Session ready."
        ScreenCaptureForegroundService.PipelineState.Starting -> "Starting capture session."
        ScreenCaptureForegroundService.PipelineState.Running -> "Session live — overlay floating."
        ScreenCaptureForegroundService.PipelineState.Working -> "Reading screen text."
        ScreenCaptureForegroundService.PipelineState.ConsentMissing -> "Capture permission missing — restart session."
        ScreenCaptureForegroundService.PipelineState.StartFailed -> "Session failed to start — press START and accept every prompt."
        ScreenCaptureForegroundService.PipelineState.OcrFailed -> "Text recognition failed — retry."
        ScreenCaptureForegroundService.PipelineState.NetworkFailed -> "Translation unreachable — check connection."
    }
}

@Composable
private fun ContactDialog(onClose: () -> Unit) {
    val hostContext = LocalContext.current
    Dialog(onDismissRequest = onClose) {
        Surface(
            color = Obsidian,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.border(1.dp, AshBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "CONTACT", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "Reach us on Telegram",
                    style = MaterialTheme.typography.titleLarge
                )
                ContactRow(
                    handle = "thorfin963",
                    onOpen = { openTelegramProfile(hostContext, "thorfin963") }
                )
                HorizontalDivider(color = AshBorder, thickness = 1.dp)
                ContactRow(
                    handle = "ya_ali963",
                    onOpen = { openTelegramProfile(hostContext, "ya_ali963") }
                )
                HorizontalDivider(color = AshBorder, thickness = 1.dp)
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Obsidian,
                        contentColor = BoneWhite
                    )
                ) {
                    Text(text = "CLOSE", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ContactRow(handle: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "@" + handle, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onOpen,
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Obsidian,
                contentColor = BoneWhite
            )
        ) {
            Text(text = "TELEGRAM", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun openTelegramProfile(hostContext: Context, profileName: String) {
    val profileUri = Uri.parse("https://t.me/" + profileName)
    val viewIntent = Intent(Intent.ACTION_VIEW, profileUri)
    hostContext.startActivity(viewIntent)
}
