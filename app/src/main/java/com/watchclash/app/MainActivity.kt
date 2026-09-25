package com.watchclash.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleBtn: Button
    private var running = false

    private val VPN_REQ = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 手表屏小，代码搭 UI
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "未连接"
            textSize = 16f
        }

        toggleBtn = Button(this).apply {
            text = "启动"
            setOnClickListener { onToggle() }
        }

        root.addView(statusText)
        root.addView(toggleBtn)
        setContentView(root)

        updateUi()
    }

    private fun onToggle() {
        if (!running) {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, VPN_REQ)
            } else {
                startVpn()
            }
        } else {
            stopVpn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQ) {
            if (resultCode == RESULT_OK) {
                startVpn()
            } else {
                statusText.text = "VPN 授权被拒绝"
            }
        }
    }

    private fun startVpn() {
        ConfigWriter.ensureConfig(this)

        val i = Intent(this, ClashVpnService::class.java).apply {
            action = ClashVpnService.ACTION_START
        }
        startForegroundService(i)
        running = true
        updateUi()
    }

    private fun stopVpn() {
        val i = Intent(this, ClashVpnService::class.java).apply {
            action = ClashVpnService.ACTION_STOP
        }
        startService(i)
        running = false
        updateUi()
    }

    private fun updateUi() {
        statusText.text = if (running) "已连接" else "未连接"
        toggleBtn.text = if (running) "停止" else "启动"
    }
}
