# AuraChat — Codex Project Memory

## Project
Android AI chat app powered by Google Gemini (Firebase AI SDK). Portfolio project.
PRD: `/Users/patricklarocque/Downloads/AuraChat_PRD.docx`

## Tech Stack
- Language: Kotlin, Jetpack Compose (Material 3)
- Architecture: Clean MVVM + Repository
- AI: Firebase AI (Gemini) — `firebase-ai` dependency
- DB: Room + KSP
- DI: Hilt
- Nav: Navigation Compose
- Image: Coil
- Build: Gradle KTS, AGP 9.2.0-alpha03, Kotlin 2.3.10, KSP 2.3.6

## Key Config Notes
- AGP 9.2.0-alpha03 bundles Kotlin Android internally — do NOT apply `kotlin.android` plugin explicitly (causes duplicate extension error)
- `compileSdk` uses AGP 9.x DSL: `compileSdk { version = release(36) { minorApiLevel = 1 } }`
- KSP version is `2.3.6` (must match Kotlin 2.3.10)
- ⚠️ IDE shows Kotlin version mismatch errors (IDE Kotlin plugin < 2.3.10) — these are IDE-only, Gradle build is fine
- Gemini API key loaded from `local.properties` → `BuildConfig.GEMINI_API_KEY`

## Package Structure
`com.aurachat/`
- `data/local/` — Room DAOs, Database, Entities
- `data/remote/` — GeminiDataSource
- `data/repository/` — RoomChatRepository
- `domain/model/` — ChatSession, ChatMessage
- `domain/repository/` — ChatRepository interface
- `domain/usecase/` — use cases
- `presentation/home/` — HomeScreen, HomeViewModel
- `presentation/chat/` — ChatScreen, ChatViewModel
- `presentation/history/` — DrawerContent, HistoryViewModel
- `presentation/settings/` — SettingsScreen, SettingsViewModel (Phase 7 — not yet created)
- `ui/theme/` — Color.kt, Type.kt, Theme.kt
- `ui/components/` — shared composables (MessageBubble, MarkdownText)

## Build Status by Phase
*(Task Plan uses 9 phases P1–P9; mapped here to 8 for clarity)*
- Phase 1 ✅ — Project setup, Gradle, Hilt, Theme (Color/Type/Theme.kt), MainActivity scaffold
- Phase 2 ✅ — Room entities, DAOs, AuraChatDatabase, DatabaseModule
- Phase 3 ✅ — FirebaseAIDataSource streaming Flow, AIModule, GeminiDataSourceImpl
- Phase 4 ✅ — ChatRepository interface + RoomChatRepository, RepositoryModule, all 7 use cases
- Phase 5 ✅ — ChatScreen, ChatViewModel, streaming bubble, MessageBubble, Markdown rendering (MarkdownText.kt), Image attachment (photo picker → Bitmap → GeminiVision)
- Phase 6 ✅ — DrawerContent, HistoryViewModel (search filter), swipe-to-delete (SwipeToDismissBox end-to-start, CASCADE delete)
- Phase 7 ⬜ — Settings: DataStore + SettingsRepository (P8-T1), SettingsScreen + SettingsViewModel (P8-T2), wire model selection to AIModule (P8-T3)
- Phase 8 ⬜ — Polish: error states + retry (P9-T1), typing indicator animation (P9-T2), screen transitions (P9-T3), final review (P9-T4)

## Phase 7 — Settings Detail (next up)
- P8-T1: `SettingsRepository` interface + `DataStoreSettingsRepository`; key = selected model (default `gemini-2.0-flash`); available models: `['gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-1.5-pro']`; add `SettingsModule` Hilt binding
- P8-T2: `SettingsViewModel` reads/writes via SettingsRepository; `SettingsScreen` with RadioButton model picker + About section (app version, GitHub link)
- P8-T3: Update `AIModule` / `GeminiModule` to read selected model from `SettingsRepository` so model switch in settings affects next chat

## Nav Routes (MainActivity.kt — NavRoutes object)
- `home` → HomeScreen ✅
- `chat/{sessionId}` → ChatScreen ✅ (Long arg via NavType.LongType + SavedStateHandle)
- `settings` → SettingsPlaceholder (Phase 7 will replace)

## Streaming Handoff Pattern (Phase 5)
- `streamingText: String?` in ChatUiState — null = no stream, `""` = started, non-empty = live text
- `observeMessages()` clears `streamingText` atomically with Room emission: `if (state.isStreaming) state.streamingText else null`
- This works because `Dispatchers.Main.immediate` guarantees `isStreaming = false` runs before Room's queued notification fires

## AMOLED Color Palette
| Token | Hex | Usage |
|---|---|---|
| AuraBackground | #000000 | Screen backgrounds |
| AuraSurface | #0D0D0D | Cards, bubbles |
| AuraSurfaceVariant | #1A1A1A | Input bars, drawer |
| AuraPrimary | #8AB4F8 | Buttons, links |
| AuraTertiary | #D2A8FF | Suggestion chips |
| AuraOnBackground | #E8EAED | Primary text |
| AuraOnSurfaceVariant | #9AA0A6 | Timestamps |
