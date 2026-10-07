# KFC Android 抓包实验室

本目录已经落地一套隔离的 Android 模拟器与 HTTPS 抓包环境，用于在本机测试账号下观察 `KFC_Brand.apk` 的真实请求链路。

## 当前状态

- Android Emulator 37.2.12，AVD 为 Android 6.0 / API 23 / x86。
- APK 包名：`com.yek.android.kfc.activitys`。
- mitmproxy 12.2.3，设备代理为 `10.0.2.2:8080`。
- 已验证普通 HTTPS 可解密，并捕获到 KFC 资源、配置、定位和日志域名的真实请求。
- `appcoupon.kfc.com.cn`、`dynamicad.kfc.com.cn`、`resmkt.kfc.com.cn` 等连接出现独立证书校验失败。这说明不同网络栈/域名的信任策略并不一致；本环境不会承诺所有链路都能直接解密。

## 一键启动

在 PowerShell 中执行：

```powershell
Set-Location 'D:\wanghui\kfcuniapp'
& '.\android\lab\scripts\Start-KfcCaptureLab.ps1'
```

脚本会完成以下工作：

1. 启动 mitmweb，并创建一个带时间戳的 `.mitm` 原始流文件。
2. 启动 Android 模拟器并等待系统启动完成。
3. 在尚未安装时安装 APK。
4. 设置模拟器代理。
5. 把 mitmproxy CA 写入这个测试模拟器的系统证书目录。
6. 启动 KFC App。

启动脚本会输出带临时访问令牌的 `Capture UI` 地址；复制该完整地址到浏览器，即可实时查看请求、请求头、请求体和响应内容。也可随时执行状态脚本查看当前地址。

如需清空 App 数据后重走首次启动流程：

```powershell
& '.\android\lab\scripts\Start-KfcCaptureLab.ps1' -ResetApp
```

`-ResetApp` 会删除模拟器内该 App 的登录态和本地数据，不会删除 APK、源码或抓包文件。

## 查看状态与停止

```powershell
& '.\android\lab\scripts\Show-KfcCaptureStatus.ps1'
& '.\android\lab\scripts\Save-KfcCapture.ps1'
& '.\android\lab\scripts\Stop-KfcCaptureLab.ps1'
```

保存脚本会把 mitmweb 内存中的当前流量立即写入本次 `.mitm` 文件。停止脚本会先自动执行一次保存，再关闭模拟器和代理，并保留所有 `.mitm` 文件。

## 推荐的实际采集顺序

每个业务阶段单独启动一次实验室，方便按流文件区分：

1. 首次启动及隐私授权。
2. 手机号短信登录/注册。
3. 首页初始化。
4. 定位与选择餐厅。
5. 进入菜单、切换分类、查看商品。
6. 购物车与提交订单前页面。

验证码必须由账号持有人本人在模拟器中输入。不要进行真实付款；支付页只观察到提交订单前为止。

## 数据安全

`.mitm` 文件可能包含手机号、登录令牌、Cookie、设备标识、定位和签名字段，已经被 `.gitignore` 排除。不要把原始流文件提交到 Git、发到群聊或上传到第三方服务。分析报告只记录接口结构和脱敏后的样例。

## 证书与局限

这个 API 23 测试镜像允许 `adb root`。CA 会在每次启动脚本时重新写入，因为模拟器重启后 `/system` 的临时改动可能消失。普通系统网络栈已经验证可用；若某个域名仍显示 `Client TLS handshake failed`，通常意味着 App 使用了独立 TrustManager、证书固定或另一套网络 SDK。此时仍可保留域名、时间、连接结果和静态源码证据，但不在没有明确授权和风险评估的情况下绕过它。
