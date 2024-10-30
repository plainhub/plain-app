package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R


@Composable
fun PIconTextSmallButton(
    icon: Int,
    text: String,
    click: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { click() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().defaultMinSize(minWidth = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PIcon(
                icon = painterResource(icon),
                contentDescription = text,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
            )
        }
    }
}

@Composable
fun IconTextSmallButtonShare(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.share_2,
        text = stringResource(R.string.share),
        click = click
    )
}

@Composable
fun IconTextSmallButtonLabel(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.label,
        text = stringResource(R.string.add_to_tags),
        click = click
    )
}


@Composable
fun IconTextSmallButtonLabelOff(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.label_off,
        text = stringResource(R.string.remove_from_tags),
        click = click
    )
}


@Composable
fun IconTextSmallButtonDelete(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.delete_forever,
        text = stringResource(R.string.delete),
        click = click
    )
}

@Composable
fun IconTextSmallButtonRename(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.pen,
        text = stringResource(R.string.rename),
        click = click
    )
}

@Composable
fun IconTextSmallButtonCut(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.scissors,
        text = stringResource(R.string.cut),
        click = click
    )
}

@Composable
fun IconTextSmallButtonCopy(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.copy,
        text = stringResource(R.string.copy),
        click = click
    )
}

@Composable
fun IconTextSmallButtonPlaylistAdd(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.playlist_add,
        text = stringResource(R.string.add_to_playlist),
        click = click
    )
}

@Composable
fun IconTextSmallButtonRestore(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.archive_restore,
        text = stringResource(R.string.restore),
        click = click
    )
}


@Composable
fun IconTrashButton(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.trash_2,
        text = stringResource(R.string.move_to_trash),
        click = click
    )
}

@Composable
fun IconTextSmallButtonZip(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.package2,
        text = stringResource(R.string.compress),
        click = click
    )
}


@Composable
fun IconTextSmallButtonUnzip(
    click: () -> Unit,
) {
    PIconTextSmallButton(
        R.drawable.package_open,
        text = stringResource(R.string.decompress),
        click = click
    )
}