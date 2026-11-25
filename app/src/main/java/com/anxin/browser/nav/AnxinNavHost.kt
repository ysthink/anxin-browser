package com.anxin.browser.nav

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anxin.browser.AppGraph
import com.anxin.browser.MainActivity
import com.anxin.browser.PendingLink
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.MenuEntry
import com.anxin.browser.feature.browser.BrowserScreen
import com.anxin.browser.feature.bookmarks.BookmarksScreen
import com.anxin.browser.feature.downloads.DownloadsScreen
import com.anxin.browser.feature.extensions.ExtensionsScreen
import com.anxin.browser.feature.history.HistoryScreen
import com.anxin.browser.feature.home.HomeScreen
import com.anxin.browser.feature.incognito.IncognitoScreen
import com.anxin.browser.feature.media.MediaScreen
import com.anxin.browser.feature.newtab.NewTabScreen
import com.anxin.browser.feature.player.DEMO_STREAM_URL
import com.anxin.browser.feature.player.PlayerScreen
import com.anxin.browser.feature.settings.HomeSettingsScreen
import com.anxin.browser.feature.settings.PlaySettingsScreen
import com.anxin.browser.feature.settings.SearchSettingsScreen
import com.anxin.browser.feature.settings.SettingsScreen
import com.anxin.browser.feature.settings.WebSettingsScreen
import com.anxin.browser.feature.tabs.TabsScreen
import com.anxin.browser.ui.AddShortcutDialog
import com.anxin.browser.ui.MenuActions
import com.anxin.browser.ui.MenuSheet
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.shareUrl
import kotlinx.coroutines.launch
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val TABS = "tabs"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val PLAYER = "player?url={url}"
    const val INCOGNITO = "incognito"
    const val BROWSER = "browser"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
    const val MEDIA = "media"
    const val HOME_SETTINGS = "home_settings"
    const val SEARCH_SETTINGS = "search_settings"
    const val WEB_SETTINGS = "web_settings"
    const val PLAY_SETTINGS = "play_settings"
    const val EXTENSIONS = "extensions"

    fun player(url: String): String =
        "player?url=" + URLEncoder.encode(url, "UTF-8")
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun AnxinNavHost() {
    val context = LocalContext.current
    val graph = remember { AppGraph.get(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // ---- 状态收集 ----
    val tabs by graph.engine.tabs.collectAsStateWithLifecycle()
    val activeTabId by graph.engine.activeTabId.collectAsStateWithLifecycle()
    val downloads by graph.downloads.items.collectAsStateWithLifecycle()
    val mediaResources by graph.engine.mediaResources.collectAsStateWithLifecycle()
    val shortcuts by graph.shortcuts.shortcuts.collectAsStateWithLifecycle(initialValue = com.anxin.browser.data.ShortcutStore.defaults)
    val signedIn by graph.settings.signedIn.collectAsStateWithLifecycle(initialValue = false)
    val adBlock by graph.settings.adBlockEnabled.collectAsStateWithLifecycle(initialValue = true)
    val doNotTrack by graph.settings.doNotTrack.collectAsStateWithLifecycle(initialValue = true)
    val searchEngine by graph.settings.searchEngine.collectAsStateWithLifecycle(initialValue = Urls.DEFAULT_ENGINE)
    val bookmarks by graph.bookmarksRepo.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    val showEngineBar by graph.settings.showEngineBar.collectAsStateWithLifecycle(initialValue = true)
    val textZoom by graph.settings.textZoomPercent.collectAsStateWithLifecycle(initialValue = 100)
    val desktopMode by graph.settings.desktopMode.collectAsStateWithLifecycle(initialValue = false)
    val nightMode by graph.settings.nightMode.collectAsStateWithLifecycle(initialValue = false)
    val fullscreenMode by graph.settings.fullscreenMode.collectAsStateWithLifecycle(initialValue = false)
    val exitConfirm by graph.settings.exitConfirm.collectAsStateWithLifecycle(initialValue = true)
    val homepageMode by graph.settings.homepageMode.collectAsStateWithLifecycle(initialValue = "default")
    val customHomepage by graph.settings.customHomepage.collectAsStateWithLifecycle(initialValue = "")
    val wallpaperIndex by graph.settings.wallpaperIndex.collectAsStateWithLifecycle(initialValue = 0)
    val menuLayout by graph.settings.menuLayout.collectAsStateWithLifecycle(initialValue = MenuEntry.defaultLayout())
    val playDynamicVideo by graph.settings.playDynamicVideo.collectAsStateWithLifecycle(initialValue = false)
    val backgroundPlay by graph.settings.backgroundPlay.collectAsStateWithLifecycle(initialValue = true)
    val autoMiniWindow by graph.settings.autoMiniWindow.collectAsStateWithLifecycle(initialValue = false)
    val pipEnabled by graph.settings.pipEnabled.collectAsStateWithLifecycle(initialValue = true)
    val history by graph.bookmarksRepo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val frequent by graph.bookmarksRepo.frequentlyVisited().collectAsStateWithLifecycle(initialValue = emptyList())
    val searches by graph.history.searches.collectAsStateWithLifecycle(initialValue = emptyList())

    var showMenu by remember { mutableStateOf(false) }
    var menuEditing by remember { mutableStateOf(false) }
    var showAddShortcut by remember { mutableStateOf(false) }
    var showEnginePicker by remember { mutableStateOf(false) }
    var showFind by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var showExitConfirm by remember { mutableStateOf(false) }
    var menuUrl by remember { mutableStateOf("") }
    var readerJson by remember { mutableStateOf<String?>(null) }

    // 扩展中心
    val extensions by graph.extensions.observe().collectAsStateWithLifecycle()
    val extDisclaimerShown by graph.settings.extDisclaimerShown.collectAsStateWithLifecycle(initialValue = true)

    val extensionImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val resolver = context.contentResolver
                    val displayName = runCatching {
                        resolver.query(
                            uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null, null, null,
                        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    }.getOrNull() ?: "extension_${System.currentTimeMillis()}"
                    val dst = java.io.File(context.cacheDir, "ext_import.bin")
                    resolver.openInputStream(uri)?.use { input ->
                        dst.outputStream().use { output -> output.write(input.readBytes()) }
                    }
                    val imported = graph.extensions.importFromFile(dst.absolutePath, displayName)
                    dst.delete()
                    imported
                }.getOrNull()
                if (result != null) {
                    android.widget.Toast.makeText(context, "已安装扩展：${result.name}", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "导入失败：需要 zip(manifest.json+script.js) 或 .user.js",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun currentTab() = tabs.firstOrNull { it.id == activeTabId }

    fun openBrowser(url: String, incognito: Boolean = false) {
        graph.engine.createTab(url, incognito)
        navController.navigate(Routes.BROWSER) { launchSingleTop = true }
    }

    fun recordSearch(query: String) {
        graph.history.recordSearch(query)
    }

    fun homeTarget(): String =
        if (homepageMode == "custom" && customHomepage.isNotBlank()) customHomepage else ""

    // 引擎设置同步（避免首帧触发桌面 UA 刷新）
    LaunchedEffect(textZoom) { graph.engine.setTextZoom(textZoom) }
    LaunchedEffect(nightMode) { graph.engine.setForceDark(nightMode) }
    var prevDesktop by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(desktopMode) {
        if (prevDesktop != null && prevDesktop != desktopMode) {
            graph.engine.setDesktopMode(desktopMode)
        }
        prevDesktop = desktopMode
    }
    LaunchedEffect(adBlock) { graph.engine.setAdBlock(adBlock) }

    // ---- 会话恢复 + 会话保存 ----
    var sessionReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val saved = runCatching { graph.session.read() }.getOrNull()
        if (saved != null && graph.engine.tabs.value.isEmpty()) {
            graph.engine.restoreSession(saved.first, saved.second)
        }
        sessionReady = true
    }
    LaunchedEffect(tabs, activeTabId) {
        if (!sessionReady) return@LaunchedEffect
        runCatching { graph.session.save(tabs, activeTabId) }
    }

    // 桌面快捷方式 / 外部链接打开（前台冷热启动都覆盖）
    val pendingLink by com.anxin.browser.PendingLink.url.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        val url = pendingLink
        if (!url.isNullOrBlank() && url.startsWith("http")) {
            com.anxin.browser.PendingLink.url.value = null
            openBrowser(url)
        }
    }

    fun actionsFor(back: () -> Unit): NavActions = NavActions(
        back = back,
        forward = { if (!graph.engine.goForward()) toast("没有可前进的页面") },
        tabs = { navController.navigate(Routes.TABS) { launchSingleTop = true } },
        menu = {
            menuUrl = currentTab()?.url.orEmpty()
            showMenu = true
        },
        newTab = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
    )

    fun navigateTo(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    // ---- 菜单动作映射 ----
    fun handleMenuAction(id: String) {
        val tab = currentTab()
        when (id) {
            "bookmark" -> navigateTo(Routes.BOOKMARKS)
            "history" -> navigateTo(Routes.HISTORY)
            "downloads" -> navigateTo(Routes.DOWNLOADS)
            "videos", "sniff", "images" -> navigateTo(Routes.MEDIA)
            "night" -> scope.launch {
                graph.settings.setNightMode(!nightMode)
                toast(if (!nightMode) "夜间模式已开启" else "夜间模式已关闭")
            }
            "incognito" -> navigateTo(Routes.INCOGNITO)
            "addBookmark" -> {
                if (tab != null && !tab.isIncognito &&
                    (tab.url.startsWith("https://") || tab.url.startsWith("http://"))
                ) {
                    scope.launch {
                        graph.bookmarksRepo.addBookmark(tab.title, tab.url)
                        toast("已添加书签")
                    }
                } else toast("当前页面无法添加书签")
            }
            "refresh" -> graph.engine.reload()
            "exit" -> {
                if (exitConfirm) showExitConfirm = true
                else context.findActivity()?.finish()
            }
            "reading" -> {
                val tab = currentTab()
                if (tab != null && tab.url.startsWith("http")) {
                    toast("正在提取正文…")
                    graph.engine.extractReadablePage { json ->
                        val ok = json != null && runCatching {
                            org.json.JSONObject(json).optString("text").length > 160
                        }.getOrDefault(false)
                        scope.launch {
                            if (ok) readerJson = json else toast("正文识别失败，请刷新后重试")
                        }
                    }
                } else toast("当前页面不支持阅读模式")
            }
            "fullscreen" -> scope.launch {
                graph.settings.setFullscreenMode(!fullscreenMode)
                toast(if (!fullscreenMode) "全屏模式已开启" else "全屏模式已关闭")
            }
            "desktop" -> scope.launch {
                graph.settings.setDesktopMode(!desktopMode)
                toast(if (!desktopMode) "已切换为电脑版" else "已切换为手机版")
            }
            "extensions" -> navigateTo(Routes.EXTENSIONS)
            "translate" -> {
                if (tab != null && tab.url.startsWith("http")) {
                    openBrowser(
                        "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=" +
                            URLEncoder.encode(tab.url, "UTF-8"),
                    )
                } else toast("当前页面无法翻译")
            }
            "addToDesktop" -> {
                if (tab != null && tab.url.startsWith("http")) {
                    runCatching {
                        val sm = context.getSystemService(ShortcutManager::class.java) ?: return@runCatching
                        if (!sm.isRequestPinShortcutSupported) {
                            toast("系统不支持固定快捷方式")
                        } else {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                putExtra("open_url", tab.url)
                            }
                            val info = ShortcutInfo.Builder(context, "anxin_pin_${tab.url.hashCode()}")
                                .setShortLabel(tab.title.ifBlank { Urls.prettyHost(tab.url) })
                                .setLongLabel(tab.title.ifBlank { tab.url })
                                .setIcon(Icon.createWithResource(context, context.applicationInfo.icon))
                                .setIntent(intent)
                                .build()
                            sm.requestPinShortcut(info, null)
                            toast("已请求添加到桌面")
                        }
                    }.onFailure { toast("添加失败：${it.message}") }
                } else toast("当前页面无法添加")
            }
            "find" -> {
                showFind = true
                findText = ""
            }
            "saveOffline" -> {
                val path = graph.engine.saveOfflinePage()
                toast(if (path != null) "已保存离线网页" else "保存失败")
            }
            "textSize", "zoom" -> navigateTo(Routes.WEB_SETTINGS)
            "admark" -> scope.launch {
                graph.settings.setAdBlock(!adBlock)
                toast(if (!adBlock) "广告过滤已开启" else "广告过滤已关闭")
            }
            "print" -> {
                val activity = context.findActivity()
                val adapter = graph.engine.createPrintAdapter("安心浏览器文档")
                val printManager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && adapter != null) {
                    runCatching {
                        printManager.print(
                            "安心浏览器文档",
                            adapter as PrintDocumentAdapter,
                            null,
                        )
                    }
                } else toast("打印不可用")
            }
        }
    }

    // ---- 移动 / 显隐菜单布局 ----
    fun moveMenu(index: Int, up: Boolean) {
        val list = menuLayout.toMutableList()
        val target = if (up) index - 1 else index + 1
        if (target < 0 || target >= list.size) return
        val item = list.removeAt(index)
        list.add(target, item)
        scope.launch { graph.settings.setMenuLayout(list) }
    }

    fun toggleMenuVisible(id: String, visible: Boolean) {
        scope.launch {
            graph.settings.setMenuLayout(menuLayout.map { if (it.id == id) it.copy(visible = visible) else it })
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                shortcuts = shortcuts,
                actions = actionsFor(back = {
                    if (exitConfirm) showExitConfirm = true
                    else context.findActivity()?.finish()
                }).copy(
                    newTab = { navigateTo(Routes.BOOKMARKS) },
                    primaryLabel = "书签",
                    primaryIcon = Icons.Rounded.Bookmark,
                ),
                tabCount = tabs.size,
                wallpaperIndex = wallpaperIndex,
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        recordSearch(query)
                        openBrowser(Urls.normalize(query, searchEngine))
                    }
                },
                onOpenUrl = { url -> openBrowser(url) },
                onAddShortcut = { showAddShortcut = true },
                onRemoveShortcut = { url ->
                    scope.launch { graph.shortcuts.remove(url) }
                    toast("已移除快捷入口")
                },
                onOpenHomeSettings = { navigateTo(Routes.HOME_SETTINGS) },
                searchEngine = searchEngine,
                onPickEngine = { showEnginePicker = true },
            )
        }

        composable(Routes.SEARCH) {
            NewTabScreen(
                actions = actionsFor(back = { navController.popBackStack() }),
                tabCount = tabs.size,
                onClose = { navController.popBackStack() },
                onAddShortcut = { showAddShortcut = true },
                searchEngine = searchEngine,
                showEngineBar = showEngineBar,
                onPickEngine = { showEnginePicker = true },
                searches = searches,
                onClearSearches = {
                    graph.history.clearSearches()
                    toast("已清空搜索历史")
                },
                bookmarks = bookmarks,
                frequent = frequent,
                onSubmit = { query ->
                    if (query.isNotBlank()) {
                        recordSearch(query)
                        openBrowser(Urls.normalize(query, searchEngine))
                    }
                },
                onOpenUrl = { url -> openBrowser(url) },
                onOpenIncognito = { navigateTo(Routes.INCOGNITO) },
            )
        }

        composable(Routes.TABS) {
            TabsScreen(
                tabs = tabs,
                activeTabId = activeTabId,
                closedTabCount = graph.engine.closedTabCount,
                onBack = { navController.popBackStack() },
                onSelectTab = { id ->
                    graph.engine.selectTab(id)
                    navController.navigate(Routes.BROWSER) { launchSingleTop = true }
                },
                onCloseTab = { graph.engine.closeTab(it) },
                onNewTab = { navigateTo(Routes.SEARCH) },
                onNewIncognito = { navigateTo(Routes.INCOGNITO) },
                onClearAll = {
                    tabs.forEach { graph.engine.closeTab(it.id) }
                    toast("已清空全部标签")
                },
                onRestore = {
                    val restored = graph.engine.restoreLastClosed()
                    if (restored != null) {
                        navController.navigate(Routes.BROWSER) { launchSingleTop = true }
                    } else toast("没有可恢复的标签")
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                items = downloads,
                onBack = { navController.popBackStack() },
                onOpenFolder = {
                    runCatching {
                        context.startActivity(
                            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                onPause = { graph.downloads.pause(it) },
                onClearFinished = { graph.downloads.clearFinished() },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                signedIn = signedIn,
                adBlockEnabled = adBlock,
                doNotTrack = doNotTrack,
                exitConfirm = exitConfirm,
                searchEngine = searchEngine,
                textZoom = textZoom,
                showEngineBar = showEngineBar,
                onEngineBarChange = { value -> scope.launch { graph.settings.setEngineBar(value) } },
                onExitConfirmChange = { value -> scope.launch { graph.settings.setExitConfirm(value) } },
                onAdBlockChange = { value -> scope.launch { graph.settings.setAdBlock(value) } },
                onDoNotTrackChange = { value -> scope.launch { graph.settings.setDoNotTrack(value) } },
                onSignIn = { toast("账号体系待接入") },
                onOpenBookmarks = { navigateTo(Routes.BOOKMARKS) },
                onOpenHistory = { navigateTo(Routes.HISTORY) },
                onOpenHomeSettings = { navigateTo(Routes.HOME_SETTINGS) },
                onOpenSearchSettings = { navigateTo(Routes.SEARCH_SETTINGS) },
                onOpenWebSettings = { navigateTo(Routes.WEB_SETTINGS) },
                onOpenPlaySettings = { navigateTo(Routes.PLAY_SETTINGS) },
                onOpenSearchEngine = { showEnginePicker = true },
                onOpenIncognito = { navigateTo(Routes.INCOGNITO) },
                onOpenExtensions = { navigateTo(Routes.EXTENSIONS) },
                onOpenDownloads = { navigateTo(Routes.DOWNLOADS) },
                onOpenFeedback = { toast("反馈渠道：app@anxin.dev（占位）") },
                onOpenAbout = { toast("安心浏览器 v0.2.0 · WebView 阶段") },
                onResetDefaults = {
                    scope.launch {
                        graph.settings.resetAll()
                        toast("已恢复默认设置")
                    }
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                androidx.navigation.navArgument("url") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            PlayerScreen(
                title = entry.titleArg() ?: "视频播放",
                videoUrl = entry.urlArg() ?: DEMO_STREAM_URL,
                onBack = { navController.popBackStack() },
                onDownload = {
                    val url = entry.urlArg() ?: DEMO_STREAM_URL
                    graph.downloads.enqueue(
                        url = url,
                        mimeType = "application/vnd.apple.mpegurl",
                        suggestedName = "视频_${System.currentTimeMillis()}.m3u8",
                    )
                    toast("已加入下载队列")
                },
                actions = actionsFor(back = { navController.popBackStack() }),
                tabCount = tabs.size,
            )
        }

        composable(Routes.INCOGNITO) {
            IncognitoScreen(
                onBack = { navController.popBackStack() },
                onStart = { openBrowser("https://www.bing.com", incognito = true) },
                actions = actionsFor(back = { navController.popBackStack() }),
                tabCount = tabs.size,
            )
        }

        composable(Routes.BROWSER) {
            BrowserScreen(
                engine = graph.engine,
                tabs = tabs,
                activeTabId = activeTabId,
                searchEngine = searchEngine,
                hideTopBars = fullscreenMode,
                nightMode = nightMode,
                onExit = {
                    if (!navController.popBackStack()) navController.navigate(Routes.HOME)
                },
                onSearchSubmitted = { query -> recordSearch(query) },
                actions = actionsFor(
                    back = {
                        if (!graph.engine.goBack() && !navController.popBackStack()) {
                            navController.navigate(Routes.HOME)
                        }
                    }
                ).copy(
                    newTab = {
                        val target = homeTarget()
                        if (target.isNotBlank()) openBrowser(target)
                        else {
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        }
                    },
                    primaryLabel = "主页",
                    primaryIcon = Icons.Rounded.Home,
                ),
                tabCount = tabs.size,
            )
        }

        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                bookmarks = bookmarks,
                onBack = { navController.popBackStack() },
                onOpen = { url -> openBrowser(url) },
                onRemove = { url ->
                    scope.launch {
                        graph.bookmarksRepo.removeBookmark(url)
                        toast("已删除书签")
                    }
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                entries = history,
                onBack = { navController.popBackStack() },
                onOpen = { url -> openBrowser(url) },
                onRemove = { url -> scope.launch { graph.bookmarksRepo.removeHistory(url) } },
                onClear = {
                    scope.launch {
                        graph.bookmarksRepo.clearHistory()
                        toast("已清空历史")
                    }
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.MEDIA) {
            MediaScreen(
                resources = mediaResources,
                onBack = { navController.popBackStack() },
                onPlay = { res -> navigateTo(Routes.player(res.url)) },
                onClear = {
                    graph.engine.clearMediaResources()
                    toast("已清空")
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.EXTENSIONS) {
            ExtensionsScreen(
                extensions = extensions,
                showDisclaimer = !extDisclaimerShown,
                onBack = { navController.popBackStack() },
                onImport = { extensionImportLauncher.launch("*/*") },
                onToggle = { id, enabled -> scope.launch { graph.extensions.setEnabled(id, enabled) } },
                onPin = { id, pinned -> scope.launch { graph.extensions.setPinned(id, pinned) } },
                onDelete = { id -> scope.launch { graph.extensions.remove(id) } },
                onDisclaimerAccept = { scope.launch { graph.settings.setExtDisclaimerShown() } },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.HOME_SETTINGS) {
            HomeSettingsScreen(
                mode = homepageMode,
                customHomepage = customHomepage,
                wallpaperIndex = wallpaperIndex,
                onModeChange = { mode -> scope.launch { graph.settings.setHomepageMode(mode) } },
                onCustomHomepageChange = { url -> scope.launch { graph.settings.setCustomHomepage(url) } },
                onWallpaperChange = { index -> scope.launch { graph.settings.setWallpaperIndex(index) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SEARCH_SETTINGS) {
            SearchSettingsScreen(
                searchEngine = searchEngine,
                showEngineBar = showEngineBar,
                onOpenSearchEngine = { showEnginePicker = true },
                onEngineBarChange = { value -> scope.launch { graph.settings.setEngineBar(value) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.WEB_SETTINGS) {
            WebSettingsScreen(
                textZoom = textZoom,
                desktopMode = desktopMode,
                onTextZoomChange = { value -> scope.launch { graph.settings.setTextZoom(value) } },
                onDesktopModeChange = { value ->
                    scope.launch {
                        graph.settings.setDesktopMode(value)
                        toast(if (value) "已切换为电脑版" else "已切换为手机版")
                    }
                },
                onResetWebSettings = {
                    scope.launch {
                        graph.settings.setTextZoom(100)
                        graph.settings.setDesktopMode(false)
                        toast("已恢复默认显示设置")
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PLAY_SETTINGS) {
            PlaySettingsScreen(
                playDynamicVideo = playDynamicVideo,
                backgroundPlay = backgroundPlay,
                autoMiniWindow = autoMiniWindow,
                pipEnabled = pipEnabled,
                onPlayDynamicVideoChange = { value -> scope.launch { graph.settings.setPlayDynamicVideo(value) } },
                onBackgroundPlayChange = { value -> scope.launch { graph.settings.setBackgroundPlay(value) } },
                onAutoMiniWindowChange = { value -> scope.launch { graph.settings.setAutoMiniWindow(value) } },
                onPipChange = { value -> scope.launch { graph.settings.setPip(value) } },
                onBack = { navController.popBackStack() },
            )
        }
    }

    // ---- 全局弹层 ----

    readerJson?.let { json ->
        Box(Modifier.fillMaxSize()) {
            com.anxin.browser.feature.reader.ReaderScreen(rawJson = json) {
                readerJson = null
            }
        }
    }

    if (showAddShortcut) {
        AddShortcutDialog(
            bookmarks = bookmarks,
            history = history,
            onDismiss = { showAddShortcut = false },
            onSelectBookmark = { title, url ->
                scope.launch { graph.shortcuts.add(title, url) }
                toast("已添加快捷入口")
            },
            onSelectHistory = { title, url ->
                scope.launch { graph.shortcuts.add(title.ifBlank { Urls.prettyHost(url) }, url) }
                toast("已添加快捷入口")
            },
            onNewBookmark = { label, url ->
                scope.launch { graph.shortcuts.add(label, url) }
                toast("已添加快捷入口")
            },
        )
    }

    if (showMenu) {
        MenuSheet(
            layout = menuLayout,
            editing = menuEditing,
            activeIds = buildSet {
                if (nightMode) add("night")
                if (fullscreenMode) add("fullscreen")
                if (desktopMode) add("desktop")
            },
            onDismiss = {
                menuEditing = false
                showMenu = false
            },
            onAction = { id -> handleMenuAction(id) },
            onEditToggle = {
                menuEditing = !menuEditing
                if (menuEditing) toast("上下移动排序，开关控制显隐")
            },
            onMove = { index, up -> moveMenu(index, up) },
            onToggleVisible = { id, visible -> toggleMenuVisible(id, visible) },
            onShare = {
                shareUrl(context, menuUrl)
                showMenu = false
            },
            onSettings = {
                showMenu = false
                navigateTo(Routes.SETTINGS)
            },
        )
    }

    if (showEnginePicker) {
        EnginePickerDialog(
            current = searchEngine,
            onDismiss = { showEnginePicker = false },
            onPick = { name ->
                scope.launch {
                    graph.settings.setSearchEngine(name)
                    toast("已切换为 $name")
                }
                showEnginePicker = false
            },
        )
    }

    if (showFind) {
        AlertDialog(
            onDismissRequest = { showFind = false },
            title = { Text("页面查找") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = findText,
                    onValueChange = { findText = it },
                    singleLine = true,
                    placeholder = { Text("输入关键词高亮显示") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = graph.engine.findInPage(findText)
                    toast(if (count > 0) "已高亮「${findText}」" else "未找到匹配内容")
                }) { Text("查找") }
            },
            dismissButton = {
                TextButton(onClick = { showFind = false }) { Text("关闭") }
            },
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            containerColor = Color(0xFFFDFEFF),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFFF5A5F),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("退出安心浏览器？", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Text(
                    "退出后将关闭全部标签页，下次打开为全新会话。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7B98),
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showExitConfirm = false
                        context.findActivity()?.finish()
                    },
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5A5F),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.heightIn(min = 42.dp),
                ) {
                    Text("退出", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirm = false },
                    modifier = Modifier.heightIn(min = 42.dp),
                ) {
                    Text("继续浏览", color = Color(0xFF3D6BFF), fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

private fun NavBackStackEntry.titleArg(): String? =
    arguments?.getString("url")?.let { Urls.prettyHost(it).ifBlank { "视频播放" } }

private fun NavBackStackEntry.urlArg(): String? =
    arguments?.getString("url")?.takeIf { it.isNotBlank() }

@Composable
private fun EnginePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val ink = Color(0xFF0D1730)
    val inkTertiary = Color(0xFF9AA8BE)
    val brand = Color(0xFF3D6BFF)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFDFEFF),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text("搜索引擎", style = MaterialTheme.typography.titleLarge)
                Text("选择默认搜索入口", style = MaterialTheme.typography.labelSmall, color = inkTertiary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Urls.searchEngines.forEach { name ->
                    val selected = name == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Color(0xFFEAF0FF) else Color(0xFFF3F6FC))
                            .clickable { onPick(name) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "当前引擎",
                                tint = brand,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Text(
                    "自定义引擎将在内核阶段接入",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkTertiary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
