package com.anxin.browser.feature.browser

import android.content.Intent
import androidx.activity.compose.BackHandler
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anxin.browser.core.engine.BrowserEngine
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.ThinProgressBar
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    engine: BrowserEngine,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    searchEngine: String,
    hideTopBars: Boolean = false,
    nightMode: Boolean = false,
    onExit: () -> Unit,
    onSearchSubmitted: (String) -> Unit = {},
    actions: NavActions,
    tabCount: Int,
) {
    val palette = AnxinPalette
    val context = LocalContext.current
    val active = tabs.firstOrNull { it.id == activeTabId }
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    BackHandler {
        if (!engine.goBack()) onExit()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEFF4FF), Color(0xFFF8FAFF)))),
    ) {
        if (!hideTopBars) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AddressChip(
                        modifier = Modifier.weight(1f),
                        title = active?.title ?: "新标签页",
                        url = active?.url ?: "about:blank",
                        isIncognito = active?.isIncognito == true,
                        editing = editing,
                        input = input,
                        onInputChange = { input = it },
                        onClick = {
                            input = active?.url ?: ""
                            editing = true
                        },
                        onSubmit = {
                            if (input.isNotBlank()) {
                                onSearchSubmitted(input)
                                engine.loadUrl(Urls.normalize(input, searchEngine))
                            }
                            editing = false
                        },
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (active?.isLoading == true) {
                    ThinProgressBar(
                        progress = (active.progress / 100f).coerceIn(0.04f, 1f),
                        modifier = Modifier.padding(horizontal = 18.dp),
                        height = 2.dp,
                    )
                }
            }
        }

        AndroidView(
            factory = { ctx -> FrameLayout(ctx) },
            update = { container ->
                val view = engine.activeContentView() as? View
                if (view != null && view.parent !== container) {
                    (view.parent as? ViewGroup)?.removeView(view)
                    container.removeAllViews()
                    container.addView(view)
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        AnxinActionBar(
            actions = actions,
            tabCount = tabCount,
        )
    }
}

@Composable
private fun AddressChip(
    modifier: Modifier = Modifier,
    title: String,
    url: String,
    isIncognito: Boolean,
    editing: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = modifier
            .height(44.dp)
            .glassPanel(radius = 22.dp, alpha = 0.88f, borderAlpha = 0.9f, shadowElevation = 10.dp)
            .clickable(enabled = !editing, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = palette.inkTertiary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        if (editing) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.ink, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                cursorBrush = SolidColor(palette.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "收起",
                tint = palette.inkTertiary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onSubmit() },
            )
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    title.ifBlank { "新标签页" },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Urls.prettyHost(url),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TopAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .glassPanel(radius = 20.dp, alpha = 0.85f, shadowElevation = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = AnxinPalette.ink, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = AnxinPalette.brand, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AnxinPalette.inkSecondary)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AnxinPalette.inkSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = AnxinPalette.ink)
    }
}

@Suppress("unused")
private fun WebView.detachFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}
