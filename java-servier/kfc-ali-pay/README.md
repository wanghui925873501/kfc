# kfc-ali-pay

独立的 KFC 支付宝支付编排子项目。设计参考 `local-life-ali-pay-v2` 的幂等账本、提交边界、结果对账和 CDP 隔离思路，但不自动输入支付密码、不点击最终付款。

## 正确的 UniApp 支付主流程

1. `java-servier` 使用当前 KFC 登录会话调用 `/cashier/unifiedOrderPay`。
2. 将响应中的 `paymentContent` 交给本服务创建幂等支付会话。
3. UniApp App 端使用 `uni.requestPayment({ provider: 'alipay', orderInfo })` 唤起支付宝。
4. UniApp 把 success/fail/cancel 作为提示上报；本服务统一进入 `CONFIRMING`，不直接认定支付结果。
5. 本服务通过 `java-servier` 查询 KFC `/cashier/preorder/queryOrderDetail`。
6. KFC 返回 `102 预约成功`时才记为 `PAID`；返回 `504 已取消`时记为 `CANCELLED`。

这部分可以自动化。用户仍必须在支付宝官方界面确认真实付款；服务不会代替用户输入密码或绕过支付确认。

## 接口

- `POST /kfc-ali-pay/api/v1/payments`：创建幂等支付会话，返回 UniApp `provider/orderInfo`。
- `POST /kfc-ali-pay/api/v1/payments/{bizId}/launched`：标记已唤起支付宝。
- `POST /kfc-ali-pay/api/v1/payments/{bizId}/client-result`：记录非权威客户端结果。
- `GET /kfc-ali-pay/api/v1/payments/{bizId}?refresh=true`：查询并按需刷新状态。
- `POST /kfc-ali-pay/api/v1/payments/{bizId}/reconcile`：可信内部服务写入 KFC 查询结果。
- `POST /kfc-ali-pay/api/v1/payments/{bizId}/browser-handoff`：可选 CDP 打开支付宝 H5 页面，只做人工交接。

## 本地启动

```powershell
Set-Location 'D:\wanghui\kfcuniapp\java-servier'
mvn -s .mvn/settings.xml -pl kfc-ali-pay -am test
mvn -s .mvn/settings.xml -pl kfc-ali-pay spring-boot:run
```

默认端口 `8301`，健康检查：

```text
GET http://127.0.0.1:8301/kfc-ali-pay/actuator/health
```

## 配置边界

- `KFC_ORDER_RECONCILIATION_ENABLED=true` 后才会定时访问自有 `java-servier` 订单查询接口。
- `KFC_ALI_PAY_CDP_ENABLED=true` 后才允许 CDP 浏览器交接。
- `paymentContent/paymentUrl` 都是一次性敏感支付数据，不写日志、不放 Git、不长期缓存。
- 当前账本是开发用内存实现；生产必须替换为数据库唯一索引与持久化状态机。

## 风控验证码

KFC 订单风控不是支付宝能力，不能由本项目代解。合规实现见 [GeeTest v3 接入边界](docs/GEE_TEST_V3_FLOW.md)。
