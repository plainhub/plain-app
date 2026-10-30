package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.ui.permissions.getGrantAccessText

import com.ismartcoding.plain.ui.base.AnimatedIconContainer

import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.web.accessFeatureOf
import com.ismartcoding.plain.graphql.Mutation
import kotlinx.coroutines.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.platform.permissions.FeaturePermission
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.lib.sendEvent

@Composable
fun NeedPermissionColumn(
    icon: DrawableResource,
    permission: FeaturePermission
) {
    val source = LocalContentSource.current
    val scope = rememberCoroutineScope()
    var requestJob by remember(source) { mutableStateOf<Job?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(800)) + expandVertically(
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AnimatedIconContainer(icon)

                Text(
                    text = if (source is ContentSource.Remote) stringResource(Res.string.remote_permission_required) else permission.permission.getGrantAccessText(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )

                Button(
                    onClick = {
                        if (source is ContentSource.Remote) {
                            requestJob?.cancel()
                            requestJob = scope.launchSafe {
                                source.api.execute(Mutation.openWebSettings(permission.permission.accessFeatureOf()))
                            }
                        } else sendEvent(RequestPermissionsEvent(*permission.permissions.toTypedArray()))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .shadow(8.dp, MaterialTheme.shapes.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(if (source is ContentSource.Remote) Res.string.request_permission else Res.string.grant_access),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
