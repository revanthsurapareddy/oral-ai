# Implementation Plan: Refine Outlines and Improve Backend Integration

This plan addresses two main points:
1.  **Refine Visuals**: Make the dashed outlines thinner and less obtrusive.
2.  **Improve Backend "Focus"**: Update the app to use detection coordinates from the Render backend (YOLO-style) if available, and provide better feedback on the connection status.

## Proposed Changes

### OralAI Scan App

#### [MODIFY] [ImageOverlayUtils.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/ImageOverlayUtils.kt)
- Reduce `strokeWidth` for both blue and yellow lines (e.g., from `0.012f`/`0.015f` to `0.005f`/`0.007f`).
- Update `drawAnalysisOutlines` to accept optional `cx`, `cy`, and `radius` parameters so it can "focus" on specific detected areas.

#### [MODIFY] [AnalyzeScreen.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/AnalyzeScreen.kt)
- **Coordinate Extraction**: Look for `detections`, `bbox`, or `box` in the JSON response from Render.
- **Dynamic Drawing**: If coordinates are found, pass them to `ImageOverlayUtils.drawAnalysisOutlines`.
- **Backend Visibility**: Add logging (using `println` or `Log.d`) to confirm when a request is sent to Render and when a response is received, helping the user track the "workflow".
- **Better Feedback**: Update the `isAnalyzing` state to show more descriptive text (e.g., "Connecting to server...", "Processing image...") if possible, or at least ensure the timeout/error handling is clear.

#### [MODIFY] [ResultScreen.kt](file:///C:/Users/penna/Documents/oral-ai/android/app/src/main/java/com/oralai/scan/ResultScreen.kt)
- Update the call to `drawAnalysisOutlines` to use any saved coordinates from `SessionManager` (will need to add coordinate storage to `SessionManager`).

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors.

### Manual Verification
- Test the analysis flow.
- Verify the lines are visibly thinner.
- Check Logcat for "Render Request" logs to verify the backend connection.
- If the backend returns a bounding box, verify the outlines move to that specific area instead of the center.
