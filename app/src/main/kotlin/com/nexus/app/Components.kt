@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.getValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Earthy tone pairs from the design sheet, picked by title so a game keeps its colour.
private val tones = listOf(
    0xFF4A4720 to 0xFF2B2912, 0xFF463A24 to 0xFF291F12, 0xFF4A2C26 to 0xFF2A1712,
    0xFF4A2230 to 0xFF2A1219, 0xFF244A33 to 0xFF132A1D, 0xFF243450 to 0xFF121D2E,
)

fun placeholderBrush(title: String): Brush {
    val (a, b) = tones[((title.hashCode().toLong() and 0x7fffffff) % tones.size).toInt()]
    return Brush.verticalGradient(listOf(Color(a), Color(b)))
}

/** Artwork when there is one; otherwise a tone block carrying the title, like the design sheet. */
@Composable
fun Cover(uri: String?, title: String, modifier: Modifier = Modifier, radius: Int = 8, showTitle: Boolean = true) {
    Box(Modifier.then(modifier).clip(RoundedCornerShape(radius.dp)).background(placeholderBrush(title))) {
        if (uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else if (showTitle) Text(
            title, Modifier.align(Alignment.BottomStart).padding(8.dp),
            style = MaterialTheme.typography.labelMedium, color = NexusColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
    }
}

val ScrimBrush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xD9000000))

/** Small grey label above a group, as in the design sheet. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
        content()
    }
}

/** Glass surface: used for the one emphasized block on a screen. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NexusColors.Glass)
            .border(1.dp, NexusColors.GlassBorder, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    GlassSurface(modifier.height(44.dp).pressClick(enabled, onClick), RoundedCornerShape(14.dp), Color(0xFF2A2A2E)) {
        Text(text, Modifier.align(Alignment.Center), style = MaterialTheme.typography.labelLarge, color = if (enabled) NexusColors.TextPrimary else NexusColors.Muted)
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)

/** Label on one side, strong figure on the other, hairline below. */
@Composable
fun LabeledValue(label: String, value: String, note: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (note != null) Hint(note)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        HorizontalDivider(color = NexusColors.Border)
    }
}

/** Text filters with a teal underline on the chosen one. */
@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(options.size) { idx ->
            val opt = options[idx]
            val on = opt == selected
            Column(
                Modifier.width(IntrinsicSize.Max).heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp)).clickable { onPick(opt) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label(opt), style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    color = if (on) NexusColors.TextPrimary else NexusColors.Muted, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(if (on) NexusColors.Accent else Color.Transparent))
            }
        }
    }
}

@Composable
fun NexusSwitch(checked: Boolean, onChange: (Boolean) -> Unit) = Switch(
    checked, onChange,
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = NexusColors.Accent, checkedBorderColor = NexusColors.Accent,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = NexusColors.SurfaceHigh, uncheckedBorderColor = NexusColors.GlassBorder,
    ),
)

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
        action?.invoke()
    }
}

/** Large title with an optional grey line under it. */
@Composable
fun PageTitle(text: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
        }
        trailing?.invoke()
    }
}

@Composable
fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    GlassSurface(Modifier.size(44.dp).pressClick(true, onClick), CircleShape, Color(0xFF2A2A2E)) {
        Icon(icon, description, Modifier.align(Alignment.Center).size(20.dp), tint = NexusColors.TextPrimary)
    }
}

/** Teal pill button with the glow of the design board. */
@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3CC8A8), Color(0xFF2A9D8F)))).pressClick(true, onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = NexusColors.OnAccent)
        Text(text, style = MaterialTheme.typography.labelLarge, color = NexusColors.OnAccent)
    }
}

/**
 * Segmented control with a teal indicator that slides between items on a spring.
 * Works in both directions: the Row mirrors by itself and `offset` is layout-direction aware.
 */
@Composable
fun SegmentedControl(count: Int, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, item: @Composable (index: Int, selected: Boolean) -> Unit) {
    val anim = LocalAnimations.current
    GlassSurface(modifier.height(44.dp), RoundedCornerShape(22.dp), Color(0xFF232325)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(3.dp)) {
            val w = maxWidth / count
            val x by animateDpAsState(w * selected.coerceAtLeast(0), if (anim) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium) else snap(), label = "seg")
            if (selected >= 0) Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(19.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF3CC8A8), Color(0xFF2FB39A)))))
            Row(Modifier.fillMaxSize()) {
                for (i in 0 until count) Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(selected = i == selected, role = Role.Tab, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center,
                ) { item(i, i == selected) }
            }
        }
    }
}

@Composable
fun SegmentLabel(text: String, selected: Boolean) = Text(
    text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
    color = if (selected) NexusColors.OnAccent else NexusColors.TextSecondary,
)

/** Small mark for the platform a game comes from. Simplified letter marks, not the official brand artwork. */
@Composable
fun PlatformBadge(type: com.nexus.core.database.SourceType, modifier: Modifier = Modifier) {
    val (mark, color) = when (type) {
        com.nexus.core.database.SourceType.STEAM -> "S" to Color(0xFF66C0F4)
        com.nexus.core.database.SourceType.EPIC -> "E" to Color(0xFFEDEDED)
        com.nexus.core.database.SourceType.GOG -> "G" to Color(0xFFB57BFF)
        com.nexus.core.database.SourceType.OTHER_LAUNCHER -> "L" to Color(0xFFFBBF24)
        com.nexus.core.database.SourceType.STANDALONE -> "PC" to Color(0xFF9AE6B4)
        com.nexus.core.database.SourceType.DRM_FREE -> "D" to Color(0xFF9AE6B4)
        com.nexus.core.database.SourceType.EMULATOR -> "EM" to Color(0xFFFF9F7A)
        com.nexus.core.database.SourceType.PHYSICAL -> "CD" to Color(0xFFA1A1A6)
        com.nexus.core.database.SourceType.CUSTOM -> "C" to Color(0xFFA1A1A6)
        com.nexus.core.database.SourceType.MANUAL -> "M" to NexusColors.Accent
    }
    Box(
        modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xB3141415)).border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
            .semantics { contentDescription = type.label() },
        contentAlignment = Alignment.Center,
    ) { Text(mark, color = color, fontSize = if (mark.length > 1) 8.sp else 11.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp) }
}

/** Moving light band used by skeleton blocks. Static when motion is reduced. */
fun Modifier.shimmer(): Modifier = composed {
    val anim = LocalAnimations.current
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x")
    this.drawWithContent {
        drawContent()
        if (anim) drawRect(Brush.linearGradient(listOf(Color.Transparent, Color(0x1FFFFFFF), Color.Transparent), Offset(size.width * x, 0f), Offset(size.width * (x + 0.6f), size.height * 0.4f)))
    }
}

@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: Int = 10) {
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(NexusColors.SurfaceHigh).shimmer())
}

@Composable
fun ListSkeleton(rows: Int = 4) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Skeleton(Modifier.size(width = 44.dp, height = 60.dp), 8)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skeleton(Modifier.fillMaxWidth(0.6f).height(14.dp), 6)
                    Skeleton(Modifier.fillMaxWidth(0.35f).height(10.dp), 5)
                }
            }
        }
    }
}

@Composable
fun GridSkeleton(items: Int = 6) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat((items + 2) / 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { Skeleton(Modifier.weight(1f).aspectRatio(2f / 3f), 16) }
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Skeleton(Modifier.fillMaxWidth().height(110.dp), 16)
        Skeleton(Modifier.fillMaxWidth(0.4f).height(14.dp), 6)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { Skeleton(Modifier.weight(1f).aspectRatio(2f / 3f), 8) } }
    }
}

@Composable
fun StatsSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Skeleton(Modifier.fillMaxWidth(0.3f).height(14.dp), 6)
                repeat(3) { Skeleton(Modifier.fillMaxWidth().height(12.dp), 6) }
            }
        }
    }
}

@Composable
fun DetailSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Skeleton(Modifier.fillMaxWidth().height(240.dp), 0)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Skeleton(Modifier.fillMaxWidth().height(44.dp), 22)
            Skeleton(Modifier.fillMaxWidth().height(100.dp), 16)
            Skeleton(Modifier.fillMaxWidth(0.5f).height(14.dp), 6)
        }
    }
}

data class GroupRow(
    val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null, val subtitle: String? = null,
    val value: String? = null, val trailing: (@Composable () -> Unit)? = null, val onClick: (() -> Unit)? = null,
)

/** iOS-style grouped rows: one rounded block, tinted icon tile, hairlines between, chevron when the row opens something. */
@Composable
fun GroupedRows(rows: List<GroupRow>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NexusColors.Surface)) {
        rows.forEachIndexed { i, r ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).then(if (r.onClick != null) Modifier.clickable(onClick = r.onClick) else Modifier).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (r.icon != null) Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(NexusColors.Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(r.icon, null, Modifier.size(18.dp), tint = NexusColors.Accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(r.label, style = MaterialTheme.typography.bodyLarge)
                    if (r.subtitle != null) Hint(r.subtitle)
                }
                if (r.value != null) Text(r.value, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
                r.trailing?.invoke()
                if (r.onClick != null && r.trailing == null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NexusColors.Muted)
            }
            if (i < rows.lastIndex) HorizontalDivider(Modifier.padding(start = if (r.icon != null) 58.dp else 16.dp), color = NexusColors.Border)
        }
    }
}

/** Generic placeholder while a screen loads. Screens with a known shape use their own skeleton. */
@Composable
fun Loading(modifier: Modifier = Modifier) = Box(modifier) { ListSkeleton() }

@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Back", "بازگشت"), Modifier.size(30.dp)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) { content(); Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun BarRow(label: String, value: Float, max: Float, valueText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(valueText, style = MonoStyle)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(NexusColors.SurfaceHigh)) {
            Box(Modifier.fillMaxWidth(if (max <= 0f) 0f else (value / max).coerceIn(0f, 1f)).height(4.dp).background(NexusColors.Accent))
        }
    }
}
