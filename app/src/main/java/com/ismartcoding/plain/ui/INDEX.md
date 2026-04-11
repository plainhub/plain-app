# ui/ — Compose UI Layer

> 100+ files | Package: `com.ismartcoding.plain.ui`

## Structure

```
ui/
├── base/              # 60+ reusable P-prefixed composables
├── components/        # 27+ domain-specific components
├── extensions/        # UI extension functions
├── helpers/           # UI helper functions
├── models/            # 38 ViewModels + V-prefixed data classes
├── nav/               # Navigation routes (Routing.kt)
├── page/              # Feature screens
└── theme/             # Material3 theming
```

## models/ — ViewModels (38 files)

| ViewModel | Feature | Key State |
|-----------|---------|-----------|
| MainViewModel | App root, navigation | Global app state |
| AudioViewModel | Audio library browse | List, search, select |
| AudioPlaylistViewModel | Audio playlist | Current playlist |
| ImagesViewModel | Image gallery | Grid, search, select |
| VideosViewModel | Video gallery | Grid, search, select |
| FilesViewModel | File browser | Path, sort, search |
| DocsViewModel | Document viewer | Doc list |
| NotesViewModel | Notes list | List, search, select |
| NoteViewModel | Single note edit | Note content |
| FeedsViewModel | RSS feed list | Feeds, tags |
| FeedEntriesViewModel | Feed entries | Entries, filter |
| FeedEntryViewModel | Single entry | Entry content |
| FeedSettingsViewModel | Feed settings | Settings state |
| ChatViewModel | Chat messages | Messages, input, files |
| ChannelViewModel | Group channel | Channel state |
| PeerViewModel | Peer management | Peer list |
| NearbyViewModel | Nearby discovery | Discovery state |
| CastViewModel | DLNA casting | Cast state |
| AppsViewModel | App management | Package list |
| PomodoroViewModel | Pomodoro timer | Timer state |
| SessionsViewModel | Web sessions | Session list |
| MdEditorViewModel | Markdown editor | Editor content |
| ScanHistoryViewModel | Scan history | History list |
| BackupRestoreViewModel | Backup/restore | Backup state |
| TagsViewModel | Tag management | Tag list |
| TextFileViewModel | Text file viewer | File content |
| UpdateViewModel | App update | Update check |
| WebConsoleViewModel | Web console | Console state |
| NotificationSettingsViewModel | Notification settings | Settings |
| MediaFoldersViewModel | Media folders | Folder list |
| BaseMediaViewModel | Abstract base | Search, select, load |

**Data classes**: VChat.kt, VClickText.kt, VPackage.kt, VTabData.kt, FolderOption.kt
**Interfaces**: ISearchableViewModel, ISelectableViewModel, MediaPreviewData

## page/ — Feature Screens

| Directory | Files | Key Pages |
|-----------|-------|-----------|
| root/ | 15 | RootPage, 5 tab contents (Home/Chat/Audio/Images/Videos), 5 top bars, home features |
| chat/ | 29 | ChatPage, NearbyPage, ChannelChatInfoPage, PeerChatInfoPage + 20 components |
| feeds/ | 10 | FeedsPage, FeedEntriesPage, FeedEntryPage, FeedSettingsPage + dialogs/sheets |
| audio/ | 10 | AudioPlayerPage, AudioPlaylistPage, SleepTimerPage + 6 components |
| files/ | 7 | FilesPage + breadcrumb, list content/item, paste bar components |
| web/ | 7 | WebSettingsPage, WebSecurityPage, WebDevPage, SessionsPage, NotificationSettingsPage |
| settings/ | 6 | SettingsPage, BackupRestorePage, DarkThemePage, LanguagePage, UpdateDialog |
| notes/ | 4 | NotePage, NotesPage + bottom sheet |
| pomodoro/ | 4 | PomodoroPage, PomodoroSettingsDialog |
| scan/ | 4 | ScanPage, ScanHistoryPage + 2 components |
| docs/ | 3 | DocsPage + bottom sheet |
| images/ | 3 | ViewImageBottomSheet + page state |
| videos/ | 3 | ViewVideoBottomSheet + page state |
| tags/ | 3 | Dialogs for tag selection/batch |
| cast/ | 3 | CastDialog, AudioCastPlayerBar, AudioCastPlaylistPage |
| apps/ | 2 | AppPage, AppsPage |
| tools/ | 1 | SoundMeterPage |
| Top-level | 7 | Main.kt, TextPage, PdfPage, OtherFilePage, TextFilePage, 2 bottom sheets |

## base/ — Reusable Composables (60+ files, P-prefixed)

Key composables: PAlert, PBottomSheet, PCard, PDialog, PDropdown, PIconButton, PListItem, PLoading, PModalBottomSheet, PScaffold, PSearchBar, PSwitch, PTabRow, PTextButton, PTopAppBar, etc.

## nav/ — Navigation (2 files)

| File | Purpose |
|------|---------|
| Routing.kt | All route definitions and NavHost setup |
| NavHostController.kt | Navigation controller extensions |
