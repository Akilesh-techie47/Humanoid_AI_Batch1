# Humanoid AI — Master Upgrade Prompt Doc
*A living ground-truth document for maintaining context across AI sessions.*

## 1. Project Context Block
**PROJECT**: Humanoid AI — an Android companion app under ARMSUN Technologies (patent US9578159). A fisheye-lens-based, context-aware, proactive AI companion that runs a continuous observe -> listen -> understand -> decide -> speak loop, JARVIS-style.

**STACK**: 
- **Kotlin**: 2.1.0 (Stable)
- **AGP**: 8.7.3 (Stable - downgraded from 9.2.1 for KSP/Room stability)
- **UI**: Jetpack Compose, Compose BOM 2024.12.01
- **Database**: SQLCipher AES-256 encrypted Room DB + Android Keystore (PrivacyVault)
- **Vision**: CameraX + ML Kit Face Detection 16.1.7 + FaceNet TFLite 2.16.1 + OpenCV 4.9.0
- **AI**: Gemini 2.5 Flash via Generative AI SDK

**CURRENT STATE (CEA v1.4 - Intelligence Upgrade):**
- **Vision**: Enhanced pipeline implemented (AWB, Denoise, Sharpen, CLAHE) in `FrameEnhancer.kt`.
- **Speech**: Natural expressive engine with `SpeechFormatter` (SSML, chunking) and `SpeechOutputRouter`.
- **Hearing**: Audio-Vision Fusion (Sound alerts) + DoA HUD foundation (Radar Blip).
- **Security**: Voice-based user identification and danger-state trust checkpoints.
- **Memory**: Encrypted Long-Term Memory (Room DB) complete and integrated.
- **Customization**: 8-option HUD customization and 10-preset layout architecture built.

**KEY LESSONS / CONSTRAINTS:**
- **Gradle**: Use `alias()`, not `id()`. Stay on AGP 8.7.3 / Kotlin 2.1.0 to avoid KSP `unexpected jvm signature V` errors.
- **Vision**: `ImageAnalysis` MUST use `OUTPUT_IMAGE_FORMAT_YUV_420_888`. Use `InputImage.fromBitmap()` after correction.
- **Microphone**: Android `SpeechRecognizer` requires Main-Thread sync and explicit hard-cancels to avoid `RECOGNIZER_BUSY`.
- **Package**: `com.humanoidai` is the fixed root.

## 2. Standing Roadmap
| Item | Status | Notes |
| :--- | :--- | :--- |
| **Fisheye Calibration** | In Progress | Fine-tuning OpenCV dewarp matrices |
| **Radial JARVIS UI** | **NEXT** | Building the first high-tech radial preset |
| **ElevenLabs Integration** | Planned | Upgrading fallback TTS to premium voice |
| **Path B: Recovery** | Not Started | Critical event replay/recovery logic |
| **Path C: 5-Tier Sec** | Not Started | Advanced user trust-level logic |
| **Nightly Reflection** | Built | Stubbed logic in `NightlyReflectionWorker` |

## 3. Session Hygiene
- Always paste the **Project Context Block** at the start of a new session.
- Request one package at a time for large architectural upgrades.
- Confirm build stability (`:app:assembleDebug`) after every file modification.

---
**Last Updated**: Current Session (CEA v1.4 Integration)
