package com.wanghui.kfc.server.controller.rnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 返回商品详情页展示和后续规格选择所需的只读数据。 */
@Data
@Schema(name = "RnOrderProductDetailVo", description = "RN 点餐商品详情")
public class RnOrderProductDetailVo {
    /** 供业务服务逐项设置返回字段。 */
    public RnOrderProductDetailVo() { }

    /** 商品链接标识。 */
    private String linkId;
    /** 商品展示名称。 */
    private String name;
    /** 商品详情说明。 */
    private String description;
    /** 商品主图片 URL。 */
    private String imageUrl;
    /** 当前销售价格的十进制文本。 */
    private String price;
    /** 标准价格的十进制文本。 */
    private String standardPrice;
    /** 单次最小购买数量。 */
    private Integer minQuantity;
    /** 单次最大购买数量。 */
    private Integer maxQuantity;
    /** 当前商品是否允许选择。 */
    private boolean available;
    /** 套餐或规格选择组。 */
    private List<OptionGroup> optionGroups = new ArrayList<>();

    /** 描述商品详情中的一组套餐组成或规格选择。 */
    @Data
    @Schema(name = "RnOrderOptionGroup", description = "商品规格选择组")
    public static class OptionGroup {
        /** 供映射器逐项设置选择组字段。 */
        public OptionGroup() { }

        /** 上游选择组标识。 */
        private String id;
        /** 选择组名称。 */
        private String name;
        /** 最少选择数量。 */
        private Integer minQuantity;
        /** 最多选择数量。 */
        private Integer maxQuantity;
        /** 该组可选择的商品项。 */
        private List<OptionItem> items = new ArrayList<>();
    }

    /** 描述商品详情选择组中的一个可选项。 */
    @Data
    @Schema(name = "RnOrderOptionItem", description = "商品规格选项")
    public static class OptionItem {
        /** 供映射器逐项设置选项字段。 */
        public OptionItem() { }

        /** 选项商品链接标识。 */
        private String linkId;
        /** 选项展示名称。 */
        private String name;
        /** 选项图片 URL。 */
        private String imageUrl;
        /** 默认选择数量。 */
        private Integer defaultQuantity;
        /** 最少选择数量。 */
        private Integer minQuantity;
        /** 最多选择数量。 */
        private Integer maxQuantity;
        /** 相对套餐基础价的调整金额文本。 */
        private String adjustPrice;
        /** 当前选项是否可选择。 */
        private boolean available;
    }
}
