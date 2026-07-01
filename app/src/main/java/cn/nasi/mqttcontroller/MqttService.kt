package cn.nasi.mqttcontroller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttService : Service() {

    companion object {
        // 广播 Action
        const val ACTION_STATUS = "cn.nasi.mqttcontroller.STATUS"
        const val ACTION_LOG    = "cn.nasi.mqttcontroller.LOG"

        // 指令 Action
        const val CMD_CONNECT      = "cn.nasi.mqttcontroller.CONNECT"
        const val CMD_DISCONNECT   = "cn.nasi.mqttcontroller.DISCONNECT"
        const val CMD_PUBLISH      = "cn.nasi.mqttcontroller.PUBLISH"
        const val CMD_QUERY_STATUS = "cn.nasi.mqttcontroller.QUERY_STATUS"

        // Extra 键
        const val EXTRA_BROKER    = "broker"
        const val EXTRA_PORT      = "port"
        const val EXTRA_CLIENT_ID = "client_id"
        const val EXTRA_APP_ID    = "app_id"   // 目标 appId（如 app1）
        const val EXTRA_PAYLOAD   = "payload"
        const val EXTRA_STATUS    = "status"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_MESSAGE   = "message"

        private const val CHANNEL_ID       = "mqtt_service_channel"
        private const val NOTIFICATION_ID  = 1001
    }

    private var mqttClient: MqttClient? = null
    private var isConnected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("MQTT 控制器运行中"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CMD_CONNECT -> {
                val broker   = intent.getStringExtra(EXTRA_BROKER)    ?: MqttConfig.BROKER
                val port     = intent.getIntExtra(EXTRA_PORT,           MqttConfig.PORT)
                val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)  ?: MqttConfig.CLIENT_ID
                connectMqtt(broker, port, clientId)
            }
            CMD_DISCONNECT -> disconnectMqtt()
            CMD_PUBLISH -> {
                val appId   = intent.getStringExtra(EXTRA_APP_ID)  ?: return START_NOT_STICKY
                val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: return START_NOT_STICKY
                publishMessage(appId, payload)
            }
            CMD_QUERY_STATUS -> {
                broadcastStatus(if (isConnected) "已连接" else "未连接", isConnected)
            }
        }
        return START_NOT_STICKY
    }

    private fun connectMqtt(broker: String, port: Int, clientId: String) {
        Thread {
            try {
                broadcastLog("正在连接 $broker:$port ...")
                mqttClient?.disconnect()
                val serverUri = "tcp://$broker:$port"
                mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        isConnected = false
                        broadcastStatus("连接断开", false)
                        broadcastLog("连接已断开: ${cause?.message}")
                        updateNotification("MQTT 已断开")
                    }
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        broadcastLog("收到消息 [$topic]: ${String(message.payload)}")
                    }
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                val opts = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                }
                mqttClient?.connect(opts)
                isConnected = true
                broadcastStatus("已连接 $broker:$port", true)
                broadcastLog("连接成功！")
                updateNotification("已连接 $broker")
            } catch (e: Exception) {
                isConnected = false
                broadcastStatus("连接失败", false)
                broadcastLog("连接失败: ${e.message}")
            }
        }.start()
    }

    private fun disconnectMqtt() {
        Thread {
            try {
                mqttClient?.disconnect()
                mqttClient = null
                isConnected = false
                broadcastStatus("已断开", false)
                broadcastLog("已主动断开连接")
                updateNotification("MQTT 控制器运行中")
            } catch (e: Exception) {
                broadcastLog("断开连接时出错: ${e.message}")
            }
        }.start()
    }

    private fun publishMessage(appId: String, payload: String) {
        if (!isConnected || mqttClient == null) {
            broadcastLog("未连接，无法发送")
            return
        }
        Thread {
            try {
                val topic   = MqttConfig.getTopicForApp(appId)
                val message = MqttMessage(payload.toByteArray()).apply { qos = 1 }
                mqttClient?.publish(topic, message)
                broadcastLog("[$appId] $payload → $topic")
            } catch (e: Exception) {
                broadcastLog("发送失败: ${e.message}")
            }
        }.start()
    }

    // ---- 广播工具 ----

    private fun broadcastStatus(status: String, connected: Boolean) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ACTION_STATUS).apply {
                putExtra(EXTRA_STATUS, status)
                putExtra(EXTRA_CONNECTED, connected)
            }
        )
    }

    private fun broadcastLog(msg: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ACTION_LOG).apply { putExtra(EXTRA_MESSAGE, msg) }
        )
    }

    // ---- 通知 ----

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "MQTT 服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "MQTT 后台连接通知" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MQTT 控制器")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mqttClient?.disconnect() } catch (_: Exception) {}
    }
}
