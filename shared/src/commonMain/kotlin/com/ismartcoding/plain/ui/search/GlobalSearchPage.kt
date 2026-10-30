package com.ismartcoding.plain.ui.search

import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.search.GlobalSearchAction
import com.ismartcoding.plain.ui.search.GlobalSearchDomain
import com.ismartcoding.plain.ui.search.GlobalSearchHit
import com.ismartcoding.plain.ui.search.GlobalSearchSource
import com.ismartcoding.plain.ui.search.GlobalSearchViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.navigation.MainNavScaffold
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.lib.withIO
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Slack-style global search tab: one query across notes, audio, photos,
 * videos, docs, files, feeds, chat messages and apps. Results render with
 * each domain's own list item component; the sticky type chips narrow scope.
 */
@Composable
fun GlobalSearchPage(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    viewModel: GlobalSearchViewModel,
    onTabSelected: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val domains = viewModel.domains.value
    val query = viewModel.queryText.value
    val domainScope = viewModel.domain.value

    // Runs on every entry (and return from a viewer): re-syncs loaded hits
    // with their source data (note edits, feed read flags, ...) in place, so
    // no full reload flash.
    LaunchedEffect(viewModel) {
        viewModel.refreshHits()
        merge(
            AppSubscriptions.contentChanged.changes(),
            AppSubscriptions.messagesCreated.changes(),
            AppSubscriptions.messagesUpdated.changes(),
            AppSubscriptions.messagesDeleted.changes(),
            AppSubscriptions.peersChanged.changes(),
                AppSubscriptions.channelsChanged.changes(),
        ).collect { viewModel.refreshHits(GlobalSearchDomain.CHAT) }
    }

    LaunchedEffect(viewModel, "search-permissions") {
        AppSubscriptions.permissionsUpdated.changes().collect { if (viewModel.queryText.value.isNotBlank()) viewModel.search() }
    }

    // Live suggestions: re-arm the debounce on every keystroke; a submit or a
    // recent-term tap flips `submitted` before this fires, so it stays quiet.
    // The `searchedQuery` guard keeps this from wiping and reloading the
    // result list when the page re-enters composition after returning from
    // a viewer page (the query has not changed, so there is nothing to search).
    LaunchedEffect(viewModel.queryText.value) {
        if (query.isNotBlank() && !viewModel.submitted.value) {
            delay(300)
            if (!viewModel.submitted.value && viewModel.searchedQuery.value != query.trim()) {
                viewModel.search()
            }
        }
    }

    val rowContext = rememberGlobalSearchRowContext(navController, audioQueueVM)
    val rowContexts = rememberGlobalSearchDeviceContexts(
        rowContext, viewModel.domainStates.values.flatMap { state -> state.devices.map { it.device } }.distinct(),
    )
    val onOpen: (GlobalSearchHit) -> Unit = { hit ->
        when (val action = hit.action) {
            is GlobalSearchAction.Navigate -> {
                val device = hit.device
                if (device is com.ismartcoding.plain.rust.graphql.ContentSource.Remote || com.ismartcoding.plain.ui.app.MainViewModel.selectedDevice.value != null) {
                    val feature: com.ismartcoding.plain.platform.system.AppFeatureType
                    var id = ""
                    var path = ""
                    var title = ""
                    when (val route = action.route) {
                        is com.ismartcoding.plain.ui.navigation.Routing.NoteDetail -> { feature = com.ismartcoding.plain.platform.system.AppFeatureType.NOTES; id = route.id }
                        is com.ismartcoding.plain.ui.navigation.Routing.FeedEntry -> { feature = com.ismartcoding.plain.platform.system.AppFeatureType.FEEDS; id = route.id }
                        is com.ismartcoding.plain.ui.navigation.Routing.AppDetails -> { feature = com.ismartcoding.plain.platform.system.AppFeatureType.APPS; id = route.id }
                        is com.ismartcoding.plain.ui.navigation.Routing.Files -> { feature = com.ismartcoding.plain.platform.system.AppFeatureType.FILES; path = route.folderPath }
                        is com.ismartcoding.plain.ui.navigation.Routing.Chat -> { feature = com.ismartcoding.plain.platform.system.AppFeatureType.CHAT; id = route.id }
                        is com.ismartcoding.plain.ui.navigation.Routing.Docs -> {
                            feature = com.ismartcoding.plain.platform.system.AppFeatureType.DOCS
                            val doc = (hit.source as GlobalSearchSource.Doc).doc
                            id = doc.id; path = doc.path; title = doc.title
                        }
                        else -> error("Unsupported remote search result")
                    }
                    val identity = (device as? com.ismartcoding.plain.rust.graphql.ContentSource.Remote)?.client?.let {
                        com.ismartcoding.plain.ui.app.DeviceIdentity(it.peerId, it.publicKey)
                    }
                    com.ismartcoding.plain.ui.app.MainViewModel.openDeviceContent(identity, com.ismartcoding.plain.ui.app.DeviceContentEntry(feature, id, path, title))
                } else navController.navigate(action.route)
            }
            is GlobalSearchAction.PlayAudio -> checkNotificationPermission(Res.string.audio_notification_prompt) {
                scope.launch(Dispatchers.Default) { rowContexts.getValue(hit.device).audioQueueVM.enqueueAndPlayAsync(action.audio) }
            }
        }
    }

    // Tapping an image/video hit opens the shared fullscreen previewer seeded
    // with every loaded hit of that section, at the tapped index. Same flow as
    // ImagesPage: the row thumbnail is a registered TransformImageView, so
    // open zooms in from it and close zooms back into it.
    val onPreviewMedia: (GlobalSearchHit, TransformItemState) -> Unit = { hit, itemState ->
        val domain = if (hit.source is GlobalSearchSource.Video) GlobalSearchDomain.VIDEOS else GlobalSearchDomain.IMAGES
        val hits = viewModel.domainStates[domain]?.hits?.value ?: emptyList()
        scope.launch {
            withIO {
                val selected = hit.previewItem() ?: return@withIO
                MediaPreviewData.setDataAsync(itemState, hits.mapNotNull { it.previewItem() }, selected)
            }
            val index = MediaPreviewData.items.indexOfFirst { it.id == hit.previewItem()?.id }
            if (index >= 0) {
                rowContext.previewerState.openTransform(index = index, itemState = itemState)
            }
        }
    }

    MainNavScaffold(
        selectedIndex = 3,
        onTabSelected = onTabSelected,
        topBar = { GlobalSearchTopBar(viewModel, domainScope) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .hideKeyboardOnTap(focusManager, keyboard),
        ) {
            // All scopes stay visible while searching so switching filters is always possible.
            GlobalSearchScopeChips(
                selected = domainScope,
                chips = listOf<GlobalSearchDomain?>(null) + domains,
                onSelect = viewModel::setDomain,
            )

            if (query.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (domainScope != null) {
                        Box(Modifier.padding(16.dp)) {
                            PAlert(
                                description = stringResource(Res.string.search_scope_hint, stringResource(domainScope.labelRes)),
                                type = AlertType.INFO,
                            )
                        }
                    }
                    GlobalSearchRecentSection(
                        recent = viewModel.recentQueries.value,
                        onSearch = viewModel::searchFromRecent,
                        onRemove = viewModel::removeRecent,
                        onClear = viewModel::clearRecent,
                    )
                    BottomSpace(paddingValues)
                }
            } else {
                SearchResults(
                    viewModel = viewModel,
                    domains = domains,
                    paddingValues = paddingValues,
                    rowContexts = rowContexts,
                    onOpen = onOpen,
                    onPreviewMedia = onPreviewMedia,
                )
            }
        }
    }

    MediaPreviewer(state = rowContext.previewerState)
}

/**
 * Hides the search keyboard on any tap in the content area — result rows
 * (whose own clickables consume the tap), chips and blank space alike. The
 * search bar lives in the top bar and stays focusable.
 */
private fun Modifier.hideKeyboardOnTap(focusManager: FocusManager, keyboard: SoftwareKeyboardController?): Modifier =
    pointerInput(focusManager) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.none { it.pressed }) break
            }
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
