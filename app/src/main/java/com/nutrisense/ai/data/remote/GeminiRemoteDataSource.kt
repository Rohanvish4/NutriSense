package com.nutrisense.ai.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nutrisense.ai.data.remote.dto.MealAnalysisDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote Data Source responsible for executing food analysis API calls against Google Gemini.
 *
 * Utilizes multimodal inputs (image bitmap + system prompt) and enforces rigid JSON output schemas
 * to ensure 100% parseable structured nutrition data.
 */
@Singleton
class GeminiRemoteDataSource @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val json: Json
) {

    /**
     * Analyzes a food image bitmap using Gemini multimodal prediction.
     *
     * @param bitmap The cropped ROI or full food image to analyze.
     * @return [Result] wrapping [MealAnalysisDto] on success or formatted exception on failure.
     */
    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<MealAnalysisDto> = withContext(Dispatchers.IO) {
        try {
            val prompt = content {
                image(bitmap)
                text(
                    "Analyze the provided food image in detail. Identify all distinct food items and ingredients, " +
                    "estimate their portion weights in grams, calculate individual and aggregate calories and macronutrients " +
                    "(protein, carbohydrates, fats, dietary fiber), assign dietary indicators (e.g. Keto, High-Protein), " +
                    "and flag potential allergens. Output strictly structured JSON adhering to the target schema."
                )
            }

            val response = generativeModel.generateContent(prompt)
            val responseText = response.text
                ?: throw IllegalStateException("Gemini AI API returned a null or empty response content.")

            val dto = json.decodeFromString<MealAnalysisDto>(responseText)
            Result.success(dto)
        } catch (e: Throwable) {
            val rawMessage = e.message ?: "Unknown error during food analysis."
            val cleanMessage = when {
                rawMessage.contains("404") || rawMessage.contains("NOT_FOUND", ignoreCase = true) ->
                    "Model endpoint not found (404). Please ensure the Generative Language API is enabled in your Google Cloud project."
                rawMessage.contains("403") || rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ->
                    "Permission denied (403). Please verify that your API key is valid and authorized."
                rawMessage.contains("429") || rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                    "Rate limit or quota exceeded (429). Please wait a few moments and try again."
                else -> rawMessage
            }
            Result.failure(Exception(cleanMessage, e))
        }
    }
}
