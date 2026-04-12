package com.ismartcoding.plain.ui.page.audio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audio.components.AudioPlayerBar
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.home.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioVM: AudioViewModel = viewModel(key = "audioVM"),
    tagsVM: TagsViewModel = viewModel(key = "audioTagsVM"),
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "audioFoldersVM"),
    castVM: CastViewModel = viewModel(key = "audioCastVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioState = AudioPageState.create(audioVM, tagsVM, mediaFoldersVM)
    val pagerState = audioState.pagerState
    val scrollBehavior = audioState.scrollBehavior
    val tagsState = audioState.tagsState
    val tagsMapState = audioState.tagsMapState
    val dragSelectState = audioState.dragSelectState
    val itemsState = audioState.itemsState
    val scrollState = audioState.scrollState
    val isAudioPlaying by AudioPlayer.isPlayingFlow.collectAsState()

    val tabs = remember(tagsState, audioVM.total.intValue, audioVM.totalTrash.intValue) {
        val baseTabs = mutableListOf(VTabData(LocaleHelper.getString(R.string.all), "all", audioVM.total.intValue))
        if (AppFeatureType.MEDIA_TRASH.has()) baseTabs.add(VTabData(LocaleHelper.getString(R.string.trash), "trash", audioVM.totalTrash.intValue))
        baseTabs.addAll(tagsState.map { VTabData(it.name, it.id, it.count) })
        baseTabs
    }

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            withIO { audioVM.loadAsync(context, tagsVM); audioPlaylistVM.loadAsync(context); mediaFoldersVM.loadAsync(context) }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    BackHandler(enabled = dragSelectState.selectMode || castVM.castMode.value || audioVM.showSearchBar.value) {
        when {
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            audioVM.showSearchBar.value && (!audioVM.searchActive.value || audioVM.queryText.value.isEmpty()) -> {
                audioVM.exitSearchMode(); audioVM.showLoading.value = true
                scope.launch(Dispatchers.IO) { audioVM.loadAsync(context, tagsVM) }
            }
        }
    }

    AudioPageEffects(audioState, audioVM, audioPlaylistVM, tagsVM, mediaFoldersVM)

    LaunchedEffect(pagerState.currentPage) {
        val tab = tabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        when (tab.value) {
            "all" -> { audioVM.trash.value = false; audioVM.tag.value = null }
            "trash" -> { audioVM.trash.value = true; audioVM.tag.value = null }
            else -> { audioVM.trash.value = false; audioVM.tag.value = tagsState.find { it.id == tab.value } }
        }
        scope.launch { scrollBehavior.reset(); audioVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) ?: scrollState.scrollToItem(0) }
        scope.launch(Dispatchers.IO) { audioVM.loadAsync(context, tagsVM) }
    }

    val audioTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry -> entry.value.mapNotNull { relation -> tagsState.find { it.id == relation.tagId } } }
    }

    ViewAudioBottomSheet(audioVM = audioVM, tagsVM = tagsVM, tagsMapState = tagsMapState, tagsState = tagsState, dragSelectState = dragSelectState, castVM = castVM)
    MediaFoldersBottomSheet(audioVM, mediaFoldersVM, tagsVM)
    if (audioVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { audioVM.showTagsDialog.value = false } }
    CastDialog(castVM)

    PScaffold(
        topBar = {
            MediaTopBar(
                navController = navController,
                mediaVM = audioVM,
                tagsVM = tagsVM,
                castVM = castVM,
                dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = audioState.bucketsMap,
                itemsState = itemsState,
                scrollToTop = { scope.launch { audioVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } },
                defaultNavigationIcon = { NavigationBackIcon { navController.popBackStack() } },
                onSortSelected = { _, sortBy ->
                    scope.launch(Dispatchers.IO) { AudioSortByPreference.putAsync(context, sortBy); audioVM.sortBy.value = sortBy; audioVM.loadAsync(context, tagsVM) }
                },
                onSearchAction = { ctx, tv -> scope.launch(Dispatchers.IO) { audioVM.loadAsync(ctx, tv) } },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioPlaylistVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!audioVM.hasPermission.value) { NeedPermissionColumn(R.drawable.music, AppFeatureType.FILES.getPermission()!!); return@Column }
                if (!dragSelectState.selectMode) {
                    PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, s ->
                            PFilterChip(modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.scrollToPage(index) } },
                                label = { Text(if (index == 0) s.title + " (" + s.count + ")" else if (audioVM.bucketId.value.isNotEmpty() || audioVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})") })
                        }
                    }
                }
                if (pagerState.pageCount == 0) { NoDataColumn(loading = audioVM.showLoading.value, search = audioVM.showSearchBar.value); return@Column }
                AudioPageList(pagerState, scrollBehavior, dragSelectState, itemsState, audioVM, audioPlaylistVM,
                    tagsVM, castVM, audioTagsMap, isAudioPlaying, topRefreshLayoutState, paddingValues)
            }
            AudioPlayerBar(audioPlaylistVM, castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = audioState.dragSelectState)
            AudioCastPlayerBar(castVM = castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = audioState.dragSelectState)
        }
    }
}
