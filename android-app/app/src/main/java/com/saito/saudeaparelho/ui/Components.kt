package com.saito.saudeaparelho.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saito.saudeaparelho.model.Finding
import com.saito.saudeaparelho.model.Severity

@Composable
fun ScoreGauge(score: Int, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(score / 100f, tween(900), label = "score")
    val color = scoreColor(score)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(180.dp)) {
            val stroke = 18.dp.toPx()
            val d = size.minDimension - stroke
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(track, 135f, 270f, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * anim, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                when {
                    score >= 80 -> "Saudável"
                    score >= 55 -> "Precisa de atenção"
                    else -> "Em estado crítico"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

/** Barra de uso: [used] de 0 a 1. */
@Composable
fun UsageBar(label: String, used: Float, text: String) {
    val color = when {
        used >= 0.9f -> Bad
        used >= 0.75f -> Warn
        else -> Good
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { used.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {}
        )
    }
}

@Composable
fun SeverityIcon(s: Severity, modifier: Modifier = Modifier) {
    val icon = when (s) {
        Severity.OK -> Icons.Filled.CheckCircle
        Severity.INFO -> Icons.Filled.Info
        Severity.WARNING -> Icons.Filled.Warning
        Severity.CRITICAL -> Icons.Filled.Error
    }
    Icon(icon, contentDescription = s.label, tint = severityColor(s), modifier = modifier)
}

@Composable
fun FindingCard(f: Finding, onFix: (Finding) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(Modifier.padding(14.dp)) {
            SeverityIcon(f.severity, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${f.category.label} · ${f.severity.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor(f.severity)
                )
                Text(f.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(f.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                f.fix?.let { fix ->
                    FilledTonalButton(onClick = { onFix(f) }, modifier = Modifier.padding(top = 4.dp)) {
                        Text(fix.label)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(title: String, subtitle: String, actionLabel: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
