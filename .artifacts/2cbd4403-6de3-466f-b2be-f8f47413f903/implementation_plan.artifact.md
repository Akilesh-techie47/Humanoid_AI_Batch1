# Implementation Plan - Face Recognition Robustness & Stability (CEA v1.4+)

This plan addresses the noise, spoofing vulnerabilities, and edge cases in the current face recognition system.

## User Review Required

> [!IMPORTANT]
> - **Temporal Smoothing**: Identity will now be decided over a rolling window (5-10 frames) rather than per-frame. This adds a slight latency (approx 200ms) to initial identification but eliminates flickering.
> - **Liveness Check**: A static photo will no longer trigger "Confirmed" status; blink or head movement will be required for high-trust actions.
> - **Multi-Angle Enrollment**: The visitor enrollment UI will change to prompt for head rotations, similar to the Owner enrollment.

## Proposed Changes

### 1. Vision & Tracking
#### [MODIFY] [DetectedPerson.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/vision/DetectedPerson.kt)
- Add `trackId: Int` and `livenessScore: Float`.

#### [NEW] [FaceTracker.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/vision/FaceTracker.kt)
- Centroid/IOU based tracker to maintain identity across frames.
- Manages a rolling average of similarity scores per `trackId`.

#### [MODIFY] [FaceAnalyzer.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/vision/FaceAnalyzer.kt)
- Integrate `FaceTracker`.
- Implement **Fisheye Edge Weighting**: Reduce confidence for faces near frame boundaries.
- Implement **Liveness Pass**: Use ML Kit `eyeOpenProbability` and head Euler angles to detect blinks/movement.

### 2. Enrollment
#### [MODIFY] [EnrollmentScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/EnrollmentScreen.kt)
- Upgrade to multi-angle capture.
- Store up to 3-5 distinct "viewpoint" embeddings per person.

### 3. ML & Recognition
#### [MODIFY] [FaceRecognitionManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ml/FaceRecognitionManager.kt)
- Allow `knownFaces` to store a list of embeddings.
- Match against the *best* embedding in the set.
- Implement the "Probable" confidence band (e.g., 0.65 - 0.85).

## Verification Plan

### Automated Tests
- `./gradlew :app:assembleDebug`
- Test `FaceTracker` IOU logic with simulated bounding boxes.

### Manual Verification
- **Stability**: Verify that "Unknown" flickers are gone when lighting changes.
- **Spoofing**: Verify that a static photo (no blinks) stays in the "Probable/Unknown" state.
- **Angles**: Verify recognition works at 30-degree profiles after multi-angle enrollment.
