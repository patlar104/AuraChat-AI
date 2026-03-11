# AuraChat

An Android AI chat app powered by Google Gemini — built as a portfolio project demonstrating Clean Architecture, Jetpack Compose, and modern Android development practices.

## Features

- Real-time streaming responses from Gemini (text appears word-by-word as it generates)
- Image attachment support — pick a photo and ask Gemini about it
- Full conversation history with swipe-to-delete
- Model picker (Gemini 2.5 Flash, 2.0 Flash, 1.5 Flash, 1.5 Pro)
- AMOLED-optimised dark theme (true black backgrounds)
- Markdown rendering: bold, italic, inline code, code blocks, bullet lists
- Typing indicator animation while the model responds
- Error state with one-tap retry

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean MVVM + Repository pattern |
| AI | Firebase AI SDK (Gemini) |
| Database | Room (SQLite) |
| Dependency Injection | Hilt |
| Navigation | Navigation Compose |
| Settings persistence | Jetpack DataStore (Preferences) |
| Image loading | Coil |
| Build | Gradle KTS, AGP 9.2.0, Kotlin 2.3.10, KSP |
| Testing | JUnit 4, MockK, Turbine, kotlinx-coroutines-test |

## Architecture

```
com.aurachat/
├── data/
│   ├── local/          # Room entities, DAOs, database
│   ├── remote/         # GeminiDataSource (Firebase AI)
│   ├── repository/     # RoomChatRepository
│   └── settings/       # DataStoreSettingsRepository
├── domain/
│   ├── model/          # ChatSession, ChatMessage (pure Kotlin)
│   ├── repository/     # ChatRepository, SettingsRepository (interfaces)
│   └── usecase/        # 7 use cases (one responsibility each)
├── presentation/
│   ├── home/           # HomeScreen + HomeViewModel
│   ├── chat/           # ChatScreen + ChatViewModel
│   ├── history/        # DrawerContent + HistoryViewModel
│   └── settings/       # SettingsScreen + SettingsViewModel
├── di/                 # Hilt modules
└── ui/
    ├── theme/          # Color, Type, Theme
    └── components/     # MessageBubble, MarkdownText
```

Dependencies flow inward: `presentation → domain ← data`. The domain layer contains no Android framework types.

## Setup

1. Clone the repository
2. Authenticate GitHub CLI for Git operations:
   ```bash
   gh auth login
   gh auth status
   ```
3. Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
4. Create `local.properties` in the project root (if it doesn't exist) and add:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
5. Open in Android Studio and run on a device or emulator (API 24+)

> Each clone or worktree needs its own `local.properties`. The app builds and syncs without an API key, but Gemini requests will fail until `GEMINI_API_KEY` is set. Do not commit credentials, PATs, or machine-specific SDK paths.

## Toolchain

- Gradle runtime: JDK 21
- App bytecode target: Java 11 / Kotlin JVM target 11
- Git transport: HTTPS with `gh`-managed credentials

Use Android Studio's embedded JDK 21 or a system JDK 21 for sync/builds. Avoid storing credential helpers, user identity, or branch workflow preferences in repo-tracked files.

## Git Workflow

- Default branch: `main`
- Feature branches: create short-lived branches from `main`
- Pull strategy: set these locally, not in the repo:
  ```bash
  git config --global pull.rebase true
  git config --global fetch.prune true
  git config --global rebase.autoStash true
  git config --global push.autoSetupRemote true
  git config --global rerere.enabled true
  ```
- Remote auth: prefer `gh auth git-credential`; avoid manually storing PATs in local repo config
- Token scope hygiene: this repo does not need admin-level GitHub scopes for normal clone, push, PR, and workflow operations; rotate overly broad `gh` auth sessions when practical
- Pull requests: branch protection expects PR-based changes to `main`; direct pushes should be reserved for repository admins only

If your machine already has multiple GitHub credential helpers configured, simplify them so `gh auth git-credential` is the GitHub-specific helper and remove redundant GitHub helper entries from your global Git config.

## Running Tests

```bash
./gradlew testDebugUnitTest
```

The test suite covers all 7 use cases, HomeViewModel, ChatViewModel, HistoryViewModel, SettingsViewModel, and DataStoreSettingsRepository using MockK and Turbine.
