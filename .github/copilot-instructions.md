# AuraChat Copilot Instructions

- AuraChat is an Android app built with Kotlin, Jetpack Compose Material 3, Hilt, Room, Coil, and Firebase AI (Gemini).
- Preserve the existing Clean MVVM plus repository structure and package layout under `com.aurachat`.
- Prefer focused, minimal edits that fit the current architecture instead of introducing parallel patterns.
- Keep Compose UI state explicit and stable, especially around streaming chat updates and Room-backed state observation.
- Keep navigation contracts typed and route-safe.
- Do not add the `kotlin.android` plugin explicitly. AGP `9.2.0-alpha03` already bundles it.
- Preserve the AGP 9.x `compileSdk` DSL and keep KSP compatible with Kotlin `2.3.10`.
- Never hardcode secrets or API keys. `BuildConfig.GEMINI_API_KEY` must remain sourced from `local.properties`.
- When editing Gradle, Compose, Room, or coroutine code, prefer project conventions over generic Android boilerplate.
- See [CLAUDE.md](../CLAUDE.md) for the current project roadmap and feature-phase context.
