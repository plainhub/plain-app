package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.ChannelViewModel

@Composable
fun ChannelInvitePage(
    channelId: String,
    channelName: String,
    ownerPeerId: String,
    ownerPeerName: String,
    channelVM: ChannelViewModel,
    activity: MainActivity,
    navController: NavHostController,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 48.dp),
    ) {
        item {
            VerticalSpace(40.dp)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.hash),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            VerticalSpace(dp = 24.dp)
        }
        item {
            Text(
                text = stringResource(Res.string.channel_invite),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 8.dp)
            Text(
                text = stringResource(Res.string.channel_invite_message, ownerPeerName, channelName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            VerticalSpace(dp = 40.dp)
        }
        item {
            PCard {
                PListItem(title = stringResource(Res.string.channel_name), value = channelName)
                PListItem(title = stringResource(Res.string.inviter), value = ownerPeerName)
            }
        }
        item {
            VerticalSpace(dp = 56.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                PFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.accept),
                    buttonSize = ButtonSize.EXTRA_LARGE,
                    onClick = {
                        channelVM.acceptInvite(channelId) { navController.popBackStack() }
                    },
                )
                VerticalSpace(24.dp)
                PFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.decline),
                    buttonSize = ButtonSize.EXTRA_LARGE,
                    type = ButtonType.DANGER,
                    onClick = {
                        channelVM.declineInvite(activity, channelId)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
