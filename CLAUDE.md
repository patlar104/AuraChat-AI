# AuraChat — Claude Project Memory

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
- Build: Gradle KTS, AGP 9.2.0-alpha02, Kotlin 2.1.0

## Key Config Notes
- AGP 9.2.0-alpha02 bundles Kotlin Android internally — do NOT apply `kotlin.android` plugin explicitly (causes duplicate extension error)
- `compileSdk` uses AGP 9.x DSL: `compileSdk { version = release(36) { minorApiLevel = 1 } }`
- KSP version must match Kotlin: `2.1.0-1.0.29`
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
- `presentation/settings/` — SettingsScreen, SettingsViewModel
- `ui/theme/` — Color.kt, Type.kt, Theme.kt
- `ui/components/` — shared composables (MessageBubble)

## Build Status by Phase
- Phase 1 ✅ — Theme (Color/Type/Theme.kt), MainActivity with NavHost + ModalNavigationDrawer
- Phase 2 ✅ — Room entities, DAOs, Database, Repository
- Phase 3 ✅ — GeminiDataSource + streaming Flow + SendMessageUseCase
- Phase 4 ✅ — HomeScreen + HomeViewModel (suggestion chips, HomeInputBar, navigateToSessionId nav)
- Phase 5 ✅ — ChatScreen, ChatViewModel, streaming bubble, MessageBubble
- Phase 6 ⬜ — DrawerContent, HistoryViewModel, swipe-to-delete
- Phase 7 ⬜ — SettingsScreen, EncryptedSharedPreferences, DataStore
- Phase 8 ⬜ — Animations, error handling, polish

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
