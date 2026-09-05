package com.humanoidai.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

// -----------------------------------------------------------------
// FaceEmbeddingHelper
// -----------------------------------------------------------------
// Loads facenet.tflite from assets and runs inference on a face crop.
// Returns a normalized 128-dimensional float embedding vector.
// Input: face Bitmap (any size — resized internally to 160x160)
// Output: FloatArray of size 128
// -----------------------------------------------------------------
class FaceEmbeddingHelper(private val context: Context) {

    companion object {
        private const val MODEL_FILE   = "facenet.tflite"
        private const val INPUT_SIZE   = 160        // FaceNet expects 160×160
        private const val EMBEDDING_DIM = 128       // FaceNet output dimension
        private const val IMAGE_MEAN   = 127.5f
        private const val IMAGE_STD    = 128.0f
    }

    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile())
    }

    // Reuse ByteBuffer to avoid frequent allocations (Phase 11 Optimization)
    private val inputBuffer = ByteBuffer.allocateDirect(
        4 * INPUT_SIZE * INPUT_SIZE * 3 // float32, 3 channels
    ).apply { order(ByteOrder.nativeOrder()) }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /**
     * Takes a face crop bitmap (any size) and returns a 128-dim embedding.
     */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        // Optimization: Ensure input is exactly what FaceNet expects
        val resized = if (faceBitmap.width == INPUT_SIZE && faceBitmap.height == INPUT_SIZE) {
            faceBitmap
        } else {
            Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        }
        
        bitmapToByteBuffer(resized)
        val output = Array(1) { FloatArray(EMBEDDING_DIM) }
        interpreter.run(inputBuffer, output)
        
        // Clean up temporary scaled bitmap
        if (resized != faceBitmap) {
            resized.recycle()
        }
        
        return normalize(output[0])
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8)  and 0xFF)
            val b = (pixel          and 0xFF)
            inputBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            inputBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            inputBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }
    }

    /**
     * L2-normalize the embedding so cosine similarity == dot product.
     */
    private fun normalize(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.map { it * it }.sum())
        return if (norm > 0f) FloatArray(embedding.size) { embedding[it] / norm }
        else embedding
    }

    fun close() {
        interpreter.close()
    }
}
