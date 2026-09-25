package com.watchclash.app

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 订阅管理：
 *  - 从用户填入的 URL 下载订阅内容
 *  - 判断是 Clash yaml 还是 base64 节点列表
 *  - Clash yaml -> 直接落盘 filesDir/config.yaml
 *  - base64 节点 -> 暂不支持（返回明确错误）
 *  - 订阅 URL 存 SharedPreferences，绝不硬编码进代码
 */
object SubscriptionManager {

    private const val PREF = "watchclash_pref"
    private const val KEY_URL = "subscription_url"

    /** 读取上次保存的订阅 URL */
    fun loadUrl(ctx: Context): String {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_URL, "") ?: ""
    }

    /** 保存订阅 URL */
    fun saveUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url.trim()).apply()
    }

    /** 下载结果 */
    sealed class Result {
        data class Ok(val bytes: Int) : Result()
        data class Err(val msg: String) : Result()
    }

    /**
     * 下载订阅并更新 config.yaml。
     * 阻塞方法，必须在后台线程调用。
     */
    fun update(ctx: Context, urlRaw: String): Result {
        val url = urlRaw.trim()
        if (url.isEmpty()) return Result.Err("订阅链接为空")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return Result.Err("链接必须以 http(s):// 开头")
        }

        val text = try {
            download(url)
        } catch (e: Exception) {
            return Result.Err("下载失败: ${e.message}")
        }

        if (text.isBlank()) return Result.Err("下载内容为空")

        // 判断格式：Clash yaml 一定含 proxies: 或 proxy-groups: 或 rules:
        val isClashYaml = text.contains("proxies:") ||
                text.contains("proxy-groups:") ||
                text.contains("proxy-providers:")

        if (!isClashYaml) {
            // 可能是 base64 节点订阅，或其它格式
            return Result.Err("不是 Clash 订阅格式（未发现 proxies/proxy-groups）\n请在机场后台选「Clash」订阅链接")
        }

        return try {
            val f = File(ctx.filesDir, "config.yaml")
            f.writeText(text)
            saveUrl(ctx, url)
            Result.Ok(text.length)
        } catch (e: Exception) {
            Result.Err("写入 config.yaml 失败: ${e.message}")
        }
    }

    /** 简单 HTTP GET（带 UA，很多机场校验 UA） */
    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "ClashforWindows/0.20.39")
        conn.setRequestProperty("Accept", "*/*")
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code")
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
