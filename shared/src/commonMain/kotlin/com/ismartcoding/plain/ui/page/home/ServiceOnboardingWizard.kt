package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.events.OpenNotificationSettingsEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestNotificationPermissionEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isAppForegrounded
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.shouldShowRationale
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PStatusBadge
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.orange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class WizardStep { CHECKLIST, NOTIFICATION, STORAGE, DONE }

private enum class NotifCard { ASK, DENIED }

private enum class PermStatus { ALLOWED, SKIPPED, NOT_ENABLED }

/**
 * First-run permission onboarding for the HTTP service (Android 13+). Shows
 * one explanation card per permission and only fires the matching system
 * prompt when the user taps the action button, so system dialogs never arrive
 * back-to-back. Storage access is asked at most once here and never re-prompted
 * afterwards (no banner); overlay permission intentionally stays out of the
 * wizard — it keeps its existing home page alert.
 *
 * [onStartService] fires when the user reaches the final step (service starts
 * while the receipt is shown); [onClose] closes the sheet from the final step
 * or cancels from any earlier step (no start).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOnboardingWizard(
    onStartService: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(WizardStep.CHECKLIST) }
    var notifCard by remember { mutableStateOf(NotifCard.ASK) }
    var notificationGranted by remember { mutableStateOf(false) }
    var notifRationale by remember { mutableStateOf(false) }
    var notifAwaitSettingsReturn by remember { mutableStateOf(false) }
    var skipStorage by remember { mutableStateOf(false) }
    var storageSkipped by remember { mutableStateOf(false) }
    var storageGrantedNow by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // Incremented per "Open system settings" tap so the watcher effect can
    // restart for retries (a boolean key would only fire once).
    var storageRequest by remember { mutableStateOf(0) }
    val storageInitiallyGranted = remember { Permission.WRITE_EXTERNAL_STORAGE.isGranted() }
    val totalSteps = if (storageInitiallyGranted || skipStorage) 1 else 2

    fun finishAndStart(skipped: Boolean, grantedNow: Boolean) {
        if (skipped) storageSkipped = true
        storageGrantedNow = grantedNow
        onStartService()
        step = WizardStep.DONE
    }

    fun advanceAfterNotification() {
        if (step != WizardStep.NOTIFICATION) return
        val storageNeeded = !storageInitiallyGranted && !skipStorage && !Permission.WRITE_EXTERNAL_STORAGE.isGranted()
        if (storageNeeded) {
            step = WizardStep.STORAGE
        } else {
            finishAndStart(skipped = skipStorage, grantedNow = false)
        }
    }

    // Notification verdicts come from PermissionsResultEvent, which both the
    // runtime dialog and the settings intent launcher emit. NOTE: settings
    // jumps carry FLAG_ACTIVITY_NEW_TASK, so their activity result arrives
    // IMMEDIATELY with a stale value — the real "user came back" moment fires
    // no event. That is why the settings path is additionally covered by the
    // foreground watcher below.
    LaunchedEffect(step) {
        if (step != WizardStep.NOTIFICATION) return@LaunchedEffect
        val sysPermission = Permission.POST_NOTIFICATIONS.toSysPermission()
        launch {
            Channel.sharedFlow
                .filterIsInstance<PermissionsResultEvent>()
                .filter { it.map.containsKey(sysPermission) }
                .collect { event ->
                    busy = false
                    if (event.map[sysPermission] == true) {
                        notificationGranted = true
                        advanceAfterNotification()
                    } else {
                        // Rationale is captured per denial: true = the runtime
                        // dialog can be retried, false = permanently denied and
                        // only the settings page can change the state.
                        notifRationale = Permission.POST_NOTIFICATIONS.shouldShowRationale()
                        notifCard = NotifCard.DENIED
                    }
                }
        }
        while (isActive) {
            if (notifAwaitSettingsReturn && !isAppForegrounded()) {
                while (isAppForegrounded() == false && isActive) delay(400)
                if (!isActive) break
                delay(1200) // let the system settle the toggle
                if (Permission.POST_NOTIFICATIONS.isGranted()) {
                    busy = false
                    notificationGranted = true
                    advanceAfterNotification()
                    break
                } else {
                    notifRationale = Permission.POST_NOTIFICATIONS.shouldShowRationale()
                    notifCard = NotifCard.DENIED
                }
            }
            delay(400)
        }
    }

    // Storage: same NEW_TASK caveat as above — the settings jump's activity
    // result is useless, so watch for the app to come back and read the real
    // permission state. Keyed on a per-tap counter so retries re-arm it.
    LaunchedEffect(step, storageRequest) {
        if (step != WizardStep.STORAGE || storageRequest == 0) return@LaunchedEffect
        val returned = withTimeoutOrNull(300_000L) {
            while (isAppForegrounded()) delay(400)
            while (!isAppForegrounded()) delay(400)
            delay(1200)
            true
        }
        busy = false
        // Timeout with no settings round-trip (settings failed to open, or the
        // user never left): stay on this step instead of starting behind the
        // user's back; the button is tappable again.
        if (returned == true && step == WizardStep.STORAGE) {
            finishAndStart(skipped = false, grantedNow = Permission.WRITE_EXTERNAL_STORAGE.isGranted())
        }
    }

    PModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            when (step) {
                WizardStep.CHECKLIST -> {
                    VerticalSpace(24.dp)
                    Text(
                        text = stringResource(Res.string.perm_wizard_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    VerticalSpace(8.dp)
                    Text(
                        text = stringResource(Res.string.perm_wizard_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalSpace(24.dp)
                    PCard {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            PListItem(
                                icon = Res.drawable.bell,
                                title = stringResource(Res.string.perm_wizard_notif_name),
                                subtitle = stringResource(Res.string.perm_wizard_notif_desc),
                                titleTrailing = {
                                    PStatusBadge(
                                        text = stringResource(Res.string.perm_wizard_required),
                                        type = BadgeType.INFO,
                                    )
                                },
                            )
                            PListItem(
                                icon = Res.drawable.folder,
                                title = stringResource(Res.string.perm_wizard_storage_name),
                                subtitle = stringResource(Res.string.perm_wizard_storage_desc),
                                titleTrailing = {
                                    PStatusBadge(
                                        text = stringResource(Res.string.perm_wizard_optional),
                                        type = BadgeType.NEUTRAL,
                                    )
                                },
                            )
                        }
                    }
                    VerticalSpace(32.dp)
                    PFilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.perm_wizard_start),
                        onClick = { step = WizardStep.NOTIFICATION },
                    )
                    PTextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.perm_wizard_skip_optional),
                        buttonSize = ButtonSize.LARGE,
                        onClick = {
                            skipStorage = true
                            step = WizardStep.NOTIFICATION
                        },
                    )
                }

                WizardStep.NOTIFICATION -> {
                    VerticalSpace(24.dp)
                    StepHeader(current = 1, total = totalSteps)
                    if (notifCard == NotifCard.ASK) {
                        ExplanationStep(
                            icon = Res.drawable.bell,
                            title = stringResource(Res.string.perm_wizard_notif_title),
                            body = stringResource(Res.string.perm_wizard_notif_body),
                        ) {
                            PFilledButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.allow),
                                isLoading = busy,
                                onClick = {
                                    busy = true
                                    // Runtime dialog first (falls back to the settings
                                    // page only on pre-T). The verdict arrives via the
                                    // PermissionsResultEvent collector above.
                                    sendEvent(RequestNotificationPermissionEvent())
                                },
                            )
                            PTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.cancel),
                                buttonSize = ButtonSize.LARGE,
                                onClick = onClose,
                            )
                        }
                    } else {
                        ExplanationStep(
                            icon = Res.drawable.bell,
                            title = stringResource(Res.string.perm_wizard_notif_denied_title),
                            body = stringResource(Res.string.perm_wizard_notif_denied_body),
                            warning = true,
                        ) {
                            if (notifRationale) {
                                PFilledButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(Res.string.allow),
                                    isLoading = busy,
                                    onClick = {
                                        busy = true
                                        // Soft denial: the runtime dialog can still
                                        // be shown — offer a retry before any settings
                                        // detour.
                                        sendEvent(RequestNotificationPermissionEvent())
                                    },
                                )
                            } else {
                                PFilledButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(Res.string.perm_wizard_open_notification_settings),
                                    isLoading = busy,
                                    onClick = {
                                        busy = true
                                        // Permanently denied: the system dialog can no
                                        // longer appear; the settings page is the only
                                        // way forward.
                                        notifAwaitSettingsReturn = true
                                        sendEvent(OpenNotificationSettingsEvent())
                                    },
                                )
                            }
                            PTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.perm_wizard_continue),
                                buttonSize = ButtonSize.LARGE,
                                onClick = { advanceAfterNotification() },
                            )
                        }
                    }
                }

                WizardStep.STORAGE -> {
                    VerticalSpace(24.dp)
                    StepHeader(current = 2, total = 2)
                    ExplanationStep(
                        icon = Res.drawable.folder,
                        title = stringResource(Res.string.perm_wizard_storage_title),
                        body = stringResource(Res.string.perm_wizard_storage_body),
                    ) {
                        PFilledButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.perm_wizard_storage_open_settings),
                            isLoading = busy,
                            onClick = {
                                busy = true
                                storageRequest++
                                scope.launch {
                                    ApiPermissionsPreference.putAsync(Permission.WRITE_EXTERNAL_STORAGE, true)
                                    sendEvent(RequestPermissionsEvent(Permission.WRITE_EXTERNAL_STORAGE))
                                }
                            },
                        )
                        PTextButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.perm_wizard_storage_skip),
                            buttonSize = ButtonSize.LARGE,
                            onClick = { finishAndStart(skipped = true, grantedNow = false) },
                        )
                    }
                }

                WizardStep.DONE -> {
                    val notifStatus = if (notificationGranted) PermStatus.ALLOWED else PermStatus.NOT_ENABLED
                    val storageStatus = when {
                        storageInitiallyGranted || storageGrantedNow -> PermStatus.ALLOWED
                        storageSkipped -> PermStatus.SKIPPED
                        else -> PermStatus.NOT_ENABLED
                    }
                    VerticalSpace(32.dp)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WizardIcon(
                            resource = Res.drawable.check,
                            tint = MaterialTheme.colorScheme.green,
                            container = MaterialTheme.colorScheme.green.copy(alpha = 0.15f),
                        )
                    }
                    VerticalSpace(16.dp)
                    Text(
                        text = stringResource(Res.string.perm_wizard_done_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VerticalSpace(8.dp)
                    Text(
                        text = stringResource(Res.string.perm_wizard_done_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VerticalSpace(24.dp)
                    PCard {
                        Column {
                            StatusRow(
                                label = stringResource(Res.string.perm_wizard_notif_name),
                                status = notifStatus,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            StatusRow(
                                label = stringResource(Res.string.perm_wizard_storage_name),
                                status = storageStatus,
                            )
                        }
                    }
                    VerticalSpace(32.dp)
                    PFilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.done),
                        onClick = onClose,
                    )
                }
            }
            BottomSpace()
        }
    }
}

@Composable
private fun StepHeader(current: Int, total: Int) {
    Text(
        text = stringResource(Res.string.perm_wizard_step_of, current, total),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ExplanationStep(
    icon: DrawableResource,
    title: String,
    body: String,
    warning: Boolean = false,
    actions: @Composable () -> Unit,
) {
    VerticalSpace(24.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WizardIcon(icon, warning)
    }
    VerticalSpace(16.dp)
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpace(8.dp)
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpace(32.dp)
    actions()
}

@Composable
private fun WizardIcon(
    resource: DrawableResource,
    warning: Boolean = false,
    tint: Color? = null,
    container: Color? = null,
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(container ?: MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(resource),
                contentDescription = null,
                tint = tint ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        if (warning) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.orange),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: PermStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status == PermStatus.ALLOWED) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.green,
                    modifier = Modifier.size(20.dp),
                )
            }
            val text = when (status) {
                PermStatus.ALLOWED -> stringResource(Res.string.perm_wizard_allowed)
                PermStatus.SKIPPED -> stringResource(Res.string.perm_wizard_skipped)
                PermStatus.NOT_ENABLED -> stringResource(Res.string.perm_wizard_not_on)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (status == PermStatus.ALLOWED) MaterialTheme.colorScheme.green
                else MaterialTheme.colorScheme.grey,
            )
        }
    }
}

// (awaitPermissionResult was removed: settings jumps carry FLAG_ACTIVITY_NEW_TASK
// so startActivityForResult returns immediately — the wizard polls isGranted()
// on app resume instead of trusting activity results for those flows.)
