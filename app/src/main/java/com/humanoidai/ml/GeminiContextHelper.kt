package com.humanoidai.ml

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiContextHelper(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun interpretRoom(bitmap: Bitmap, ownerName: String): String = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text("You are the brain of Humanoid AI. You are looking through a fisheye lens. " +
                         "Describe the room and what people are doing. The owner's name is $ownerName. " +
                         "Be concise (max 20 words). If you see security risks, mention them.")
                }
            )
            response.text ?: "Room analysis unavailable."
        } catch (e: Exception) {
            "Visual interpretation error."
        }
    }
}