# Java 服务前端对接文档（手机号通行与短信登录）

> 文档版本：2026-10-08  
> 对应模块：`java-servier/server`  
> 当前可正式对接：手机号短信发送、手机号验证码登录  
> 鉴权方式：内部系统以手机号作为唯一通行参数

## 1. 服务地址与在线文档

本地默认端口为 `19111`，前端按实际环境替换主机名：

```text
http://<server-host>:19111
```

服务启动后可查看：

- Swagger UI：`http://<server-host>:19111/swagger-ui.html`
- OpenAPI JSON：`http://<server-host>:19111/v3/api-docs`

## 2. 当前接口清单

| 接口 | 方法 | 是否允许未登录手机号访问 | 对接状态 |
| --- | --- | --- | --- |
| `/api/v1/login/sms-code/send` | POST | 是 | 可正式对接 |
| `/api/v1/login/sms-code/login` | POST | 是 | 可正式对接 |
| `/api/v1/catalog/menu` | POST | 否 | 仅联通骨架，字段和上游地址待验证，前端暂勿对接 |

目前没有正式的门店接口。后续菜单、门店接口上线后，同样只需携带手机号，前端不需要接触 KFC token 和设备信息。

## 3. 手机号通行规则

### 3.1 基本规则

- 所有 `/api/v1/**` 请求都必须携带 `phone`。
- 手机号格式与当前后端校验完全一致：`^1[0-9]{10}$`。
- POST JSON 请求将 `phone` 放在请求体顶层。
- GET 或没有 JSON 请求体的请求使用查询参数 `?phone=...`。
- 不要同时在查询参数和 JSON 请求体中传两个不同手机号；后端优先读取查询参数。
- 不使用 `Authorization`、Cookie 或 `sessionId` 判断业务登录态。
- 登录响应中的 `session` 是兼容字段，前端可以忽略；后续业务请求仍然只传手机号。

POST 请求格式：

```json
{
  "phone": "13800000000",
  "其他业务字段": "..."
}
```

GET 请求格式：

```text
GET /api/v1/example?phone=13800000000&其他参数=...
```

### 3.2 后端如何判断已登录

每次请求进入 Controller 前，后端都会根据手机号查询本地 `kfc_user`：

- 用户及完整 token、安装、设备数据都存在：视为已登录，建立本次请求上下文。
- 没有 `kfc_user`，或 token 明文和密文都为空：视为未登录。
- token 明密文缺少一项、两者不一致，或安装/设备数据缺失：返回 HTTP 409 `LOGIN_STATE_CORRUPTED`。

登录接口允许未登录手机号继续执行发码或验证码登录；其他业务接口未登录时返回 HTTP 401 `LOGIN_REQUIRED`。

## 4. 统一响应结构

### 4.1 成功响应

HTTP 状态码为 2xx，且响应体 `code` 为 `OK`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

### 4.2 失败响应

```json
{
  "code": "LOGIN_REQUIRED",
  "message": "该手机号尚未登录",
  "data": null
}
```

前端必须同时判断 HTTP 状态码和响应体 `code`，不能只判断 HTTP 200。

## 5. 推荐登录流程

```text
输入/取得手机号
       │
       ▼
调用发送验证码接口
       │
       ├─ loggedIn=true ──────────────► 直接进入业务页面
       │
       └─ nextAction=ENTER_SMS_CODE
                    │
                    ▼
              展示验证码输入框
                    │
                    ▼
              调用验证码登录接口
                    │
                    ▼
                进入业务页面
```

前端判断以 `loggedIn` 为主。收到 `loggedIn=true` 后不要再调用验证码登录接口。

## 6. 发送短信验证码

### 6.1 请求

```http
POST /api/v1/login/sms-code/send
Content-Type: application/json
```

```json
{
  "phone": "13800000000"
}
```

字段说明：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `phone` | string | 是 | 11 位，以 `1` 开头；正则 `^1[0-9]{10}$` |

### 6.2 未登录且发码成功

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "loggedIn": false,
    "smsSent": true,
    "nextAction": "ENTER_SMS_CODE",
    "session": null
  }
}
```

前端行为：展示验证码输入框并开始本地倒计时。

### 6.3 数据库已有登录态

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "loggedIn": true,
    "smsSent": false,
    "nextAction": "USE_SESSION",
    "session": {
      "sessionId": "兼容字段，前端可忽略",
      "installationId": "兼容字段，前端可忽略",
      "expiresInSeconds": 28800
    }
  }
}
```

此时后端不会调用 KFC 发码接口。前端直接进入业务页面，不展示验证码输入框。

### 6.4 返回字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `loggedIn` | boolean | 当前手机号是否已经具备完整本地登录态 |
| `smsSent` | boolean | 本次请求是否实际确认发送了短信 |
| `nextAction` | string | `ENTER_SMS_CODE` 或兼容枚举值 `USE_SESSION` |
| `session` | object/null | 兼容字段；发码时为 null，已有登录态时有值 |

## 7. 手机号验证码登录

### 7.1 请求

```http
POST /api/v1/login/sms-code/login
Content-Type: application/json
```

```json
{
  "phone": "13800000000",
  "smsCode": "123456"
}
```

字段说明：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `phone` | string | 是 | 11 位，以 `1` 开头；正则 `^1[0-9]{10}$` |
| `smsCode` | string | 是 | 6 位数字；正则 `^[0-9]{6}$` |

即使该手机号可能已经登录，此接口在进入业务方法前仍会校验 `smsCode`。因此前端收到发码接口的 `loggedIn=true` 后，不要调用本接口。

### 7.2 登录成功

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "reusedStoredToken": false,
    "session": {
      "sessionId": "兼容字段，前端可忽略",
      "installationId": "兼容字段，前端可忽略",
      "expiresInSeconds": 28800
    }
  }
}
```

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `reusedStoredToken` | boolean | `false` 表示本次调用 KFC 登录成功；`true` 表示复用数据库已有 token |
| `session` | object | 兼容字段，前端无需保存或携带 |

验证码只用于当次登录，不会持久化。

## 8. 前端状态与异常处理

### 8.1 建议状态机

| 当前场景 | 判断条件 | 前端行为 |
| --- | --- | --- |
| 发码接口发现已登录 | `loggedIn=true` | 直接进入业务页面 |
| 短信已发送 | `smsSent=true` 且 `nextAction=ENTER_SMS_CODE` | 展示验证码输入框并倒计时 |
| 验证码登录成功 | HTTP 2xx 且 `code=OK` | 进入业务页面 |
| 业务接口要求登录 | HTTP 401 且 `code=LOGIN_REQUIRED` | 返回手机号登录流程，并先调用发码接口检查状态 |
| 本地数据异常 | HTTP 409 且 `code=LOGIN_STATE_CORRUPTED` | 停止自动重试，提示联系后端处理 |
| 操作过于频繁 | HTTP 429 | 禁止连点，按页面倒计时后再允许操作 |

### 8.2 不要自动重放登录请求

短信发送和验证码登录都有外部副作用及服务端防重复窗口。网络超时或 5xx 时不要在请求库中自动重试，应提示用户手动确认后再次操作。

## 9. 错误码

| HTTP | code | 触发场景 | 前端处理建议 |
| --- | --- | --- | --- |
| 400 | `PHONE_REQUIRED` | 未携带手机号 | 提示填写手机号 |
| 400 | `INVALID_PHONE` | 手机号不符合 `^1[0-9]{10}$` | 提示检查手机号 |
| 400 | `INVALID_REQUEST` | JSON 非法、验证码缺失或参数校验失败 | 提示检查输入，不要原样展示技术异常 |
| 401 | `LOGIN_REQUIRED` | 普通业务接口对应手机号未登录 | 进入短信登录流程 |
| 409 | `LOGIN_STATE_CORRUPTED` | 本地 token、用户、安装或设备数据不一致 | 停止自动重试，联系后端处理 |
| 422 | `SMS_CODE_LOGIN_REJECTED` | KFC 未确认验证码登录成功 | 展示后端提示，允许用户重新输入验证码 |
| 429 | `LOGIN_BUSY` | 同一手机号已有登录请求处理中 | 禁止重复点击，稍后重试 |
| 429 | `SMS_SEND_TOO_FREQUENT` | 发码冷却期未结束 | 保持倒计时，稍后再发 |
| 429 | `LOGIN_TOO_FREQUENT` | 验证码登录提交过于频繁 | 禁止重复提交，稍后重试 |
| 502 | `SMS_SEND_REJECTED` | KFC 未确认短信发送成功 | 提示发送失败，不要自动重试 |
| 502 | `UPSTREAM_ERROR` | KFC 上游连接或处理失败 | 提示服务暂不可用，允许稍后手动重试 |
| 503 | `LOGIN_GUARD_UNAVAILABLE` | Redis 登录保护不可用 | 提示服务暂不可用 |
| 503 | `LOCAL_USER_SAVE_FAILED` | 本地用户保存失败 | 提示服务暂不可用并联系后端 |
| 503 | `UPSTREAM_DISABLED` | 上游或本地必要配置未就绪 | 联系后端检查部署配置 |

后端 `message` 可用于提示，但前端业务分支必须以稳定的 `code` 为准。

## 10. UniApp 请求封装示例

```js
const API_BASE_URL = 'http://<server-host>:19111'

export function postApi(path, payload) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${path}`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: payload,
      success(response) {
        const body = response.data
        if (response.statusCode >= 200
          && response.statusCode < 300
          && body
          && body.code === 'OK') {
          resolve(body.data)
          return
        }
        reject({
          statusCode: response.statusCode,
          code: body?.code || 'NETWORK_ERROR',
          message: body?.message || '请求失败'
        })
      },
      fail(error) {
        reject({
          statusCode: 0,
          code: 'NETWORK_ERROR',
          message: error.errMsg || '网络连接失败'
        })
      }
    })
  })
}

export function sendSmsCode(phone) {
  return postApi('/api/v1/login/sms-code/send', { phone })
}

export function loginBySmsCode(phone, smsCode) {
  return postApi('/api/v1/login/sms-code/login', {
    phone,
    smsCode
  })
}
```

页面调用示例：

```js
const sendResult = await sendSmsCode(phone)

if (sendResult.loggedIn) {
  // 直接进入业务页面
  return
}

if (sendResult.nextAction === 'ENTER_SMS_CODE') {
  // 展示验证码输入框并开始倒计时
}

await loginBySmsCode(phone, smsCode)
// 登录成功，进入业务页面；后续请求继续传 phone
```

## 11. TypeScript 类型参考

```ts
export interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
}

export interface LoginSession {
  sessionId: string
  installationId: string
  expiresInSeconds: number
}

export type LoginNextAction = 'ENTER_SMS_CODE' | 'USE_SESSION'

export interface SendSmsCodeResult {
  loggedIn: boolean
  smsSent: boolean
  nextAction: LoginNextAction
  session: LoginSession | null
}

export interface SmsCodeLoginResult {
  reusedStoredToken: boolean
  session: LoginSession
}
```

`LoginSession` 仅为兼容当前后端响应定义，前端不需要持久化，也不需要在后续请求中传回。

## 12. 联调验收清单

- 未填写手机号时，前端阻止提交。
- 发码按钮请求中不可重复点击。
- `loggedIn=true` 时不显示验证码输入框，不调用验证码登录接口。
- `loggedIn=false` 且 `smsSent=true` 时才进入验证码输入流程。
- 验证码严格按字符串传递，保留可能存在的前导零。
- 后续 `/api/v1/**` 业务请求均携带顶层 `phone`。
- 收到 `LOGIN_REQUIRED` 时回到登录流程。
- 收到 `LOGIN_STATE_CORRUPTED` 时不循环发码或登录。
- 不保存、不展示 KFC token；后端响应也不会下发该 token。
- 不对短信发送或验证码登录配置自动重试。

