package com.watchclash.app

import android.content.Context
import java.io.File

/**
 * 首启把 assets 里的 GeoIP.dat / GeoSite.dat 复制到 filesDir 根目录。
 *
 * 为什么必须是 filesDir 根目录：
 * mihomo 的 constant/path.go 里 GeoIP()/GeoSite() 直接 os.ReadDir(homeDir)
 * 扫描 homeDir 根目录，文件名固定为 GeoIP.dat / GeoSite.dat。
 * 而 SetHomeDir(filesDir) 我们是在 Go 侧调用的，所以目标目录必须是 filesDir。
 *
 * 缺省行为：assets 里没有 geo 文件时，只是规则匹配失效，内核仍能启动，不阻断。
 */
object GeoInstaller {

    private val GEO_FILES = listOf("GeoIP.dat", "GeoSite.dat")

    fun ensureGeo(ctx: Context) {
        val dstDir = ctx.filesDir
        for (name in GEO_FILES) {
            val dst = File(dstDir, name)
            if (dst.exists() && dst.length() > 0) continue
            try {
                ctx.assets.open(name).use { input ->
                    dst.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // assets 里没有这个文件 -> 忽略，内核缺规则的降级运行
            }
        }
    }
}
