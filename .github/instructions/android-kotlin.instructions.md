---
name: Android Kotlin Standards
description: Kotlin, Gradle, and architecture rules for AuraChat Android code.
applyTo: "**/*.{kt,kts}"
---

# AuraChat Android Kotlin Standards

- Keep implementation aligned with Clean MVVM plus repository boundaries.
- Prefer `StateFlow`-based UI state and avoid introducing race-prone ownership between Room, streaming responses, and UI effects.
- Preserve Compose-first UI patterns and Material 3 usage.
- Keep DI in Hilt modules and persistence in Room instead of bypassing those layers.
- Do not add the `kotlin.android` plugin explicitly in Gradle files.
- Keep the AGP 9.x `compileSdk` DSL and keep KSP compatible with Kotlin `2.3.10`.
- Keep secrets out of source control and out of Gradle files.
