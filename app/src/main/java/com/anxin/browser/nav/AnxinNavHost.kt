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
import com.anxin.browser.design.AnxinDialogContainer
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.anxinSurfaceMuted
import com.anxin.browser.feature.browser.BrowserScreen
import com.anxin.browser.feature.downloads.DownloadsScreen
import com.anxin.browser.feature.extensions.ExtensionsScreen
import com.anxin.browser.feature.saved.SavedScreen
import com.anxin.browser.feature.home.HomeScreen
import com.anxin.browser.feature.incognito.IncognitoScreen
import com.anxin.browser.feature.media.MediaScreen
import com.anxin.browser.feature.newtab.NewTabScreen
import com.anxin.browser.feature.player.DEMO_STREAM_URL
import com.anxin.browser.feature.player.PlayerScreen
import com.anxin.browser.feature.settings.AboutScreen
import com.anxin.browser.feature.settings.LicensesScreen
import com.anxin.browser.feature.settings.PrivacyScreen
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

    /** 扩展中心指定页签：0=我的扩展，1=扩展探索。 */
    fun extensions(tab: Int): String = "extensions?tab=$tab"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"

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
    val bookmarkItems by graph.bookmarksRepo.bookmarkItems.collectAsStateWithLifecycle(initialValue = emptyList())
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
    val recentSites by graph.bookmarksRepo.recentlyVisited().collectAsStateWithLifecycle(initialValue = emptyList())
    val showShortcutsOnHome by graph.settings.showShortcutsOnHome.collectAsStateWithLifecycle(initialValue = true)
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
    val showExtInMenu by graph.settings.showExtInMenu.collectAsStateWithLifecycle(initialValue = false)
    // 菜单里点「扩展程序」先弹快捷面板，再决定去管理页还是探索页。
    var showExtSheet by remember { mutableStateOf(false) }

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

    // 网页里点下载时给一句即时反馈：否则直链文件会让页面停在空白页，用户不知道发生了什么。
    LaunchedEffect(Unit) {
        graph.downloads.events.collect { name -> toast("开始下载：$name") }
    }

    // 重复下载被拦下等需要解释的情况。
    LaunchedEffect(Unit) {
        graph.downloads.info.collect { text -> toast(text) }
    }

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
            // 下载直链不是"可浏览的页面"：恢复它必然把文件再下一次。
            val downloadUrls = graph.downloads.items.value.mapTo(HashSet()) { it.url }
            val restorable = saved.first.filterNot { it.url in downloadUrls }
            graph.engine.restoreSession(restorable, saved.second)
        }
        sessionReady = true
    }
    LaunchedEffect(tabs, activeTabId) {
        if (!sessionReady) return@LaunchedEffect
        // 同理，保存时也把直链标签剔掉，避免把"只会触发下载的页面"写进下一次会话。
        val downloadUrls = graph.downloads.items.value.mapTo(HashSet()) { it.url }
        val restorable = tabs.filterNot { it.url in downloadUrls }
        runCatching { graph.session.save(restorable, activeTabId) }
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

    // 点下载通知进来：直达下载页。
    val openDownloads by PendingLink.openDownloads.collectAsStateWithLifecycle()
    LaunchedEffect(openDownloads) {
        if (openDownloads) {
            PendingLink.openDownloads.value = false
            navigateTo(Routes.DOWNLOADS)
        }
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
            "extensions" -> scope.launch {
                // 菜单弹层刚被移出组合，同一帧再弹新 ModalBottomSheet 在 MIUI 上
                // 会被窗口焦点竞态直接 dismiss——错开一帧窗口拆卸时间。
                kotlinx.coroutines.delay(200)
                showExtSheet = true
            }
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

    // 系统返回键统一拦截。不能用 WindowInsets 判断键盘（MIUI + 微信键盘实测：
    // insets 的 ime 可见性时滞/误报，键盘弹着也可能报 false），改用视图焦点判断：
    // 输入框持有焦点 → 本次返回只收键盘并清焦点（残留焦点正是“收起后再点不弹
    // 键盘”的根因）；无焦点 → 正常返回；到根部 → 退出确认，而不是直接退到桌面。
    val rootView = androidx.compose.ui.platform.LocalView.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    androidx.activity.compose.BackHandler(enabled = true) {
        val currentRoute = navController.currentDestination?.route
        if (rootView.findFocus() != null) {
            android.util.Log.d("AnxinBack", "back@route=$currentRoute -> hide ime (focused)")
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            val popped = navController.popBackStack()
            android.util.Log.d("AnxinBack", "back@route=$currentRoute popped=$popped")
            if (!popped) {
                if (exitConfirm) showExitConfirm = true
                else context.findActivity()?.finish()
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                shortcuts = shortcuts,
                recentSites = recentSites,
                showShortcuts = showShortcutsOnHome,
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
                closed = graph.engine.closedTabs,
                onBack = { navController.popBackStack() },
                onSelectTab = { id ->
                    graph.engine.selectTab(id)
                    navController.navigate(Routes.BROWSER) { launchSingleTop = true }
                },
                onCloseTab = { graph.engine.closeTab(it) },
                onNewTab = { navigateTo(Routes.SEARCH) },
                onNewIncognito = { navigateTo(Routes.INCOGNITO) },
                onClearTabs = {
                    tabs.filterNot { it.isIncognito }.forEach { graph.engine.closeTab(it.id) }
                    toast("已关闭全部窗口")
                },
                onClearIncognito = {
                    graph.engine.closeAllIncognito()
                    toast("已关闭全部无痕窗口")
                },
                onClearClosed = {
                    graph.engine.clearClosedTabs()
                    toast("已清空最近关闭")
                },
                onRestoreClosed = { entry ->
                    val restored = graph.engine.restoreClosed(entry)
                    if (restored != null) {
                        navController.navigate(Routes.BROWSER) { launchSingleTop = true }
                    } else toast("该窗口已不存在")
                },
                onRestoreLast = {
                    val restored = graph.engine.restoreLastClosed()
                    if (restored != null) {
                        navController.navigate(Routes.BROWSER) { launchSingleTop = true }
                    } else toast("没有可恢复的窗口")
                },
            )
        }

        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                items = downloads,
                media = mediaResources,
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
                onResume = { graph.downloads.resume(it) },
                onDelete = { id ->
                    graph.downloads.remove(id)
                    toast("已删除该条下载")
                },
                onClearFinished = { graph.downloads.clearFinished() },
                onPlayMedia = { res -> navigateTo(Routes.player(res.url)) },
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
                nightMode = nightMode,
                onEngineBarChange = { value -> scope.launch { graph.settings.setEngineBar(value) } },
                onNightModeChange = { value -> scope.launch { graph.settings.setNightMode(value) } },
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
                onOpenAbout = { navigateTo(Routes.ABOUT) },
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
                    // 扩展名要跟着源走：之前一律按 HLS 存成 .m3u8，
                    // 直接播 mp4 时下回来的文件扩展名和内容对不上，系统播放器打不开。
                    val ext = url.substringAfterLast('/').substringBefore('?')
                        .substringAfterLast('.', "").lowercase()
                    if (ext == "m3u8" || ext == "mpd") {
                        toast("暂不支持 HLS/DASH 分片下载")
                    } else {
                        val safeExt = ext.takeIf {
                            it in setOf("mp4", "m4v", "mkv", "webm", "mov", "flv", "ts")
                        } ?: "mp4"
                        val mime = when (safeExt) {
                            "webm" -> "video/webm"
                            "mkv" -> "video/x-matroska"
                            "mov" -> "video/quicktime"
                            "flv" -> "video/x-flv"
                            "ts" -> "video/mp2t"
                            else -> "video/mp4"
                        }
                        graph.downloads.enqueue(
                            url = url,
                            mimeType = mime,
                            suggestedName = "视频_${System.currentTimeMillis()}.$safeExt",
                        )
                        toast("已加入下载队列")
                    }
                },
                actions = actionsFor(back = { navController.popBackStack() }),
                tabCount = tabs.size,
            )
        }

        composable(Routes.INCOGNITO) {
            IncognitoScreen(
                onBack = { navController.popBackStack() },
                // 直接搜索/输入网址：以无痕标签打开，不再有"开始无痕浏览"按钮层。
                onSearch = { query ->
                    openBrowser(Urls.normalize(query, searchEngine), incognito = true)
                },
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
            SavedScreen(
                initialPage = 0,
                bookmarks = bookmarkItems,
                history = history,
                onClose = { navController.popBackStack() },
                onOpen = { url -> openBrowser(url) },
                onRemoveBookmark = { url ->
                    scope.launch {
                        graph.bookmarksRepo.removeBookmark(url)
                        toast("已删除书签")
                    }
                },
                onRemoveHistory = { url -> scope.launch { graph.bookmarksRepo.removeHistory(url) } },
                onClearHistory = {
                    scope.launch {
                        graph.bookmarksRepo.clearHistory()
                        toast("已清空历史")
                    }
                },
                // 保存页手动添加：标题可留空（自动取网址），分组可选或新建。
                onAddBookmark = { title, url, group ->
                    scope.launch {
                        graph.bookmarksRepo.addBookmark(title, url, group)
                        toast("已添加书签")
                    }
                },
                canAddBookmark = true,
            )
        }

        composable(Routes.HISTORY) {
            SavedScreen(
                initialPage = 1,
                bookmarks = bookmarkItems,
                history = history,
                onClose = { navController.popBackStack() },
                onOpen = { url -> openBrowser(url) },
                onRemoveBookmark = { url ->
                    scope.launch {
                        graph.bookmarksRepo.removeBookmark(url)
                        toast("已删除书签")
                    }
                },
                onRemoveHistory = { url -> scope.launch { graph.bookmarksRepo.removeHistory(url) } },
                onClearHistory = {
                    scope.launch {
                        graph.bookmarksRepo.clearHistory()
                        toast("已清空历史")
                    }
                },
                onAddBookmark = { title, url, group ->
                    scope.launch {
                        graph.bookmarksRepo.addBookmark(title, url, group)
                        toast("已添加书签")
                    }
                },
                canAddBookmark = true,
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

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onPrivacy = { navigateTo(Routes.PRIVACY) },
                onLicenses = { navigateTo(Routes.LICENSES) },
            )
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LICENSES) {
            LicensesScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "extensions?tab={tab}",
            arguments = listOf(
                androidx.navigation.navArgument("tab") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { entry ->
            ExtensionsScreen(
                extensions = extensions,
                showDisclaimer = !extDisclaimerShown,
                initialTab = entry.arguments?.getInt("tab") ?: 0,
                showExtInMenu = showExtInMenu,
                onBack = { navController.popBackStack() },
                onImport = { extensionImportLauncher.launch("*/*") },
                onToggle = { id, enabled -> scope.launch { graph.extensions.setEnabled(id, enabled) } },
                onPin = { id, pinned -> scope.launch { graph.extensions.setPinned(id, pinned) } },
                onDelete = { id -> scope.launch { graph.extensions.remove(id) } },
                onDisclaimerAccept = { scope.launch { graph.settings.setExtDisclaimerShown() } },
                onShowExtInMenuChange = { enabled ->
                    scope.launch {
                        graph.settings.setShowExtInMenu(enabled)
                        toast(if (enabled) "菜单将显示常用扩展" else "菜单常用扩展已关闭")
                    }
                },
                onOpenStore = { url ->
                    openBrowser(url)
                    toast("获取页面已打开：下载 .user.js 或 zip 后回扩展中心导入")
                },
                actions = actionsFor(back = { navController.popBackStack() }),
            )
        }

        composable(Routes.HOME_SETTINGS) {
            HomeSettingsScreen(
                mode = homepageMode,
                customHomepage = customHomepage,
                wallpaperIndex = wallpaperIndex,
                showShortcuts = showShortcutsOnHome,
                onModeChange = { mode -> scope.launch { graph.settings.setHomepageMode(mode) } },
                onCustomHomepageChange = { url -> scope.launch { graph.settings.setCustomHomepage(url) } },
                onWallpaperChange = { index -> scope.launch { graph.settings.setWallpaperIndex(index) } },
                onShowShortcutsChange = { enabled -> scope.launch { graph.settings.setShowShortcutsOnHome(enabled) } },
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

    // 菜单「扩展程序」快捷面板：装了几个、启停一下，或跳管理/探索。
    if (showExtSheet) {
        com.anxin.browser.feature.extensions.ExtQuickSheet(
            extensions = extensions,
            onDismiss = { showExtSheet = false },
            onToggle = { id, enabled -> scope.launch { graph.extensions.setEnabled(id, enabled) } },
            onManageMine = {
                showExtSheet = false
                navigateTo(Routes.extensions(0))
            },
            onExplore = {
                showExtSheet = false
                navigateTo(Routes.extensions(1))
            },
        )
    }

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
                // 无痕与阅读模式也反映开关状态，让菜单项能看到"已选中"。
                if (currentTab()?.isIncognito == true) add("incognito")
                if (readerJson != null) add("reading")
            },
            // 菜单显示常用扩展：只露出固定（pinned）的扩展，开关即启停。
            commonExtensions = if (showExtInMenu) extensions.filter { it.pinned } else emptyList(),
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
            onToggleCommonExtension = { id, enabled ->
                scope.launch { graph.extensions.setEnabled(id, enabled) }
            },
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
            containerColor = AnxinDialogContainer,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ExitToApp,
                        contentDescription = null,
                        tint = AnxinPalette.danger,
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
                    color = AnxinPalette.inkSecondary,
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
                        containerColor = AnxinPalette.danger,
                        contentColor = AnxinPalette.onAccent,
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
                    Text("继续浏览", color = AnxinPalette.brand, fontWeight = FontWeight.SemiBold)
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
    val palette = AnxinPalette
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AnxinDialogContainer,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text("搜索引擎", style = MaterialTheme.typography.titleLarge)
                Text("选择默认搜索入口", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
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
                            .background(if (selected) anxinField() else anxinSurfaceMuted())
                            .clickable { onPick(name) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.ink,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "当前引擎",
                                tint = palette.brand,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Text(
                    "自定义引擎将在内核阶段接入",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
