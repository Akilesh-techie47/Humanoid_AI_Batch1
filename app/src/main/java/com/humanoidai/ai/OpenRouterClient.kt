package com.humanoidai.ai

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP Client for OpenRouter API.
 */
class OpenRouterClient(
    private val apiKey: String,
    private var model: String = "google/gemini-flash-1.5"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"

    fun updateModel(newModel: String) {
        model = newModel
    }

    suspend fun generate(request: AIRequest): String = withContext(Dispatchers.IO) {
        val payload = buildPayload(request, stream = false)

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://aura360.ai")
            .header("X-Title", "Aura 360°")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("OpenRouterClient", "[OR_ERROR] status=${response.code} $errorBody")
                    throw OpenRouterException(response.code, errorBody ?: "Unknown error")
                }
                val body = response.body?.string() ?: throw IOException("Empty response")
                val orResponse = gson.fromJson(body, ORChatResponse::class.java)
                orResponse.choices.firstOrNull()?.message?.content ?: ""
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun stream(request: AIRequest): Flow<String> = flow {
        val payload = buildPayload(request, stream = true)

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://aura360.ai")
            .header("X-Title", "Aura 360°")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("OpenRouterClient", "[OR_STREAM_ERROR] status=${response.code} $errorBody")
                throw OpenRouterException(response.code, errorBody ?: "Unknown error")
            }
            val source = response.body?.source() ?: throw IOException("Empty body")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = gson.fromJson(data, ORChatStreamResponse::class.java)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (content != null) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunks
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPayload(request: AIRequest, stream: Boolean): Map<String, Any> {
        val messages = mutableListOf<Map<String, String>>()
        
        request.systemInstructions?.let {
            messages.add(mapOf("role" to "system", "content" to it))
        }

        request.history.forEach { msg ->
            messages.add(mapOf(
                "role" to if (msg.isUser) "user" else "assistant",
                "content" to msg.text
            ))
        }

        messages.add(mapOf("role" to "user", "content" to request.prompt))

        return mutableMapOf(
            "model" to model,
            "messages" to messages,
            "stream" to stream,
            "temperature" to request.temperature,
            "max_tokens" to request.maxTokens
        )
    }

    private data class ORChatResponse(val choices: List<Choice>)
    private data class Choice(val message: Message)
    private data class Message(val content: String)

    private data class ORChatStreamResponse(val choices: List<StreamChoice>)
    private data class StreamChoice(val delta: Delta)
    private data class Delta(val content: String?)
}

class OpenRouterException(val code: Int, message: String) : IOException(message)
