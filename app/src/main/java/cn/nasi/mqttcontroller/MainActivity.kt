package cn.nasi.mqttcontroller

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cn.nasi.mqttcontroller.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // 接收来自 MqttService 的广播
    private val mqttReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MqttService.ACTION_STATUS -> {
                    val status = intent.getStringExtra(MqttService.EXTRA_STATUS) ?: return
                    val connected = intent.getBooleanExtra(MqttService.EXTRA_CONNECTED, false)
                    updateStatus(status, connected)
                }
                MqttService.ACTION_LOG -> {
                    val msg = intent.getStringExtra(MqttService.EXTRA_MESSAGE) ?: return
                    appendLog(msg)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 UI 配置值（可在界面直接修改）
        binding.etBroker.setText(MqttConfig.BROKER)
        binding.etPort.setText(MqttConfig.PORT.toString())
        binding.etClientId.setText(MqttConfig.CLIENT_ID)
        binding.etTopic.setText(MqttConfig.TOPIC)

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        // 按钮点击事件
        binding.btnConnect.setOnClickListener {
            val broker = binding.etBroker.text.toString().trim()
            val portStr = binding.etPort.text.toString().trim()
            val clientId = binding.etClientId.text.toString().trim()
            val topic = binding.etTopic.text.toString().trim()

            if (broker.isEmpty() || portStr.isEmpty() || clientId.isEmpty() || topic.isEmpty()) {
                Toast.makeText(this, "请填写完整配置信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val port = portStr.toIntOrNull() ?: run {
                Toast.makeText(this, "端口号无效", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MqttService::class.java).apply {
                action = MqttService.CMD_CONNECT
                putExtra(MqttService.EXTRA_BROKER, broker)
                putExtra(MqttService.EXTRA_PORT, port)
                putExtra(MqttService.EXTRA_CLIENT_ID, clientId)
                putExtra(MqttService.EXTRA_TOPIC, topic)
            }
            ContextCompat.startForegroundService(this, intent)
        }

        binding.btnDisconnect.setOnClickListener {
            val intent = Intent(this, MqttService::class.java).apply {
                action = MqttService.CMD_DISCONNECT
            }
            startService(intent)
        }

        binding.btnStart.setOnClickListener {
            sendCommand("start")
        }

        binding.btnStop.setOnClickListener {
            sendCommand("stop")
        }

        binding.btnClearLog.setOnClickListener {
            logBuilder.clear()
            binding.tvLog.text = ""
        }

        updateStatus("未连接", false)
        appendLog("App 已启动，请配置并连接 MQTT Broker")
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(MqttService.ACTION_STATUS)
            addAction(MqttService.ACTION_LOG)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter)
        // 获取当前服务状态
        val intent = Intent(this, MqttService::class.java).apply {
            action = MqttService.CMD_QUERY_STATUS
        }
        startService(intent)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver)
    }

    private fun sendCommand(payload: String) {
        val intent = Intent(this, MqttService::class.java).apply {
            action = MqttService.CMD_PUBLISH
            putExtra(MqttService.EXTRA_PAYLOAD, payload)
        }
        startService(intent)
    }

    private fun updateStatus(status: String, connected: Boolean) {
        binding.tvStatus.text = status
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (connected) R.color.status_connected else R.color.status_disconnected
            )
        )
        binding.btnConnect.isEnabled = !connected
        binding.btnDisconnect.isEnabled = connected
        binding.btnStart.isEnabled = connected
        binding.btnStop.isEnabled = connected
    }

    private fun appendLog(msg: String) {
        val time = dateFormat.format(Date())
        logBuilder.append("[$time] $msg\n")
        binding.tvLog.text = logBuilder.toString()
        // 自动滚动到底部
        binding.scrollLog.post {
            binding.scrollLog.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
