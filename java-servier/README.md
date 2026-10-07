# KFC uni-app 后端骨架

代码与注释规范见 [AGENTS.md](AGENTS.md)。所有公开 API 执行 Javadoc 检查。

这是一个**单体部署、多 Maven 模块**的 Spring Boot 项目。只有 `server` 产出可执行 JAR；模块划分用于固定依赖方向，不是微服务。

```text
java-servier/
├─ common/   统一返回等共享类型
├─ db/       本地 MySQL 的 entity、mapper、service、迁移 SQL
├─ kfcapi/   第三方域名登记、HTTPS 传输、鉴权扩展点、已确认的接口封装
└─ server/   uni-app 调用的 MVC 接口、内部业务编排、Redis 配置、启动入口
```

依赖方向：`server → db/kfcapi/common`，`db/kfcapi → common`。外部接口只从 `server` 暴露；不开放任意 URL 或路径透传。技术版本：JDK 21、Spring Boot 3.5.12、MyBatis-Plus 3.5.17、MySQL、Redis、Spring MVC、Flyway。

## 五个上游地址的当前依据

| 配置键 | 当前默认值 | APK 中的证据 | 接口归属可信度 |
| --- | --- | --- | --- |
| `ordering` | `https://order.kfc.com.cn` | `kfc-ordering-delivery/modules/01122.js` 的预点餐页面 URL；`01517.js` 有 `/api/v2/menu/list` POST | 页面域名已见，菜单接口是否同域**待抓包确认** |
| `login` | `https://login.kfc.com.cn` | `01290.js` 的 `baseDomain` 与 `KBS/api`；`01249.js` 有 `/user/token/valid` POST | 配置和路径均有证据，完整协议待确认 |
| `coupon` | `https://appcoupon.kfc.com.cn` | `01290.js` 的 `baseDomainCoupon`；`KFC_Coupon_App/modules/02027.js` 的 `domainCode: appcoupon` + POST | 路由有证据，参数与鉴权待确认 |
| `mall` | `https://appmall.kfc.com.cn` | `01290.js` 的 `baseDomainAppmall`；`kfc-ordering-delivery/modules/02625.js` 的 `domainCode: appmall` + GET | 路由有证据，参数与鉴权待确认 |
| `prime` | `https://appprime.kfc.com.cn` | `01290.js` 的 `baseDomainPrime`；`KFC_App_Activity/modules/03179.js` 的 `domainCode: appprime` + GET | 路由有证据，参数与鉴权待确认 |

APK 里还有 `m.4008823823.com.cn`、`appcommon.kfc.com.cn` 等候选。这里的五个地址是首批建模，并非 APK 只访问这五个域名。五个域名各有一个内部客户端及一个有静态调用证据的方法。新增接口时，在 `kfcapi` 中按业务增加固定路径的方法，并记录来源、HTTP 方法、请求字段、鉴权规则、响应样本。`UpstreamGateway` 只供这些内部封装使用。

## 当前可调用骨架

- `POST /api/v1/catalog/menu` → `CatalogService` → `OrderingApi.menuList` → 固定 `/api/v2/menu/list`。这是联通样例；目标 API 域名、必填参数和签名仍待确认。
- `LoginApi.validateToken`、`CouponApi.availableCoupons`、`MallApi.productByActivityId`、`PrimeApi.userCard` 是内部封装，**尚未向 uni-app 暴露**。它们的路径/方法来自静态 bundle；完整请求和鉴权仍需联调。会员卡方法的上游 token 应由服务端会话安全取得，不要让 uni-app 任意传入。
- 上游开关默认关闭，避免把未确认的地址与参数发送到生产系统。即使开启，在实现 `UpstreamAuthentication` 前也会明确拒绝请求。需要用正式服务端凭据实现签名、token、风控或设备信息，并完成联调。不要复制 APK 内的客户端密钥或把它们提交到仓库。
- 本地 `app_user` 表是 `db` 模块示例，用 Flyway 自动建表；只存昵称和手机号哈希，不保存第三方 token。该表尚未接入登录流程。
- Redis 已由 Spring Boot 配置连接信息，后续用于会话、幂等和短期缓存；当前不缓存个人订单/优惠券数据。

## 启动

1. 安装 JDK 21、MySQL 和 Redis，创建数据库与用户。例如：

   ```sql
   CREATE DATABASE kfc_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'kfc_app'@'localhost' IDENTIFIED BY 'change-me';
   GRANT ALL PRIVILEGES ON kfc_app.* TO 'kfc_app'@'localhost';
   ```

2. 设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`；Redis 不在本机时再设 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`。上游地址用 `KFC_ORDERING_URL`、`KFC_LOGIN_URL`、`KFC_COUPON_URL`、`KFC_MALL_URL`、`KFC_PRIME_URL` 覆盖。确认协议并配好鉴权后才设 `KFC_UPSTREAM_ENABLED=true`。
3. 在本目录运行 `mvn -s .mvn/settings.xml clean verify`，然后 `java -jar server/target/server-0.1.0-SNAPSHOT.jar`。检查 `GET /actuator/health`。首次启动会执行 `db/src/main/resources/db/migration/V1__app_user.sql`。

项目内 `.mvn/settings.xml` 把 Maven 缓存放到当前工作目录的 `.m2`，避免使用开发机上无写权限的全局 Maven 仓库。

## 后续联调次序

1. 用授权测试环境抓取五个域名实际的请求域名、方法、头、参数和响应；区分页面域名与 API 域名。
2. 实现服务端凭据与签名，确定 uni-app 用户身份映射、会话、限流与错误码。
3. 再把门店、菜单、优惠券、购物车、下单、支付按业务编排接入 `server`。下单和支付需另做幂等、状态核对和回调验签。
