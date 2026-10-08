# 前端 RN 点餐浏览接口

当前接口只覆盖城市、门店关键词搜索、菜单和商品详情浏览，不包含购物车、下单或支付。所有请求都需要顶层 `phone`，并要求该手机号已有完整登录态。

## 流程约定

1. 调用 `POST /api/v1/rn-order/flows` 初始化流程，保存返回的 `flowId`。
2. 用同一手机号和 `flowId` 调用门店、菜单、商品详情接口。
3. `flowId` 默认空闲 15 分钟过期，每次成功的后续调用会续期；过期后重新初始化。
4. `flowId` 与手机号绑定。前端不得记录或接触上游 `sessionId`、Cookie、ticket、设备标识或签名字段。

统一成功响应为 `{"code":"OK","message":"success","data":...}`。流程不存在、过期或不属于当前手机号时返回 HTTP 409 和 `RN_ORDER_FLOW_EXPIRED`；上游没有确认成功时返回 HTTP 502 和 `RN_ORDER_REJECTED`。

## 初始化与城市

`POST /api/v1/rn-order/flows`

```json
{
  "phone": "测试账号手机号",
  "latitude": "31.2304",
  "longitude": "121.4737",
  "gbCityCode": "310000"
}
```

`data` 包含 `flowId`、`expiresInSeconds`、`currentCity`、`allCities` 和 `hotCities`。城市公开字段为 `cityCode`、`gbCityCode`、`nameZh`、`nameEn`、`abbr`、`latitude`、`longitude`。

## 门店

`POST /api/v1/rn-order/stores`

```json
{
  "phone": "测试账号手机号",
  "flowId": "初始化返回的流程标识",
  "latitude": "31.2304",
  "longitude": "121.4737",
  "gbCityCode": "310000",
  "storeCode": ""
}
```

`data` 包含 `nearbyStores` 和 `customerStores`。每个门店只返回页面使用的编码、名称、公开地址、城市、经纬度、距离、营业时间、状态、图片、收藏状态和预约能力。

## 门店关键词搜索

`POST /api/v1/rn-order/stores/search`

```json
{
  "phone": "测试账号手机号",
  "flowId": "初始化返回的流程标识",
  "latitude": "31.2304",
  "longitude": "121.4737",
  "gbCityCode": "310000",
  "cityName": "示例市",
  "keyword": "广场"
}
```

经纬度使用用户手动选择城市的中心坐标，不要求浏览器定位权限。`cityName` 和 `keyword` 会在服务端去除首尾空白，关键词最长 50 个字符。`data.stores` 返回与门店查询相同的公开字段；没有匹配门店时返回空数组。

## 菜单

`POST /api/v1/rn-order/menu`

请求字段与门店接口相同，但 `storeCode` 必填。`data` 包含已校验的 `store`、`tabs`、`categories` 和 `banners`；分类内的 `products` 提供 `linkId`、名称、描述、图片、价格、数量范围、可售状态和是否需要选择规格。

## 商品详情

`POST /api/v1/rn-order/products/detail`

```json
{
  "phone": "测试账号手机号",
  "flowId": "初始化返回的流程标识",
  "linkId": "菜单商品标识"
}
```

`data` 返回商品名称、说明、图片、价格、数量范围、可售状态和 `optionGroups`。本阶段前端只展示这些数据，不提交选择结果，也不触发加购或结算。

## 当前明确未接通

- 推荐门店弹层和按门店补充查询
- 菜单辅助活动、标签和规则接口
- 购物车、提交订单、取消订单和支付
