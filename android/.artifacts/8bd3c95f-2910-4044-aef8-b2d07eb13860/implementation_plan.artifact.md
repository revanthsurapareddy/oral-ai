# Fix Unresolved Reference: supabase

The project is currently experiencing a build error where `supabase` is an unresolved reference in `DashboardScreen.kt`. This is likely due to duplicate or conflicting definitions of the Supabase client across `MainActivity.kt` and `Supabase.kt`, or visibility issues with the top-level property in `MainActivity.kt`.

## Proposed Changes

I will centralize the Supabase client definition into a single, dedicated file (`Supabase.kt`) as a top-level property. This is a cleaner architecture and ensures that the variable is correctly seen by all files in the `com.oralai.scan` package.

### 1. [Supabase](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/Supabase.kt)
- Convert `object Supabase` to a top-level `val supabase`.
- This matches the naming convention used throughout the app's screens.

### 2. [MainActivity](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/MainActivity.kt)
- Remove the redundant `val supabase` definition.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the unresolved reference is resolved.
- Run `./gradlew :app:assembleDebug` to ensure the entire project builds successfully.
