package com.watchclash.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import mihomo.Mihomo
import java.io.File

class ClashVpnService : android.app.Service() {

    companion object {
        const val ACTION_START = "com.watchclash.app.START"
        const val ACTION_STOP  = "com.watchclash.app.STOP"
        private const val CH_ID = "clash_vpn"
        private const val NOTI_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP  -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTI_ID, buildNotification())
        GeoInstaller.ensureGeo(this)
        val homeDir = filesDir.absolutePath
        val cfgPath = File(filesDir, "config.yaml").absolutePath
        val err = Mihomo.start(homeDir, cfgPath, -1L, "")
        if (err.isNotEmpty()) { updateNotification("启动失败: " + err) } else { updateNotification("本地代理已启动") }
    }

    private fun stopVpn() {
        Mihomo.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Mihomo.stop()
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTI_ID, NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("WatchClash")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build())
    }
}
