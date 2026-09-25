package com.watchclash.app

import android.content.Context
import java.io.File

/**
 * 写最小 config.yaml。
 *
 * 关键：tun.enable 必须为 false。
 * 因为 tun 是我们在 Kotlin 侧用 VpnService.Builder 建的，然后通过 FileDescriptor
 * 注入内核，内核自己不该再建 tun（双保险，避免内核重复建 tun 失败）。
 */
object ConfigWriter {

    fun ensureConfig(ctx: Context): String {
        val f = File(ctx.filesDir, "config.yaml")
        if (!f.exists() || f.length() == 0L) {
            f.writeText(DEFAULT_CONFIG)
        }
        return f.absolutePath
    }

    private val DEFAULT_CONFIG = """
mixed-port: 7890
allow-lan: false
mode: rule
log-level: warning
external-controller: 127.0.0.1:9090

tun:
  enable: false

dns:
  enable: true
  listen: 0.0.0.0:1053
  ipv6: false
  enhanced-mode: fake-ip
  fake-ip-range: 172.19.0.1/16
  nameserver:
    - 223.5.5.5
    - 119.29.29.29

proxies: []

proxy-groups:
  - name: PROXY
    type: select
    proxies:
      - DIRECT

rules:
  - MATCH,PROXY
""".trimIndent()
}
