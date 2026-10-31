# plain-common

Reusable Kotlin Multiplatform code extracted from PlainApp. Source packages are
kept unchanged under `com.ismartcoding.plain`.

The module includes common helpers, crypto, extensions, logcat, XML/RSS support,
and shared Compose Multiplatform themes, components, icons, editors and scanners. Supporting sources are included here so this module
has no dependency on the host application.

BLE protocol and native codec adapters belong to [plain-rs features](../../plain-desktop/plain-rs/README.md). Platform GATT adapters stay in the host shared module. This Kotlin library contains no BLE source.

## Maven coordinates

```kotlin
implementation("com.ismartcoding:plain-common:0.2.0")
```

Add the public GitHub Pages Maven repository to the consuming project's
repositories:

```kotlin
maven {
    url = uri("https://plainhub.github.io/plain-app/maven")
}
```

Replace the previous `plain-common` and `plain-ui` dependencies with this single
dependency. Existing Kotlin packages and `com.ismartcoding.plain.ui.resources`
imports remain unchanged.

Releases use tags named
`plain-common-v<version>`; the default local version can be changed with
`-PplainCommonVersion=…`.

## Publishing

The `Publish plain-common` GitHub Actions workflow builds the KMP publications
and adds the Maven files to the dedicated `maven` branch under `maven/` when a
`plain-common-v<version>` tag is pushed. The workflow also syncs `policy.html` and `terms.html` from main so their existing public URLs stay available. It can also be run manually with a
version number. The workflow uses its temporary `GITHUB_TOKEN`; no publishing
credentials are stored in the repository.

For a local Maven cache, run `./gradlew :plain-common:publishToMavenLocal`.

## Compose UI contract

Shared Compose Multiplatform components in `com.ismartcoding.plain.ui`.
This is the component selection entry point for PlainApp and PlainNet (plain-router-app).

Readable UI text has a minimum size of 14sp, including supporting text, timestamps, navigation labels and button labels. Use the shared `MaterialTheme.typography` styles; do not shrink text to fit. Let labels wrap or adjust layout, and preserve system font scaling. Compact controls retain a 48dp touch target. Keep page calls to state, events and layout modifiers; shared visual defaults belong to components and the theme.
Before editing UI, choose the scene below, open its compiled example, and read the actual component signature. Pages compose existing components and connect state and events.

| Scene | Use | Compiled example |
|---|---|---|
| Page and top bar | `PScaffold` + `PTopAppBar` | [ComponentShowcasePage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ComponentShowcasePage.kt) |
| Primary, outlined, text action | `PFilledButton`, `POutlinedButton`, `PTextButton`; size via `ButtonSize`, role via `ButtonType` | [Button states](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ShowcaseComponentStates.kt) |
| Text input, password, validation | `PTextField`; choose `enabled` and `readOnly` independently | [Form states](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ShowcaseComponentStates.kt) |
| Single choice between a few options | `PSegmentedButtons` | [Segmented buttons](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ShowcaseSegmentedButtons.kt) |
| Cards, list rows, banners, filters, removable chips | `PCard`, `PListItem`, `PBanner`, `PFilterChip`, `PInputChip` | [Component showcase](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ComponentShowcasePage.kt) |
| Bottom sheet | `PModalBottomSheet` + `PBottomSheetTopAppBar`; primary actions use `PSheetPrimaryActionsCard`, overflow uses `PSheetActionCard` | [FileInfoBottomSheet](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/files/FileInfoBottomSheet.kt) |
| Confirmation and input dialog | Host `confirmActionAsync` / `DialogHelper`, `TextFieldDialog`, `RadioDialog`; selection rows use `PDialogRadioRow` | [DialogHelper](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/components/DialogHelper.kt) |
| Icon with label below | `PIconTextSmallButton` for compact toolbar; `PIconTextActionButton` for shortcuts; `PSheetPrimaryAction` for sheet actions | [State showcase](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/ShowcaseComponentStates.kt) |
| Boolean / radio / simple icon control | Official Material3 `Switch`, `RadioButton`, `IconButton`; use host icon shortcuts when applicable | [Dark theme choices](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/settings/DarkThemePage.kt) |
| Live scrolling curve with gradient fill and optional markers/selection | `LiveLineChart` + `LiveChartPlayback`; callers map samples and own domain meaning | [SoundMeterPage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/tools/SoundMeterPage.kt) |
| Media seek, animated waveform, countdown | `PlayerSlider`, `WaveSlider`, `CircularTimer` | [PomodoroPage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/pomodoro/PomodoroPage.kt) |
| Large document, scan, drag selection, fast scroll, pull refresh | `CodeEditor`, `QrCodeScanner`, `gridDragSelect`, fastscroll and pullrefresh packages | [TextFilePage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/media/TextFilePage.kt) |

The first eight scenes require the existing project components. Basic layout (`Box`, `Row`, `Column`), text, images and controls in the official-control row can use Compose directly. Specialized editor, scanner and gesture engines keep their own behavior; inspect their contracts before extending them. Host dialog helpers use each application's resources and state, and stay outside this library.

Use `AppTheme` as the complete theme entry. Keep the brand palette, soft dark text and AMOLED behavior; dynamic color is excluded. Shapes come from the theme: `medium` for ordinary cards/inputs, `large` for prominent preview/banner containers, smaller roles for nested surfaces, and `CircleShape` for capsules/discs. Exact dynamic geometry belongs to its specialized implementation. Brand sizes are defined in [Shapes](src/commonMain/kotlin/com/ismartcoding/plain/ui/theme/Shapes.kt), not Material default sizes.

`PullToRefresh` headers scroll out with content during an active refresh. Pull distance stops at the header height so long drags cannot push the page out of view. Scrolling preserves the request; `userEnable = false` prevents new gestures from starting a refresh while allowing the active header to retract.

`NoDataColumn` is a static centered empty state with no internal scroll container; it can be placed inside an existing lazy-list item.

`PAlert` owns 16dp internal content padding and 8dp gaps before visible action or detail sections. Empty or collapsed sections add no gap. It emits one layout root with no outer margin or trailing spacer; callers supply page margins and spacing through their layout or the root modifier.

Use the `PListItem` content-slot overload for a third description line or custom content. Slots supply text; the component provides the standard title, subtitle and description typography and owns line spacing, leading/trailing layout, dimming, and merged semantics. Optional `minHeight` keeps compact rows; `statusColor` draws a 4dp stripe at the layout start. String and key/value calls retain their existing defaults.

Public layout components accept a root `modifier`. Actions use `onClick`, `enabled` and `onDismissRequest`. Required actions have no default empty callback; optional actions are nullable and create no click target when absent. Each distinct public component has its own file; overloads of the same component may share it. Code comments use English and explain only non-obvious decisions.

Read parameter behavior in the source: `PScaffold` consumes horizontal insets and passes only top/bottom padding to content; `PTextField` uses the theme’s rounded shape and fill without an underline, retains password/clear/paste actions (`showPasteAction = false` omits paste for search), and renders its own error message; `PInputChip.onClose` determines whether the close target exists (`onClick = null` makes the whole chip a remove action); `PListItem.dimmed` is visual dimming and its two title extension slots are mutually exclusive. Sheet primary rows accept at most four actions, including in RTL. Button loading prevents interaction while preserving label width. Do not recreate these policies in business pages.

Run `bash scripts/check-ui.sh` from plain-app or plain-router-app after UI changes, followed by the host application's build. The shared Kotlin PSI checker resolves explicit imports, aliases, wildcard imports and fully qualified calls; it ignores comments and strings, rejects new raw Material page/sheet/button/form calls and literal shapes, and checks public library layout contracts. It generates the component symbol index under the application’s `build/reports/ui-check/` from source. Legacy business occurrences are frozen by exact file, function, symbol and count in each application's `scripts/ui-component-baseline.tsv`; entries name remaining migration debt or specialized geometry. Do not add entries to get a new violation through. Delete an entry when migrating its owner. No package-wide exclusion is supported.

This syntax check does not perform whole-program type inference or detect an arbitrary layout recreating a component. Compiler checks, the scene index, examples and review remain necessary. The showcase includes disabled/loading/error/read-only states and RTL/large-text controls; device checks must also cover light, dark, AMOLED, TalkBack, keyboard and neighboring touch targets. Performance changes need release traces before claiming a measured speedup.
