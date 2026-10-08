package com.wanghui.kfc.server.controller.rnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 返回已校验门店和菜单页的主体浏览数据。 */
@Data
@Schema(name = "RnOrderMenuVo", description = "RN 点餐菜单主体")
public class RnOrderMenuVo {
    /** 供业务服务逐项设置返回字段。 */
    public RnOrderMenuVo() { }

    /** 本次菜单对应的已校验门店。 */
    private RnOrderStoresVo.Store store;
    /** 菜单顶部频道标签。 */
    private List<MenuTab> tabs = new ArrayList<>();
    /** 左侧分类及其商品列表。 */
    private List<Category> categories = new ArrayList<>();
    /** 菜单主响应自带的场景 Banner。 */
    private List<Banner> banners = new ArrayList<>();

    /** 描述菜单顶部频道标签。 */
    @Data
    @Schema(name = "RnOrderMenuTab", description = "菜单频道标签")
    public static class MenuTab {
        /** 供映射器逐项设置频道字段。 */
        public MenuTab() { }

        /** 频道标识。 */
        private String id;
        /** 频道名称。 */
        private String name;
        /** 排序值。 */
        private Integer sort;
    }

    /** 描述菜单左侧分类和该分类商品。 */
    @Data
    @Schema(name = "RnOrderMenuCategory", description = "菜单分类")
    public static class Category {
        /** 供映射器逐项设置分类字段。 */
        public Category() { }

        /** 分类标识。 */
        private String classId;
        /** 分类名称。 */
        private String name;
        /** 分类可选图片 URL。 */
        private String imageUrl;
        /** 分类排序值。 */
        private Integer sort;
        /** 分类中的可浏览商品。 */
        private List<Product> products = new ArrayList<>();
    }

    /** 描述菜单列表中可进入详情的商品摘要。 */
    @Data
    @Schema(name = "RnOrderMenuProduct", description = "菜单商品摘要")
    public static class Product {
        /** 供映射器逐项设置商品字段。 */
        public Product() { }

        /** 商品链接标识，用于查询详情。 */
        private String linkId;
        /** 商品所属分类标识。 */
        private String classId;
        /** 商品展示名称。 */
        private String name;
        /** 商品简短描述。 */
        private String description;
        /** 商品展示图片 URL。 */
        private String imageUrl;
        /** 当前销售价格的十进制文本。 */
        private String price;
        /** 标准价格的十进制文本。 */
        private String standardPrice;
        /** 单次最小购买数量。 */
        private Integer minQuantity;
        /** 单次最大购买数量。 */
        private Integer maxQuantity;
        /** 当前商品是否允许在菜单中选择。 */
        private boolean available;
        /** 是否需要进入详情选择套餐组成或规格。 */
        private boolean requiresOptions;
    }

    /** 描述菜单主响应直接返回的展示 Banner。 */
    @Data
    @Schema(name = "RnOrderMenuBanner", description = "菜单场景 Banner")
    public static class Banner {
        /** 供映射器逐项设置 Banner 字段。 */
        public Banner() { }

        /** Banner 标识。 */
        private String id;
        /** Banner 标题。 */
        private String title;
        /** Banner 图片 URL。 */
        private String imageUrl;
        /** 展示排序值。 */
        private Integer sort;
    }
}
