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
  See [CLAUDE.md](../CLAUDE.md) for the current project roadmap and feature-phase context.

# Agent Prompt Examples

- "Build the app and run unit tests"
- "Add a new use case to domain/usecase for chat summarization"
- "Fix Room streaming bug in ChatViewModel"
- "Polish error state UI in ChatScreen"
- "Implement SettingsScreen with model picker and About section"

# Specialized Instructions

## Tests

- applyTo: **/src/test/**
- Use JUnit 4, MockK, Turbine, kotlinx-coroutines-test
- Prefer coroutine test dispatchers and Room in-memory DB for unit tests
- Keep test names descriptive and phase-aligned

## Settings Phase (P8)

- applyTo: **/presentation/settings/**, **/data/settings/**
- Use DataStore for settings persistence
- SettingsRepository must expose selected model as Flow<String>
- SettingsViewModel should update model selection atomically

## Polish Phase (P9)

- applyTo: **/presentation/**, **/ui/components/**
- Add error state handling, retry logic, typing indicator animation, and screen transitions
- Prefer explicit UI state and minimal side effects

# Custom Agent Hooks/Skills

- Streaming chat: Use explicit streamingText in ChatUiState, clear atomically with Room emission
- Room observation: Prefer StateFlow, avoid race-prone ownership
- Error handling: Add retry logic and error state composables
- Navigation: Typed route contracts, long args for chat/{sessionId}
