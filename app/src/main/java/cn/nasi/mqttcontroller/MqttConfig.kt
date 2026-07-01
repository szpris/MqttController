package cn.nasi.mqttcontroller

/**
 * MQTT 配置常量
 * Topic 格式：nasi/app/{appId}/cmd
 * Payload：start / stop
 */
object MqttConfig {
    const val BROKER = "uc.nasi.cn"
    const val PORT = 51883
    const val CLIENT_ID = "android_controller"

    // Topic 前缀和后缀，完整 topic = "$TOPIC_PREFIX/$appId/$TOPIC_SUFFIX"
    const val TOPIC_PREFIX = "nasi/app"
    const val TOPIC_SUFFIX = "cmd"

    fun getTopicForApp(appId: String) = "$TOPIC_PREFIX/$appId/$TOPIC_SUFFIX"

    // 9 个 App 列表
    val APP_LIST = listOf(
        "app1", "app2", "app3",
        "app4", "app5", "app6",
        "app7", "app8", "app9"
    )
}
