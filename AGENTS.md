# Autonomous Agent Instruction: Screen Translator Android App

## Role and Objective
You are an elite Android Systems Engineer and UI Architect. Your objective is to build a complete, production-ready native Android screen translation application from scratch, apply the design specifications defined in `design.md`, push the repository to a new GitHub repository, and establish a GitHub Actions workflow to build the APK automatically.

---

## Strict Development Rules
1. **Zero Code Comments:** Under no circumstances should you generate single-line (`//`) or multi-line (`/* */`) comments in any source code file (Kotlin, Gradle, or XML). The code must be self-documenting through precise, expressive naming and clean separation of concerns.
2. **No Placeholder / Random Naming:** Banned names include `temp`, `foo`, `test`, `data1`, `dummy`, or generic arbitrary IDs. All classes, variables, and functions must have deterministic domain-specific names.
3. **Design Conformity:** Before writing UI code, inspect and parse `design.md`. Adhere strictly to the typography, color tokens, and architectural constraints specified within it.
4. **Clean Code & Architecture:** Structure the project using clean modular patterns:
   - `core/network`: Translation network layer.
   - `core/ocr`: Text recognition extraction engine.
   - `service`: Foreground service, MediaProjection capture, and overlay management.
   - `ui/theme`: Design tokens, typography, and color definitions.
   - `ui/screens`: Compose screens and interactive overlays.

---

## Step-by-Step Execution Plan

### Step 1: Read and Parse Design Specs
* Read `design.md` from the project root.
* Extract typography hierarchies (Neue Montreal or system sans-serif fallback, weight 400 enforcement, 52sp Display, no drop-shadows, 0dp border-radius default).
* Apply color tokens: Obsidian `#101010`, Bone White `#FFFDF9`, Graphite Veil `#495764`, Ash Border `#403F3F`, Fog Blue `#6F879C`, and chromatic prism accents `#FF2A2A`, `#2A7FFF`, `#2AFF2A`.

### Step 2: Project Scaffolding
Initialize an Android application with Gradle Kotlin DSL (`build.gradle.kts`):
* `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
* Jetpack Compose enabled with Compose Compiler
* Dependencies:
  - `com.squareup.okhttp3:okhttp:4.12.0`
  - `com.google.mlkit:text-recognition:16.0.1`
  - `androidx.core:core-ktx:1.13.1`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.8.4`
  - `androidx.activity:activity-compose:1.9.1`
  - `androidx.compose.ui:ui`
  - `androidx.compose.material3:material3`

### Step 3: Core Implementation Blueprints

#### 1. Network Layer (Google GTX Engine)
File: `app/src/main/java/com/vivid/translator/core/network/GtxTranslationClient.kt`
```kotlin
package com.vivid.translator.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

class GtxTranslationClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun translate(
        queryText: String,
        targetLanguage: String,
        sourceLanguage: String = "auto"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (queryText.isBlank()) return@withContext Result.success("")

        val requestUrl = HttpUrl.Builder()
            .scheme("https")
            .host("translate.googleapis.com")
            .addPathSegment("translate_a")
            .addPathSegment("single")
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", sourceLanguage)
            .addQueryParameter("tl", targetLanguage)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", queryText)
            .build()

        val httpRequest = Request.Builder()
            .url(requestUrl)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }
                val rawBody = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                val translatedText = parseGtxResponse(rawBody)
                Result.success(translatedText)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun parseGtxResponse(rawJson: String): String {
        val rootArray = JSONArray(rawJson)
        val sentencesArray = rootArray.optJSONArray(0) ?: return ""
        val stringBuilder = StringBuilder()
        for (i in 0 until sentencesArray.length()) {
            val sentenceBlock = sentencesArray.optJSONArray(i)
            val translatedSegment = sentenceBlock?.optString(0)
            if (!translatedSegment.isNullOrEmpty()) {
                stringBuilder.append(translatedSegment)
            }
        }
        return stringBuilder.toString()
    }
}

2. OCR Engine (ML Kit)
File: app/src/main/java/com/vivid/translator/core/ocr/TextRecognitionEngine.kt
package com.vivid.translator.core.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TextRecognitionEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText.text)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}

3. Android Manifest & Permissions
File: app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Vivid Translate"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <service
            android:name=".service.ScreenCaptureForegroundService"
            android:foregroundServiceType="mediaProjection"
            android:exported="false" />

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>

4. Design Theme Tokens
File: app/src/main/java/com/vivid/translator/ui/theme/Color.kt
package com.vivid.translator.ui.theme

import androidx.compose.ui.graphics.Color

val Obsidian = Color(0xFF101010)
val GraphiteVeil = Color(0xFF495764)
val BoneWhite = Color(0xFFFFFDF9)
val AshBorder = Color(0xFF403F3F)
val FogBlue = Color(0xFF6F879C)
val PureBlack = Color(0xFF000000)
val PrismRed = Color(0xFFFF2A2A)
val PrismCyan = Color(0xFF2A7FFF)
val PrismLime = Color(0xFF2AFF2A)

File: app/src/main/java/com/vivid/translator/ui/theme/Type.kt
package com.vivid.translator.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val Typography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).em,
        color = BoneWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
        color = BoneWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.01).em,
        color = BoneWhite
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        color = FogBlue
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.02.em,
        color = BoneWhite
    )
)

Step 4: GitHub Actions Workflow
Create file .github/workflows/build-apk.yml to compile the app and publish the APK artifact on every push.
name: Build Android APK

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Set Up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Grant Execute Permission to Gradle Wrapper
        run: chmod +x gradlew

      - name: Assemble Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk

Step 5: Git Version Control and Remote Push
Once all files are generated and verified:
 * Initialize the git repository:
   git init
git branch -M main

 * Create a standard .gitignore for Android (ignoring .gradle/, build/, *.iml, .idea/, local.properties).
 * Add files and make initial commit:
   git add .
git commit -m "Initialize Vivid screen translator with CI workflow and design tokens"

 * Create a new repository using the GitHub CLI (gh) and push:
   gh repo create screen-translator-vivid --public --source=. --remote=origin --push

   (If gh CLI is unauthenticated, prompt the user for the remote URL and execute git remote add origin <URL> followed by git push -u origin main).


