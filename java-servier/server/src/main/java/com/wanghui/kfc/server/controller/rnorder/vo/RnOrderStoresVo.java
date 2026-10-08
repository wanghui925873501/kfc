package com.wanghui.kfc.server.controller.rnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 返回选店页需要的附近门店和账号常用门店。 */
@Data
@Schema(name = "RnOrderStoresVo", description = "RN 点餐门店查询结果")
public class RnOrderStoresVo {
    /** 供业务服务逐项设置返回字段。 */
    public RnOrderStoresVo() { }

    /** 当前定位附近的可见门店。 */
    private List<Store> nearbyStores = new ArrayList<>();
    /** 当前账号收藏或常用的门店。 */
    private List<Store> customerStores = new ArrayList<>();

    /** 描述选店页与菜单页共同使用的门店公开字段。 */
    @Data
    @Schema(name = "RnOrderStore", description = "RN 点餐门店")
    public static class Store {
        /** 供映射器逐项设置门店字段。 */
        public Store() { }

        /** 门店业务编码。 */
        private String storeCode;
        /** 门店展示名称。 */
        private String storeName;
        /** 门店公开地址。 */
        private String address;
        /** 门店所在城市名。 */
        private String cityName;
        /** 门店纬度文本。 */
        private String latitude;
        /** 门店经度文本。 */
        private String longitude;
        /** 上游计算的距离展示值。 */
        private String distance;
        /** 手机定位到门店的距离展示值。 */
        private String phoneDistance;
        /** 营业开始时间。 */
        private String startTime;
        /** 营业结束时间。 */
        private String endTime;
        /** 上游门店状态。 */
        private String status;
        /** 门店状态补充说明。 */
        private String statusComments;
        /** 可选门店公开图片 URL。 */
        private String imageUrl;
        /** 当前账号是否收藏此门店。 */
        private boolean favorite;
        /** 是否支持立即点餐。 */
        private boolean immediate;
        /** 是否支持预约点餐。 */
        private boolean booking;
    }
}
