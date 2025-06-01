package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.AudioSortByPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.page.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.audio.components.AudioPlayerBar
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TabContentAudio(
    audioState: AudioPageState,
    audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    castVM: CastViewModel,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow

    val pagerState = audioState.pagerState
    val scrollBehavior = audioState.scrollBehavior
    val tagsState = audioState.tagsState
    val tagsMapState = audioState.tagsMapState
    val dragSelectState = audioState.dragSelectState
    val itemsState = audioState.itemsState
    val scrollState = audioState.scrollState
    val tabs = remember(tagsState, audioVM.total.intValue) {
        listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", audioVM.total.intValue),
            *tagsState.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    val isAudioPlaying by AudioPlayer.isPlayingFlow.collectAsState()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    audioVM.loadAsync(context, tagsVM)
                    audioPlaylistVM.loadAsync(context)
                    mediaFoldersVM.loadAsync(context)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (audioVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                audioVM.sortBy.value = AudioSortByPreference.getValueAsync(context)
                audioVM.loadAsync(context, tagsVM)
                audioPlaylistVM.loadAsync(context)
                mediaFoldersVM.loadAsync(context)
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                    scope.launch(Dispatchers.IO) {
                        audioVM.sortBy.value = AudioSortByPreference.getValueAsync(context)
                        audioVM.loadAsync(context, tagsVM)
                    }
                }
            }
        }
    }

    LaunchedEffect(dragSelectState.selectMode, !context.isGestureInteractionMode()) {
        if (dragSelectState.selectMode || !context.isGestureInteractionMode()) {
            scrollBehavior.reset()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val tab = tabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (tab.value == "all") {
            audioVM.trash.value = false
            audioVM.tag.value = null
        } else {
            val tag = tagsState.find { it.id == tab.value }
            audioVM.trash.value = false
            audioVM.tag.value = tag
        }
        scope.launch {
            scrollBehavior.reset()
            audioVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) ?: scrollState.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            audioVM.loadAsync(context, tagsVM)
        }
    }

    val audioTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry ->
            entry.value.mapNotNull { relation ->
                tagsState.find { it.id == relation.tagId }
            }
        }
    }

    if (audioVM.showSortDialog.value) {
        FileSortDialog(audioVM.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) {
                AudioSortByPreference.putAsync(context, it)
                audioVM.sortBy.value = it
                audioVM.loadAsync(context, tagsVM)
            }
        }, onDismiss = {
            audioVM.showSortDialog.value = false
        })
    }

    ViewAudioBottomSheet(
        audioVM = audioVM,
        tagsVM = tagsVM,
        tagsMapState = tagsMapState,
        tagsState = tagsState,
        dragSelectState = dragSelectState,
    )

    MediaFoldersBottomSheet(audioVM, mediaFoldersVM, tagsVM)

    if (audioVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) {
            audioVM.showTagsDialog.value = false
        }
    }

    CastDialog(castVM)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!audioVM.hasPermission.value) {
                NeedPermissionColumn(R.drawable.music, AppFeatureType.FILES.getPermission()!!)
                return@Column
            }

            if (!dragSelectState.selectMode) {
                PScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, s ->
                        PFilterChip(
                            modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                            },
                            label = {
                                if (index == 0) {
                                    Text(text = s.title + " (" + s.count + ")")
                                } else {
                                    Text(if (audioVM.bucketId.value.isNotEmpty() || audioVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                                }
                            }
                        )
                    }
                }
            }

            if (pagerState.pageCount == 0) {
                NoDataColumn(loading = audioVM.showLoading.value, search = audioVM.showSearchBar.value)
                return@Column
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                PullToRefresh(
                    refreshLayoutState = topRefreshLayoutState,
                    userEnable = !dragSelectState.selectMode,
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (itemsState.isNotEmpty()) {
                            val scrollState = rememberLazyListState()
                            audioVM.scrollStateMap[index] = scrollState
                            LazyColumnScrollbar(
                                state = scrollState,
                            ) {
                                LazyColumn(
                                    Modifier
                                        .fillMaxSize()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                                        .listDragSelect(
                                            items = itemsState,
                                            state = dragSelectState
                                        ),
                                    state = scrollState,
                                ) {
                                    item {
                                        TopSpace()
                                    }
                                    items(
                                        items = itemsState,
                                        key = { it.id }
                                    ) { item ->
                                        val tags = audioTagsMap[item.id] ?: emptyList()
                                        AudioListItem(
                                            item = item,
                                            audioVM = audioVM,
                                            audioPlaylistVM,
                                            tagsVM = tagsVM,
                                            castVM = castVM,
                                            tags = tags,
                                            pagerState = pagerState,
                                            dragSelectState = dragSelectState,
                                            isCurrentlyPlaying = isAudioPlaying && audioPlaylistVM.selectedPath.value == item.path,
                                            isInPlaylist = audioPlaylistVM.isInPlaylist(item.path),
                                        )
                                        VerticalSpace(dp = 8.dp)
                                    }

                                    item(key = "loadMore") {
                                        if (itemsState.isNotEmpty() && !audioVM.noMore.value) {
                                            LaunchedEffect(Unit) {
                                                scope.launch(Dispatchers.IO) {
                                                    withIO { audioVM.moreAsync(context, tagsVM) }
                                                }
                                            }
                                        }
                                        LoadMoreRefreshContent(audioVM.noMore.value)
                                    }

                                    item(
                                        key = "bottomSpace"
                                    ) {
                                        BottomSpace(paddingValues)
                                    }
                                }
                            }
                        } else {
                            NoDataColumn(loading = audioVM.showLoading.value, search = audioVM.showSearchBar.value)
                        }
                    }
                }
            }
        }

        AudioPlayerBar(
            audioPlaylistVM,
            castVM,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            dragSelectState = audioState.dragSelectState
        )
        
        AudioCastPlayerBar(
            castVM = castVM,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            dragSelectState = audioState.dragSelectState
        )
    }
}
