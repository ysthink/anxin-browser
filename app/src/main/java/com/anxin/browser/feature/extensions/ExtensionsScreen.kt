package com.anxin.browser.feature.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.data.extension.UserExtension
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPrimaryButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

/**
 * 扩展中心：类扩展（脚本包）列表 + 启用/停用/固定/删除 + 首次免责声明。
 * 内核侧 Chrome 扩展接入后，本页切换数据源即可（ExtensionKernelBridge）。
 */
@Composable
fun ExtensionsScreen(
    extensions: List<UserExtension>,
    showDisclaimer: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onPin: (id: String, pinned: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    onDisclaimerAccept: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    var pendingDelete by remember { mutableStateOf<UserExtension?>(null) }

    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Rounded.ArrowBack, "返回", size = 36.dp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                PageTitle(title = "扩展中心", count = extensions.size)
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    Icons.Rounded.PushPin,
                    "导入扩展",
                    size = 36.dp,
                    onClick = onImport,
                )
            }

            Text(
                "支持 .user.js 脚本或 zip 包（manifest.json + script.js），本地安装、仅本机生效",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            if (extensions.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Extension,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("还没有安装扩展", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Text(
                            "点击右上角按钮导入本地扩展包",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkTertiary,
                        )
                        Spacer(Modifier.height(20.dp))
                        GlassPrimaryButton(
                            text = "导入扩展",
                            modifier = Modifier.padding(horizontal = 48.dp),
                            onClick = onImport,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                ) {
                    items(extensions, key = { it.id }) { ext ->
                        ExtensionCard(
                            ext = ext,
                            onToggle = { onToggle(ext.id, it) },
                            onPin = { onPin(ext.id, it) },
                            onDelete = { pendingDelete = ext },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        AnxinActionBar(
            actions = actions,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFFFDFEFF),
            shape = RoundedCornerShape(24.dp),
            title = { Text("使用前须知", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "扩展脚本将在你浏览的网页中运行，可能读取或修改页面内容。\n\n" +
                        "请仅安装可信来源的扩展包；企业场景建议只导入内部签名的脚本。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7B98),
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = onDisclaimerAccept,
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AnxinPalette.brand,
                        contentColor = Color.White,
                    ),
                ) { Text("我已知晓", fontWeight = FontWeight.SemiBold) }
            },
        )
    }

    pendingDelete?.let { ext ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除扩展") },
            text = { Text("确定删除「${ext.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(ext.id)
                    pendingDelete = null
                }) { Text("删除", color = Color(0xFFFF5A5F)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ExtensionCard(
    ext: UserExtension,
    onToggle: (Boolean) -> Unit,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                radius = 20.dp,
                alpha = if (ext.enabled) 0.6f else 0.4f,
                shadowElevation = 10.dp,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 图标：首字母 + 品牌渐变
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF9FC0FF),
                            if (ext.enabled) palette.brand else Color(0xFF9AA8BE),
                            palette.violet,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ext.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ext.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (ext.enabled) palette.ink else palette.inkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "v${ext.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
                if (ext.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "已固定",
                        tint = palette.brand,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            if (ext.description.isNotBlank()) {
                Text(
                    ext.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "匹配：${ext.matches.joinToString().take(46)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.PushPin,
            contentDescription = if (ext.pinned) "取消固定" else "固定",
            tint = if (ext.pinned) palette.brand else palette.inkTertiary.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .clickable { onPin(!ext.pinned) }
                .padding(2.dp),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.Delete,
            contentDescription = "删除扩展",
            tint = palette.inkTertiary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete)
                .padding(2.dp),
        )
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = ext.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x33000000),
            ),
        )
    }
}
