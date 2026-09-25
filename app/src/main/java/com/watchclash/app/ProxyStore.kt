package com.watchclash.app

import android.content.Context

/**
 * 代理地址存储（简版）
 *
 * 保存用户填写的本地代理地址（默认 127.0.0.1:7890），
 * 供 TGWrist 等其它 App 手动指向本机 mihomo 的 mixed-port。
 *
 * 存储方式与 SubscriptionManager 一致：SharedPreferences。
 */
object ProxyStore {
    private const val PREF = "watchclash_pref"
    private const val KEY_HOST = "proxy_host"
    private const val KEY_PORT = "proxy_port"

    const val DEFAULT_HOST = "127.0.0.1"
    const val DEFAULT_PORT = "7890"

    /** 读取保存的 host，未设置则返回默认值 */
    fun loadHost(ctx: Context): String {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }

    /** 读取保存的 port，未设置则返回默认值 */
    fun loadPort(ctx: Context): String {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }

    /** 保存 host + port */
    fun save(ctx: Context, host: String, port: String) {
        val h = host.trim().ifEmpty { DEFAULT_HOST }
        val p = port.trim().ifEmpty { DEFAULT_PORT }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOST, h)
            .putString(KEY_PORT, p)
            .apply()
    }

    /** 组合成 "host:port" */
    fun display(ctx: Context): String = "${loadHost(ctx)}:${loadPort(ctx)}"
}
