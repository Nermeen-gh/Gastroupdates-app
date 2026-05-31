package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_INSTRUCTION = """
        You are GastroAssistant AI, a brilliant and highly specialized clinical AI assistant in gastroenterology, hepatology, and endoscopy. 
        Your goal is to assist clinicians, researchers, and students by providing high-fidelity, evidence-based, up-to-date research, guidelines, and drug approvals up to the year 2026.
        When answering questions:
        1. Always be professional, concise, objective, and clinically accurate.
        2. Where applicable, cite actual 2025/2026 guidelines (e.g., AGA, AASLD, ASGE, Baveno VIII, EASL, ESGE) or trials.
        3. Break down complex medical algorithms into clear, actionable bullet points.
        4. Organize answers with clear headings (e.g., Clinical Presentation, Guidance Updates, Actionable Takeaways, References).
        5. Include a prominent disclaimer: "Disclaimer: This information is for educational and clinical reference purposes only. It is not a substitute for professional clinical judgment or direct patient care."
    """

    suspend fun generateClinicalResponse(userPrompt: String, customApiKey: String? = null): String = withContext(Dispatchers.IO) {
        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        
        // Clean key selection: use custom key if supplied, otherwise fall back to buildConfig key
        val finalKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY") {
            buildConfigKey.trim()
        } else {
            ""
        }

        if (finalKey.isEmpty()) {
            return@withContext "API_KEY_ERROR: Gemini API Key is missing. Please add your GEMINI_API_KEY in the Secrets panel of AI Studio, or enter a custom key in the settings tab to activate the clinical assistant."
        }

        try {
            val requestBodyJson = JSONObject().apply {
                // systemInstruction block
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", SYSTEM_INSTRUCTION)
                        })
                    })
                })

                // contents block
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })

                // Optional generation tuning
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4) // clinical precision
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val reqBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$finalKey"
            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string()

            if (!response.isSuccessful) {
                val errorMsg = JSONObject(rawResponse ?: "{}").optJSONObject("error")?.optString("message") 
                    ?: "HTTP Error ${response.code}"
                Log.e(TAG, "API Call FAILED. Safe Response: $errorMsg")
                return@withContext "Error from Gemini API: $errorMsg"
            }

            if (rawResponse == null) {
                return@withContext "Error: Received empty response body away from Gemini API."
            }

            // Extract text from standard Gemini response JSON
            val responseJson = JSONObject(rawResponse)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext "Error: No candidates returned in Gemini model output."
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            if (contentObj == null) {
                return@withContext "Error: Content object is empty in candidates block."
            }

            val parts = contentObj.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext "Error: No parts returned in candidate content block."
            }

            val textResult = parts.getJSONObject(0).optString("text")
            if (textResult.isNullOrBlank()) {
                return@withContext "Error: Empty text field in response candidate."
            }

            return@withContext textResult

        } catch (e: Exception) {
            Log.e(TAG, "Error generating response: ${e.message}", e)
            return@withContext "Network Error: ${e.localizedMessage}. Please check your active internet connection and try again."
        }
    }
}
