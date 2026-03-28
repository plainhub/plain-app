# Copilot Instructions for PlainApp

> **Start here**: Read `docs/ARCHITECTURE.md` for full project structure, directory map, and naming conventions.

## Key Rules

- **No DI framework** — Use companion objects, singletons, `SystemServices.kt`. Never add Hilt/Dagger.
- **Event bus** — `sendEvent()` / `receiveEvent<T>` from `lib/channel/`. Use for cross-component communication.
- **Coroutines** — Use `coIO`, `coMain` from `lib/helpers/CoroutinesHelper.kt` for side effects.
- **State** — `MutableStateFlow` in ViewModels, collected by Compose.
- **Short paths** — `app/.../plain/` = `app/src/main/java/com/ismartcoding/plain/`, `lib/.../lib/` = `lib/src/main/java/com/ismartcoding/lib/`
- **Max 150 lines per file** — Split into smaller files when a file reaches 150 lines. It is strictly forbidden to reduce line count by removing blank lines or whitespace — every split must be a genuine logical decomposition.

## Naming Conventions

| Prefix/Suffix | Meaning | Example |
|---------------|---------|---------|
| `D` prefix | Data/DB entity | `DChat`, `DAudio`, `DNote` |
| `V` prefix | View data class | `VChat`, `VPackage` |
| `P` prefix | Reusable Compose component | `PAlert`, `PCard`, `PSwitch` |
| `*Helper.kt` | Stateless utility | `NoteHelper`, `TagHelper` |
| `*ViewModel.kt` | ViewModel | `ChatViewModel`, `AudioViewModel` |
| `*Page.kt` | Full screen composable | `ChatPage`, `NotesPage` |

## Build Commands

```bash
./gradlew :app:assembleGithubDebug     # GitHub debug APK
./gradlew :app:assembleGoogleRelease   # Google Play release
./gradlew test                         # Unit tests
```

## i18n

String resources are split by feature: `app/src/main/res/values/strings_{feature}.xml`. 16 locales under `values-{locale}/`.

**Sync translations** ("同步翻译"):
```bash
node scripts/i18n-find-untranslated.mjs   # detect missing keys
node scripts/i18n-translate-todo.mjs       # translate via Google Translate
node scripts/i18n-apply-todo.mjs           # apply to locale files
node scripts/i18n-find-untranslated.mjs    # verify: "Total: 0 missing, 0 untranslated"
```
