package com.wanghui.kfc.server.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.kfcapi.OrderingApi;
import org.springframework.stereotype.Service;

/** 菜单业务编排，隔离 MVC 层与肯德基上游接口。 */
@Service
public class CatalogService {
    /** 菜单查询使用的点餐上游客户端。 */
    private final OrderingApi orderingApi;

    /**
     * 创建菜单业务服务。
     * @param orderingApi 点餐上游接口
     */
    public CatalogService(OrderingApi orderingApi) { this.orderingApi = orderingApi; }

    /**
     * 查询上游菜单。
     *
     * @param request 菜单查询 JSON；字段协议仍待联调
     * @return 上游菜单 JSON
     */
    public JsonNode menu(JsonNode request) {
        return orderingApi.menuList(request);
    }
}
