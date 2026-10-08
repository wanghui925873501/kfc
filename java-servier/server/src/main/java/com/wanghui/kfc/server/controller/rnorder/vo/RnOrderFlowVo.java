package com.wanghui.kfc.server.controller.rnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

/** 返回前端安全流程标识和城市选择页所需的数据。 */
@Data
@Schema(name = "RnOrderFlowVo", description = "RN 点餐流程初始化结果")
public class RnOrderFlowVo {
    /** 供业务服务逐项设置返回字段。 */
    public RnOrderFlowVo() { }

    /** 服务端随机流程标识，不包含上游 sessionId 或 Cookie。 */
    @ToString.Exclude
    private String flowId;
    /** 流程空闲过期秒数。 */
    private long expiresInSeconds;
    /** 当前经纬度反查得到的城市。 */
    private City currentCity;
    /** 城市选择页的完整城市列表。 */
    private List<City> allCities = new ArrayList<>();
    /** 城市选择页顶部的热门城市。 */
    private List<City> hotCities = new ArrayList<>();

    /** 描述城市选择和定位所需的公开城市字段。 */
    @Data
    @Schema(name = "RnOrderCity", description = "RN 点餐城市")
    public static class City {
        /** 供映射器逐项设置城市字段。 */
        public City() { }

        /** 上游内部城市编码。 */
        private String cityCode;
        /** 国标城市编码。 */
        private String gbCityCode;
        /** 中文城市名。 */
        private String nameZh;
        /** 英文城市名。 */
        private String nameEn;
        /** 城市拼音或缩写。 */
        private String abbr;
        /** 城市中心纬度文本。 */
        private String latitude;
        /** 城市中心经度文本。 */
        private String longitude;
    }
}
