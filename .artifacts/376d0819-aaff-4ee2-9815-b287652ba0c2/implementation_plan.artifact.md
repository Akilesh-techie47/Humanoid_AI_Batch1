# Implementation Plan - UI Personalization Fix & Agent Interaction Stability

This plan addresses critical bugs in UI customization (ROI centering), resolves the silent AI issue, and organizes the Environment HUD to eliminate mess and overlaps.

## User Review Required

> [!IMPORTANT]
> - **Functional Customization**: I will fix the bug where the camera ROI stays centered even after you change its position. It will now correctly follow your "Top Left", "Bottom Right", etc. settings.
> - **Voice Interaction Fix**: I will increase the pitch slightly to `0.80f` (still deep male) to ensure maximum compatibility with the Android TTS engine. I will also move the "Wake Word" initialization to occur *after* you enter the HUD to ensure it has microphone access.
> - **HUD Re-Organization**: I will remove the hardcoded vertical sidebar and integrate it as a modular component. This prevents detections from overlapping with your top bar menu or status indicators.

## Proposed Changes

### [Component] UI / Layout Customization

#### [MODIFY] [LayoutCustomizationEngine.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/domain/manager/LayoutCustomizationEngine.kt)
- **Anchor Application**: Update the pipeline to apply the `anchor` derived from the `roi.primaryPositionX/Y` coordinates in the customization state. This makes the camera move as intended.

### [Component] UI / HUD Rendering

#### [MODIFY] [HUDRenderer.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/HUDRenderer.kt)
- **Integrate Secondary ROI**: Remove the hardcoded sidebar `Box`.
- **Modularization**: Ensure `SecondaryRoiBlip` is rendered through the component list, allowing it to respect its own anchor and visibility settings.
- **De-clutter**: Remove redundant `AlertBanner` calls that might be doubling up.

#### [MODIFY] [LayoutRegistry.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/domain/manager/LayoutRegistry.kt)
- **Sidebar Definition**: Ensure every layout preset has a clear definition for where the `SECONDARY_ROI` (sidebar) should appear (usually `CENTER_START`).

### [Component] AI Agent / Interaction

#### [MODIFY] [AndroidSpeechEngine.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/voice/AndroidSpeechEngine.kt) & [TTSManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/voice/TTSManager.kt)
- Adjust pitch to `0.80f` for reliability.
- Add more logging to track if the engine is truly ready.

#### [MODIFY] [EnvironmentScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/EnvironmentScreen.kt)
- **Wake Call**: Call `companionEngine.wake()` inside this screen's `LaunchedEffect` instead of just at the app start. This ensures the microphone is ready exactly when you need it.

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
- **ROI Test**: Go to Customization. Change camera to "Top Left". Return to HUD. Verify it moved.
- **Wake Word Test**: Say "Humanoid". Verify the AI responds "At your service, Sir".
- **Visual Check**: Verify the HUD elements are neatly organized and no longer overlapping.
