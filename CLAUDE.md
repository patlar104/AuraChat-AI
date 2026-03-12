# AuraChat — Claude Project Memory

## Project
Android AI chat app powered by Google Gemini (Firebase AI SDK). Portfolio project demonstrating clean MVVM architecture, real-time streaming, and Room persistence.

## Tech Stack
- Language: Kotlin, Jetpack Compose (Material 3)
- Architecture: Clean MVVM + Repository pattern
- AI: Firebase AI (Gemini) — `firebase-ai` dependency
- DB: Room 2.8.4 + KSP
- DI: Hilt 2.59.2
- Nav: Navigation Compose (Kotlinx Serialization type-safe routes)
- Image: Coil 2.7.0
- Settings: DataStore Preferences 1.2.0
- Build: Gradle KTS, AGP 9.1.0, Kotlin 2.3.10, KSP 2.3.6

## Key Config Notes
- AGP 9.1.0 bundles Kotlin Android internally — do NOT apply `kotlin.android` plugin explicitly (causes duplicate extension error)
- `compileSdk` uses AGP 9.x DSL: `compileSdk { version = release(36) { minorApiLevel = 1 } }`
- `minSdk = 24`, `targetSdk = 36`, `jvmToolchain(17)`
- KSP version `2.3.6` must match Kotlin `2.3.10`
- ⚠️ IDE shows Kotlin version mismatch errors (IDE Kotlin plugin < 2.3.10) — IDE-only, Gradle build is fine
- Gemini API key loaded from `local.properties` → `BuildConfig.GEMINI_API_KEY`
- Room schema exported to `app/schemas/` (KSP argument)
- Compose BOM: `2026.02.01`

## Package Structure
`com.aurachat/`
- `data/local/` — Room entities (`ChatSessionEntity`, `ChatMessageEntity`), DAOs (`ChatSessionDao`, `ChatMessageDao`), `AuraChatDatabase`
- `data/remote/` — `GeminiDataSource` interface + `GeminiDataSourceImpl`
- `data/repository/` — `RoomChatRepository`
- `data/settings/` — `DataStoreSettingsRepository`
- `domain/model/` — `ChatSession`, `ChatMessage`, `MessageRole`
- `domain/repository/` — `ChatRepository`, `SettingsRepository` interfaces
- `domain/usecase/` — 9 use cases (see Use Cases section)
- `domain/error/` — `DomainError` sealed class hierarchy
- `di/` — Hilt modules (`DatabaseModule`, `GeminiBindingModule`, `SettingsModule`, `SettingsBindingModule`, `AppGraphEntryPoint`)
- `navigation/` — `NavRoutes` (type-safe Kotlinx Serialization routes)
- `presentation/home/` — `HomeScreen`, `HomeViewModel`, `HomeUiState`
- `presentation/chat/` — `ChatScreen`, `ChatViewModel`, `ChatUiState`
- `presentation/history/` — `DrawerContent`, `HistoryViewModel`, `HistoryUiState`
- `presentation/settings/` — `SettingsScreen`, `SettingsViewModel`, `SettingsUiState`
- `ui/theme/` — `Color.kt`, `Type.kt`, `Theme.kt`
- `ui/components/` — `MessageBubble.kt`, `MarkdownText.kt`, `TestTags.kt`
- `util/` — `Constants.kt`, `ImageAttachmentStore.kt`
- Root: `MainActivity.kt`, `AuraChatApplication.kt`

## Build Status by Phase
- Phase 1 ✅ — Project setup, Gradle, Hilt, Theme (Color/Type/Theme.kt), MainActivity scaffold
- Phase 2 ✅ — Room entities, DAOs, AuraChatDatabase, DatabaseModule
- Phase 3 ✅ — GeminiDataSourceImpl streaming Flow, GeminiBindingModule
- Phase 4 ✅ — ChatRepository interface + RoomChatRepository, RepositoryModule, 9 use cases, DomainError hierarchy
- Phase 5 ✅ — ChatScreen, ChatViewModel, streaming bubble, MessageBubble, MarkdownText, image attachment (photo picker → Bitmap → GeminiVision)
- Phase 6 ✅ — DrawerContent, HistoryViewModel (search filter), swipe-to-delete (SwipeToDismissBox end-to-start, CASCADE delete)
- Phase 7 ✅ — Settings: DataStore + SettingsRepository, SettingsScreen + SettingsViewModel (RadioButton model picker, About section), model selection wired to GeminiDataSourceImpl
- Phase 8 ⬜ — Polish: error states + retry (P9-T1), typing indicator animation (P9-T2), screen transitions (P9-T3), final review (P9-T4)

## Use Cases (9 total, all @Inject)
1. `CreateSessionUseCase` — validates title, creates Room session
2. `SendMessageUseCase` — saves user message, streams Gemini response, saves AI response, auto-titles session on first message (truncated to 60 chars)
3. `SaveMessageUseCase` — persists message with validation
4. `GetMessagesUseCase` — observes messages for session as `Flow<List<ChatMessage>>`
5. `GetSessionsUseCase` — observes all sessions as `Flow<List<ChatSession>>`
6. `DeleteSessionUseCase` — deletes session (Room CASCADE removes messages)
7. `UpdateSessionTitleUseCase` — updates session title with validation
8. `StartChatSessionUseCase` — creates session with a `pendingInitialPrompt` field for auto-send
9. `ConsumePendingInitialPromptUseCase` — claims pending prompt atomically via `withTransaction` (one-shot)

## Error Hierarchy (`domain/error/DomainError.kt`)
```kotlin
sealed class DomainError {
    object NetworkError : DomainError()
    data class ApiError(val code: Int) : DomainError()
    data class DatabaseError(val cause: Throwable) : DomainError()
    data class ValidationError(val message: String) : DomainError()
    data class UnknownError(val cause: Throwable) : DomainError()
}
```

## Nav Routes (`navigation/NavRoutes.kt`)
Type-safe routes via Kotlinx Serialization — NOT string-based routes:
```kotlin
@Serializable object HomeRoute
@Serializable data class ChatRoute(val sessionId: Long)
@Serializable object SettingsRoute
```
Navigation uses `launchSingleTop = true` and slide+fade transitions for all routes.

## Gemini / AI Configuration (`util/Constants.kt`)
- `DEFAULT_MODEL = "gemini-2.5-flash"`
- `AVAILABLE_MODELS = ["gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"]`
- `HISTORY_LIMIT = 20` (past messages sent to Gemini; error messages excluded)
- `TEMPERATURE = 0.7`, `TOP_K = 40`, `TOP_P = 0.95`, `MAX_OUTPUT_TOKENS = 8192`
- `MAX_VISION_IMAGE_EDGE_PX = 1024` (Bitmap downscaled before sending)
- `GeminiDataSourceImpl` reads selected model from `SettingsRepository` on each request (dynamic model switching, no singleton GenerativeModel)

## Streaming Handoff Pattern (Phase 5)
- `streamingText: String?` in `ChatUiState` — `null` = no stream, `""` = stream started, non-empty = live text
- `observeMessages()` in `ChatViewModel` clears `streamingText` atomically with Room emission:
  `if (state.isStreaming) state.streamingText else null`
- Works because `Dispatchers.Main.immediate` guarantees `isStreaming = false` runs before Room's queued notification fires

## Image Attachment Flow
1. Photo picker → `Uri`
2. `ImageAttachmentStore.importSelectedImage()` copies to app storage → `pendingImageUri` in state
3. On send: `ImageAttachmentStore.decodeVisionBitmap()` downscales to ≤1024px max edge
4. `Bitmap` passed to `GeminiDataSourceImpl` → multimodal path via `generateContentStream`
5. Text-only requests use chat session history; image requests bypass history (single-turn vision)
6. `lastFailedPrompt` / `lastFailedImageUri` stored in `ChatUiState` for retry without re-typing

## Session Auto-Titling
- `SendMessageUseCase` detects first message (empty history before save)
- Calls `UpdateSessionTitleUseCase` with `prompt.take(MAX_TITLE_LENGTH)` (60 chars)

## Session Denormalization
`RoomChatRepository.saveMessage()` updates `ChatSessionEntity` fields (`preview`, `messageCount`, `updatedAt`) on every message save to avoid JOIN queries in history list.

## Settings Implementation
- `DataStoreSettingsRepository` persists selected model in `settings.preferences_pb` file
- `SettingsScreen` has RadioButton model picker + About section (app version, GitHub link)
- Model change in Settings affects the next chat immediately (read per-request in `GeminiDataSourceImpl`)

## Room Database
- **Database**: `AuraChatDatabase` version 3, destructive migration enabled (dev mode)
- **Entities**:
  - `ChatSessionEntity` — indexed on `updated_at`
  - `ChatMessageEntity` — ForeignKey CASCADE to sessions, composite index `(session_id, timestamp)`
- **Mappers**: `toDomain()` and `toEntity()` extension functions on all entities
- Schema: exported to `app/schemas/` for migration tracking

## DI Modules
- `DatabaseModule` — singleton `AuraChatDatabase`, provides DAOs
- `RepositoryModule` — binds `RoomChatRepository` → `ChatRepository`
- `GeminiBindingModule` — binds `GeminiDataSourceImpl` → `GeminiDataSource` (no singleton model)
- `SettingsModule` — provides singleton `DataStore<Preferences>`
- `SettingsBindingModule` — binds `DataStoreSettingsRepository` → `SettingsRepository`
- `AppGraphEntryPoint` — `@EntryPoint` for runtime graph access

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
| Error | #F28B82 | Error states |

## Testing
- **Unit Tests** (JUnit 5 + MockK + Turbine): 9 use case tests, 4 ViewModel tests (`HomeViewModelTest`, `ChatViewModelTest`, `HistoryViewModelTest`, `SettingsViewModelTest`), `DataStoreSettingsRepositoryTest`, `ImageAttachmentStoreTest`
- **Instrumented Tests** (Espresso): `HomeScreenTest`, `ChatScreenTest`, `DrawerContentTest`, `SettingsScreenTest`, `RoomChatRepositoryTest`, `MainActivitySmokeTest`
- **Screenshot Tests**: `AuraChatScreenshotTests`, `ScreenshotFixtures`
- Test tags defined in `ui/components/TestTags.kt` for all interactive elements
- JUnit 5 platform enabled in `app/build.gradle.kts`

## Phase 8 — Polish Detail (next up)
- P9-T1: Error states + retry UI refinement (infrastructure exists in `ChatUiState.lastFailedPrompt`)
- P9-T2: Typing indicator animation (referenced in TestTags, not yet in ChatScreen)
- P9-T3: Screen transitions (slide+fade partially implemented in NavHost)
- P9-T4: Final review — proguard rules, release build validation, README update
