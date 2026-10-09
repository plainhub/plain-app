# plain-ui

Shared Compose Multiplatform components in `com.ismartcoding.plain.ui`.
This is the component selection entry point for PlainApp and PlainNet (plain-router-app).
Before editing UI, choose the scene below, open its compiled example, and read the actual component signature. Pages compose existing components and connect state and events.

| Scene | Use | Compiled example |
|---|---|---|
| Page and top bar | `PScaffold` + `PTopAppBar` | [ComponentShowcasePage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ComponentShowcasePage.kt) |
| Primary, outlined, text action | `PFilledButton`, `POutlinedButton`, `PTextButton`; size via `ButtonSize`, role via `ButtonType` | [Button states](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ShowcaseComponentStates.kt) |
| Text input, password, validation | `PTextField`; choose `enabled` and `readOnly` independently | [Form states](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ShowcaseComponentStates.kt) |
| Single choice between a few options | `PSegmentedButtons` | [Segmented buttons](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ShowcaseSegmentedButtons.kt) |
| Cards, list rows, banners, filters, removable chips | `PCard`, `PListItem`, `PBanner`, `PFilterChip`, `PInputChip` | [Component showcase](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ComponentShowcasePage.kt) |
| Bottom sheet | `PModalBottomSheet` + `PBottomSheetTopAppBar`; primary actions use `PSheetPrimaryActionsCard`, overflow uses `PSheetActionCard` | [FileInfoBottomSheet](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/files/FileInfoBottomSheet.kt) |
| Confirmation and input dialog | Host `confirmActionAsync` / `DialogHelper`, `TextFieldDialog`, `RadioDialog`; selection rows use `PDialogRadioRow` | [DialogHelper](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/helpers/DialogHelper.kt) |
| Icon with label below | `PIconTextSmallButton` for compact toolbar; `PIconTextActionButton` for shortcuts; `PSheetPrimaryAction` for sheet actions | [State showcase](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/ShowcaseComponentStates.kt) |
| Boolean / radio / simple icon control | Official Material3 `Switch`, `RadioButton`, `IconButton`; use host icon shortcuts when applicable | [Dark theme choices](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/settings/DarkThemePage.kt) |
| Media seek, animated waveform, countdown | `PlayerSlider`, `WaveSlider`, `CircularTimer` | [PomodoroPage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/pomodoro/PomodoroPage.kt) |
| Large document, scan, drag selection, fast scroll, pull refresh | `CodeEditor`, `QrCodeScanner`, `gridDragSelect`, fastscroll and pullrefresh packages | [TextFilePage](../shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/TextFilePage.kt) |

The first eight scenes require the existing project components. Basic layout (`Box`, `Row`, `Column`), text, images and controls in the official-control row can use Compose directly. Specialized editor, scanner and gesture engines keep their own behavior; inspect their contracts before extending them. Host dialog helpers use each application's resources and state, and stay outside this library.

Use `AppTheme` as the complete theme entry. Keep the brand palette, soft dark text and AMOLED behavior; dynamic color is excluded. Shapes come from the theme: `medium` for ordinary cards/inputs, `large` for prominent preview/banner containers, smaller roles for nested surfaces, and `CircleShape` for capsules/discs. Exact dynamic geometry belongs to its specialized implementation. Brand sizes are defined in [Shapes](src/commonMain/kotlin/com/ismartcoding/plain/ui/theme/Shapes.kt), not Material default sizes.

Public layout components accept a root `modifier`. Actions use `onClick`, `enabled` and `onDismissRequest`. Required actions have no default empty callback; optional actions are nullable and create no click target when absent. Each distinct public component has its own file; overloads of the same component may share it. Code comments use English and explain only non-obvious decisions.

Read parameter behavior in the source: `PScaffold` consumes horizontal insets and passes only top/bottom padding to content; `PTextField` renders its own error message; `PInputChip.onClose` determines whether the close target exists (`onClick = null` makes the whole chip a remove action); `PListItem.dimmed` is visual dimming and its two title extension slots are mutually exclusive. Sheet primary rows accept at most four actions, including in RTL. Button loading prevents interaction while preserving label width. Do not recreate these policies in business pages.

Run `bash scripts/check-ui.sh` from plain-app or plain-router-app after UI changes, followed by the host application's build. The shared Kotlin PSI checker resolves explicit imports, aliases, wildcard imports and fully qualified calls; it ignores comments and strings, rejects new raw Material page/sheet/button/form calls and literal shapes, and checks public library layout contracts. It generates the component symbol index under the application’s `build/reports/ui-check/` from source. Legacy business occurrences are frozen by exact file, function, symbol and count in each application's `scripts/ui-component-baseline.tsv`; entries name remaining migration debt or specialized geometry. Do not add entries to get a new violation through. Delete an entry when migrating its owner. No package-wide exclusion is supported.

This syntax check does not perform whole-program type inference or detect an arbitrary layout recreating a component. Compiler checks, the scene index, examples and review remain necessary. The showcase includes disabled/loading/error/read-only states and RTL/large-text controls; device checks must also cover light, dark, AMOLED, TalkBack, keyboard and neighboring touch targets. Performance changes need release traces before claiming a measured speedup.

Maven coordinate: `com.ismartcoding:plain-ui`, repository `https://plainhub.github.io/plain-app/maven`.
The `Publish plain-ui` workflow publishes tagged `plain-ui-v<version>` releases on the dedicated `maven` branch. Consumers must upgrade after publication; local cross-project validation can selectively substitute `plain-ui`, preserving their own `plain-common` version.
