# Humanoid AI — UI Restoration & Alignment Plan

This plan restores the Humanoid AI UI to its previously clean and aligned state, focusing on the Main HUD, Settings, and Customization screens.

## User Review Required

> [!IMPORTANT]
> **HUD Engine Switch**: I am reverting the `EnvironmentScreen` to use the hardcoded `CameraHomeHud` design, as it represents the "Last Known Good" clean UI. The modular `HUDRenderer` will be stabilized but moved to a secondary role until its alignment logic is perfected.

## Proposed Changes

### Main HUD Restoration
Restoring the visually consistent and thematic HUD.

#### [MODIFY] [EnvironmentScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/EnvironmentScreen.kt)
- Revert the call from `HUDRenderer` back to `CameraHomeHud`.
- Ensure all parameters (detected persons, AI status, mic state) are correctly passed to `CameraHomeHud`.
- Fix the `Scaffold` background and padding to prevent layout shifts.

#### [MODIFY] [HUDRenderer.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/HUDRenderer.kt)
- Fix the duplicated and broken `PrimaryRoiOverlay` code.
- Clean up the `HUDComponentWrapper` alignment logic to prevent overlaps.

---

### Settings & Spacing Alignment
Ensuring consistent margins and typography across auxiliary screens.

#### [MODIFY] [SettingsScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/SettingsScreen.kt)
- Audit spacing between sections and rows.
- Ensure all settings cards have consistent height and internal padding.
- Fix any overlapping text in descriptions.

#### [MODIFY] [LayoutCustomizationScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/LayoutCustomizationScreen.kt)
- Align section icons with titles.
- Ensure the background dot grid does not interfere with control visibility.
- Fix spacing in the `SelectionGrid` component.

---

### Global UI Refinement
- **Typography**: Verify font sizes follow the `Screen Title -> Section Title -> Content` hierarchy.
- **Z-Order**: Ensure overlays (like the Drawer and Dialogs) appear correctly above the HUD.
- **System Bars**: Confirm `statusBarsPadding()` and `navigationBarsPadding()` are used consistently without doubling up.

## Verification Plan

### Visual QA
- Launch Splash and verify alignment.
- Verify Main HUD across different themes (Neon Cyan, Crystal Frost, etc.).
- Inspect Settings screen for consistent card spacing.
- Verify Customization screen controls and preview alignment.

### Functional Verification
- Confirm Camera, Microphone, and AI interactions still work within the restored UI.
- Verify Theme switching updates the HUD correctly.
