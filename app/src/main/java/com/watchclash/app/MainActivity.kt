package com.watchclash.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleBtn: Button
    private lateinit var subEdit: EditText
    private lateinit var subBtn: Button
    private lateinit var subStatus: TextView
    private var running = false
    private val VPN_REQ = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // ---- 订阅区 ----
        val subLabel = TextView(this).apply {
            text = "订阅链接（Clash 格式）"
            textSize = 13f
            setPadding(0, 24, 0, 4)
        }
        subEdit = EditText(this).apply {
            hint = "https://.../clash?token=..."
            textSize = 12f
            setSingleLine(true)
            // 载入上次保存的链接
            setText(SubscriptionManager.loadUrl(this@MainActivity))
        }
        subBtn = Button(this).apply {
            text = "更新订阅"
            setOnClickListener { onUpdateSub() }
        }
        subStatus = TextView(this).apply {
            text = ""
            textSize = 12f
        }

        root.addView(statusText)
        root.addView(toggleBtn)
        root.addView(subLabel)
        root.addView(subEdit)
        root.addView(subBtn)
        root.addView(subStatus)

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

    /** 下载订阅（后台线程），完成后提示 */
    private fun onUpdateSub() {
        val url = subEdit.text.toString().trim()
        subStatus.text = "下载中..."
        subBtn.isEnabled = false
        thread {
            val res = SubscriptionManager.update(this, url)
            runOnUiThread {
                subBtn.isEnabled = true
                when (res) {
                    is SubscriptionManager.Result.Ok -> {
                        subStatus.text = "订阅已更新（${res.bytes} 字符）\n重启 VPN 后生效"
                        Toast.makeText(this, "订阅更新成功", Toast.LENGTH_SHORT).show()
                    }
                    is SubscriptionManager.Result.Err -> {
                        subStatus.text = "失败: ${res.msg}"
                    }
                }
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
