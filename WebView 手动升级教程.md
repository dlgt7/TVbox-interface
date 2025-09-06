📘 WebView 手动升级教程（适用于安卓电视/盒子）
✅ 教程目标
升级系统内置的 WebView 组件（如从 83 版本升级至 106+），以提升如 TVBox 等应用的网页解析能力，解决部分视频无法播放或加载失败的问题。
🧰 所需工具
电脑（Windows 系统）
盒子/电视已开启 ADB 调试
盒子具备 Root 权限 或刷入支持 ADB Root 的固件
安装包：新版 WebView（推荐 LineageOS 版，包名与原系统一致，无需修改框架）
工具：ADB 工具包、MT管理器（可选）、U盘（用于传输安装包）
📦 WebView 下载地址（推荐）
APKMirror - Android System WebView
LineageOS WebView（推荐）
酷安、Softpedia 等平台也可获取
🧪 升级步骤（ADB 法）

1. 连接设备
   bash
   复制
   adb connect 盒子IP
2. 查看当前 WebView 版本
   方法一：安装 Via 浏览器 打开检测网址
   方法二：ADB 命令打开检测页
   bash
   复制
   adb shell am start -a android.intent.action.VIEW -d https://liulanmi.com/labs/core.html
3. 备份原 WebView
   bash
   复制
   adb shell pm path com.android.webview
   adb pull /product/app/webview/webview.apk C:\Users\你的用户名\Downloads
4. 获取写入权限
   bash
   复制
   adb shell
   su
   mount -o remount,rw,seclabel,relatime /dev/block/dm-1 /product
5. 删除旧版本
   bash
   复制
   adb shell rm -rf /product/app/webview/webview.apk
6. 安装新版本
   将新版 WebView（如 webview_106.0.5259.72.apk）放入 U 盘
   使用小白文件管理器或 ADB 安装：
   bash
   复制
   adb install webview_106.0.5259.72.apk
7. 重启设备
   bash
   复制
   adb reboot
   ✅ 验证是否成功
   重启后再次访问检测网址，确认 WebView 版本已更新。
   ⚠️ 注意事项
   若设备未 Root，无法完成 /product 目录挂载和替换操作。
   若使用的是 Google 原生 WebView，可能需要修改 framework-res.apk，操作更复杂，不推荐新手尝试。
   建议优先使用 LineageOS 版 WebView，兼容性好，无需修改系统框架。
   📌 总结
   WebView 升级适用于：
   使用 TVBox、影视类 APP 出现网页加载失败、视频解析异常
   想提升系统网页兼容性和安全性
   有一定刷机/ADB 操作经验的用户
