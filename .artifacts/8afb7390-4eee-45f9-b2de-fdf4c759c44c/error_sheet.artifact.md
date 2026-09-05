# Humanoid AI - System Integrity & Error Sheet
**Version:** CEA v1.4 (Demo Stable)
**Generated:** 2026-08-31 15:20

This sheet tracks remaining runtime issues, configuration gaps, and critical stability markers identified during regression testing.

## 🔴 Critical Issues (Fix Required)

| Component | Error Description | Impact | Status |
| :--- | :--- | :--- | :--- |
| **OpenCV** | `UnsatisfiedLinkError: libc++_shared.so not found` | Fisheye correction and advanced vision filters are disabled. | ⚠️ Pending NDK Fix |
| **Face Recognition** | Live Similarity (0.72) vs. Threshold (0.87) | System may fail to recognize owner reliably. | 🔍 Tuning Needed |
| **Mic Management** | Rapid restart beeping (system-level) | Distracting user experience and potential mic lock. | ✅ Fixed (Muting + 2s Delay) |
| **Gemini AI** | `Invalid authentication credentials` | AI cannot generate responses (Speech "Neural link error"). | ✅ Fixed (Key Updated) |

## 🟡 Warning / Non-Critical

| Category | Observation | Recommended Action |
| :--- | :--- | :--- |
| **Performance** | `Skipped 153 frames!` on startup. | Offload `FaceRecognition` initialization to background thread. |
| **Memory** | `A resource failed to call close` in logs. | Verify all `ImageProxy` objects are closed in `FaceAnalyzer`. |
| **UI/UX** | Splash screen delay is 2s static. | Transition immediately once `AIManager` and `TrustFramework` are ready. |

## 🟢 Stability Verified (Pass)

- [x] **Firebase Auth**: OTP/Email authentication restored and functional.
- [x] **Database**: SQLCipher encryption active; schema version 4 verified.
- [x] **Interaction**: JARVIS-style conciseness (max 15-30 words) enforced.
- [x] **Speech**: System stream muting successfully hides "start-listening" beeps.
- [x] **Proactive Thoughts**: 20s cooldown engine functioning with Gemini 1.5 Flash.

## 🛠 Environmental Fixes
1. **Gemini Access**: Ensure `local.properties` contains a key starting with `AIza`.
2. **NDK Support**: Add `arguments "-DANDROID_STL=c++_shared"` to `externalNativeBuild` in `:app` build.gradle if vision filters are required.

> [!IMPORTANT]
> The threshold for face recognition is currently very high (0.87). If the AI fails to greet you, lower this to **0.78** in `FaceRecognitionManager.kt` for better demo reliability under varied lighting.
