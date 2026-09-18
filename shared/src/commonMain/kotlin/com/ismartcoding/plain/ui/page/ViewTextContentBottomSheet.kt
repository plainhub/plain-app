package com.ismartcoding.plain.ui.page
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.platform.shareText
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.TextFileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewTextContentBottomSheet(
    textFileVM: TextFileViewModel,
    content: String,
) {
    val onDismiss = {
        textFileVM.showMoreActions.value = false
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(16.dp)
            }
            item {
                PSheetPrimaryActionsCard {
                    PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
                        shareText(content)
                        onDismiss()
                    }
                    PSheetPrimaryAction(Res.drawable.arrow_up_to_line, stringResource(Res.string.jump_to_top)) {
                        textFileVM.gotoTop()
                        onDismiss()
                    }
                    PSheetPrimaryAction(Res.drawable.arrow_down_to_line, stringResource(Res.string.jump_to_bottom)) {
                        textFileVM.gotoEnd()
                        onDismiss()
                    }
                }
            }
            item {
                VerticalSpace(dp = 24.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = stringResource(Res.string.wrap_content), action = {
                        PSwitch(
                            activated = textFileVM.controller.wrapContent.value,
                        ) {
                            textFileVM.toggleWrapContent()
                        }
                        HorizontalSpace(8.dp)
                    })
                }
            }

            item {
                BottomSpace()
            }
        }
    }
}
