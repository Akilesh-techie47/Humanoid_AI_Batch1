# Task: Access Unification, UI Cleanup & Male Voice Enforcement

- `[/]` Full Access Unification
    - `[ ]` Remove `GUEST` logic from `SessionManager.kt`.
    - `[ ]` Update `BiometricVerificationScreen.kt` for universal access.
    - `[ ]` Always show full sidebar in `SidePanelDrawer.kt`.
- `[/]` HUD Overlap Fix
    - `[ ]` Relocate Alert Banner in `LayoutRegistry.kt` to avoid Top Bar collision.
    - `[ ]` Suppress system notifications while in HUD in `NavGraph.kt`.
- `[/]` 100% Male Voice Enforcement
    - `[ ]` Force pitch shift to `0.78f` in `AndroidSpeechEngine.kt`.
    - `[ ]` Synchronize `TTSManager.kt` pitch.
- `[ ]` Verify build and persona consistency.
