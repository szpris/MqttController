package cn.nasi.mqttcontroller

/**
 * MQTT 默认配置常量
 * 对应云主机 Python 端配置：
 *   BROKER = "uc.nasi.cn"
 *   PORT = 51883
 *   CLIENT_ID = "app_Server"
 *   TOPIC = "nasi/app/info"
 */
object MqttConfig {
    const val BROKER = "uc.nasi.cn"
    const val PORT = 51883
    const val CLIENT_ID = "android_controller"
    const val TOPIC = "nasi/app/info"
}
