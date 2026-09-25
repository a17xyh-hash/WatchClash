package com.watchclash.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

/**
 * WatchClash 主界面 —— Material 3 圆屏适配版
 *
 * 设计要点（Wear OS 圆屏）：
 *  - Box + Gravity.CENTER 让内容居于圆屏内切圆，避免被裁切
 *  - 整体包一层 ScrollView，小屏也不至于顶出屏幕
 *  - 使用 MD3 组件：MaterialButton / TextInputLayout
 *  - 顶部一个状态圆点（GradientDrawable 动态着色）直观显示连接状态
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var toggleBtn: MaterialButton
    private lateinit var subEdit: TextInputEditText
    private lateinit var subBtn: MaterialButton
    private lateinit var subStatus: TextView

    private var running = false
    private val VPN_REQ = 1001

    // MD3 主色（与 themes.xml 保持一致）
    private val colorOn  = Color.parseColor("#4DD0E1")
    private val colorOff = Color.parseColor("#7A7A7A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------- 根容器：居中，保证落在圆屏内切范围 ----------
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // ---------- 状态圆点 ----------
        statusDot = View(this).apply {
            val lp = LinearLayout.LayoutParams(dp(14), dp(14))
            lp.bottomMargin = dp(8)
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorOff)
            }
        }

        // ---------- 状态文字 ----------
        statusText = TextView(this).apply {
            text = "未连接"
            textSize = 17f
            gravity = Gravity.CENTER
        }

        // ---------- 启动/停止 按钮（MD3 药丸） ----------
        toggleBtn = MaterialButton(this).apply {
            text = "启动"
            isAllCaps = false
            textSize = 16f
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
            setOnClickListener { onToggle() }
        }

        // ---------- 订阅输入框（MD3 TextInputLayout） ----------
        val subEditHolder = TextInputEditText(this).apply {
            hint = "https://.../subscribe?token=..."
            textSize = 12f
            isSingleLine = false
            setText(SubscriptionManager.loadUrl(this@MainActivity))
        }
        val subLayout = TextInputLayout(this).apply {
            hint = "订阅链接"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(20)
            layoutParams = lp
            addView(subEditHolder)
        }
        @Suppress("UNCHECKED_CAST")
        subEdit = subEditHolder

        // ---------- 更新订阅 按钮 ----------
        subBtn = MaterialButton(this).apply {
            text = "更新订阅"
            isAllCaps = false
            textSize = 14f
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener { onUpdateSub() }
        }

        // ---------- 订阅状态 ----------
        subStatus = TextView(this).apply {
            text = ""
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        col.addView(statusDot)
        col.addView(statusText)
        col.addView(toggleBtn)
        col.addView(subLayout)
        col.addView(subBtn)
        col.addView(subStatus)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(col, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        outer.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        setContentView(outer)
        updateUi()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

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

    /** 下载订阅（后台线程），完成后回主线程提示 */
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
        (statusDot.background as? GradientDrawable)?.setColor(if (running) colorOn else colorOff)
    }
}
