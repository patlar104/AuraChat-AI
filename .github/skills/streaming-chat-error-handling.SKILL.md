---
name: Streaming Chat & Error Handling
category: agent-hook
applyTo:
  - app/src/main/java/com/aurachat/presentation/chat/ChatViewModel.kt
  - app/src/main/java/com/aurachat/presentation/chat/ChatUiState.kt
  - app/src/main/java/com/aurachat/presentation/chat/ChatScreen.kt
  - app/src/test/java/com/aurachat/presentation/chat/ChatViewModelTest.kt
---

# Streaming Chat & Error Handling Skill

**Purpose:**
Automate streaming chat UI, Room observation, and error state handling in Compose screens and ViewModels.

**Principles:**

- Use explicit `streamingText` in `ChatUiState`, clear atomically with Room emission.
- Prefer `StateFlow` for UI state, avoid race-prone ownership.
- Add retry logic and error state composables.
- Typed navigation contracts (e.g., `chat/{sessionId}` as Long).

**Agent Hooks:**

- Streaming: When `isStreaming` is true, update `streamingText` word-by-word; clear on Room emission.
- Error: On error, set `errorMessage` in UI state; show error composable with retry button.
- Retry: On retry, clear `errorMessage` and re-send last input.

**Example Prompts:**

- "Fix streaming bug in ChatViewModel"
- "Add retry logic to ChatScreen error state"
- "Polish error handling for Room observation"

**Best Practices:**

- Always clear `streamingText` atomically with Room emission to avoid UI race conditions.
- Use `StateFlow` for all UI state exposure in ViewModels.
- Error composables should be minimal and explicit, with a single retry action.
- Navigation arguments must be typed and route-safe.
