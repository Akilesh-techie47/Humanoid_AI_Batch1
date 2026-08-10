# Task: UI Personalization Fix & Agent Voice Recovery

- `[x]` Fix ROI Customization Bug
    - `[x]` Update `LayoutCustomizationEngine.kt` to apply user-selected anchors for the camera.
- `[x]` Organize Environment HUD (De-clutter)
    - `[x]` Remove hardcoded sidebar from `HUDRenderer.kt`.
    - `[x]` Integrate `SECONDARY_ROI` as a modular HUD component.
    - `[x]` Refine `LayoutRegistry.kt` component placements.
- `[x]` Recover AI Voice & Interaction
    - `[x]` Tune pitch to `0.80f` in `AndroidSpeechEngine.kt` and `TTSManager.kt`.
    - `[x]` Ensure `companionEngine.wake()` is called on HUD entry in `EnvironmentScreen.kt`.
- `[x]` Final build and verification of ROI movement and Wake Word.
