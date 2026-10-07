package com.wanghui.kfc.server.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供给 uni-app 的菜单查询接口。 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    /** 接收并编排菜单请求的本地业务服务。 */
    private final CatalogService service;

    /**
     * 创建菜单控制器。
     * @param service 菜单业务服务
     */
    public CatalogController(CatalogService service) { this.service = service; }

    /**
     * 将菜单查询交给内部业务服务，当前仅作为联通骨架。
     *
     * @param request JSON 对象形式的菜单查询参数
     * @return 统一封装的菜单响应
     * @throws IllegalArgumentException 请求体不是 JSON 对象时
     */
    @PostMapping(value = "/menu", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<JsonNode> menu(@RequestBody JsonNode request) {
        if (!request.isObject()) throw new IllegalArgumentException("JSON object required");
        return ApiResponse.ok(service.menu(request));
    }
}
