# MQTT Controller Android App

基于 Eclipse Paho MQTT 库的 Android 远程控制应用，通过 MQTT 协议向云主机发送 start/stop 指令控制服务启停。

## 配置参数

| 参数 | 默认值 |
|------|--------|
| Broker | uc.nasi.cn |
| Port | 51883 |
| Topic | nasi/app/info |
| Payload | start / stop |

## 功能

- 连接/断开 MQTT Broker（支持界面修改配置）
- 一键发送 `start` / `stop` 指令
- 前台服务保持后台连接
- 实时日志显示

## 编译

```bash
./gradlew assembleRelease
```

APK 路径: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 云主机配对 Python 端

见仓库根目录 `server/mqtt_controller.py`
