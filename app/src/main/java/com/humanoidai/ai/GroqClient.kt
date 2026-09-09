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
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP Client for Groq Cloud API (OpenAI Compatible).
 */
class GroqClient(
    private val apiKey: String,
    private var model: String = "llama3-8b-8192"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"

    fun updateModel(newModel: String) {
        model = newModel
    }

    suspend fun generate(request: AIRequest): String = withContext(Dispatchers.IO) {
        val payload = buildPayload(request, stream = false)

        val httpRequest = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                val metadata = extractRateLimits(response)
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("GroqClient", "[GROQ_LIMIT] status=${response.code} $metadata")
                    throw GroqException(response.code, errorBody ?: "Unknown error", metadata)
                }
                
                val body = response.body?.string() ?: throw IOException("Empty response")
                val groqResponse = gson.fromJson(body, GroqChatResponse::class.java)
                groqResponse.choices.firstOrNull()?.message?.content ?: ""
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
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val metadata = extractRateLimits(response)
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("GroqClient", "[GROQ_LIMIT_STREAM] status=${response.code} $metadata")
                throw GroqException(response.code, errorBody ?: "Unknown error", metadata)
            }
            
            val source = response.body?.source() ?: throw IOException("Empty body")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = gson.fromJson(data, GroqChatStreamResponse::class.java)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (content != null) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunks or metadata
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractRateLimits(response: Response): Map<String, Any> {
        val headers = response.headers
        val limits = mutableMapOf<String, Any>()
        
        headers["x-ratelimit-remaining-requests"]?.let { limits["remainingRequests"] = it }
        headers["x-ratelimit-remaining-tokens"]?.let { limits["remainingTokens"] = it }
        headers["x-ratelimit-reset-requests"]?.let { limits["resetRequests"] = it }
        headers["x-ratelimit-reset-tokens"]?.let { limits["resetTokens"] = it }
        headers["retry-after"]?.let { limits["retryAfter"] = it }
        
        return limits
    }

    private fun buildPayload(request: AIRequest, stream: Boolean): Map<String, Any> {
        val messages = mutableListOf<Map<String, String>>()
        
        // Add system instructions if present
        request.systemInstructions?.let {
            messages.add(mapOf("role" to "system", "content" to it))
        }

        // Add history
        request.history.forEach { msg ->
            messages.add(mapOf(
                "role" to if (msg.isUser) "user" else "assistant",
                "content" to msg.text
            ))
        }

        // Add current prompt
        messages.add(mapOf("role" to "user", "content" to request.prompt))

        return mutableMapOf(
            "model" to model,
            "messages" to messages,
            "stream" to stream,
            "temperature" to request.temperature,
            "max_tokens" to request.maxTokens
        )
    }

    private data class GroqChatResponse(val choices: List<Choice>)
    private data class Choice(val message: Message)
    private data class Message(val content: String)

    private data class GroqChatStreamResponse(val choices: List<StreamChoice>)
    private data class StreamChoice(val delta: Delta)
    private data class Delta(val content: String?)
}

class GroqException(val code: Int, message: String, val metadata: Map<String, Any>) : IOException(message)
