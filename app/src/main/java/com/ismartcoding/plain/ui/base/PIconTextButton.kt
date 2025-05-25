package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun PIconTextButton(
    icon: Any,
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PIcon(
                icon = icon,
                contentDescription = text,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PIconTextActionButton(
    icon: Int,
    text: String,
    click: () -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
                .copy(
                    containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
                ),
            onClick = click
        ) {
            PIcon(
                icon = painterResource(icon),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun IconTextSelectButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.list_checks,
        text = stringResource(R.string.select),
        click = click
    )
}

@Composable
fun IconTextDeleteButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.delete_forever,
        text = stringResource(R.string.delete),
        click = click
    )
}

@Composable
fun IconTextTrashButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.trash_2,
        text = stringResource(R.string.trash),
        click = click
    )
}

@Composable
fun IconTextShareButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.share_2,
        text = stringResource(R.string.share),
        click = click
    )
}

@Composable
fun IconTextOpenWithButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.square_arrow_out_up_right,
        text = stringResource(R.string.open_with),
        click = click
    )
}

@Composable
fun IconTextScanQrCodeButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.scan_qr_code,
        text = stringResource(R.string.scan_qrcode),
        click = click
    )
}


@Composable
fun IconTextRenameButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.pen,
        text = stringResource(R.string.rename),
        click = click
    )
}

@Composable
fun IconTextEditButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.square_pen,
        text = stringResource(R.string.edit),
        click = click
    )
}

@Composable
fun IconTextRestoreButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.archive_restore,
        text = stringResource(R.string.restore),
        click = click
    )
}



@Composable
fun IconTextToTopButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.arrow_up_to_line,
        text = stringResource(R.string.jump_to_top),
        click = click
    )
}

@Composable
fun IconTextToBottomButton(
    click: () -> Unit,
) {
    PIconTextActionButton(
        R.drawable.arrow_down_to_line,
        text = stringResource(R.string.jump_to_bottom),
        click = click
    )
}