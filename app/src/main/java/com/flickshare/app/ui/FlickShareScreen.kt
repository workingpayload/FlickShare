package com.flickshare.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flickshare.app.discovery.ReceiverEndpoint
import com.flickshare.app.discovery.ReceiverState
import com.flickshare.app.gesture.SensorArming
import com.flickshare.app.gesture.ThrowAim
import com.flickshare.app.gesture.WristFlick
import com.flickshare.app.network.FileTransferClient
import com.flickshare.app.network.StagedFile
import com.flickshare.app.network.UploadProgress
import com.flickshare.app.network.UploadResult
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val HANDS_FREE_FLICK_PREF = "hands_free_flick"
private const val BUMP_TO_SEND_PREF = "bump_to_send"

@Composable
private fun GestureToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            tint = if (checked) Teal else Coral,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = OnSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Coral,
            ),
        )
    }
}

/** Status-line hint for gestures that work without holding the card, or null when none is armed. */
private fun handsFreeGestureHint(flick: Boolean, bump: Boolean): String? = when {
    flick && bump -> "FLICK PHONE OR TAP IT TWICE ON PC"
    flick -> "FLICK PHONE TOWARD PC"
    bump -> "TAP PHONE TWICE ON PC"
    else -> null
}

sealed interface TransferUiState {
    data object Idle : TransferUiState
    data class Uploading(val progress: UploadProgress) : TransferUiState
    data class CaughtOnDesktop(val result: UploadResult, val launchModeLabel: String) : TransferUiState
    data class Failed(val errorMessage: String) : TransferUiState
}

@Composable
fun FlickShareTheme(
    palette: MusicSmPalette = DarkPalette,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(palette) { buildMusicSmColorScheme(palette) }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MusicSmTypography,
            content = content,
        )
    }
}

@Composable
fun FlickShareScreen(
    receiverState: ReceiverState,
    discoveredEndpoints: List<ReceiverEndpoint>,
    stagedFiles: List<StagedFile>,
    transferState: TransferUiState,
    isSensorAvailable: Boolean,
    isBumpAvailable: Boolean,
    isCompassAvailable: Boolean,
    isAimCalibrated: Boolean,
    onCalibrateAim: () -> Unit,
    onClearAim: () -> Unit,
    wristFlickEvents: Flow<WristFlick>,
    onSensorArmChanged: (SensorArming) -> Unit,
    onTriggerTouchHaptics: () -> Unit,
    onFilesPicked: (List<Uri>) -> Unit,
    onClearFiles: () -> Unit,
    onStartTransfer: (launchModeLabel: String, aim: ThrowAim?) -> Unit,
    onResetTransferState: () -> Unit,
    onRescanReceiver: () -> Unit,
    onSelectEndpoint: (ReceiverEndpoint) -> Unit,
    onSetManualEndpoint: (host: String, port: Int) -> Unit,
    onForgetDeviceAndRescan: () -> Unit,
) {
    val context = LocalContext.current
    val themePrefs = remember(context) {
        context.getSharedPreferences("flickshare_theme_prefs", Context.MODE_PRIVATE)
    }
    var selectedAccentKey by remember {
        mutableStateOf(themePrefs.getString("accent_key", "coral") ?: "coral")
    }
    var useAmoled by remember {
        mutableStateOf(themePrefs.getBoolean("amoled_dark", false))
    }
    var handsFreeFlick by remember {
        mutableStateOf(themePrefs.getBoolean(HANDS_FREE_FLICK_PREF, false))
    }
    var bumpToSend by remember {
        mutableStateOf(themePrefs.getBoolean(BUMP_TO_SEND_PREF, false))
    }

    val activePalette = remember(selectedAccentKey, useAmoled) {
        val base = if (useAmoled) AmoledPalette else DarkPalette
        val preset = AccentPresets.firstOrNull { it.key == selectedAccentKey } ?: AccentPresets.first()
        base.withAccent(preset.color)
    }

    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
        }
    }

    var currentTab by remember { mutableStateOf(FlickShareTab.THROW) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var pendingThrowLabel by remember { mutableStateOf<String?>(null) }
    var pendingThrowAim by remember { mutableStateOf<ThrowAim?>(null) }
    var isGrabbed by remember { mutableStateOf(false) }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val currentReceiverState by rememberUpdatedState(receiverState)
    val currentDiscoveredEndpoints by rememberUpdatedState(discoveredEndpoints)
    val currentStagedFiles by rememberUpdatedState(stagedFiles)
    val currentTransferState by rememberUpdatedState(transferState)

    // Arm ThrowSensorController while the card is held, or always when a hands-free gesture is on
    val isReadyToThrow = currentTab == FlickShareTab.THROW &&
        stagedFiles.isNotEmpty() &&
        !showManualIpDialog &&
        (transferState is TransferUiState.Idle || transferState is TransferUiState.Failed)
    val handsFreeHint = if (isReadyToThrow && !isGrabbed) {
        handsFreeGestureHint(
            flick = handsFreeFlick && isSensorAvailable,
            bump = bumpToSend && isBumpAvailable,
        )
    } else {
        null
    }
    var sensorArmEpoch by remember { mutableStateOf(0) }
    LaunchedEffect(isReadyToThrow, isGrabbed, handsFreeFlick, bumpToSend, sensorArmEpoch) {
        onSensorArmChanged(
            SensorArming(
                flick = isReadyToThrow && (isGrabbed || handsFreeFlick),
                handsFree = !isGrabbed,
                bump = isReadyToThrow && bumpToSend,
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onSensorArmChanged(SensorArming())
        }
    }

    // Reset card coordinates whenever returning to Idle or staging new files
    LaunchedEffect(transferState, stagedFiles) {
        if (transferState is TransferUiState.Idle || transferState is TransferUiState.Failed) {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    // If a throw was waiting on one-time initial device setup, launch it as soon as connected
    LaunchedEffect(receiverState, pendingThrowLabel) {
        val label = pendingThrowLabel
        if (label != null && receiverState is ReceiverState.Connected && stagedFiles.isNotEmpty()) {
            pendingThrowLabel = null
            currentTab = FlickShareTab.THROW
            onTriggerTouchHaptics()
            val flyJob = coroutineScope.launch {
                offsetY.animateTo(
                    targetValue = -2200f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                )
            }
            flyJob.join()
            onStartTransfer(label, pendingThrowAim)
            pendingThrowAim = null
        }
    }

    // Listen for physical wrist-flick throws while holding the card
    LaunchedEffect(wristFlickEvents) {
        wristFlickEvents.collect { flick ->
            if (currentStagedFiles.isEmpty()) return@collect
            if (currentTransferState !is TransferUiState.Idle &&
                currentTransferState !is TransferUiState.Failed
            ) {
                return@collect
            }

            isGrabbed = false
            onSensorArmChanged(SensorArming())

            val launchLabel = if (flick.isBump) {
                String.format(Locale.US, "Tap to Share (%.1f m/s²)", flick.magnitude)
            } else {
                String.format(Locale.US, "Wrist Flick (%.1f m/s²)", flick.magnitude)
            }
            if (currentReceiverState !is ReceiverState.Connected) {
                val firstDiscovered = currentDiscoveredEndpoints.firstOrNull()
                if (firstDiscovered != null) {
                    onSelectEndpoint(firstDiscovered)
                } else {
                    pendingThrowLabel = launchLabel
                    pendingThrowAim = flick.aim
                    showManualIpDialog = true
                    coroutineScope.launch { offsetX.animateTo(0f, spring()) }
                    coroutineScope.launch { offsetY.animateTo(0f, spring()) }
                    return@collect
                }
            }

            val animJob = coroutineScope.launch {
                launch {
                    offsetY.animateTo(
                        targetValue = -2200f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                    )
                }
                launch {
                    offsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 280),
                    )
                }
            }
            animJob.join()
            onStartTransfer(launchLabel, flick.aim)
            // The detector disarmed itself; re-sync even if transferState lands back in the same state.
            sensorArmEpoch++
        }
    }

    val triggerAccessibleThrow: () -> Unit = {
        val launchLabel = "Instant Glass Launch"
        currentTab = FlickShareTab.THROW
        if (currentReceiverState !is ReceiverState.Connected) {
            val firstDiscovered = currentDiscoveredEndpoints.firstOrNull()
            if (firstDiscovered != null) {
                onSelectEndpoint(firstDiscovered)
                pendingThrowLabel = launchLabel
            } else {
                pendingThrowLabel = launchLabel
                showManualIpDialog = true
            }
        } else {
            onTriggerTouchHaptics()
            coroutineScope.launch {
                val flyJob = launch {
                    offsetY.animateTo(
                        targetValue = -2200f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                    )
                }
                flyJob.join()
                onStartTransfer(launchLabel, null)
            }
        }
    }

    val hazeState = rememberHazeState()
    val liquidBackdrop = rememberLayerBackdrop()

    FlickShareTheme(palette = activePalette) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            if (showManualIpDialog) {
                val currentConnected = (receiverState as? ReceiverState.Connected)?.endpoint
                val initialHost = currentConnected?.hostAddress.orEmpty()
                val initialPort = currentConnected?.port ?: 8765
                ManualEndpointDialog(
                    currentEndpoint = currentConnected,
                    discoveredEndpoints = discoveredEndpoints,
                    initialHost = initialHost,
                    initialPort = initialPort,
                    onDismiss = {
                        showManualIpDialog = false
                        pendingThrowLabel = null
                        pendingThrowAim = null
                    },
                    onSelectEndpoint = { endpoint ->
                        showManualIpDialog = false
                        onSelectEndpoint(endpoint)
                    },
                    onConfirm = { host, port ->
                        showManualIpDialog = false
                        onSetManualEndpoint(host, port)
                    },
                    onForgetAndRescan = {
                        onForgetDeviceAndRescan()
                    },
                )
            }

            // MusicSMRoot structure:
            // 1. Inner recorded layer with .layerBackdrop(liquidBackdrop) + .glassBackdrop(hazeState) + .background(AppBackground)
            // 2. Floating Liquid Glass chrome outside the recorded layer using LocalLiquidBackdrop
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(liquidBackdrop)
                        .glassBackdrop(hazeState)
                        .background(AppBackground),
                ) {
                    // Stitch ambient mesh orbs inside the recorded layer so the Liquid Glass navbar refracts them!
                    StitchAmbientMesh(
                        dragOffsetX = offsetX.value,
                        dragOffsetY = offsetY.value,
                        isGrabbed = isGrabbed,
                        isReceiverConnected = receiverState is ReceiverState.Connected,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = FLOATING_BAR_SPACE,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TopStitchHeader(
                            stagedCount = stagedFiles.size,
                            isSensorAvailable = isSensorAvailable,
                            isGrabbed = isGrabbed,
                            handsFreeHint = handsFreeHint,
                            onRescan = onRescanReceiver,
                            onOpenDeviceDialog = { showManualIpDialog = true },
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.98f, animationSpec = tween(160)))
                                },
                                label = "TabContentTransition",
                            ) { tab ->
                                when (tab) {
                                    FlickShareTab.THROW -> {
                                        ThrowTabStage(
                                            receiverState = receiverState,
                                            discoveredEndpoints = discoveredEndpoints,
                                            stagedFiles = stagedFiles,
                                            transferState = transferState,
                                            isSensorAvailable = isSensorAvailable,
                                            isGrabbed = isGrabbed,
                                            handsFreeHint = handsFreeHint,
                                            onGrabChanged = { isGrabbed = it },
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                                            onClearFiles = onClearFiles,
                                            onInstantThrow = triggerAccessibleThrow,
                                            onResetTransferState = onResetTransferState,
                                            onTriggerTouchHaptics = onTriggerTouchHaptics,
                                            onStartTransfer = onStartTransfer,
                                            onSelectEndpoint = onSelectEndpoint,
                                            onNeedDeviceDialog = { label, aim ->
                                                pendingThrowLabel = label
                                                pendingThrowAim = aim
                                                showManualIpDialog = true
                                            },
                                        )
                                    }

                                    FlickShareTab.DECK -> {
                                        DeckTabContent(
                                            stagedFiles = stagedFiles,
                                            onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                                            onClearFiles = onClearFiles,
                                            onThrowNow = triggerAccessibleThrow,
                                        )
                                    }

                                    FlickShareTab.DEVICES -> {
                                        DevicesAndThemeTabContent(
                                            receiverState = receiverState,
                                            discoveredEndpoints = discoveredEndpoints,
                                            selectedAccentKey = selectedAccentKey,
                                            useAmoled = useAmoled,
                                            onSelectAccent = { key ->
                                                selectedAccentKey = key
                                                themePrefs.edit().putString("accent_key", key).apply()
                                            },
                                            onToggleAmoled = { enabled ->
                                                useAmoled = enabled
                                                themePrefs.edit().putBoolean("amoled_dark", enabled).apply()
                                            },
                                            isSensorAvailable = isSensorAvailable,
                                            handsFreeFlick = handsFreeFlick,
                                            onToggleHandsFreeFlick = { enabled ->
                                                handsFreeFlick = enabled
                                                themePrefs.edit().putBoolean(HANDS_FREE_FLICK_PREF, enabled).apply()
                                            },
                                            isBumpAvailable = isBumpAvailable,
                                            bumpToSend = bumpToSend,
                                            onToggleBumpToSend = { enabled ->
                                                bumpToSend = enabled
                                                themePrefs.edit().putBoolean(BUMP_TO_SEND_PREF, enabled).apply()
                                            },
                                            onSelectEndpoint = onSelectEndpoint,
                                            onOpenManualIp = { showManualIpDialog = true },
                                            onRescan = onRescanReceiver,
                                            onForgetAndRescan = onForgetDeviceAndRescan,
                                            isCompassAvailable = isCompassAvailable,
                                            isAimCalibrated = isAimCalibrated,
                                            onCalibrateAim = onCalibrateAim,
                                            onClearAim = onClearAim,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Liquid Glass Chrome outside the recorded backdrop layer (exact MusicSM pattern!)
                CompositionLocalProvider(LocalLiquidBackdrop provides liquidBackdrop) {
                    val hasStaged = stagedFiles.isNotEmpty() &&
                        (transferState is TransferUiState.Idle || transferState is TransferUiState.Failed)

                    FloatingBottomBar(
                        currentTab = currentTab,
                        onSelectTab = { tab -> currentTab = tab },
                        actionIcon = if (hasStaged) Icons.Filled.RocketLaunch else Icons.Filled.Add,
                        actionContentDescription = if (hasStaged) {
                            "Quick Throw staged files to Desktop"
                        } else {
                            "Stage files from phone to throw"
                        },
                        actionTint = if (hasStaged) Coral else OnSurface,
                        onActionClick = {
                            if (hasStaged) {
                                triggerAccessibleThrow()
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                        miniStatusPill = {
                            MiniReceiverStatusPill(
                                receiverState = receiverState,
                                transferState = transferState,
                                onRescan = onRescanReceiver,
                                onOpenSwitcher = { showManualIpDialog = true },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStitchHeader(
    stagedCount: Int,
    isSensorAvailable: Boolean,
    isGrabbed: Boolean,
    handsFreeHint: String?,
    onRescan: () -> Unit,
    onOpenDeviceDialog: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPanel(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                tint = Coral.copy(alpha = 0.22f),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "FlickShare",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                )
                Text(
                    text = when {
                        isGrabbed && isSensorAvailable -> "SENSOR ARMED • FLICK WRIST OR FLING UP"
                        handsFreeHint != null -> "HANDS-FREE • $handsFreeHint"
                        stagedCount > 0 -> "$stagedCount FILE(S) IN DECK • READY TO THROW"
                        else -> "LIQUID GLASS LAN THROW"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGrabbed || handsFreeHint != null) Teal else OnSurfaceMuted,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPanel(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                tint = GlassFillStrong,
            ) {
                IconButton(onClick = onRescan, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan LAN for Desktop Receiver",
                        tint = OnSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            GlassPanel(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                tint = GlassFillStrong,
            ) {
                IconButton(onClick = onOpenDeviceDialog, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Desktop Receiver",
                        tint = Coral,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

/**
 * Floating Liquid Glass Mini Status Pill above the BottomNavBar (modeled directly on MusicSM's MiniPlayer).
 * Displays the locked Desktop Receiver (or active transfer progress bar) and opens the Device Switcher on tap.
 */
@Composable
private fun MiniReceiverStatusPill(
    receiverState: ReceiverState,
    transferState: TransferUiState,
    onRescan: () -> Unit,
    onOpenSwitcher: () -> Unit,
) {
    val (accentColor, statusIcon, titleText, subtitleText) = when (receiverState) {
        is ReceiverState.Connected -> StatusVisuals(
            accentColor = Teal,
            icon = Icons.Default.Computer,
            title = receiverState.endpoint.serviceName,
            subtitle = "${receiverState.endpoint.hostAddress}:${receiverState.endpoint.port} • Locked (Tap to switch)",
        )
        is ReceiverState.Scanning -> StatusVisuals(
            accentColor = Coral,
            icon = Icons.Default.WifiFind,
            title = "Scanning LAN for Desktop...",
            subtitle = "Tap to pick or enter Desktop IP",
        )
        is ReceiverState.Error -> StatusVisuals(
            accentColor = Coral,
            icon = Icons.Default.ErrorOutline,
            title = receiverState.message,
            subtitle = "Tap to choose or configure Desktop IP",
        )
    }

    val uploadProgress = (transferState as? TransferUiState.Uploading)?.progress?.fraction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$titleText. $subtitleText"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenSwitcher,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (receiverState is ReceiverState.Scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor,
                    )
                } else {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onRescan,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan LAN",
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = onOpenSwitcher,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Desktop Receiver",
                    tint = Coral,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (uploadProgress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Coral, Teal),
                            ),
                        ),
                )
            }
        }
    }
}

private data class StatusVisuals(
    val accentColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
private fun ThrowTabStage(
    receiverState: ReceiverState,
    discoveredEndpoints: List<ReceiverEndpoint>,
    stagedFiles: List<StagedFile>,
    transferState: TransferUiState,
    isSensorAvailable: Boolean,
    isGrabbed: Boolean,
    handsFreeHint: String?,
    onGrabChanged: (Boolean) -> Unit,
    offsetX: Animatable<Float, *>,
    offsetY: Animatable<Float, *>,
    onPickFiles: () -> Unit,
    onClearFiles: () -> Unit,
    onInstantThrow: () -> Unit,
    onResetTransferState: () -> Unit,
    onTriggerTouchHaptics: () -> Unit,
    onStartTransfer: (String, ThrowAim?) -> Unit,
    onSelectEndpoint: (ReceiverEndpoint) -> Unit,
    onNeedDeviceDialog: (String, ThrowAim?) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentReceiverState by rememberUpdatedState(receiverState)
    val currentDiscoveredEndpoints by rememberUpdatedState(discoveredEndpoints)

    AnimatedContent(
        targetState = transferState,
        // Progress updates must reuse the upload card instead of replaying its entrance.
        contentKey = { it::class },
        transitionSpec = {
            (fadeIn(tween(260)) + scaleIn(initialScale = 0.92f, animationSpec = tween(260)))
                .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.95f, animationSpec = tween(180)))
        },
        label = "TransferStageTransition",
    ) { uiState ->
        when (uiState) {
            is TransferUiState.Uploading -> {
                UploadingProgressCard(progress = uiState.progress)
            }

            is TransferUiState.CaughtOnDesktop -> {
                CaughtOnDesktopCard(
                    result = uiState.result,
                    launchModeLabel = uiState.launchModeLabel,
                    onThrowAgain = onResetTransferState,
                    onPickNewFiles = {
                        onResetTransferState()
                        onPickFiles()
                    },
                )
            }

            is TransferUiState.Idle,
            is TransferUiState.Failed -> {
                val errorMsg = (uiState as? TransferUiState.Failed)?.errorMessage
                if (stagedFiles.isEmpty()) {
                    EmptyStageCard(onPickFiles = onPickFiles)
                } else {
                    DraggableThrowDeck(
                        stagedFiles = stagedFiles,
                        receiverState = receiverState,
                        isSensorAvailable = isSensorAvailable,
                        isGrabbed = isGrabbed,
                        handsFreeHint = handsFreeHint,
                        errorBanner = errorMsg,
                        offsetX = offsetX.value,
                        offsetY = offsetY.value,
                        onAddMoreFiles = onPickFiles,
                        onInstantThrow = onInstantThrow,
                        onClearFiles = onClearFiles,
                        modifier = Modifier.pointerInput(stagedFiles, receiverState) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                onGrabChanged(true)
                                coroutineScope.launch {
                                    offsetX.stop()
                                    offsetY.stop()
                                }

                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                var pointerId = down.id
                                var lastPosition = down.position
                                var stillGrabbed = true
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change: PointerInputChange =
                                        event.changes.firstOrNull { it.id == pointerId }
                                            ?: event.changes.firstOrNull()
                                            ?: break

                                    pointerId = change.id
                                    lastPosition = change.position
                                    if (!change.pressed) {
                                        velocityTracker.addPosition(
                                            change.uptimeMillis,
                                            change.position,
                                        )
                                        break
                                    }

                                    velocityTracker.addPosition(
                                        change.uptimeMillis,
                                        change.position,
                                    )
                                    val delta = change.positionChange()
                                    if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                                        change.consume()
                                        coroutineScope.launch {
                                            offsetX.snapTo(offsetX.value + delta.x)
                                            offsetY.snapTo(offsetY.value + delta.y)
                                        }
                                    }
                                }

                                onGrabChanged(false)
                                if (!stillGrabbed) return@awaitEachGesture

                                val velocity = velocityTracker.calculateVelocity()
                                val vy = velocity.y
                                val vx = velocity.x
                                val shouldThrow =
                                    vy < -1800f || (offsetY.value < -250f && vy < -850f)

                                if (shouldThrow) {
                                    val launchLabel =
                                        "Touch Fling (${(-vy / 1000f).roundToInt()}k px/s)"
                                    val drag = lastPosition - down.position
                                    val flingAim = ThrowAim.fromFling(vx, vy, drag.x, drag.y)
                                    if (currentReceiverState !is ReceiverState.Connected) {
                                        val firstDiscovered = currentDiscoveredEndpoints.firstOrNull()
                                        if (firstDiscovered != null) {
                                            onSelectEndpoint(firstDiscovered)
                                            onNeedDeviceDialog(launchLabel, flingAim)
                                        } else {
                                            onNeedDeviceDialog(launchLabel, flingAim)
                                        }
                                        coroutineScope.launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow,
                                                ),
                                            )
                                        }
                                        coroutineScope.launch {
                                            offsetY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow,
                                                ),
                                            )
                                        }
                                    } else {
                                        onTriggerTouchHaptics()
                                        coroutineScope.launch {
                                            val flyJob = launch {
                                                launch {
                                                    offsetY.animateTo(
                                                        targetValue = -2200f,
                                                        animationSpec = tween(
                                                            durationMillis = 280,
                                                            easing = FastOutLinearInEasing,
                                                        ),
                                                    )
                                                }
                                                launch {
                                                    offsetX.animateTo(
                                                        targetValue = offsetX.value + (vx * 0.12f).coerceIn(-360f, 360f),
                                                        animationSpec = tween(durationMillis = 280),
                                                    )
                                                }
                                            }
                                            flyJob.join()
                                            onStartTransfer(launchLabel, flingAim)
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        )
                                    }
                                    coroutineScope.launch {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStageCard(onPickFiles: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyOrbFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FloatingIconY",
    )

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        tint = GlassFillStrong,
        borderWidth = 1.2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassPanel(
                modifier = Modifier
                    .size(82.dp)
                    .offset(y = floatY.dp),
                shape = CircleShape,
                tint = Coral.copy(alpha = 0.22f),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Stage a File to Throw",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Share any photo, video, or file from Android's Share Sheet — or tap + below and flick your phone toward your Desktop.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(26.dp))

            LiquidGlassPillButton(
                text = "Pick Files to Throw",
                icon = Icons.Default.Add,
                onClick = onPickFiles,
                accentColor = Coral,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                contentDescriptionText = "Pick files from device storage to throw to desktop",
            )
        }
    }
}

@Composable
private fun DraggableThrowDeck(
    stagedFiles: List<StagedFile>,
    receiverState: ReceiverState,
    isSensorAvailable: Boolean,
    isGrabbed: Boolean,
    handsFreeHint: String?,
    errorBanner: String?,
    offsetX: Float,
    offsetY: Float,
    onAddMoreFiles: () -> Unit,
    onInstantThrow: () -> Unit,
    onClearFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val upwardProgress = (-offsetY / 320f).coerceIn(0f, 1f)

    val baseGrabScale by animateFloatAsState(
        targetValue = if (isGrabbed) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "CardGrabScale",
    )
    val liquidStretchY = 1f + (upwardProgress * 0.065f)
    val liquidSquishX = 1f - (upwardProgress * 0.03f)

    val totalBytes = remember(stagedFiles) {
        stagedFiles.sumOf { max(0L, it.sizeBytes) }
    }

    val cardAccent = if (isGrabbed) Teal else Coral

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LiquidLaunchPortal(
            isGrabbed = isGrabbed,
            isConnected = receiverState is ReceiverState.Connected,
            upwardDragProgress = upwardProgress,
            statusLabel = if (isGrabbed) {
                if (isSensorAvailable) "ARMED • FLING UP OR FLICK WRIST (>12 m/s²)"
                else "ARMED • FLING CARD UPWARD TOWARD PC"
            } else if (handsFreeHint != null) {
                "HANDS-FREE • $handsFreeHint"
            } else {
                "HOLD CARD TO ARM • FLING UP OR FLICK WRIST"
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
        )

        if (stagedFiles.size > 1) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset {
                        IntOffset(
                            (offsetX * 0.45f).roundToInt(),
                            (offsetY * 0.45f - 20f).roundToInt(),
                        )
                    }
                    .graphicsLayer {
                        scaleX = 0.92f
                        scaleY = 0.92f
                        alpha = 0.55f
                    }
                    .height(250.dp),
                shape = RoundedCornerShape(32.dp),
                tint = GlassFillStrong,
            ) {}
        }

        GlassPanel(
            modifier = modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer {
                    scaleX = baseGrabScale * liquidSquishX
                    scaleY = baseGrabScale * liquidStretchY
                    rotationZ = (offsetX / 25f).coerceIn(-15f, 15f)
                    rotationX = (-offsetY / 32f).coerceIn(-14f, 14f)
                    rotationY = (offsetX / 28f).coerceIn(-14f, 14f)
                    cameraDistance = 14f * density
                },
            shape = RoundedCornerShape(32.dp),
            tint = if (isGrabbed) cardAccent.copy(alpha = 0.16f) else GlassFillStrong,
            borderWidth = if (isGrabbed) 1.8.dp else 1.2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassPanel(
                        shape = RoundedCornerShape(50),
                        tint = cardAccent.copy(alpha = 0.20f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = cardAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGrabbed) "GRABBED • SENSOR ARMED" else "LIQUID PAYLOAD READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurface,
                            )
                        }
                    }

                    GlassPanel(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        tint = GlassFill,
                    ) {
                        IconButton(
                            onClick = onClearFiles,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear staged files",
                                tint = OnSurface,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (stagedFiles.size == 1) {
                        stagedFiles.first().name
                    } else {
                        "${stagedFiles.size} Files Staged in Deck"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Payload • ${FileTransferClient.formatFileSize(totalBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardAccent,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stagedFiles.take(3).forEach { file ->
                        GlassPanel(
                            shape = RoundedCornerShape(16.dp),
                            tint = GlassFill,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Coral.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Coral,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${file.mimeType} • ${FileTransferClient.formatFileSize(file.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    if (stagedFiles.size > 3) {
                        Text(
                            text = "+ ${stagedFiles.size - 3} more file(s) in Deck tab",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                if (!errorBanner.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassPanel(
                        shape = RoundedCornerShape(14.dp),
                        tint = Coral.copy(alpha = 0.22f),
                    ) {
                        Text(
                            text = errorBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LiquidGlassPillButton(
                        text = "Add More",
                        icon = Icons.Default.Add,
                        onClick = onAddMoreFiles,
                        accentColor = Lavender,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                    )

                    LiquidGlassPillButton(
                        text = "Quick Throw",
                        icon = Icons.Default.RocketLaunch,
                        onClick = onInstantThrow,
                        accentColor = Coral,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckTabContent(
    stagedFiles: List<StagedFile>,
    onPickFiles: () -> Unit,
    onClearFiles: () -> Unit,
    onThrowNow: () -> Unit,
) {
    val totalBytes = remember(stagedFiles) { stagedFiles.sumOf { max(0L, it.sizeBytes) } }

    GlassPanel(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(30.dp),
        tint = GlassFillStrong,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Payload Deck",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                    )
                    Text(
                        text = "${stagedFiles.size} file(s) • ${FileTransferClient.formatFileSize(totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Coral,
                    )
                }

                if (stagedFiles.isNotEmpty()) {
                    GlassPanel(
                        shape = RoundedCornerShape(50),
                        tint = GlassFill,
                        modifier = Modifier.clickable { onClearFiles() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all staged files",
                                tint = Coral,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurface,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (stagedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No files staged yet.\nTap + on the Liquid Glass bar below to add files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(stagedFiles) { file ->
                        GlassPanel(
                            shape = RoundedCornerShape(18.dp),
                            tint = GlassFill,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Coral.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Coral,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                        color = OnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${file.mimeType} • ${FileTransferClient.formatFileSize(file.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LiquidGlassPillButton(
                    text = "Stage Files",
                    icon = Icons.Default.Add,
                    onClick = onPickFiles,
                    accentColor = Lavender,
                    isPrimary = stagedFiles.isEmpty(),
                    modifier = Modifier.weight(1f),
                )
                if (stagedFiles.isNotEmpty()) {
                    LiquidGlassPillButton(
                        text = "Launch All",
                        icon = Icons.Default.RocketLaunch,
                        onClick = onThrowNow,
                        accentColor = Coral,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DevicesAndThemeTabContent(
    receiverState: ReceiverState,
    discoveredEndpoints: List<ReceiverEndpoint>,
    selectedAccentKey: String,
    useAmoled: Boolean,
    onSelectAccent: (String) -> Unit,
    onToggleAmoled: (Boolean) -> Unit,
    isSensorAvailable: Boolean,
    handsFreeFlick: Boolean,
    onToggleHandsFreeFlick: (Boolean) -> Unit,
    isBumpAvailable: Boolean,
    bumpToSend: Boolean,
    onToggleBumpToSend: (Boolean) -> Unit,
    onSelectEndpoint: (ReceiverEndpoint) -> Unit,
    onOpenManualIp: () -> Unit,
    onRescan: () -> Unit,
    onForgetAndRescan: () -> Unit,
    isCompassAvailable: Boolean,
    isAimCalibrated: Boolean,
    onCalibrateAim: () -> Unit,
    onClearAim: () -> Unit,
) {
    val currentConnected = (receiverState as? ReceiverState.Connected)?.endpoint

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tint = GlassFillStrong,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desktop Receivers",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                            )
                            Text(
                                text = "One-time connection is remembered automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted,
                            )
                        }
                        IconButton(onClick = onRescan) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan LAN",
                                tint = Coral,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (discoveredEndpoints.isEmpty()) {
                        Text(
                            text = "No receivers discovered yet. Tap Manual IP below or Rescan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceMuted,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            discoveredEndpoints.forEach { device ->
                                val isCurrent = currentConnected != null &&
                                    currentConnected.hostAddress.equals(device.hostAddress, ignoreCase = true) &&
                                    currentConnected.port == device.port

                                GlassPanel(
                                    shape = RoundedCornerShape(18.dp),
                                    tint = if (isCurrent) Teal.copy(alpha = 0.18f) else GlassFill,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectEndpoint(device) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Computer,
                                                contentDescription = null,
                                                tint = if (isCurrent) Teal else OnSurface,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = device.serviceName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                                    color = OnSurface,
                                                )
                                                Text(
                                                    text = "${device.hostAddress}:${device.port}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnSurfaceMuted,
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isCurrent) "LOCKED" else "CONNECT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) Teal else Coral,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LiquidGlassPillButton(
                            text = "Manual IP",
                            icon = Icons.Default.Edit,
                            onClick = onOpenManualIp,
                            accentColor = Coral,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (currentConnected != null) {
                            LiquidGlassPillButton(
                                text = "Forget",
                                icon = Icons.Default.Close,
                                onClick = onForgetAndRescan,
                                accentColor = Lavender,
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // Throw direction: compass calibration so the PC's liquid glass enters from your side of the room
        if (currentConnected != null && isCompassAvailable) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    tint = GlassFillStrong,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = if (isAimCalibrated) Teal else Coral,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Throw Direction",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (isAimCalibrated) "COMPASS" else "GESTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAimCalibrated) Teal else OnSurfaceMuted,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAimCalibrated) {
                                "Glass on ${currentConnected.serviceName} enters from the side of the room you throw from."
                            } else {
                                "Stand in front of your PC, point the top of your phone at the screen and tap Point at PC. " +
                                    "Until then, the angle of your fling or wrist flick picks the side."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceMuted,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LiquidGlassPillButton(
                                text = if (isAimCalibrated) "Re-aim" else "Point at PC",
                                icon = Icons.Default.Explore,
                                onClick = onCalibrateAim,
                                accentColor = Coral,
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (isAimCalibrated) {
                                LiquidGlassPillButton(
                                    text = "Reset",
                                    icon = Icons.Default.Close,
                                    onClick = onClearAim,
                                    accentColor = Lavender,
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Throw gestures that work without holding the card
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tint = GlassFillStrong,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GestureToggleRow(
                        title = "Hands-free Flick",
                        description = if (isSensorAvailable) {
                            "Flick the phone toward your PC to send staged files without holding the card. " +
                                "Needs a sharper flick, so bumps and setting the phone down won't send."
                        } else {
                            "This phone has no motion sensor."
                        },
                        checked = handsFreeFlick && isSensorAvailable,
                        enabled = isSensorAvailable,
                        onCheckedChange = onToggleHandsFreeFlick,
                    )
                    GestureToggleRow(
                        title = "Tap to Share",
                        description = if (isBumpAvailable) {
                            "Tap the phone twice against your PC or laptop to send staged files to the " +
                                "connected PC. A single knock, like putting the phone down, won't send."
                        } else {
                            "This phone has no accelerometer."
                        },
                        checked = bumpToSend && isBumpAvailable,
                        enabled = isBumpAvailable,
                        onCheckedChange = onToggleBumpToSend,
                    )
                }
            }
        }

        // Accent & AMOLED Liquid Glass Appearance Card
        item {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tint = GlassFillStrong,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Coral,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Liquid Glass Theme",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AccentPresets) { preset ->
                            val selected = preset.key == selectedAccentKey
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onSelectAccent(preset.key) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(preset.color)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) OnSurface else OnSurfaceMuted,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "AMOLED Pitch Black",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = OnSurface,
                            )
                            Text(
                                text = "Pure #000000 backdrop behind Liquid Glass lenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted,
                            )
                        }
                        Switch(
                            checked = useAmoled,
                            onCheckedChange = onToggleAmoled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Coral,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadingProgressCard(progress: UploadProgress) {
    val percentage = (progress.fraction * 100f).roundToInt().coerceIn(0, 100)

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Uploading ${progress.currentFileName}, $percentage percent complete"
            },
        shape = RoundedCornerShape(32.dp),
        tint = GlassFillStrong,
        borderWidth = 1.5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LiquidOrbitalProgressGauge(
                fraction = progress.fraction,
                percentage = percentage,
                speedMbps = progress.speedMbps,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Streaming to Desktop...",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = progress.currentFileName,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { progress.fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = Coral,
                trackColor = Color.White.copy(alpha = 0.12f),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${FileTransferClient.formatFileSize(progress.bytesSent)} of ${FileTransferClient.formatFileSize(progress.totalBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun CaughtOnDesktopCard(
    result: UploadResult,
    launchModeLabel: String,
    onThrowAgain: () -> Unit,
    onPickNewFiles: () -> Unit,
) {
    val speedMbps = remember(result) {
        val sec = max(0.001f, result.durationMs / 1000f)
        (result.totalBytes / (1024f * 1024f)) / sec
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Caught on Desktop! ${result.filesTransferred} files transferred."
            },
        shape = RoundedCornerShape(32.dp),
        tint = GlassFillStrong,
        borderWidth = 1.6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassPanel(
                modifier = Modifier.size(74.dp),
                shape = CircleShape,
                tint = Teal.copy(alpha = 0.22f),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Caught on Desktop!",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${result.filesTransferred} file(s) • ${FileTransferClient.formatFileSize(result.totalBytes)} • ${String.format(Locale.US, "%.1f MB/s", speedMbps)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Teal,
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassPanel(
                shape = RoundedCornerShape(50),
                tint = GlassFill,
            ) {
                Text(
                    text = "Launched via $launchModeLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LiquidGlassPillButton(
                    text = "Throw Again",
                    icon = Icons.Default.Refresh,
                    onClick = onThrowAgain,
                    accentColor = Lavender,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                )

                LiquidGlassPillButton(
                    text = "New Files",
                    icon = Icons.Default.Add,
                    onClick = onPickNewFiles,
                    accentColor = Coral,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ManualEndpointDialog(
    currentEndpoint: ReceiverEndpoint?,
    discoveredEndpoints: List<ReceiverEndpoint>,
    initialHost: String,
    initialPort: Int,
    onDismiss: () -> Unit,
    onSelectEndpoint: (ReceiverEndpoint) -> Unit,
    onConfirm: (host: String, port: Int) -> Unit,
    onForgetAndRescan: () -> Unit,
) {
    var hostInput by remember(initialHost) { mutableStateOf(initialHost) }
    var portInput by remember(initialPort) { mutableStateOf(initialPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceMuted,
        title = {
            Text(
                text = if (currentEndpoint != null) "Switch or Edit Desktop Device" else "Connect Desktop Receiver (One-Time)",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "FlickShare remembers your chosen desktop automatically so you never have to select it again. You can switch devices anytime here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )

                if (discoveredEndpoints.isNotEmpty()) {
                    Text(
                        text = "Saved & Discovered Devices",
                        style = MaterialTheme.typography.labelLarge,
                        color = Coral,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        discoveredEndpoints.take(4).forEach { device ->
                            val isCurrent = currentEndpoint != null &&
                                currentEndpoint.hostAddress.equals(device.hostAddress, ignoreCase = true) &&
                                currentEndpoint.port == device.port
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) {
                                    Teal.copy(alpha = 0.18f)
                                } else {
                                    Color.White.copy(alpha = 0.07f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCurrent) Teal else Color.White.copy(alpha = 0.15f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectEndpoint(device) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.serviceName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${device.hostAddress}:${device.port}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceMuted,
                                        )
                                    }
                                    Text(
                                        text = if (isCurrent) "LOCKED" else "USE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) Teal else Coral,
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Desktop IP Address") },
                    placeholder = { Text("192.168.1.14") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    placeholder = { Text("8765") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (currentEndpoint != null) {
                    TextButton(
                        onClick = {
                            onForgetAndRescan()
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            text = "Forget Saved Device & Rescan",
                            color = Coral,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPort = portInput.toIntOrNull() ?: 8765
                    if (hostInput.isNotBlank()) {
                        onConfirm(hostInput.trim(), parsedPort)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
            ) {
                Text("Save & Connect", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceMuted)
            }
        },
    )
}
