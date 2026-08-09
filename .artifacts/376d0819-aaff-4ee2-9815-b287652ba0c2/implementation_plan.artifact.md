# Implementation Plan - Final Access Unification & UI Overlap Fix

The user wants to completely remove the "Restricted Access" feature and fix the remaining UI overlapping issues (messy structure). Additionally, the AI voice must be strictly male across all parts of the app.

## User Review Required

> [!IMPORTANT]
> - **Full Access for All**: I will remove the `GUEST` role entirely. Any authenticated user will have full owner privileges.
> - **Notification Silencing**: I will explicitly silence standard Android notifications while the HUD is active to prevent the "white box" overlap.
> - **UI De-cluttering**: I will relocate the Alert Banner to the bottom-start area to ensure it never overlaps with the top status bar or the camera core.
> - **Hard Male Voice Enforcement**: I will apply a pitch shift to all TTS output to ensure a deep male voice, even if the device's default voice is female.

## Proposed Changes

### [Component] Security & Access (Full Unification)

#### [MODIFY] [SessionManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/security/SessionManager.kt)
- Final removal of `UserAccessLevel.GUEST`.
- Ensure all session creation defaults to `OWNER`.

#### [MODIFY] [BiometricVerificationScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/BiometricVerificationScreen.kt)
- Simplify logic: Recognize any enrolled face -> Grant full access.

#### [MODIFY] [SidePanelDrawer.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/components/SidePanelDrawer.kt)
- Always show all navigation items (Settings, Alerts, Analytics).

### [Component] UI / HUD Polishing (Zero Overlap)

#### [MODIFY] [LayoutRegistry.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/domain/manager/LayoutRegistry.kt)
- **Relocate Alerts**: Move the `ALERTS` component to `BOTTOM_START` or a lower `offsetY` (e.g., 200dp) to prevent collision with the Top Bar.
- **Sidebar Padding**: Increase the sidebar's top padding to ensure it starts below the menu button.

#### [MODIFY] [NavGraph.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/navigation/NavGraph.kt)
- Explicitly block `NotificationHelper.sendAlert` when the `ENVIRONMENT` screen is active.

### [Component] AI Voice Persona (Total Male Enforcement)

#### [MODIFY] [AndroidSpeechEngine.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/voice/AndroidSpeechEngine.kt)
- Apply a hardcoded pitch of `0.78f` to guarantee a deep male tone.

#### [MODIFY] [TTSManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/voice/TTSManager.kt)
- Synchronize pitch to `0.78f`.

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
- **Login Check**: Log in and verify all sidebar items are visible.
- **Overlap Check**: Trigger an alert and verify the banner appears in a clear area at the bottom/side.
- **Voice Check**: Listen to the "Systems Online" greeting. It should be deep and masculine.
