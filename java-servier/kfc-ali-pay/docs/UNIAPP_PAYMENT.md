# UniApp 支付接入

App 端拿到本服务返回的 `provider` 和 `orderInfo` 后调用：

```javascript
uni.requestPayment({
  provider: payment.provider,
  orderInfo: payment.orderInfo,
  success: () => reportClientResult('SUCCESS'),
  fail: (error) => {
    const cancelled = /cancel/i.test(error.errMsg || '')
    reportClientResult(cancelled ? 'CANCELLED' : 'FAILURE')
  },
  complete: () => pollKfcPaymentStatus(payment.bizId)
})
```

安全要求：

- `success` 只代表支付宝客户端回调，不是 KFC 服务端最终结果。
- 前端必须继续查询本服务，直到 KFC 订单状态为 `102`、`504` 或明确失败。
- 不把 KFC `client_sec`、DES key、登录 token、完整请求签名逻辑放入 UniApp。
- H5 平台没有统一封装 App 支付；应使用 KFC/支付宝正式支持的 H5 支付渠道，不能长期复用抓包得到的一次性链接。

官方参考：

- https://uniapp.dcloud.net.cn/api/plugins/payment.html
- https://uniapp.dcloud.net.cn/tutorial/app-payment-alipay.html
