# Fix Compilation Error in MainActivity.kt

The project fails to build due to a type inference error in `MainActivity.kt` at the Supabase client initialization.

## Proposed Changes

### [MainActivity.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/MainActivity.kt)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/MainActivity.kt)
Add missing import for `Postgrest` plugin factory.

```diff
 import io.github.jan.supabase.createSupabaseClient
 import io.github.jan.supabase.auth.Auth
+import io.github.jan.supabase.postgrest.Postgrest
```

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the fix.
