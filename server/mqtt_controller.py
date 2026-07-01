import paho.mqtt.client as mqtt
import time
import subprocess

# --- 配置信息 ---
MQTT_BROKER = "uc.nasi.cn"
MQTT_PORT = 51883
CLIENT_ID = "app_Server"

# app1~app9 对应的 systemctl 服务名，按实际情况修改
APP_SERVICES = {
    "app1": "app1",
    "app2": "app2",
    "app3": "app3",
    "app4": "app4",
    "app5": "app5",
    "app6": "app6",
    "app7": "app7",
    "app8": "app8",
    "app9": "app9",
}

# Topic 格式：nasi/app/{appN}/cmd
TOPIC_PREFIX = "nasi/app"
TOPIC_SUFFIX = "cmd"

def get_topic(app_id: str) -> str:
    return f"{TOPIC_PREFIX}/{app_id}/{TOPIC_SUFFIX}"

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"已连接到 Broker {MQTT_BROKER}:{MQTT_PORT}")
        # 订阅所有 app 的 topic
        for app_id in APP_SERVICES:
            topic = get_topic(app_id)
            client.subscribe(topic)
            print(f"  已订阅: {topic}")
    else:
        print(f"连接失败, rc={rc}")

def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode().strip()
        topic = msg.topic
        print(f"收到消息 [{topic}]: {payload}")

        # 解析 topic 中的 app_id
        # topic 格式: nasi/app/{appN}/cmd
        parts = topic.split("/")
        if len(parts) != 4 or parts[0] != "nasi" or parts[1] != "app" or parts[3] != "cmd":
            print(f"  无效 topic 格式: {topic}")
            return

        app_id = parts[2]
        if app_id not in APP_SERVICES:
            print(f"  未知 app: {app_id}")
            return

        service_name = APP_SERVICES[app_id]

        if payload == "start":
            print(f"  执行: systemctl start {service_name}")
            result = subprocess.run(
                ["systemctl", "start", service_name],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                print(f"  {service_name} 启动成功")
            else:
                print(f"  {service_name} 启动失败: {result.stderr.strip()}")

        elif payload == "stop":
            print(f"  执行: systemctl stop {service_name}")
            result = subprocess.run(
                ["systemctl", "stop", service_name],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                print(f"  {service_name} 停止成功")
            else:
                print(f"  {service_name} 停止失败: {result.stderr.strip()}")

        else:
            print(f"  未知指令: {payload}（仅支持 start / stop）")

    except Exception as e:
        print(f"处理消息出错: {e}")

def main():
    print("=== MQTT 多应用控制服务 ===")
    print(f"Broker: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"管理应用: {', '.join(APP_SERVICES.keys())}")
    print("")

    client = mqtt.Client(client_id=CLIENT_ID)
    client.on_connect = on_connect
    client.on_message = on_message

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_start()
        print("服务运行中，按 Ctrl+C 退出...")
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n程序中断")
    finally:
        client.loop_stop()
        client.disconnect()
        print("已退出")

if __name__ == "__main__":
    main()
