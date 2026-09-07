# 激素监测（HRT Monitor）

一款**本地优先、无账户、无广告**的应用，用于跨性别女性性激素六项化验结果的记录、监测与报告。提供**安卓 APP**（Kotlin + Jetpack Compose）与**网页版 PWA**（零依赖静态站点，可部署到 Cloudflare Pages，iPhone/iPad 可添加到主屏幕），两平台功能一致、备份数据格式互通。

> 🌐 **在线使用（网页版）**：<https://hrt.isomeria.top>

## 功能

- **化验记录**：雌二醇（E2）、睾酮（T）、促黄体生成素（LH）、促卵泡生成激素（FSH）、催乳素（PRL）、孕酮（P4）六项指标，每项支持多种单位（pg/mL、pmol/L、ng/mL、ng/dL、nmol/L、IU/L、mIU/mL、µg/L），录入时自动换算为统一单位。
- **趋势图表**：自绘折线图，任意指标按时间展示趋势，支持 全部/近1年/近6月/近3月 时间范围与显示单位切换；参考范围以色带叠加显示，点击数据点查看详情。
- **参考范围管理**：内置常见参考范围（含 GAHT 目标范围）作为起点，支持自定义范围、图表显示开关、设置「目标范围」；首页与报告据此判断 偏低/范围内/偏高。
- **生活事件时间线**：开始 HRT、用药调整、更换医生、手术等关键节点。
- **个人资料**：可选填生日与 HRT 起始日期（首启引导、可跳过、设置中可改），每条记录自动显示「血检时年龄 · HRT 已进行时长」。
- **报告**：本地统计报告（一键生成、复制、分享给医生）+ AI 智能解读（OpenAI 兼容接口，地址/密钥/模型均由用户自行填写，默认预置 DeepSeek）。
- **数据导入/导出**：JSON 全量备份（合并/替换两种导入方式）与 CSV 记录导出，便于换机与分享。
- **隐私**：所有数据仅存本机应用私有目录，无账户体系，不上传任何服务器（仅在用户主动触发 AI 解读时向用户指定的接口发送数据摘要）。
- **其他**：暗色模式（跟随系统/手动），简体中文/繁體中文/跟随系统。
- **免责声明**：首次启动强制确认，报告页与设置页显著展示——所有数据与解读仅供参考，不能替代专业医疗建议。

## 安装 APK

构建产物：`app/build/outputs/apk/release/app-release.apk`（R8 压缩混淆后的 release 签名包）。

将其复制到手机后点击安装即可（需允许「安装未知来源应用」）。签名密钥见 `keystore/hrt-monitor.jks`，密码均为 `hrt-monitor`（个人使用；如需发布应用商店请自行更换密钥并妥善保管）。

## 构建

本机自带工具链位于 `.toolchain/`（Temurin JDK 17、Android SDK 35、Gradle 8.11.1），无需系统级安装：

```bash
export JAVA_HOME="$PWD/.toolchain/jdk-17.0.20.1+1"
export ANDROID_HOME="$PWD/.toolchain/sdk"
export GRADLE_USER_HOME="$PWD/.toolchain/gradle-home"

./.toolchain/gradle-8.11.1/bin/gradle assembleRelease   # 构建 APK
./.toolchain/gradle-8.11.1/bin/gradle test              # 运行单元测试（单位换算）
```

技术栈：Kotlin 2.0 + Jetpack Compose（Material 3）+ 自绘 Canvas 图表；数据以 JSON 文件存于应用私有目录，无 Room/网络依赖（AI 模块除外）。

> 从仓库克隆后构建前需先生成签名密钥（本仓库不含密钥文件）：
>
> ```bash
> mkdir -p keystore
> keytool -genkeypair -v -keystore keystore/hrt-monitor.jks -alias hrt-monitor \
>   -keyalg RSA -keysize 2048 -validity 10000 \
>   -storepass hrt-monitor -keypass hrt-monitor \
>   -dname "CN=HRT Monitor, OU=Personal, O=Personal, L=Local, C=CN"
> ```

## 数据位置与备份

- 数据文件：`Android/data/com.hrt.monitor/files/hrt_data.json`（应用私有目录，卸载即删除）
- 建议定期在「设置 → 数据管理」中**导出 JSON 备份**并保存到网盘或电脑，换机时用「导入备份」恢复。

## 网页版（PWA，iPhone / iPad / 桌面可用）

**在线地址：<https://hrt.isomeria.top>**（Cloudflare Pages 部署，随本仓库 `main` 分支推送自动更新）

`web/` 目录为零依赖的静态 PWA，功能与安卓版一致（记录、图表、参考范围、时间线、个人资料、报告、AI 解读、导入导出、简繁中文、暗色模式），且**数据格式与安卓版互通**：安卓导出的 JSON 备份可直接在网页版「设置 → 导入备份」中恢复，反之亦然。所有数据仅存浏览器本机（localStorage），无服务器。

本地预览：

```bash
cd web && python3 -m http.server 8080   # 浏览器打开 http://localhost:8080
```

部署到 Cloudflare Pages：

1. Cloudflare 控制台 → **Workers 和 Pages** → 创建 → **Pages** → 连接到 Git → 选择 `KokoroLyase/HRT-Monitor`
2. 构建配置：Framework preset 选 **None**；构建命令**留空**；构建输出目录填 **`web`**
3. 保存并部署，之后每次推送到 `main` 会自动重新部署

iPhone 使用：用 Safari 打开部署后的地址 → 分享按钮 → **添加到主屏幕**，即可像 App 一样全屏使用（应用外壳会被 Service Worker 缓存，弱网/离线也能打开）。

## 免责声明

本应用仅用于自我记录与参考：图表、参考范围与解读不构成诊断依据；任何用药调整或医疗决策，请务必咨询医生。
