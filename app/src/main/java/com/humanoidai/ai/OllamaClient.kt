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
 * HTTP Client for Ollama API.
 * Handles model discovery, text generation, and streaming.
 */
class OllamaClient(
    private val endpoint: String,
    private val model: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpoint) // Ollama usually returns "Ollama is running" on root
            .get()
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("OllamaClient", "[OLLAMA_ERROR] Health check failed: ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("OllamaClient", "[OLLAMA_ERROR] Health check exception: ${e.message}")
            false
        }
    }

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$endpoint/api/tags")
            .get()
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("OllamaClient", "[OLLAMA_ERROR] listModels failed: ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val tagsResponse = gson.fromJson(body, OllamaTagsResponse::class.java)
                tagsResponse.models.map { it.name }
            }
        } catch (e: Exception) {
            Log.e("OllamaClient", "[OLLAMA_ERROR] listModels exception: ${e.message}")
            emptyList()
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "model" to model,
            "prompt" to prompt,
            "stream" to false
        )
        
        val request = Request.Builder()
            .url("$endpoint/api/generate")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val body = response.body?.string() ?: throw IOException("Empty response")
                val ollamaResponse = gson.fromJson(body, OllamaResponse::class.java)
                ollamaResponse.response
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun stream(prompt: String): Flow<String> = flow {
        val payload = mapOf(
            "model" to model,
            "prompt" to prompt,
            "stream" to true
        )
        
        val request = Request.Builder()
            .url("$endpoint/api/generate")
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val source = response.body?.source() ?: throw IOException("Empty body")
            
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                val chunk = gson.fromJson(line, OllamaResponse::class.java)
                emit(chunk.response)
                if (chunk.done) break
            }
        }
    }.flowOn(Dispatchers.IO)

    private data class OllamaTagsResponse(val models: List<OllamaModelInfo>)
    private data class OllamaModelInfo(val name: String)
    private data class OllamaResponse(val response: String, val done: Boolean)
}
