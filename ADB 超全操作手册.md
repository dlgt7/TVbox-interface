# ADB 超全操作手册（2024-08 实测）
> 覆盖机顶盒、电视、手机全场景，每条命令均附「魔百盒可用示例」，复制即用。

---

## 1️⃣ 连接设备
| 场景 | 命令 | 魔百盒示例 |
|---|---|---|
| USB 首次连接 | `adb devices` | 出现 `192.168.1.100:5555 device` 即成功 |
| 网络连接 | `adb connect 192.168.x.x:5555` | `adb connect 192.168.1.100` |
| 断开网络 | `adb disconnect 192.168.x.x:5555` | `adb disconnect 192.168.1.100` |
| 获取序列号 | `adb get-serialno` | 返回 `192.168.1.100` |
| 检查 root | `adb root && adb remount` | 盒子提示 `remount succeeded` 即可读写 `/system` |

> 机顶盒首次网络调试：先 USB 线执行 `adb tcpip 5555`，再拔掉线用网络连接[^23^]。

---

## 2️⃣ 文件快速互传
| 场景 | 命令 | 魔百盒示例 |
|---|---|---|
| 推 APK 到 U 盘路径 | `adb push local.apk /mnt/usb/sda1/` | `adb push dbzm.apk /mnt/usb/sda1/` |
| 拉取原厂 OTA | `adb pull /cache/upgrade/ota.zip ./` | 备份前务必 `adb root && adb remount` |
| 批量推送文件夹 | `adb push myapps/ /data/app/` | 推送后 `chmod 644 /data/app/*`[^26^] |

---

## 3️⃣ 应用管理（pm / am）
| 场景 | 命令 | 魔百盒示例 |
|---|---|---|
| 列出全部包 | `adb shell pm list packages` | 找 IPTV 包名：`pm list packages | grep iptv` |
| 列出第三方 | `adb shell pm list packages -3` | 只看自己装的 APK |
| 安装（保留数据） | `adb install -r d:\dbzm.apk` | `-r` 重装并保留数据[^28^] |
| 卸载（保留数据） | `adb uninstall -k com.dangbei.tvlauncher` | 卸载当贝桌面但保留配置 |
| 禁用内置 IPTV | `adb shell pm disable-user com.huawei.iptv` | 立即隐藏，恢复用 `enable` |
| 强制停止应用 | `adb shell am force-stop com.huawei.iptv` | 停止后再禁用，防止自启 |
| 启动当贝桌面 | `adb shell am start -n com.dangbei.tvlauncher/.Launcher` | 一键切换桌面[^22^] |
| 打开设置页 | `adb shell am start -n com.android.settings/.Settings` | 盒子被屏蔽菜单时可用 |

---

## 4️⃣ 系统设置（无需 root 也能改）
| 场景 | 命令 | 魔百盒示例 |
|---|---|---|
| 关闭自动更新 | `adb shell settings put global auto_update_system 0` | 防止夜间回滚 |
| 关闭 USB 调试验证 | `adb shell settings put global adb_auth_timeout 0` | 每次插线免点确认 |
| 修改屏幕超时 10 min | `adb shell settings put system screen_off_timeout 600000` | 演示场景常用 |
| 打开 Wi-Fi | `adb shell svc wifi enable` | 隐藏 Wi-Fi 菜单时救急[^26^] |
| 关闭飞行模式 | `adb shell settings put global airplane_mode_on 0 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false` | 国科微盒子无实体键时使用 |

---

## 5️⃣ 日志与调试
| 场景 | 命令 | 魔百盒示例 |
|---|---|---|
| 实时抓日志 | `adb logcat | grep -i "dangbei"` | 看当贝桌面崩溃信息 |
| 保存完整日志 | `adb logcat -d > log.txt` | 提交给开发者排错 |
| 生成 bug 报告 | `adb bugreport ./bug.zip` | 包含 kernel + logcat[^23^] |
| 屏幕截图 | `adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png` | 无截图键的盒子秒抓图 |

---

## 6️⃣ 一键脚本（机顶盒专用）
保存为 `box_onekey.sh` → `adb push box_onekey.sh /sdcard/` → 执行：


adb shell sh /sdcard/box_onekey.sh

---

## 7️⃣脚本内容（魔百盒实测）：
#!/system/bin/sh

① 挂载可读写

mount -o remount,rw /system

mount -o remount,rw /data

② 安装当贝桌面+市场

cp /mnt/usb/sda1/dbzm.apk /data/app/

cp /mnt/usb/sda1/dbsc.apk /data/app/

chmod 644 /data/app/dbzm.apk

chmod 644 /data/app/dbsc.apk

pm install -r /data/app/dbzm.apk

pm install -r /data/app/dbsc.apk

③ 禁用 IPTV（自行替换包名）

pm disable-user com.huawei.iptv

④ 启动当贝

am start -n com.dangbei.tvlauncher/.Launcher

## 🔚常见问题速解：

| 现象                            | 解决                                               |
| ----------------------------- | ------------------------------------------------ |
| `device unauthorized`         | 盒子重新打开「USB调试」→ 手机端点「始终允许」                        |
| `error: more than one device` | 只保留一个设备，或 `adb -s 192.168.1.100:5555 <命令>` 指定 IP |
| `adb server is out of date`   | PC 端 platform-tools 升级到最新                        |
| 网络连接断开                        | 盒子休眠导致，先在「开发者选项」里关闭「以太网待机省电」                     |

## 🔚 小例子：10 秒给魔百盒装当贝市场

电脑端：

adb connect 192.168.1.100

adb push dbsc.apk /mnt/usb/sda1/

adb install -r /mnt/usb/sda1/dbsc.apk

adb shell am start -n com.dangbei.market/.MainActivity

→ 盒子立即弹出当贝市场，完成。
