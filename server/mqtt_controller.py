import paho.mqtt.client as mqtt
import time
import subprocess

# --- 配置信息（与 Android 端一致）---
MQTT_BROKER = "uc.nasi.cn"
MQTT_PORT = 51883
CLIENT_ID = "app_Server"
TOPIC = "nasi/app/info"
SERVICE_NAME = "testapp"  # systemctl 服务名，按需修改

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"已连接到 Broker {MQTT_BROKER}:{MQTT_PORT}")
        client.subscribe(TOPIC)
        print(f"已订阅主题: {TOPIC}")
    else:
        print(f"连接失败, rc={rc}")

def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode().strip()
        print(f"收到消息 [{msg.topic}]: {payload}")
        if payload == "start":
            print(f"执行: systemctl start {SERVICE_NAME}")
            subprocess.run(["systemctl", "start", SERVICE_NAME], check=True)
            print("服务已启动")
        elif payload == "stop":
            print(f"执行: systemctl stop {SERVICE_NAME}")
            subprocess.run(["systemctl", "stop", SERVICE_NAME], check=True)
            print("服务已停止")
        else:
            print(f"未知指令: {payload}")
    except subprocess.CalledProcessError as e:
        print(f"systemctl 执行失败: {e}")
    except Exception as e:
        print(f"处理消息出错: {e}")

def main():
    client = mqtt.Client(client_id=CLIENT_ID)
    client.on_connect = on_connect
    client.on_message = on_message
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_start()
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("程序中断")
    finally:
        client.loop_stop()
        client.disconnect()

if __name__ == "__main__":
    main()
