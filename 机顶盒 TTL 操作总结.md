# 机顶盒 TTL 操作全网总结（2024-08 实测）
> 覆盖晶晨/海思/瑞芯微/国科微全系，「接线→跑码→刷机→救砖」一步不漏，10 min 搞定。

---

## 1️⃣ 事前：必须准备的 5 样硬件
| 名称 | 最低要求 | 淘宝均价 | 备注 |
|---|---|---|---|
| USB-TTL 小板 | CH340/CP2102 均可 | ￥8 | 免驱优先，务必插到 **3.3V** 跳线 |
| 杜邦线 | 母-母 4 根 | ￥2 | 线芯太细可用 **网线铜丝** 代替[^42^] |
| 电脑 | Win10/11 任意 | — | USB 口≥2，关闭防火墙 |
| 串口软件 | Putty 或 MobaXterm | 免费 | 波特率 **115200** 固定 |
| 刷机工具 | 见芯片列表 | — | 晶晨=USB_Burning_Tool / 海思=HiTool / 瑞芯微=RKDevTool |

> ⚠️ **不接 VCC！** 只接 **GND→GND，TX→RX，RX→TX**[^37^]；接反出现乱码立即调换。

---

## 2️⃣ 接线：一步对位图（2024 主板实拍）
| 芯片 | 常见板 | GND | TX | RX | 实拍链接 |
|---|---|---|---|---|---|
| S905L3A | 创维 E900V22C | 4 脚 | 5 脚 | 6 脚 | [图1](https://znds.com/tv-1247549-1-1.html)[^37^] |
| Hi3798MV300 | 长虹 CM201-2 | TP9 | TP10 | TP11 | [图2](http://blog.csdn.net/qq_25796247/article/details/140498864)[^42^] |
| RK3566 | CM311-1SA | 圆点 GND | CLK 旁 TX | CLK 旁 RX | [图3](https://dmm.ink/short/rk3566) |
| GK6323 | 部分 M101 | 2 脚 | 3 脚 | 4 脚 | 孔太小可用 **缝衣针+热熔胶**固定[^42^] |

> 小技巧：主板丝印 **GND/TX/RX** 缺失时，**万用表**蜂鸣档找地，再 TTL 上电跑码，**有输出即正确**。

---

## 3️⃣ 跑码：3 步判断盒子生命体征
1. 打开 Putty → Session → Serial → COMx → Speed **115200** → Open  
2. 盒子通电 → 立刻狂按 **Ctrl+C**（海思）或 **空格**（晶晨）  
3. 出现以下任意提示即可继续刷机：

hisilicon #          ← 海思已进 uboot

=>                     ← 晶晨已进 fastboot

MStar

 若持续乱码：① 调换 TX/RX ② 检查波特率 ③ 换 USB 口。

---

## 4️⃣ 刷机：4 条主流芯片 TTL 命令

#### ① 晶晨 S905L3A/B（创维/九联/烽火）

fastboot devices

fastboot flash boot boot.img

fastboot flash system system.img

fastboot reboot

#### ② 海思 Hi3798MV300/310（长虹/华为悦盒）

setenv serverip 192.168.1.100

setenv ipaddr 192.168.1.101

saveenv

电脑 HiTool → 网口烧写 → 导入 fastboot-burn.bin → 点开始 → 盒子通电即自动烧录

#### ③ 瑞芯微 RK3566（CM311-1SA）

TTL 无需命令，按住 Recovery + 通电 → 电脑 RKDevTool 提示 Found Maskrom → 点「执行」即可。

#### ④ 国科微 GK6323（无 fastboot）
只能跑码后 mount -o remount,rw /system → ADB 装桌面，不可整包刷机。

## 5️⃣ 救砖：TTL 万能 3 连
| 现象       | TTL 现象              | 解决                     |
| -------- | ------------------- | ---------------------- |
| 卡开机 Logo | 跑码停住 `kernel panic` | 重刷 `boot.img`          |
| 黑屏无 HDMI | 只有 `BL2` 无 `kernel` | 刷「原厂完整包」               |
| 无限重启     | 循环 `=> reboot`      | 进 uboot → `reset` → 重刷 |

若 TTL 也乱码：短接 CLK-GND 进 Maskrom → 线刷底层包即可复活。

## 6️⃣ 一键校验：TTL 是否连接成功
电脑端（PowerShell）

mode COM3: baud=115200 data=8 parity=n stop=1

echo >COM3: "test"

盒子端 Putty 立即显示 test → 硬件通路 OK，可继续刷机。

## 🔚 一句话总结
TTL 只接 3 线，波特率 115200，跑码能停就成功；
晶晨 fastboot、海思 HiTool、瑞芯微 Maskrom，看清芯片再动手！

