package com.watchclash.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.widget.BoxInsetLayout
import androidx.wear.widget.RotaryScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var toggleBtn: MaterialButton
    private lateinit var subEdit: TextInputEditText
    private lateinit var subBtn: MaterialButton
    private lateinit var subStatus: TextView
    private lateinit var proxyHostEdit: TextInputEditText
    private lateinit var proxyPortEdit: TextInputEditText
    private lateinit var proxyBtn: MaterialButton
    private lateinit var proxyStatus: TextView
    private var running = false

    private val colorOn   = Color.parseColor("#4DD0E1")
    private val colorOff  = Color.parseColor("#7A7A7A")
    private val colorCard = Color.parseColor("#1E2A32")
    private val colorCardStroke = Color.parseColor("#2E4550")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 圆屏适配：BoxInsetLayout 自动处理圆屏四角安全边距
        val outer = BoxInsetLayout(this).apply {
            setBackgroundColor(Color.parseColor("#101A20"))
        }
        // 圆屏内容列：左右各留 14dp 边距，避免被圆边裁切
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.rocket)
            val lp = LinearLayout.LayoutParams(dp(30), dp(30))
            lp.bottomMargin = dp(6)
            layoutParams = lp
        }

        statusDot = View(this).apply {
            val lp = LinearLayout.LayoutParams(dp(12), dp(12))
            lp.bottomMargin = dp(6)
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorOff)
            }
        }

        statusText = TextView(this).apply {
            text = "未连接"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#E0F7FA"))
        }

        toggleBtn = MaterialButton(this).apply {
            text = "启动"
            isAllCaps = false
            textSize = 17f
            cornerRadius = dp(24)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
            setOnClickListener { onToggle() }
        }

        proxyHostEdit = TextInputEditText(this).apply {
            hint = "127.0.0.1"
            textSize = 12f
            isSingleLine = true
            setText(ProxyStore.loadHost(this@MainActivity))
        }
        val hostLayout = TextInputLayout(this).apply {
            hint = "代理地址"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(proxyHostEdit)
        }
        proxyPortEdit = TextInputEditText(this).apply {
            hint = "7890"
            textSize = 12f
            isSingleLine = true
            setText(ProxyStore.loadPort(this@MainActivity))
        }
        val portLayout = TextInputLayout(this).apply {
            hint = "端口"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp
            addView(proxyPortEdit)
        }
        proxyBtn = MaterialButton(this).apply {
            text = "保存代理地址"
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(20)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onSaveProxy() }
        }
        proxyStatus = TextView(this).apply {
            text = "其它 App 代理填：" + ProxyStore.display(this@MainActivity)
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#80CBC4"))
            setPadding(0, dp(6), 0, 0)
        }
        val proxyCard = makeCard()
        val proxyInner = proxyCard.inner()
        proxyInner.addView(hostLayout)
        proxyInner.addView(portLayout)
        proxyInner.addView(proxyBtn)
        proxyInner.addView(proxyStatus)

        val subEditHolder = TextInputEditText(this).apply {
            hint = "https://.../subscribe?token=..."
            textSize = 12f
            isSingleLine = false
            setText(SubscriptionManager.loadUrl(this@MainActivity))
        }
        subEdit = subEditHolder
        val subLayout = TextInputLayout(this).apply {
            hint = "订阅链接"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(subEditHolder)
        }
        subBtn = MaterialButton(this).apply {
            text = "更新订阅"
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(20)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onUpdateSub() }
        }
        subStatus = TextView(this).apply {
            text = ""
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#80CBC4"))
            setPadding(0, dp(6), 0, 0)
        }
        val subCard = makeCard()
        val subInner = subCard.inner()
        subInner.addView(subLayout)
        subInner.addView(subBtn)
        subInner.addView(subStatus)

        col.addView(icon)
        col.addView(statusDot)
        col.addView(statusText)
        col.addView(toggleBtn)
        col.addView(proxyCard, cardLp())
        col.addView(subCard, cardLp())

        // RotaryScrollView：表冠旋转 -> 滚动；滚动条跟随显示
        val scroll = RotaryScrollView(this).apply {
            isFillViewport = true
            isScrollbarFadingEnabled = false
            isVerticalScrollBarEnabled = true
            addView(col, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        val scrollLp = BoxInsetLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        outer.addView(scroll, scrollLp)
        setContentView(outer)
        updateUi()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun makeCard(): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            strokeWidth = dp(1)
            strokeColor = colorCardStroke
            setCardBackgroundColor(colorCard)
            setContentPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        card.addView(inner)
        return card
    }

    /** 取卡片内部容器以便继续 addView */
    private fun MaterialCardView.inner(): LinearLayout {
        return getChildAt(0) as LinearLayout
    }

    private fun cardLp(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
    }

    private fun onToggle() {
        if (!running) startVpn() else stopVpn()
    }

    private fun onSaveProxy() {
        val host = proxyHostEdit.text.toString().trim()
        val port = proxyPortEdit.text.toString().trim()
        ProxyStore.save(this, host, port)
        val d = ProxyStore.display(this)
        proxyStatus.text = "已保存：" + d + "\n其它 App 代理填这个"
        Toast.makeText(this, "代理地址已保存", Toast.LENGTH_SHORT).show()
    }

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
                        subStatus.text = "订阅已更新（" + res.bytes + " 字符）\n重启 VPN 后生效"
                        Toast.makeText(this, "订阅更新成功", Toast.LENGTH_SHORT).show()
                    }
                    is SubscriptionManager.Result.Err -> {
                        subStatus.text = "失败: " + res.msg
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
