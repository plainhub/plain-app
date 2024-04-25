package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.TextFileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewTextContentBottomSheet(
    viewModel: TextFileViewModel,
    content: String,
) {
    val context = LocalContext.current
    val onDismiss = {
        viewModel.showMoreActions.value = false
    }
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    LaunchedEffect(Unit) {
        groupButtons.addAll(
            listOf(
                GroupButton(
                    icon = Icons.Outlined.Share,
                    text = context.getString(R.string.share),
                    onClick = {
                        ShareHelper.shareText(context, content)
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.VerticalAlignTop,
                    text = context.getString(R.string.jump_to_top),
                    onClick = {
                        viewModel.gotoTop()
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.VerticalAlignBottom,
                    text = context.getString(R.string.jump_to_bottom),
                    onClick = {
                        viewModel.gotoEnd()
                        onDismiss()
                    }
                ),
            ))
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        GroupButtons(
            buttons = groupButtons
        )
        VerticalSpace(dp = 24.dp)
        PCard {
            PListItem(title = stringResource(id = R.string.wrap_content), action = {
                PSwitch(
                    activated = viewModel.wrapContent.value,
                ) {
                    viewModel.toggleWrapContent(context)
                }
            })
        }
        BottomSpace()
    }
}


