package com.flickshare.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flickshare.app.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// 1. MusicSM Stitch Glassmorphic Palette & Plus Jakarta Sans Typography
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class MusicSmPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val surfaceBright: Color,
    val accent: Color,
    val accentDark: Color,
    val accentLight: Color,
    val teal: Color,
    val lavender: Color,
    val purpleContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val onSurfaceMuted: Color,
    val divider: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassStroke: Color,
    val glassStrokeSoft: Color,
    val hazeTint: Color,
)

val DarkPalette = MusicSmPalette(
    isDark = true,
    background = Color(0xFF121318),
    surfaceLowest = Color(0xFF0D0E13),
    surfaceLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceHigh = Color(0xFF292A2F),
    surfaceHighest = Color(0xFF34343A),
    surfaceBright = Color(0xFF38393F),
    accent = Color(0xFFFF525E),
    accentDark = Color(0xFFD8323E),
    accentLight = Color(0xFFFFB3B2),
    teal = Color(0xFF00DFC1),
    lavender = Color(0xFFDDB7FF),
    purpleContainer = Color(0xFF6F00BE),
    onSurface = Color(0xFFE3E1E9),
    onSurfaceVariant = Color(0xFFE6BDBC),
    outline = Color(0xFFAD8887),
    onSurfaceMuted = Color(0x80FFFFFF),
    divider = Color(0x1FFFFFFF),
    glassFill = Color(0x1FFFFFFF),
    glassFillStrong = Color(0x40292A2F),
    glassStroke = Color(0x33FFFFFF),
    glassStrokeSoft = Color(0x1AFFFFFF),
    hazeTint = Color(0x1AFFFFFF),
)

val AmoledPalette = DarkPalette.copy(
    background = Color(0xFF000000),
    surfaceLowest = Color(0xFF000000),
    surfaceLow = Color(0xFF0A0A0E),
    surfaceContainer = Color(0xFF111216),
    surfaceHigh = Color(0xFF1B1C21),
    surfaceHighest = Color(0xFF26272D),
    surfaceBright = Color(0xFF2C2D33),
    glassFillStrong = Color(0x5518191E),
)

data class AccentPreset(val key: String, val label: String, val color: Color)

val AccentPresets: List<AccentPreset> = listOf(
    AccentPreset("coral", "Coral", Color(0xFFFF525E)),
    AccentPreset("teal", "Teal", Color(0xFF00DFC1)),
    AccentPreset("azure", "Azure", Color(0xFF4DA3FF)),
    AccentPreset("lavender", "Lavender", Color(0xFFDDB7FF)),
    AccentPreset("mint", "Mint", Color(0xFF3DDC84)),
    AccentPreset("amber", "Amber", Color(0xFFFFB547)),
)

fun MusicSmPalette.withAccent(accentColor: Color): MusicSmPalette {
    val dark = lerp(accentColor, Color.Black, 0.20f)
    val light = lerp(accentColor, Color.White, 0.45f)
    return copy(accent = accentColor, accentDark = dark, accentLight = light)
}

val LocalPalette = compositionLocalOf { DarkPalette }
val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalLiquidBackdrop = compositionLocalOf<Backdrop?> { null }

val AppBackground: Color @Composable get() = LocalPalette.current.background
val SurfaceLowest: Color @Composable get() = LocalPalette.current.surfaceLowest
val SurfaceLow: Color @Composable get() = LocalPalette.current.surfaceLow
val SurfaceContainer: Color @Composable get() = LocalPalette.current.surfaceContainer
val SurfaceHigh: Color @Composable get() = LocalPalette.current.surfaceHigh
val SurfaceHighest: Color @Composable get() = LocalPalette.current.surfaceHighest
val Coral: Color @Composable get() = LocalPalette.current.accent
val CoralDark: Color @Composable get() = LocalPalette.current.accentDark
val CoralLight: Color @Composable get() = LocalPalette.current.accentLight
val Teal: Color @Composable get() = LocalPalette.current.teal
val Lavender: Color @Composable get() = LocalPalette.current.lavender
val OnSurface: Color @Composable get() = LocalPalette.current.onSurface
val OnSurfaceVariant: Color @Composable get() = LocalPalette.current.onSurfaceVariant
val OnSurfaceMuted: Color @Composable get() = LocalPalette.current.onSurfaceMuted
val GlassFill: Color @Composable get() = LocalPalette.current.glassFill
val GlassFillStrong: Color @Composable get() = LocalPalette.current.glassFillStrong
val GlassStroke: Color @Composable get() = LocalPalette.current.glassStroke
val GlassStrokeSoft: Color @Composable get() = LocalPalette.current.glassStrokeSoft

val PlusJakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_extrabold, FontWeight.ExtraBold),
)

val MusicSmTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.2.sp
    )
)

fun buildMusicSmColorScheme(palette: MusicSmPalette) = darkColorScheme(
    primary = palette.accent,
    onPrimary = Color.White,
    primaryContainer = palette.accentDark,
    secondary = palette.teal,
    onSecondary = palette.surfaceLowest,
    tertiary = palette.lavender,
    tertiaryContainer = palette.purpleContainer,
    background = palette.background,
    onBackground = palette.onSurface,
    surface = palette.surfaceLow,
    onSurface = palette.onSurface,
    surfaceVariant = palette.surfaceHigh,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.outline,
)

// ─────────────────────────────────────────────────────────────────────────────
// 2. MusicSM Glass.kt — Real Apple Liquid Glass (Kyant Backdrop Lens + Haze)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Marks a composable tree as the backdrop source that [GlassPanel]s blur against.
 */
fun Modifier.glassBackdrop(state: HazeState): Modifier = this.hazeSource(state = state)

/**
 * Translucent glassmorphic surface from MusicSM.
 *
 * When [liquid] is true and a [LocalLiquidBackdrop] is provided, renders an Apple-style
 * Liquid Glass panel using Kyant's AGSL RuntimeShader (`vibrancy`, `blur`, and optical `lens`
 * refraction with `Highlight.Default`). Otherwise uses `Haze` real backdrop blur + diagonal
 * specular sheen + additive `BlendMode.Plus` rim.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = GlassFill,
    blurRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    rim: Boolean = true,
    sheen: Boolean = true,
    liquid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val liquidBackdrop = LocalLiquidBackdrop.current
    if (liquid && liquidBackdrop != null) {
        LiquidGlassPanel(
            backdrop = liquidBackdrop,
            modifier = modifier,
            shape = shape,
            tint = tint,
            content = content,
        )
        return
    }

    val hazeState = LocalHazeState.current
    val palette = LocalPalette.current
    val hazeTint = palette.hazeTint
    val blurMod = if (hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                blurRadius = blurRadius,
                tint = HazeTint(hazeTint),
                noiseFactor = 0f,
            ),
        )
    } else {
        Modifier
    }

    val sheenMod = if (sheen) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(600f, 600f),
            ),
        )
    } else {
        Modifier
    }

    val rimMod = if (rim && borderWidth > 0.dp) {
        Modifier.glassRim(shape = shape, strokeWidth = borderWidth)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(blurMod)
            .background(tint)
            .then(sheenMod)
            .then(rimMod),
        content = content,
    )
}

/**
 * Apple-style liquid glass backed by Kyant's Backdrop library: real AGSL RuntimeShader
 * edge refraction (`lens`) + vibrancy + specular rim highlight.
 */
@Composable
private fun LiquidGlassPanel(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = GlassFill,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeTint = LocalPalette.current.hazeTint
    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(4.dp.toPx())
                lens(
                    refractionHeight = 16.dp.toPx(),
                    refractionAmount = 32.dp.toPx(),
                    depthEffect = true,
                )
            },
            highlight = { Highlight.Default },
            onDrawSurface = {
                drawRect(hazeTint)
                drawRect(tint)
            },
        ),
        content = content,
    )
}

private fun Modifier.glassRim(shape: Shape, strokeWidth: Dp): Modifier = this.drawWithCache {
    val strokePx = strokeWidth.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.16f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    onDrawWithContent {
        drawContent()
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rounded -> {
                val rr = outline.roundRect
                drawRoundRect(
                    brush = rimBrush,
                    topLeft = Offset(rr.left + strokePx / 2f, rr.top + strokePx / 2f),
                    size = Size(rr.width - strokePx, rr.height - strokePx),
                    cornerRadius = rr.topLeftCornerRadius,
                    style = Stroke(width = strokePx),
                    blendMode = BlendMode.Plus,
                )
            }
            is androidx.compose.ui.graphics.Outline.Generic -> {
                drawPath(
                    path = outline.path,
                    brush = rimBrush,
                    style = Stroke(width = strokePx),
                    blendMode = BlendMode.Plus,
                )
            }
            is androidx.compose.ui.graphics.Outline.Rectangle -> {
                drawRect(
                    brush = rimBrush,
                    size = size,
                    style = Stroke(width = strokePx),
                    blendMode = BlendMode.Plus,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. MusicSM FloatingBottomBar.kt & Scrubbable BottomNavBar.kt
// ─────────────────────────────────────────────────────────────────────────────

enum class FlickShareTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    THROW(
        label = "Throw",
        selectedIcon = Icons.Filled.RocketLaunch,
        unselectedIcon = Icons.Outlined.RocketLaunch,
    ),
    DECK(
        label = "Deck",
        selectedIcon = Icons.Filled.FolderCopy,
        unselectedIcon = Icons.Outlined.FolderCopy,
    ),
    DEVICES(
        label = "Devices",
        selectedIcon = Icons.Filled.Computer,
        unselectedIcon = Icons.Outlined.Computer,
    ),
}

val BottomBarTabs = listOf(
    FlickShareTab.THROW,
    FlickShareTab.DECK,
    FlickShareTab.DEVICES,
)

private val BAR_HEIGHT = 64.dp
private val MINI_HEIGHT = 60.dp
private val MINI_GAP = 10.dp
private val ACTION_SIZE = 64.dp
private val GAP = 8.dp
private val SCRIM_OVERHANG = 32.dp
val FLOATING_BAR_SPACE = 156.dp

/**
 * Exact MusicSM FloatingBottomBar layout:
 * - Optional floating [miniStatusPill] (`MINI_HEIGHT = 60.dp`) in a `GlassPanel(..., liquid = true)` pill above the navbar
 * - Bottom row (`BAR_HEIGHT = 64.dp`):
 *   - Left: `GlassPanel(shape = RoundedCornerShape(50), tint = GlassFillStrong, liquid = true)` hosting [BottomNavBar]
 *   - Right: Detached `64.dp` circular `GlassPanel(shape = CircleShape, tint = GlassFillStrong, liquid = true)` quick-action button
 */
@Composable
fun FloatingBottomBar(
    currentTab: FlickShareTab,
    onSelectTab: (FlickShareTab) -> Unit,
    actionIcon: ImageVector,
    actionContentDescription: String,
    actionTint: Color = OnSurface,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniStatusPill: (@Composable () -> Unit)? = null,
) {
    val hasMini = miniStatusPill != null
    val miniReserve by animateDpAsState(
        targetValue = if (hasMini) MINI_HEIGHT + MINI_GAP else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "miniReserve",
    )

    val bg = AppBackground
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val brush = Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.35f to bg.copy(alpha = 0.55f),
                    0.70f to bg.copy(alpha = 0.88f),
                    1.00f to bg,
                )
                onDrawBehind { drawRect(brush) }
            }
            .padding(top = SCRIM_OVERHANG)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(BAR_HEIGHT + miniReserve),
    ) {
        val inner = maxWidth
        val tabsFull = inner - ACTION_SIZE - GAP

        // Top row: Floating Liquid Glass Mini Receiver / Upload Status Pill (like MusicSM MiniPlayer)
        if (miniStatusPill != null) {
            GlassPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(inner)
                    .height(MINI_HEIGHT),
                shape = RoundedCornerShape(50),
                tint = GlassFillStrong,
                liquid = true,
            ) {
                miniStatusPill()
            }
        }

        // Bottom-left pill: Scrubbable Liquid Glass BottomNavBar
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(tabsFull)
                .height(BAR_HEIGHT),
            shape = RoundedCornerShape(50),
            tint = GlassFillStrong,
            liquid = true,
        ) {
            BottomNavBar(
                current = currentTab,
                onSelect = onSelectTab,
            )
        }

        // Bottom-right detached circle: Liquid Glass Action Button (+ Pick Files / Quick Throw)
        GlassPanel(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(ACTION_SIZE)
                .semantics {
                    role = Role.Button
                    contentDescription = actionContentDescription
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onActionClick,
                ),
            shape = CircleShape,
            tint = GlassFillStrong,
            liquid = true,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = actionTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private const val PUCK_VELOCITY_SCALE = 8f
private const val MAX_SQUASH = 0.22f
private const val LIFT_GROW_X = 0.12f
private const val LIFT_GROW_Y = 0.18f
private const val LIFT_CLEAR = 0.62f
private const val LIFT_SHEEN_ALPHA = 0.28f

/**
 * Exact MusicSM BottomNavBar with a single travelling Liquid Glass puck + finger scrubbing.
 *
 * Pressing and sliding horizontally across the bar lifts the puck into a larger, clearer
 * refractive lens (`LIFT_CLEAR`, `LIFT_SHEEN_ALPHA`, additive `BlendMode.Plus` rim),
 * ticks haptics (`HapticFeedbackType.SegmentTick`) as it crosses each tab, and commits on release.
 */
@Composable
fun BottomNavBar(
    current: FlickShareTab,
    onSelect: (FlickShareTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabCount = BottomBarTabs.size
    val selectedIdx = BottomBarTabs.indexOf(current).coerceAtLeast(0)

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnSelect by rememberUpdatedState(onSelect)

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }

    val travel = remember { Animatable(selectedIdx.toFloat()) }
    LaunchedEffect(selectedIdx, scrubbing) {
        if (!scrubbing) {
            travel.animateTo(
                targetValue = selectedIdx.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    val lift by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navPuckLift",
    )

    val activeIdx = travel.value.roundToInt().coerceIn(0, tabCount - 1)
    val palette = LocalPalette.current
    val puckFill = palette.surfaceLowest.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .pointerInput(tabCount) {
                var lastTickBucket = -1
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        if (barWidthPx <= 0f) return@detectHorizontalDragGestures
                        scrubbing = true
                        val slotW = barWidthPx / tabCount
                        val raw = (startOffset.x / slotW - 0.5f).coerceIn(0f, (tabCount - 1).toFloat())
                        lastTickBucket = raw.roundToInt()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            travel.animateTo(
                                targetValue = raw,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessHigh,
                                ),
                            )
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (barWidthPx <= 0f) return@detectHorizontalDragGestures
                        change.consume()
                        val slotW = barWidthPx / tabCount
                        val raw = (change.position.x / slotW - 0.5f)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                        val bucket = raw.roundToInt()
                        if (bucket != lastTickBucket) {
                            lastTickBucket = bucket
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        scope.launch { travel.snapTo(raw) }
                    },
                    onDragEnd = {
                        val target = travel.value.roundToInt().coerceIn(0, tabCount - 1)
                        scrubbing = false
                        currentOnSelect(BottomBarTabs[target])
                        scope.launch {
                            travel.animateTo(
                                targetValue = target.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        scrubbing = false
                    },
                )
            }
            .drawWithCache {
                val rimStroke = Stroke(width = 1.dp.toPx())
                val liftedRimStroke = Stroke(width = 1.6.dp.toPx())
                val rimBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.12f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                val liftedRimBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.38f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                onDrawBehind {
                    val slotW = size.width / tabCount
                    val speed = (abs(travel.velocity) / PUCK_VELOCITY_SCALE).coerceIn(0f, MAX_SQUASH)
                    val grow = lift
                    val scaleX = (1f + speed * 0.9f) * (1f + grow * LIFT_GROW_X)
                    val scaleY = (1f - speed * 0.5f) * (1f + grow * LIFT_GROW_Y)
                    val puckW = slotW * scaleX
                    val puckH = size.height * scaleY
                    val centerX = (travel.value + 0.5f) * slotW
                    val topLeft = Offset(centerX - puckW / 2f, (size.height - puckH) / 2f)
                    val puckSize = Size(puckW, puckH)
                    val radius = CornerRadius(puckH / 2f, puckH / 2f)

                    // Base fill clears out as the puck lifts so it reads as a clear lens
                    val baseFill = puckFill.copy(alpha = puckFill.alpha * (1f - grow * LIFT_CLEAR))
                    drawRoundRect(
                        color = baseFill,
                        topLeft = topLeft,
                        size = puckSize,
                        cornerRadius = radius,
                    )

                    if (grow > 0.01f) {
                        val sheen = Brush.verticalGradient(
                            0.00f to Color.White.copy(alpha = LIFT_SHEEN_ALPHA * grow),
                            0.45f to Color.White.copy(alpha = 0.05f * grow),
                            0.55f to Color.Transparent,
                            1.00f to Color.White.copy(alpha = 0.12f * grow),
                            startY = topLeft.y,
                            endY = topLeft.y + puckH,
                        )
                        drawRoundRect(
                            brush = sheen,
                            topLeft = topLeft,
                            size = puckSize,
                            cornerRadius = radius,
                            blendMode = BlendMode.Plus,
                        )
                    }

                    drawRoundRect(
                        brush = if (grow > 0.5f) liftedRimBrush else rimBrush,
                        topLeft = topLeft,
                        size = puckSize,
                        cornerRadius = radius,
                        style = if (grow > 0.5f) liftedRimStroke else rimStroke,
                        blendMode = BlendMode.Plus,
                    )
                }
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomBarTabs.forEachIndexed { idx, dest ->
            val selected = idx == activeIdx
            val tint by animateColorAsState(
                targetValue = if (selected) Coral else OnSurfaceMuted,
                animationSpec = tween(200),
                label = "navTint",
            )
            val iconScale by animateFloatAsState(
                targetValue = when {
                    selected && scrubbing -> 1.18f
                    selected -> 1.06f
                    else -> 1f
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "navScale",
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(dest) },
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                    tint = tint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dest.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.2.sp,
                    ),
                    color = tint,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Ambient Mesh & Throw Stage Visuals (Tuned to MusicSM Stitch Palette)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StitchAmbientMesh(
    dragOffsetX: Float,
    dragOffsetY: Float,
    isGrabbed: Boolean,
    isReceiverConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "StitchAmbientMesh")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "OrbPhase1",
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = (PI / 2f).toFloat(),
        targetValue = (2.5f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "OrbPhase2",
    )

    val grabBoost by animateFloatAsState(
        targetValue = if (isGrabbed) 1.38f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GrabBoost",
    )

    val primaryOrbColor by animateColorAsState(
        targetValue = if (isGrabbed && isReceiverConnected) palette.teal else palette.accent,
        animationSpec = tween(500),
        label = "PrimaryOrbColor",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val parallaxX = (dragOffsetX * 0.14f).coerceIn(-160f, 160f)
        val parallaxY = (dragOffsetY * 0.14f).coerceIn(-220f, 220f)

        // Top-right Coral / Teal glow
        val orb1 = Offset(
            x = w * (0.62f + 0.20f * cos(phase1)) + parallaxX * 0.8f,
            y = h * (0.18f + 0.08f * sin(phase1)) + parallaxY * 0.6f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryOrbColor.copy(alpha = (0.28f * grabBoost).coerceAtMost(0.50f)),
                    primaryOrbColor.copy(alpha = 0.08f * grabBoost),
                    Color.Transparent,
                ),
                center = orb1,
                radius = w * 0.80f * grabBoost,
            ),
            radius = w * 0.80f * grabBoost,
            center = orb1,
            blendMode = BlendMode.Screen,
        )

        // Mid-left Teal glow
        val orb2 = Offset(
            x = w * (0.18f + 0.16f * sin(phase2)) - parallaxX * 0.5f,
            y = h * (0.52f + 0.12f * cos(phase2)) + parallaxY * 0.4f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.teal.copy(alpha = (0.22f * grabBoost).coerceAtMost(0.42f)),
                    palette.teal.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = orb2,
                radius = w * 0.72f,
            ),
            radius = w * 0.72f,
            center = orb2,
            blendMode = BlendMode.Screen,
        )

        // Bottom-right Lavender glow (refracts right through the Liquid Glass FloatingBottomBar!)
        val orb3 = Offset(
            x = w * (0.78f + 0.16f * cos(phase2 * 0.85f)) + parallaxX * 0.4f,
            y = h * (0.82f + 0.08f * sin(phase1 * 0.9f)) - parallaxY * 0.3f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.lavender.copy(alpha = (0.24f * grabBoost).coerceAtMost(0.45f)),
                    palette.accent.copy(alpha = 0.07f),
                    Color.Transparent,
                ),
                center = orb3,
                radius = w * 0.76f,
            ),
            radius = w * 0.76f,
            center = orb3,
            blendMode = BlendMode.Screen,
        )
    }
}

@Composable
fun LiquidLaunchPortal(
    isGrabbed: Boolean,
    isConnected: Boolean,
    upwardDragProgress: Float,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "LaunchPortalWave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isGrabbed) 1050 else 2200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "PortalRippleProgress",
    )

    val portalColor by animateColorAsState(
        targetValue = when {
            isGrabbed && isConnected -> palette.teal
            isGrabbed -> palette.accent
            isConnected -> palette.teal
            else -> palette.lavender
        },
        animationSpec = tween(300),
        label = "PortalAccentColor",
    )

    val energyScale by animateFloatAsState(
        targetValue = 1f + (upwardDragProgress * 0.25f) + (if (isGrabbed) 0.12f else 0f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PortalScale",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 52.dp)
                .graphicsLayer {
                    scaleX = energyScale
                    scaleY = energyScale
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val baseCy = size.height * 0.82f

                for (i in 0..2) {
                    val phase = (waveProgress + i * 0.33f) % 1f
                    val arcWidth = size.width * (0.38f + phase * 0.56f)
                    val arcHeight = size.height * (0.35f + phase * 0.55f)
                    val alpha = (1f - phase) * (if (isGrabbed) 0.78f else 0.42f)

                    drawArc(
                        color = portalColor.copy(alpha = alpha),
                        startAngle = 195f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(cx - arcWidth / 2f, baseCy - arcHeight),
                        size = Size(arcWidth, arcHeight * 1.4f),
                        style = Stroke(
                            width = if (isGrabbed) 3.2.dp.toPx() else 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                }

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            portalColor.copy(alpha = if (isGrabbed) 0.95f else 0.55f),
                            Color.White.copy(alpha = if (isGrabbed) 0.9f else 0.4f),
                            portalColor.copy(alpha = if (isGrabbed) 0.95f else 0.55f),
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(size.width * 0.18f, baseCy),
                    end = Offset(size.width * 0.82f, baseCy),
                    strokeWidth = if (isGrabbed) 4.dp.toPx() else 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (isGrabbed) portalColor else OnSurfaceMuted,
        )
    }
}

@Composable
fun LiquidGlassPillButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Coral,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    liquid: Boolean = false,
    contentDescriptionText: String = text,
) {
    val alpha = if (enabled) 1f else 0.45f
    val fillTint = if (isPrimary) accentColor.copy(alpha = 0.28f) else GlassFillStrong

    GlassPanel(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .semantics {
                role = Role.Button
                contentDescription = contentDescriptionText
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(50),
        tint = fillTint,
        borderWidth = if (isPrimary) 1.4.dp else 1.dp,
        liquid = liquid,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else accentColor,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface,
            )
        }
    }
}

@Composable
fun LiquidOrbitalProgressGauge(
    fraction: Float,
    percentage: Int,
    speedMbps: Float,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val clampedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SmoothUploadFraction",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "OrbitalSpinner")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
        ),
        label = "OrbitRotation",
    )

    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke * 1.4f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                color = palette.lavender.copy(alpha = 0.32f),
                startAngle = orbitAngle,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke * 0.35f, stroke * 0.35f),
                size = Size(size.width - stroke * 0.7f, size.height - stroke * 0.7f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 22f), 0f),
                ),
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        palette.accent,
                        palette.lavender,
                        palette.teal,
                        palette.accent,
                    ),
                ),
                startAngle = -90f,
                sweepAngle = 360f * clampedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1f MB/s", speedMbps),
                style = MaterialTheme.typography.labelMedium,
                color = Teal,
            )
        }
    }
}
