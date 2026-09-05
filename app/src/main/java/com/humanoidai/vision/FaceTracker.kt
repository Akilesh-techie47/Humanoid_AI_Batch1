package com.humanoidai.vision

import android.graphics.RectF
import java.util.*

/**
 * FaceTracker handles identity persistence across frames using spatial correlation (IOU).
 * It also performs temporal smoothing on recognition scores to prevent "flickering" identities.
 */
class FaceTracker {

    private val activeTracks = mutableListOf<Track>()
    private var nextTrackId = 1
    
    companion object {
        private const val IOU_THRESHOLD = 0.35f // Slightly more lenient tracking
        private const val MAX_AGE_MS = 1500L // Keep identity alive for 1.5s after frame loss
        private const val SMOOTHING_WINDOW = 12 // Increased for better stability
    }

    data class Track(
        val id: Int,
        var lastBounds: RectF,
        var lastSeen: Long,
        val nameHistory: MutableList<String> = mutableListOf(),
        val confidenceHistory: MutableList<Float> = mutableListOf()
    ) {
        fun update(bounds: RectF, name: String, confidence: Float) {
            lastBounds = bounds
            lastSeen = System.currentTimeMillis()
            
            if (name != "STABLE" && name != "ANALYZING" && name != "IDENTIFYING") {
                nameHistory.add(name)
                if (nameHistory.size > SMOOTHING_WINDOW) nameHistory.removeAt(0)
            }
            
            if (confidence >= 0f) {
                confidenceHistory.add(confidence)
                if (confidenceHistory.size > SMOOTHING_WINDOW) confidenceHistory.removeAt(0)
            }
        }

        fun getSmoothedIdentity(): Pair<String, Float> {
            if (nameHistory.isEmpty()) return "IDENTIFYING" to 0f
            
            // Majority vote for name
            val nameCounts = nameHistory.groupingBy { it }.eachCount()
            val bestName = nameCounts.maxByOrNull { it.value }?.key ?: "UNKNOWN"
            
            // Average confidence for the best name
            val avgConf = confidenceHistory.average().toFloat()
            
            return bestName to avgConf
        }
    }

    /**
     * Correlates current detections with existing tracks.
     * Returns a list of (TrackID, SmoothedName, SmoothedConfidence).
     */
    fun track(detections: List<RawDetection>): List<TrackResult> {
        val now = System.currentTimeMillis()
        val results = mutableListOf<TrackResult>()
        
        // 1. Cleanup old tracks
        activeTracks.removeAll { now - it.lastSeen > MAX_AGE_MS }

        // 2. Associate detections with tracks
        val matchedDetections = mutableSetOf<Int>()
        val matchedTracks = mutableSetOf<Int>()

        detections.forEachIndexed { dIdx, det ->
            var bestIou = 0f
            var bestTrackIdx = -1

            activeTracks.forEachIndexed { tIdx, track ->
                if (tIdx !in matchedTracks) {
                    val iou = calculateIou(det.bounds, track.lastBounds)
                    if (iou > bestIou && iou > IOU_THRESHOLD) {
                        bestIou = iou
                        bestTrackIdx = tIdx
                    }
                }
            }

            if (bestTrackIdx != -1) {
                val track = activeTracks[bestTrackIdx]
                track.update(det.bounds, det.name, det.confidence)
                val (smoothedName, smoothedConf) = track.getSmoothedIdentity()
                results.add(TrackResult(track.id, smoothedName, smoothedConf))
                matchedDetections.add(dIdx)
                matchedTracks.add(bestTrackIdx)
            }
        }

        // 3. Create new tracks for unmatched detections
        detections.forEachIndexed { dIdx, det ->
            if (dIdx !in matchedDetections) {
                val newTrack = Track(nextTrackId++, det.bounds, now)
                newTrack.update(det.bounds, det.name, det.confidence)
                activeTracks.add(newTrack)
                results.add(TrackResult(newTrack.id, det.name, det.confidence))
            }
        }

        return results
    }

    private fun calculateIou(r1: RectF, r2: RectF): Float {
        val intersection = RectF()
        if (!intersection.setIntersect(r1, r2)) return 0f
        val interArea = intersection.width() * intersection.height()
        val unionArea = (r1.width() * r1.height()) + (r2.width() * r2.height()) - interArea
        return if (unionArea > 0) interArea / unionArea else 0f
    }
}

data class RawDetection(val bounds: RectF, val name: String, val confidence: Float)
data class TrackResult(val trackId: Int, val name: String, val confidence: Float)
