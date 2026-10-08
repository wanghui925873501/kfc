package com.wanghui.kfc.server.controller.rnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 返回城市关键词匹配到的门店公开字段。 */
@Data
@Schema(name = "RnOrderStoreSearchVo", description = "RN 点餐门店关键词搜索结果")
public class RnOrderStoreSearchVo {
    /** 供业务服务逐项设置返回字段。 */
    public RnOrderStoreSearchVo() { }

    /** 匹配餐厅名称或地址的门店。 */
    private List<RnOrderStoresVo.Store> stores = new ArrayList<>();
}
