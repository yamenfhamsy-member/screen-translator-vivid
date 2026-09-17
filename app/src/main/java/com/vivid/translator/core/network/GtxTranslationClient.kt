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
        for (index in 0 until sentencesArray.length()) {
            val sentenceBlock = sentencesArray.optJSONArray(index)
            val translatedSegment = sentenceBlock?.optString(0)
            if (!translatedSegment.isNullOrEmpty()) {
                stringBuilder.append(translatedSegment)
            }
        }
        return stringBuilder.toString()
    }
}
