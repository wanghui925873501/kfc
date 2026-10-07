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

依赖方向：`server → db/kfcapi/common`，`db/kfcapi → common`。外部接口只从 `server` 暴露；不开放任意 URL 或路径透传。技术版本：JDK 21、Spring Boot 3.5.12、MyBatis-Plus 3.5.17、Lombok 1.18.48、Hutool 5.8.47、Fastjson2 2.0.65、MySQL、Redis、Spring MVC。实体和 DTO 使用成员变量与 Lombok `@Data`，便于逐项设置字段。

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
- `AppLoginApi.sendSmsCode` 和 `AppLoginApi.loginBySmsCode` 使用已抓包确认的 `https://applogin.kfcapp.cn` 两个固定 POST 路径；请求字段、DES 加密和 POST 签名已本地实现。它们目前仅供后端内部调用，未向 uni-app 暴露。Java 手动发送先遇到签名错误，修正后返回 `5910060`，APK 将其作为人工风险验证入口；短信发送成功与 Java 登录仍未验证。发送接口成功响应结构仍待确认。
- `LoginApi.validateToken`、`CouponApi.availableCoupons`、`MallApi.productByActivityId`、`PrimeApi.userCard` 是内部封装，**尚未向 uni-app 暴露**。它们的路径/方法来自静态 bundle；完整请求和鉴权仍需联调。会员卡方法的上游 token 应由服务端会话安全取得，不要让 uni-app 任意传入。
- 上游开关默认关闭。`APP_LOGIN` 仅在配置本机凭据且开启上游开关后可发送；其他候选域名仍会因未配置鉴权而拒绝请求。实际短信发送与登录还需要上层完成当次确认、限流和单次触发锁，不要把密钥或上游 token 提交到仓库或下发给 UniApp。
- `db/src/main/resources/db/migration/` 保留需手动执行的 V1–V4 SQL：V1 建立 `app_user`，V2 建立 `kfc_installation` 与 `kfc_user`，V3 为手机号和 token 增加明文/密文字段，V4 为三张表及全部字段补充 MySQL 原生 `COMMENT`。新库需按版本顺序各执行一次；已有库先核对当前结构，只执行尚未应用的脚本，尤其不能重复执行 V3 的 `ADD COLUMN`。Java 服务不会自动执行这些 SQL。明文列只供有数据库权限的本机查看，不在日志或对外响应中输出；验证码不落库。
- `KfcIdentityService` 提供安装注册/更新、登录结果关联、后续请求上下文读取；成功登录时调用方须把同次登录的手机号传给 `recordLoginSuccess`，并与上游响应一起保存。`KfcSessionStore` 只将用户与安装引用加密后放入 Redis，默认本地 TTL 为 8 小时；后续请求从 `kfc_user` 校验并读取持久化 token。客户端只持有随机本地会话标识。此 TTL 不是上游 token 有效期承诺；上游拒绝请求时仍需让用户重新登录或按已验证协议刷新。
- 独立的 `KFC_SESSION_KEY_BASE64` 是 32 字节密钥的 Base64 编码，本机真实值存于 Git 忽略的根目录 `.env.kfc-reverse.local`。`server` 从相对于 `java-servier` 启动目录的该文件自动导入；其他运行目录可通过进程环境提供同名变量。没有密钥时服务可启动，但签发会话会明确失败。
- 当前仅完成内部服务与本地测试，尚未增加 UniApp 安装注册页面/接口。后续业务接口必须通过后端会话取用户与安装上下文；城市、位置、版本、动态风控字段按当次请求或当前应用状态取得，不能长期套用抓包值。

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

手动测试的安装上下文来自根目录 Git 忽略的 `.env.kfc-reverse.local`。其中 `KFC_TEST_INSTALLATION_ID` 是本地安装记录 ID；`KFC_TEST_DEVICE_ID`、`KFC_TEST_TDID`、`KFC_TEST_APP_VERSION`、`KFC_TEST_CITY_CODE`、`KFC_TEST_USER_AGENT`、`KFC_TEST_RCSDCID`、`KFC_TEST_USER_CODE` 和 `KFC_TEST_PHONE_CIPHERTEXT` 由本机 Reqable 会话 `8600` 提取，没有写进源码。发送测试先确认手机号的本地 DES 密文与抓包一致，再创建或核对对应的 `kfc_installation` 记录；该记录用 `android-apk-test` 平台标记，避免与 UniApp 正常安装混淆。`KFC_TEST_RCSDCID` 是抓包当次的 SDK 值，重用时可能过期；即使请求字段与抓包对齐，也不能保证上游不要求人工验证。验证码登录需要另外设置当次的 `KFC_TEST_LOGIN_RCSDCID`，不能复用发送阶段的值。动态值只留在本机忽略配置，不能提交到 Git。

两个测试方法各自默认带 `@Disabled`，普通 `mvn -s .mvn/settings.xml clean verify` 不会触发真实请求。账号持有人当次授权后，只移除要运行的方法上的 `@Disabled`，运行完成后恢复注解；登录完成后清空 `smsCode`。`KFC_TEST_RUN_ID` 为本机一次手动测试批次的 UUID，短信和登录按手机号、批次和阶段分别创建单次锁，保存在根目录已忽略的 `抓包文件/登录/live-test/`。同一批次重复运行会在请求前停止；已有旧锁保留，不删除。

测试专用 HTTP 拦截器会在本机控制台打印完整请求方法、URL、请求头、原始请求体、响应状态、响应头和原始响应体，并用 Fastjson2 额外打印格式化 JSON。这些报文含个人数据和 token，只在本机查看，不要复制到聊天或提交 Git。`5910060/5910061` 会明确报告需要账号持有人进行人工验证，保留响应 `errData`，本测试不会自动重试或构造验证票据。登录成功且 `errCode=0` 时，测试经 `KfcIdentityService` 把手机号及 token 的明文/密文写入 `kfc_user`，并核对本地会话。Reqable 会话 `8600` 已保存发送成功的响应 `errCode=0`；Java 手动测试仍需以上游当次响应为准。

## 后续联调次序

1. 优先根据现有 Reqable 证据实现已确认的域名和接口；静态候选地址逐一核对实际请求的域名、方法、头、参数和响应，区分页面域名与 API 域名。
2. 实现服务端凭据与签名，确定 uni-app 用户身份映射、会话、限流与错误码。
3. 再把门店、菜单、优惠券、购物车、下单、支付按业务编排接入 `server`。下单和支付需另做幂等、状态核对和回调验签。
