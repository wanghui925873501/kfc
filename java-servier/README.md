# KFC uni-app 后端骨架

代码与注释规范见 [AGENTS.md](AGENTS.md)。所有公开 API 执行 Javadoc 检查。

这是一个多 Maven 模块的 Spring Boot 项目。核心业务由 `server` 单体部署；`kfc-ali-pay` 是可独立启动的支付宝支付编排子项目。其余模块用于固定依赖方向，不单独部署。

```text
java-servier/
├─ common/   统一返回等共享类型
├─ db/       本地 MySQL 的用户、安装记录、上游身份映射及迁移 SQL
├─ kfcapi/   第三方域名登记、HTTPS 传输、鉴权和按真实域名划分的接口封装
├─ kfc-ali-pay/ 支付宝支付会话、状态核验及可选 CDP 人工接管编排
└─ server/   uni-app 调用的 MVC 接口、内部业务编排、Redis 配置、启动入口
```

依赖方向：`server → db/kfcapi/common`，`db/kfcapi → common`。外部接口只从 `server` 暴露；不开放任意 URL 或路径透传。技术版本：JDK 21、Spring Boot 3.5.12、Springdoc OpenAPI 2.8.17、MyBatis-Plus 3.5.17、Lombok 1.18.48、Hutool 5.8.47、Fastjson2 2.0.65、MySQL、Redis、Spring MVC。实体和 DTO 使用成员变量与 Lombok `@Data`，便于逐项设置字段。

## 上游域名证据与包名

目前真实 Reqable 请求已确认短信登录使用 `https://applogin.kfcapp.cn`，选店和点餐使用 `https://rnorder.kfc.com.cn`。新增的 `apploginkfcappcn` 包只放前一个域名的接口，含 `api`、`param`、`vo`、`support`。先前按静态配置创建的几个空包不能视为对应接口已联调；后续应按每条真实请求的域名确定包名和路径。`login.kfc.com.cn` 与 `applogin.kfcapp.cn` 是两个不同上游。

原有五个候选地址仍保留为静态证据登记：

| 配置键 | 当前默认值 | APK 中的证据 | 接口归属可信度 |
| --- | --- | --- | --- |
| `ordering` | `https://order.kfc.com.cn` | `kfc-ordering-delivery/modules/01122.js` 的预点餐页面 URL；`01517.js` 有 `/api/v2/menu/list` POST | 页面域名已见，菜单接口是否同域**待抓包确认** |
| `login` | `https://login.kfc.com.cn` | `01290.js` 的 `baseDomain` 与 `KBS/api`；`01249.js` 有 `/user/token/valid` POST | 配置和路径均有证据，完整协议待确认 |
| `coupon` | `https://appcoupon.kfc.com.cn` | `01290.js` 的 `baseDomainCoupon`；`KFC_Coupon_App/modules/02027.js` 的 `domainCode: appcoupon` + POST | 路由有证据，参数与鉴权待确认 |
| `mall` | `https://appmall.kfc.com.cn` | `01290.js` 的 `baseDomainAppmall`；`kfc-ordering-delivery/modules/02625.js` 的 `domainCode: appmall` + GET | 路由有证据，参数与鉴权待确认 |
| `prime` | `https://appprime.kfc.com.cn` | `01290.js` 的 `baseDomainPrime`；`KFC_App_Activity/modules/03179.js` 的 `domainCode: appprime` + GET | 路由有证据，参数与鉴权待确认 |

APK 里还有 `m.4008823823.com.cn`、`appcommon.kfc.com.cn` 等候选。上述五个地址仅是首批静态建模，并非 APK 只访问这些域名。新增接口时，先核对 Reqable 实际 URL，再在 `kfcapi` 中增加固定路径的方法，并记录来源、HTTP 方法、请求字段、鉴权规则、响应样本。`UpstreamGateway` 只供这些内部封装使用。

## 当前可调用骨架

- `POST /api/v1/catalog/menu` → `CatalogService` → 旧 `OrderingApi.menuList` 仍指向静态候选 `order.kfc.com.cn`，仅作联通样例；真实菜单抓包 URL 属于 `rnorder.kfc.com.cn/preorder-portal`，后续应按该域名重做客户端，当前样例不要用于真实菜单请求。
- `kfcapi` 已新增 `rnorderkfccomcn` 底层客户端，固定使用真实抓包确认的 `https://rnorder.kfc.com.cn`。当前实现预点餐会话初始化、定位反查城市、城市列表、附近门店、用户常用门店、门店校验、菜单列表和商品详情；尚未接入任何 `server` Controller/Service，UniApp 当前不能调用这些方法。旧 `/api/v1/catalog/menu` 骨架也没有改接新客户端。
- RN 点餐客户端会从初始化响应保存 `sessionId`、`Set-Cookie` 和 `x-yumc-route-cell`，后续调用复用同一会话。经纬度按抓包 `encodeList` 使用既有 DES 协议加密；签名请求使用去掉 `/store-portal` 或 `/preorder-portal` 的 `/api/...` 短路径。门店查询中抓包未携带 `kb*` 的固定路径保持无签名，未知路径继续拒绝。
- RN 点餐调用上下文只接收后端已核对的 `deviceId`、`userCode`、上游 ticket、城市和 User-Agent；会话 Cookie、签名头、公共协议字段及加密后的定位均由 `kfcapi` 构造。`RN_ORDER` 默认域名写在 `UpstreamProperties` 中，如确需测试环境覆盖可配置 `kfc.upstream.urls.rn-order`，仍受 HTTPS 和无路径基础地址校验限制。
- `AppLoginApi.sendSmsCode` 和 `AppLoginApi.loginBySmsCode` 使用已抓包确认的 `https://applogin.kfcapp.cn` 两个固定 POST 路径；请求字段、DES 加密和 POST 签名已本地实现。它们只由后端登录编排服务调用，UniApp 不能直接传入上游路径、请求头或设备字段。Java 使用之前授权抓包的安装上下文曾发送成功，返回 `errCode=0`；新生成的独立虚拟设备上下文尚未完成逐条 Reqable 联调，仍以上游当次响应为准。
- `LoginApi.validateToken`、`CouponApi.availableCoupons`、`MallApi.productByActivityId`、`PrimeApi.userCard` 是内部封装，**尚未向 uni-app 暴露**。它们的路径/方法来自静态 bundle；完整请求和鉴权仍需联调。会员卡方法的上游 token 应由服务端会话安全取得，不要让 uni-app 任意传入。
- 上游开关默认关闭。`APP_LOGIN` 仅在配置本机凭据且开启上游开关后可发送；其他候选域名仍会因未配置鉴权而拒绝请求。实际短信发送与登录还需要上层完成当次确认、限流和单次触发锁，不要把密钥或上游 token 提交到仓库或下发给 UniApp。
- `db/src/main/resources/db/migration/` 保留需手动执行的 V1–V6 SQL：V1–V4 建立并注释原有三张表，V5 新增 `kfc_phone_installation`，V6 在确认无重复记录后为 `kfc_user(brand, phone_plain)` 增加唯一索引。新库需按版本顺序各执行一次；已有库只执行尚未应用的脚本。Java 服务不会自动执行这些 SQL。手机号明文及同值 DES 密文仅供有数据库权限的本机核对；验证码不落库。
- `KfcIdentityService` 提供安装注册/更新、登录结果关联、后续请求上下文读取；成功登录时调用方须把同次登录的手机号传给 `recordLoginSuccess`，并与上游响应一起保存。`KfcRequestContextService` 在每次业务请求中按手机号从 `kfc_user` 加载持久化 token、安装与设备信息。`KfcSessionStore` 仍为兼容已有登录响应而签发 Redis 会话，但后续业务接口不依赖它。
- 独立的 `KFC_SESSION_KEY_BASE64` 是 32 字节密钥的 Base64 编码，本机真实值存于 Git 忽略的根目录 `.env.kfc-reverse.local`。`server` 从相对于 `java-servier` 启动目录的该文件自动导入；其他运行目录可通过进程环境提供同名变量。没有密钥时服务可启动，但签发会话会明确失败。
- 登录 Controller 在后端内部按手机号维护安装上下文，没有向 UniApp 开放任意安装字段登记。所有 `/api/v1/**` 请求由 Spring MVC 拦截器读取顶层 `phone`；除标记为登录可选的接口外，必须能加载完整的用户、token、安装与设备上下文后才进入 Controller。城市、位置、版本、动态风控字段按当前后端客户端档案或已验证应用状态取得，不能长期套用抓包值。

## 前端短信登录接口与 OpenAPI

`server` 使用 Springdoc OpenAPI 3 生成接口文档。服务启动后可访问 `/swagger-ui.html`，OpenAPI JSON 位于 `/v3/api-docs`；可分别用 `SPRINGDOC_SWAGGER_UI_ENABLED` 和 `SPRINGDOC_API_DOCS_ENABLED` 关闭。登录 Controller、接口方法以及 `controller/login` 下的 `param`、`dto`、`vo` 已使用 `@Tag`、`@Operation`、`@ApiResponses` 和 `@Schema` 说明。

登录 Controller 的两个请求参数以 `BasePhoneParam` 为父类，手机号为必传顶层字段；后续新增的菜单、门店等参数类也应继承该父类。前端登录与后续接口的完整约定、请求响应示例和错误码见 [前端手机号通行与短信登录对接文档](docs/FRONTEND_PHONE_ACCESS_API.md)。

- `POST /api/v1/login/sms-code/send`：请求体仅接收手机号。锁内先查询 `kfc_user`；已有完整且一致的 token 时不请求 KFC，直接签发本地会话。未登录时才使用该手机号的独占安装上下文请求 KFC 发送验证码。
- `POST /api/v1/login/sms-code/login`：请求体接收手机号和六位验证码。锁内先查询 `kfc_user`；已有完整且一致的 token 时忽略验证码并直接签发本地会话。未登录时才请求 KFC 验证码登录，成功后创建或复用 `app_user`，保存 `kfc_user` 的手机号和 token 明密文并签发本地会话。

两个接口仍兼容返回本地随机 `sessionId`、绑定的 `installationId` 和本地会话有效期，但前端可以忽略这些字段；后续业务接口始终只携带手机号。接口不返回 KFC token、签名密钥、设备 ID、验证码或图形验证票据。若 token 明密文字段缺一、无法解密、不一致，或同手机号出现重复 KFC 映射，接口会失败关闭，不能把数据损坏当成未登录去调用 KFC。

同手机号的检查与上游调用由 Redis 分布式锁串行执行，并在锁内再次检查登录态。短信发送另有默认一分钟冷却，验证码登录有默认十秒防重复窗口；相应时长可通过 `KFC_LOGIN_MUTEX_TTL`、`KFC_LOGIN_SMS_COOLDOWN`、`KFC_LOGIN_VERIFY_COOLDOWN` 调整。登录客户端城市、版本和 User-Agent 由 `KFC_LOGIN_CITY_CODE`、`KFC_LOGIN_APP_VERSION`、`KFC_LOGIN_USER_AGENT` 在后端配置，前端不能任意覆盖。

所有 `/api/**` 请求默认输出成对的 `[API_REQUEST]`、`[API_RESPONSE]` 日志，包含 `requestId`、方法、URI、脱敏查询参数/JSON、HTTP 状态和耗时；同一 `requestId` 也通过 `X-Request-Id` 响应头返回。手机号仅保留前三后四，验证码、token、会话和设备标识替换为 `***`。可通过 `KFC_HTTP_LOG_ENABLED` 关闭，通过 `KFC_HTTP_LOG_MAX_BODY_LENGTH` 调整脱敏后单段日志的最大字符数。

## 启动

1. 安装 JDK 21、MySQL 和 Redis，创建数据库与用户。例如：

   ```sql
   CREATE DATABASE kfc_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'kfc_app'@'localhost' IDENTIFIED BY 'change-me';
   GRANT ALL PRIVILEGES ON kfc_app.* TO 'kfc_app'@'localhost';
   ```

2. 设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`；Redis 不在本机时再设 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。已验证的短信域名可用 `KFC_APP_LOGIN_URL` 覆盖。登录配置原先通过 `KFC_CLIENT_KEY`、`KFC_CLIENT_SEC`、`KFC_DES_KEY`、`KFC_LOGIN_REQUEST_SECRET_KEY` 环境变量读取；本机按用户当次明确要求，四项真实值已直接写入被 Git 跟踪的 `server/src/main/resources/application.yml`，不再使用这四个占位符。该文件现在含敏感值，不得提交或分享。其余候选地址仍可分别使用 `KFC_ORDERING_URL`、`KFC_LOGIN_URL`、`KFC_COUPON_URL`、`KFC_MALL_URL`、`KFC_PRIME_URL` 覆盖。准备好授权测试上下文后才设 `KFC_UPSTREAM_ENABLED=true`。
3. 先按上文手动准备 MySQL 表。在本目录运行 `mvn -s .mvn/settings.xml clean verify`，然后按需启动 `java -jar server/target/server-0.1.0-SNAPSHOT.jar` 或 `java -jar kfc-ali-pay/target/kfc-ali-pay-0.1.0-SNAPSHOT.jar`。两者均可检查 `GET /actuator/health`；启动过程不会执行建表或改表 SQL。支付子项目的接口和配置见 [`kfc-ali-pay/README.md`](kfc-ali-pay/README.md)。

项目内 `.mvn/settings.xml` 把 Maven 缓存放到当前工作目录的 `.m2`，避免使用开发机上无写权限的全局 Maven 仓库。

## 手动真实短信登录测试

`server/src/test/java/com/wanghui/kfc/server/api/apploginkfcappcn/AppLoginApiTest.java` 有两个独立方法：`sendSmsCodeTest` 负责发送验证码，`loginBySmsCodeTest` 负责用验证码登录并校验入库。两者都会先查 `kfc_user`，已有对应 token 就跳过请求。手机号写在各自方法内；登录方法内的 `smsCode` 需手动填入当次收到的验证码。

手动测试先按手机号 SHA-256 查 `kfc_phone_installation`；首次使用时生成专属 `kfc_installation`，之后复用。设备 ID 按 APK 的首次安装规则生成 UUID 加毫秒时间戳；`tdid` 使用 Android 9 无硬件标识分支的 `3 + MD5(随机 UUID)`，种子也保存于关联表。手机号明文及登录协议 DES 密文成对保存。城市编码、User-Agent、版本号在两个方法内填写为同一客户端档案；若已有记录的档案不同，测试会停止，避免悄悄切换上下文。平台标记为 `android-api28-virtual`，不会选用原先 `android-apk-test` 的抓包记录。`rcs_session_id` 单独保存为当前会话 UUID，需要新会话时显式调用 `startNewRiskSession`。该 UUID 只复现标识形状，并未生成 SDK 的 `postSensorBack` 遥测；上游仍可能要求人工验证。

两个测试方法各自默认带 `@Disabled`，普通 `mvn -s .mvn/settings.xml clean verify` 不会触发真实请求。先由用户手工执行 V5，账号持有人当次授权后，只移除要运行的方法上的 `@Disabled`，运行完成后恢复注解；登录完成后清空 `smsCode`。方法内的 `runId` 标记本次手动测试批次；短信和登录按手机号、批次和阶段分别创建单次锁，保存在根目录已忽略的 `抓包文件/登录/live-test/`。同一批次重复运行会在请求前停止；已有旧锁保留，不删除。

测试专用 HTTP 拦截器会在本机控制台打印完整请求方法、URL、请求头、原始请求体、响应状态、响应头和解压后的响应原文，并用 Fastjson2 额外打印格式化 JSON。这些报文含个人数据和 token，只在本机查看，不要复制到聊天或提交 Git。登录成功且 `errCode=0` 时，测试经 `KfcIdentityService` 把手机号及 token 的明文/密文写入 `kfc_user`，并核对本地会话。新生成上下文的真实发送结果待验证，以上游当次响应为准。

### 本机小辉极验三代服务

按用户要求，短信测试和验证码登录测试已通过 `AppLoginCaptchaService` 接入本机服务。默认 `KFC_CAPTCHA3_ENABLED=false`；启动 `小辉极验3代.exe` 并检查本机 `/health` 后，手动设置 `KFC_CAPTCHA3_ENABLED=true`。默认 URL 是 `http://127.0.0.1:16254/captcha3`；跨机器部署时设置 `KFC_CAPTCHA3_URL` 和 `KFC_CAPTCHA3_API_KEY`，密钥只保存在本机忽略配置或环境变量，不写进源码。只有 `5910060/5910061` 会启动验证：先调用 `GET /api/svc/startCaptcha`，再 POST 本机 `/captcha3`，最后调用对应的 `/api/svc/to/user/...` 一次。验证失败、缺字段或超时会停止，绝不循环重试。原测试方法的手机号/批次/阶段锁在首次业务请求前建立。

这三条 `/svc/` 接口来自 APK 静态源码，尚无本域名成功抓包；本机服务和上游请求只经过离线模拟测试。普通构建保留两个 `@Disabled`，不会触发短信或在线挑战。首次真实联调须由账号持有人当次授权，并核对请求和响应；`/captcha3` 的成功结果也不表示上游发码或登录成功，仍以上游最终 `errCode` 为准。

请求继续发送 `Accept-Encoding: gzip`，上游传输层会先解压带 `Content-Encoding: gzip` 的响应，再交给 JSON 转换器。测试控制台仍显示真实响应头，正文打印为解压后的原始 JSON 及格式化 JSON。2026-10-07 一次手动发送收到 HTTP 200 与 gzip 响应，但旧代码把压缩字节当成 JSON，无法确认该次业务 `errCode`；不能根据旧的 `Upstream connection failed` 断言短信未发送，核对手机后再决定是否需要新一批次的当次授权测试。

## 后续联调次序

1. 用授权账号的只读请求逐条验证 `RnOrderApi` 初始化、选店和菜单方法；保存真实 Reqable 证据后再把“离线协议匹配”升级为“Java 线上联调成功”。
2. 用户确认对外接口方案后，再把门店和菜单按强类型参数接入 `server`；不得复用当前旧的任意 JSON 菜单透传骨架。
3. 静态候选域名继续逐一核对真实请求的域名、方法、头、参数和响应，区分页面域名与 API 域名。
4. 购物车、下单和支付另行接入；下单和支付需增加幂等、状态核对、风控交互和回调验签，不能由本轮只读客户端自动扩展。
