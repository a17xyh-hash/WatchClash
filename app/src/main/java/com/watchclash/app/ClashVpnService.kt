package com.watchclash.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.File

class ClashVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.watchclash.app.START"
        const val ACTION_STOP  = "com.watchclash.app.STOP"
        private const val CH_ID = "clash_vpn"
        private const val NOTI_ID = 1
    }

    private var tunFd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP  -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTI_ID, buildNotification())

        // 首启解压 geo 文件
        GeoInstaller.ensureGeo(this)

        // 建 tun
        val builder = Builder()
            .setSession("WatchClash")
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("172.19.0.2")
            .setBlocking(true)
            .addDisallowedApplication(packageName)

        val pfd = builder.establish() ?: run {
            stopSelf()
            return
        }
        tunFd = pfd

        // 把 fd 交给 Go 内核（gomobile 生成的类名 = Main）
        val homeDir = filesDir.absolutePath
        val cfgPath = File(filesDir, "config.yaml").absolutePath
        val err = Mihomo.start(homeDir, cfgPath, pfd.fd, "gvisor")

        if (err.isNotEmpty()) {
            pfd.close()
            tunFd = null
            updateNotification("启动失败: $err")
        }
    }

    private fun stopVpn() {
        Mihomo.stop()
        tunFd?.close()
        tunFd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Mihomo.stop()
        tunFd?.close()
        tunFd = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "Clash VPN", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("WatchClash")
            .setContentText("运行中")
            .setSmallIcon(android.R.drawable.stat_sys_vpn_ic)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTI_ID, NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("WatchClash")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_vpn_ic)
            .build())
    }
}
