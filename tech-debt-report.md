# AuraChat — Technical Debt Report
**Generated:** 2026-03-10  
**Codebase state:** Phases 1–6 complete, Phase 7 (Settings) merged, Phase 8 (Polish) pending

---

## Scoring Key

**Priority = (Impact + Risk) × (6 − Effort)**

| Factor | Scale |
|---|---|
| Impact | 1–5: how much does this slow feature development or degrade UX? |
| Risk   | 1–5: what is the blast radius if we ignore this? |
| Effort | 1–5: how hard to fix? (lower = higher score, faster wins go up) |

---

## 🔴 Critical — Fix Immediately

### TD-01 · `ChatViewModelTest` constructor mismatch (Test Debt)
**Score: 40** · Impact 4 · Risk 4 · Effort 1

`ChatViewModel` now takes four constructor parameters, but `createViewModel()` in the test class only passes three (omitting `@ApplicationContext context: Context`). The test file does not compile in its current state, meaning **all 20 ChatViewModel unit tests are dark** — they provide zero coverage signal.

The `.bak` file (`ChatViewModelTest.kt.bak`) suggests the ViewModel constructor was recently changed and the test was partially updated, leaving a compile-time break.

**Fix:** Add a mock `ApplicationContext` to `createViewModel()`:
```kotlin
private val context: Context = mockk(relaxed = true)

private fun createViewModel() = ChatViewModel(
    savedStateHandle, context, getMessagesUseCase, sendMessageUseCase
)
```
Then also extract image-decoding to a `ImageDecoder` utility (see TD-07) so the ViewModel becomes testable without mocking `ContentResolver` calls.

---

## 🟠 High — Fix Within Current Sprint/Phase

### TD-02 · Alpha AGP 9.2.0-alpha03 (Dependency Debt)
**Score: 28** · Impact 3 · Risk 4 · Effort 2

The Android Gradle Plugin version `9.2.0-alpha03` is used as the build foundation. Alpha releases frequently introduce regressions, silently changed API contracts, or broken toolchain integration. This is especially risky given the non-standard `compileSdk { version = release(36) { minorApiLevel = 1 } }` DSL — if AGP changes how extension versioning works between alpha drops, the build silently breaks. The `CLAUDE.md` note about IDE Kotlin mismatch is already a symptom of toolchain drift.

**Fix:** Pin to the latest stable AGP (8.8.x as of early 2026) and revert `compileSdk` to the conventional integer form (`compileSdk = 35`). If the `minorApiLevel` feature is genuinely required, document *why* and track the AGP roadmap for stabilization.

---

### TD-03 · No automated build/test CI pipeline (Infrastructure Debt)
**Score: 24** · Impact 3 · Risk 3 · Effort 2

The repo has two GitHub Actions workflows — both are AI code-review hooks. There is no workflow that runs `./gradlew testDebugUnitTest` on pull requests. This means TD-01's broken tests have never been caught by CI, and any future regressions will also silently pass.

**Fix:** Add `.github/workflows/ci.yml`:
```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: ./gradlew testDebugUnitTest --stacktrace
```
Add `GEMINI_API_KEY` as a GitHub Secret (with an empty string default so build config compilation succeeds without a real key).

---

### TD-04 · Missing test coverage for HistoryViewModel, SettingsViewModel, DataStoreSettingsRepository (Test Debt)
**Score: 24** · Impact 3 · Risk 3 · Effort 2

The test suite covers `HomeViewModel`, `ChatViewModel`, and all 7 use cases. But three stateful components have zero coverage:

- `HistoryViewModel` — search filter logic, delete coordination, error branch in `catch`
- `SettingsViewModel` — model selection round-trip with DataStore
- `DataStoreSettingsRepository` — IOException fallback to defaults, write + read back

All three are straightforward to test with MockK + Turbine + a fake/in-memory DataStore. The absence is particularly risky for `HistoryViewModel` because the swipe-to-delete/cascade-delete path is complex.

---

## 🟡 Medium — Schedule for Next Phase

### TD-05 · `android.graphics.Bitmap` leaks into the domain layer (Architecture Debt)
**Score: 15** · Impact 3 · Risk 2 · Effort 3

`SendMessageUseCase` and `GeminiDataSource` both take `Bitmap?` as a parameter. `android.graphics.Bitmap` is a framework type from `android.*`, meaning the domain and data layers carry an implicit Android SDK dependency. This breaks the "domain is framework-agnostic" contract of Clean Architecture and forces any test touching these layers to run in an Android environment (or require Robolectric).

**Fix:** Replace `Bitmap?` with a thin wrapper `ImageData(val bytes: ByteArray, val mimeType: String)` in the domain layer. The ViewModel (presentation layer) is the appropriate place to decode the URI to bytes before calling into the domain. `GeminiDataSourceImpl` converts `ImageData` back to a `Bitmap` or passes bytes directly to the Firebase AI API.

---

### TD-06 · Pre-release Kotlin 2.3.10 / KSP 2.3.6 (Dependency Debt)
**Score: 15** · Impact 2 · Risk 3 · Effort 3

Neither `kotlin = "2.3.10"` nor `ksp = "2.3.6"` appear to be published stable releases. This matches the IDE warning noted in `CLAUDE.md` ("IDE Kotlin plugin < 2.3.10 — IDE-only, Gradle build is fine"), which suggests these are internal/dev builds pulled from a non-release channel. Gradle build passing today does not mean tooling stability over weeks of development.

**Fix:** Pin to the latest stable Kotlin (2.1.x) and matching KSP. If a specific 2.3.x feature is being evaluated, document it explicitly.

---

### TD-07 · Image decoding in ChatViewModel violates separation of concerns (Code Debt)
**Score: 12** · Impact 2 · Risk 1 · Effort 3

`ChatViewModel.startSend()` contains ~20 lines of `ImageDecoder`/`BitmapFactory` logic with API-version branching (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.P`). ViewModels should orchestrate state, not perform I/O or contain platform-version conditionals. This logic is also the reason the ViewModel takes `@ApplicationContext`, making it harder to test (see TD-01).

**Fix:** Extract to a `UriImageDecoder` or `ImageLoader` utility class injected into the ViewModel. This also sets the stage for the Bitmap→ImageData refactor in TD-05.

---

### TD-08 · `SendMessageUseCase` couples directly to `GeminiDataSource` (Architecture Debt)
**Score: 12** · Impact 2 · Risk 2 · Effort 3

The use case layer imports from `com.aurachat.data.remote`, breaking the dependency rule (inner layers should not reference outer layers). `GeminiDataSource` should be abstracted behind an `AIRepository` (or equivalent) interface at the domain boundary.

**Fix:** Introduce `domain/repository/AIRepository.kt` with `fun streamResponse(...): Flow<String>` and bind `GeminiDataSourceImpl` to it via `SettingsModule`. `SendMessageUseCase` depends on `AIRepository`, never on the Gemini SDK package.

---

### TD-09 · No README for a portfolio project (Documentation Debt)
**Score: 20** · Impact 3 · Risk 1 · Effort 1

There is no `README.md` at the project root. For a portfolio project intended to demonstrate clean Android architecture, the README is the first thing a reviewer reads. Without it, the project's tech choices, setup steps, and feature highlights are invisible.

**Fix:** Add a `README.md` covering: project summary, tech stack justification, architecture diagram (Clean MVVM layers), how to add `GEMINI_API_KEY` to `local.properties`, and screenshots.

---

### TD-10 · Release build minification disabled (Infrastructure Debt)
**Score: 20** · Impact 2 · Risk 2 · Effort 1

In `app/build.gradle.kts`, `isMinifyEnabled = false` for the release build type. This results in an unobfuscated APK with class and method names intact, and no dead-code elimination, increasing APK size unnecessarily.

**Fix:** Enable `isMinifyEnabled = true` for release and add keep rules for Hilt, Room, and Firebase to `proguard-rules.pro`. Start with AGP's suggested `proguard-android-optimize.txt` baseline.

---

### TD-11 · Stale/outdated AndroidX dependencies (Dependency Debt)
**Score: 20** · Impact 2 · Risk 3 · Effort 2

Several library versions are significantly behind:

| Library | Current in project | Latest stable |
|---|---|---|
| `core-ktx` | 1.10.1 | 1.16.0 |
| `activity-compose` | 1.8.0 | 1.10.0 |
| `security-crypto` | **1.1.0-alpha06** | 1.1.0-rc01 |
| `espresso-core` | 3.5.1 | 3.6.1 |
| `coil-compose` | 2.6.0 | 3.1.0 |
| `coroutines` | 1.8.1 | 1.10.1 |

The `security-crypto` alpha is particularly notable — it is used for `EncryptedSharedPreferences` API key storage and an alpha release is production-risk.

**Fix:** Run `./gradlew dependencyUpdates` (after adding the Versions plugin), bump `core-ktx`, `activity-compose`, and `security-crypto` to their current stable versions as a batch. Coil 3.x is a larger migration (API changes) and should be a separate PR.

---

## 🟢 Low — Track and Fix as Capacity Allows

### TD-12 · Home-rolled Markdown parser in `MarkdownText.kt` (Code Debt)
**Score: 9** · Impact 2 · Risk 2 · Effort 4

`MarkdownText.kt` manually parses ` ``` `, `**bold**`, `*italic*`, `` `code` ``, and `- bullet` via regex. It does not handle nested formatting, headers (`#`), links, tables, or numbered lists — all of which Gemini routinely emits. The custom parser will silently fail (rendering raw asterisks) on unsupported syntax.

**Fix:** Replace with a maintained library such as `compose-markdown` (Compose Multiplatform) or `Markwon` (renders to `AnnotatedString`). Coordinate with Phase 8 polish work.

---

### TD-13 · `.bak` files and stale `.gitkeep` files committed (Code Debt)
**Score: 10** · Impact 1 · Risk 1 · Effort 1

`ChatViewModelTest.kt.bak` and `HomeViewModelTest.kt.bak` are committed to the repo. Several `.gitkeep` files remain in directories that now contain content (e.g., `data/remote/`, `domain/model/`). These are noise in `git log` and `git diff` and give a poor impression in portfolio reviews.

**Fix:** `git rm *.bak` and `git rm **/.gitkeep` where the directory is non-empty. Add `*.bak` to `.gitignore`.

---

### TD-14 · `GeminiDataSourceImpl` creates a new `GenerativeModel` on every request (Code Debt)
**Score: 12** · Impact 2 · Risk 1 · Effort 2

The comment in `GeminiModule.kt` explains this is intentional — so model switching in Settings takes effect immediately. However, instantiating a `GenerativeModel` per send means Firebase re-initializes the model connection on every message. A better approach caches the model by name:

```kotlin
private val modelCache = ConcurrentHashMap<String, GenerativeModel>()
private fun getModel(name: String) = modelCache.getOrPut(name) { createModel(name) }
```

When settings change, the new name results in a cache miss; the old entry is never used again and GC'd.

---

### TD-15 · `RoomChatRepository.saveMessage` issues 3 DB operations per save (Code Debt)
**Score: 6** · Impact 1 · Risk 1 · Effort 3

Every message save performs: `insertMessage` + `getSessionById` + `updateSession` (3 queries) to keep session metadata denormalized. For a chat app this is not a bottleneck, but it is fragile: if the session is deleted between `insertMessage` and `getSessionById`, the `saveMessage` call silently skips the metadata update. A Room `@Transaction` annotation would make this atomic.

---

## Remediation Roadmap

### Phase 8 Polish Sprint (immediate)
Fix critical and high-priority items alongside the existing Phase 8 work since these unblock testing signal:

1. **TD-01** — Fix `ChatViewModelTest` constructor (30 min)
2. **TD-03** — Add CI workflow (1 hour)
3. **TD-04** — Write tests for `HistoryViewModel`, `SettingsViewModel`, `DataStoreSettingsRepository` (3–4 hours)
4. **TD-09** — Write README (1–2 hours)
5. **TD-10** — Enable release minification + proguard rules (1 hour)
6. **TD-13** — Remove `.bak` and stale `.gitkeep` files (15 min)

### Post-Phase 8 Hardening Sprint
Address structural issues that require broader code changes:

7. **TD-05 + TD-07** — Extract `ImageData` wrapper, move image decoding out of ViewModel (half day)
8. **TD-08** — Introduce `AIRepository` domain interface (2–3 hours)
9. **TD-02 + TD-06** — Pin AGP and Kotlin to stable releases; test build (2–4 hours, may require build fixes)
10. **TD-11** — Batch AndroidX dependency updates (1–2 hours)
11. **TD-14** — Add model cache in `GeminiDataSourceImpl` (30 min)

### Before First External Demo
12. **TD-12** — Replace custom Markdown parser with a library (half day)
13. **TD-15** — Wrap `saveMessage` in `@Transaction` (30 min)

---

## Summary Table

| ID | Item | Category | Score | Effort | Phase |
|---|---|---|---|---|---|
| TD-01 | ChatViewModelTest broken constructor | Test | **40** | Low | Phase 8 |
| TD-09 | No README | Docs | 20 | Low | Phase 8 |
| TD-10 | Release minification disabled | Infra | 20 | Low | Phase 8 |
| TD-13 | .bak files in repo | Code | 10 | Low | Phase 8 |
| TD-02 | Alpha AGP 9.2.0-alpha03 | Deps | 28 | Medium | Hardening |
| TD-03 | No CI build/test pipeline | Infra | 24 | Medium | Phase 8 |
| TD-04 | Missing ViewModel/Repository tests | Test | 24 | Medium | Phase 8 |
| TD-11 | Stale AndroidX deps | Deps | 20 | Medium | Hardening |
| TD-05 | Bitmap in domain layer | Arch | 15 | Medium | Hardening |
| TD-06 | Pre-release Kotlin 2.3.10 | Deps | 15 | Medium | Hardening |
| TD-08 | UseCase depends on GeminiDataSource | Arch | 12 | Medium | Hardening |
| TD-14 | GenerativeModel created per request | Code | 12 | Low | Hardening |
| TD-07 | Image decoding in ViewModel | Code | 12 | Medium | Hardening |
| TD-15 | N+1 in saveMessage | Code | 6 | Medium | Demo |
| TD-12 | Custom Markdown parser | Code | 9 | High | Demo |
