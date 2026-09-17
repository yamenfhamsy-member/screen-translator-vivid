package com.vivid.translator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vivid.translator.ui.theme.AshBorder
import com.vivid.translator.ui.theme.BoneWhite
import com.vivid.translator.ui.theme.Obsidian

@Composable
fun TranslationOverlayCard(
    recognizedText: String,
    translatedText: String,
    onRetranslate: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val hostContext = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, AshBorder, RoundedCornerShape(0.dp))
            .background(Obsidian)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "VIVID TRANSLATE",
            style = MaterialTheme.typography.labelSmall
        )
        if (recognizedText.isBlank() && translatedText.isBlank()) {
            Text(
                text = "Point at foreign text, then tap retranslate.",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = recognizedText.ifBlank { "Listening for text" },
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = translatedText.ifBlank { "Translating" },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onRetranslate != null) {
                OutlinedButton(
                    onClick = onRetranslate,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Obsidian,
                        contentColor = BoneWhite
                    )
                ) {
                    Text(text = "RETRANSLATE", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (translatedText.isNotBlank()) {
                OutlinedButton(
                    onClick = { copyOverlayPayload(hostContext, translatedText) },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Obsidian,
                        contentColor = BoneWhite
                    )
                ) {
                    Text(text = "COPY", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (recognizedText.isNotBlank()) {
                OutlinedButton(
                    onClick = { copyOverlayPayload(hostContext, recognizedText) },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Obsidian,
                        contentColor = BoneWhite
                    )
                ) {
                    Text(text = "COPY SOURCE", style = MaterialTheme.typography.labelSmall)
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Obsidian,
                    contentColor = BoneWhite
                )
            ) {
                Text(text = "DISMISS", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun copyOverlayPayload(hostContext: Context, payload: String) {
    val clipboardManager = hostContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("VividTranslate", payload))
}
