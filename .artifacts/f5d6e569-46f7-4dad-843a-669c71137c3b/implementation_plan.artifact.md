# Black & Grey Theme Implementation and Splash Animation

This plan outlines the steps to migrate the application's color palette from blue/cyan to a monochromatic black and grey style, and to add a professional animation to the logo during the loading sequence.

## User Review Required

> [!IMPORTANT]
> The primary accent color `AccentCyan` will be replaced by a `NeutralGrey`. This will affect all UI components using the `primary` color from the theme.
> `BackgroundDark` will be darkened to a near-black tone for a more "Core" aesthetic.

## Proposed Changes

### [Theme & Colors]

#### [MODIFY] [Theme.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/theme/Theme.kt)
- Redefine `BackgroundDark` to `0xFF0A0A0A`.
- Replace `AccentCyan` with `AccentGrey` (`0xFFD1D5DB`).
- Adjust `AccentPurple` to a darker slate or muted grey.
- Update `DarkColorScheme` to reflect these changes.

#### [MODIFY] [HudThemes.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/theme/HudThemes.kt)
- Update the default "Neon Cyan" theme (t1) to "Core Monolith" (Black/Grey).
- Review other HUD themes for consistency.

### [Splash Screen & Animation]

#### [MODIFY] [SplashScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/screens/SplashScreen.kt)
- Implement `remember { Animatable(0f) }` for the logo scale and alpha.
- Add a breathing or pulse animation to the `SmartToy` icon.
- Update UI elements to use the new theme colors.

### [General Cleanup]

#### [MODIFY] [Other UI Components]
- Search and replace hardcoded blue colors in components like `CameraHomeHud.kt` or other screen files.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to ensure no build regressions.

### Manual Verification
- Deploy to device/emulator.
- Observe the Splash Screen animation.
- Navigate through the app to verify the new theme is applied everywhere.
- Check "small things" like button states, progress indicators, and text contrast.
