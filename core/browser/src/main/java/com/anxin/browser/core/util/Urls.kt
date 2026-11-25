package com.anxin.browser.core.util

import android.net.Uri
import java.net.URLEncoder

object Urls {

    private val domainRegex = Regex(
        "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+" +
            "[a-zA-Z]{2,}(:\\d{1,5})?(/.*)?$"
    )

    const val DEFAULT_ENGINE = "必应"
    val searchEngines = listOf(DEFAULT_ENGINE, "百度", "谷歌")

    fun searchEngineName(value: String?): String = when (value) {
        "Google", "谷歌" -> "谷歌"
        "百度" -> "百度"
        else -> DEFAULT_ENGINE
    }

    private val schemes = listOf("http://", "https://")

    fun normalize(raw: String, engine: String = DEFAULT_ENGINE): String {
        val input = raw.trim()
        if (input.isEmpty()) return "about:blank"
        if (input == "about:blank" || schemes.any { input.startsWith(it, ignoreCase = true) }) return input
        if (input.contains(" ") || !input.contains(".")) return search(input, engine)
        return if (domainRegex.matches(input)) "https://$input" else search(input, engine)
    }

    fun search(keyword: String, engine: String = DEFAULT_ENGINE): String {
        val encoded = URLEncoder.encode(keyword, "UTF-8")
        return when (searchEngineName(engine)) {
            "谷歌" -> "https://www.google.com/search?q=$encoded"
            "百度" -> "https://www.baidu.com/s?wd=$encoded"
            else -> "https://www.bing.com/search?q=$encoded"
        }
    }

    fun hostOf(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return runCatching { Uri.parse(url).host ?: "" }.getOrDefault("")
    }

    fun prettyHost(url: String?): String = hostOf(url).removePrefix("www.")

    fun favicon(url: String?): String {
        val host = hostOf(url)
        return if (host.isBlank()) "" else "https://$host/favicon.ico"
    }
}
