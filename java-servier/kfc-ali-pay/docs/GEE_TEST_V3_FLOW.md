# GeeTest v3 风控接入边界

本文件约束订单提交链路。用户于 2026-10-08 单独授权的登录短信链路使用其本机“小辉极验3代”服务，代码和开关位于 `kfcapi` 与 `server` 的 `AppLoginCaptchaService`；订单链路没有接入该服务。

## KFC 实测协议

首次订单提交返回 `5910060/5910061` 时，响应包含：

- `eventId`
- `geeRegisterData.gt`
- `geeRegisterData.challenge`
- `geeRegisterData.success`

真人在官方 GeeTest 组件中验证成功后取得：

- `geetest_challenge`
- `geetest_validate`
- `geetest_seccode`

将三项序列化为 `riskCtrlParam.validateCaptcha`，同时放回首次响应的 `eventId`，再以相同订单、登录会话和设备上下文重试 `/api/v2/order/submit`。

## 可以自动化的部分

- 自动识别 `5910060/5910061`。
- 自动保存一次性 `eventId/gt/challenge/success`，并设置短过期时间和单次消费。
- 自动打开 UniApp WebView 中的 GeeTest v3 官方组件。
- 官方组件成功后自动回传三个验证票据。
- 后端自动把票据合入原始 `riskCtrlParam` 并只重试一次订单提交。
- 处理挑战过期、关闭、失败与重复提交。

## 不能做的部分

- 识图脚本自动点选。
- 验证码平台、打码服务或浏览器机器人代替最终用户操作。
- 伪造 `validate/seccode/w`、复用历史票据或绕过设备与行为校验。
- 将 KFC 返回的挑战转交第三方程序处理。

这些做法是在规避第三方反滥用控制，稳定性也很差：票据与 challenge、设备环境、行为、时间和业务事件绑定。

## 产品体验建议

使用弹出式或绑定式官方组件：正常低风险订单完全不展示验证码；只有收到 KFC 风控响应时才弹出。用户完成一次挑战后，票据回传和订单重试全部自动进行。若业务方希望真正的“无感验证”，只能由 KFC/极验在其商户配置中启用相应的风险策略，客户端不能自行把图文点选改成无感通过。

官方资料：

- GeeTest v3 接入流程：https://docs.geetest.com/captcha/guide
- GeeTest v3 Web API：https://docs.geetest.com/captcha/apirefer/api/web
- GeeTest v3 总览：https://docs.geetest.com/captcha/overview/start/
