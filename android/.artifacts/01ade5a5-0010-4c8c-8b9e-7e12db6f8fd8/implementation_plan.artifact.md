# End-to-End Optimization to Solve 503 Memory Crashes

The goal is to optimize both the Android client and the Python backend to stay within Render's 512MB RAM limit. This involves triggering the "Demo Bypass" more reliably and reducing the RAM footprint of the AI model.

## User Review Required

> [!IMPORTANT]
> I will be modifying the **Backend** code (`main.py`) directly. These changes are designed to reduce memory usage on Render's Free Tier.

## Proposed Changes

### [Backend Optimization]

#### [MODIFY] [main.py](file:///C:/Users/penna/Documents/oral-ai/backend/main.py)
- Move heavy imports (`ultralytics`, `torch`) inside the `get_model` function (Lazy Loading).
- Explicitly set `torch.set_num_threads(1)` to reduce CPU/RAM overhead.
- Call `gc.collect()` and `torch.cuda.empty_cache()` (if applicable) more aggressively.
- Ensure the model is loaded only once and kept as small as possible.

### [Android Client Optimization]

#### [MODIFY] [AnalyzeScreen.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/AnalyzeScreen.kt)
- **Smart Upload Logic:**
    - If the selected image is **under 1MB**, upload the **raw bytes**. This allows the backend to perform an MD5 "Hash Match" and return the Demo Result instantly without running the AI.
    - If the image is **over 1MB**, compress it to **224px**. This ensures that even if the AI runs, it won't crash the server.

## Verification Plan

### Manual Verification
1.  Upload one of the standard "Demo" images. Verify it hits the cache and returns a result instantly.
2.  Upload a large camera photo. Verify it compresses to 224px and the server processes it (even if slowly) without a 503 crash.
3.  Monitor Render logs (if possible) to confirm memory usage stays below 512MB.
