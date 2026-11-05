@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Games without artwork get a stable colour from their title instead of a blank box. */
fun placeholderBrush(title: String): Brush {
    val hue = ((title.hashCode().toLong() and 0x7fffffff) % 360).toFloat()
    val a = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.42f)))
    val b = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 40f) % 360f, 0.5f, 0.22f)))
    return Brush.linearGradient(listOf(a, b))
}

@Composable
fun Cover(uri: String?, title: String, modifier: Modifier = Modifier, radius: Int = 12) {
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(placeholderBrush(title)), contentAlignment = Alignment.Center) {
        Text(title.trim().take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(alpha = 0.75f))
        if (uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}

val ScrimBrush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xE6000000))

/** Grouped content without a box: a heading, then rows. Use Panel only for the one thing that needs emphasis. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = NexusColors.TextSecondary)
        content()
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NexusColors.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)

@Composable
fun LabeledValue(label: String, value: String, note: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (note != null) Hint(note)
        }
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { idx ->
            val opt = options[idx]
            FilterChip(
                selected = opt == selected, onClick = { onPick(opt) },
                label = { Text(label(opt), style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NexusColors.Accent.copy(alpha = 0.18f), selectedLabelColor = NexusColors.Accent,
                ),
            )
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
        action?.invoke()
    }
}

/** Large page title used by the four tab screens. */
@Composable
fun PageTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = NexusColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "بازگشت")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusColors.Canvas),
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
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(NexusColors.SurfaceHigh)) {
            Box(Modifier.fillMaxWidth(if (max <= 0f) 0f else (value / max).coerceIn(0f, 1f)).height(6.dp).background(NexusColors.Accent))
        }
    }
}
