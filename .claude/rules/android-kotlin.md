---
paths:
  - "**/*.{kt,kts}"
---

# Android and Kotlin Rules

- Keep AuraChat on Kotlin, Jetpack Compose Material 3, Hilt, Room, and coroutines/Flow.
- Preserve the existing Clean MVVM plus repository package structure under `com.aurachat`.
- Prefer immutable UI state and race-free Flow ownership when touching chat streaming or Room observation code.
- Keep navigation arguments explicit and typed, especially `chat/{sessionId}` long arguments.
- Do not add the `kotlin.android` plugin explicitly in Gradle files. AGP `9.2.0-alpha03` already provides it.
- Keep the AGP 9.x `compileSdk` DSL shape: `compileSdk { version = release(36) { minorApiLevel = 1 } }`.
- Keep KSP aligned with Kotlin `2.3.10`; the current project version is `2.3.6`.
- Never hardcode Gemini secrets. `BuildConfig.GEMINI_API_KEY` must continue to come from `local.properties`.
