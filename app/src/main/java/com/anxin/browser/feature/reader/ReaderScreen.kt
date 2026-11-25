package com.anxin.browser.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.glassPanel

/**
 * 阅读模式：从内核抽取的正文渲染成干净的排版视图。
 * [rawJson] 为 WebView 返回的 JSON 字符串 {title, text}。
 */
@Composable
fun ReaderScreen(
    rawJson: String,
    onDismiss: () -> Unit,
) {
    val palette = AnxinPalette
    val parsed = remember(rawJson) { parse(rawJson) }
    var fontScale by remember { mutableIntStateOf(100) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBF8)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .glassPanel(radius = 19.dp, alpha = 0.5f, shadowElevation = 6.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "退出阅读模式",
                    tint = palette.ink,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    parsed.title.ifBlank { "阅读模式" },
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "正文约 ${parsed.text.length} 字 · 无干扰排版",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
            }
            FontButton(Icons.Rounded.TextDecrease, "缩小字号") { fontScale = (fontScale - 10).coerceAtLeast(70) }
            Spacer(Modifier.width(8.dp))
            FontButton(Icons.Rounded.TextIncrease, "放大字号") { fontScale = (fontScale + 10).coerceAtMost(150) }
        }

        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .height(0.7.dp)
                .fillMaxWidth()
                .background(palette.ink.copy(alpha = 0.08f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            Text(
                parsed.title.ifBlank { "正文" },
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                lineHeight = 34.sp,
            )
            Spacer(Modifier.height(16.dp))
            parsed.paragraphs.forEachIndexed { index, paragraph ->
                Text(
                    paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.ink.copy(alpha = 0.88f),
                    fontSize = (15 * fontScale / 100f).sp,
                    lineHeight = ((15 * fontScale / 100f) * 1.75f).sp,
                )
                Spacer(Modifier.height(14.dp))
                if (index > 400) return@forEachIndexed
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FontButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .glassPanel(radius = 19.dp, alpha = 0.5f, shadowElevation = 6.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = AnxinPalette.brand, modifier = Modifier.size(19.dp))
    }
}

private data class ReaderContent(val title: String, val text: String, val paragraphs: List<String>)

private fun parse(rawJson: String): ReaderContent = runCatching {
    val obj = org.json.JSONObject(rawJson)
    val title = obj.optString("title").orEmpty()
    val text = obj.optString("text").orEmpty()
    val paragraphs = text.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("未能识别正文结构。") }
    ReaderContent(title, text, paragraphs)
}.getOrDefault(ReaderContent("阅读模式", "", listOf("内容抽取失败，请刷新页面后重试。")))
