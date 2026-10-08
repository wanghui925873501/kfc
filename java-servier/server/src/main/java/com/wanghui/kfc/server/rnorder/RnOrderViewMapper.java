package com.wanghui.kfc.server.rnorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderMenuVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderProductDetailVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoresVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 将 RN 点餐上游 JSON 白名单映射为不含凭据的前端视图。 */
@Component
public class RnOrderViewMapper {
    /** RN 点餐响应中相对图片路径对应的固定 HTTPS 源站。 */
    private static final String ASSET_ORIGIN = "https://rnorder.kfc.com.cn";

    /** 供 Spring 创建无状态映射器。 */
    public RnOrderViewMapper() { }

    /**
     * 组合定位城市和城市列表响应。
     *
     * @param locationResponse 经纬度反查城市响应
     * @param citiesResponse 城市列表响应
     * @return 前端城市选择数据
     */
    public RnOrderFlowVo flow(JsonNode locationResponse, JsonNode citiesResponse) {
        RnOrderFlowVo result = new RnOrderFlowVo();
        result.setCurrentCity(city(data(locationResponse).path("city")));
        result.setAllCities(cities(data(citiesResponse).path("allCities")));
        result.setHotCities(cities(data(citiesResponse).path("hotCities")));
        return result;
    }

    /**
     * 组合附近门店和账号常用门店响应。
     *
     * @param nearbyResponse 附近门店响应
     * @param customerResponse 常用门店响应
     * @return 选店页公开数据
     */
    public RnOrderStoresVo stores(JsonNode nearbyResponse, JsonNode customerResponse) {
        RnOrderStoresVo result = new RnOrderStoresVo();
        result.setNearbyStores(stores(data(nearbyResponse).path("stores")));
        result.setCustomerStores(stores(data(customerResponse).path("stores")));
        return result;
    }

    /**
     * 组合已校验门店和菜单主响应。
     *
     * @param store 上游已校验的完整门店节点
     * @param menuResponse 菜单主响应
     * @return 菜单页公开数据
     */
    public RnOrderMenuVo menu(JsonNode store, JsonNode menuResponse) {
        JsonNode menuData = data(menuResponse);
        RnOrderMenuVo result = new RnOrderMenuVo();
        result.setStore(store(store));
        result.setTabs(tabs(menuData.path("menuTab")));
        result.setCategories(categories(menuData.path("menuData")));
        result.setBanners(banners(menuData.path("scenceBanners")));
        return result;
    }

    /**
     * 映射商品详情响应。
     *
     * @param detailResponse 商品详情响应
     * @return 商品详情页公开数据
     */
    public RnOrderProductDetailVo detail(JsonNode detailResponse) {
        JsonNode source = data(detailResponse);
        RnOrderProductDetailVo result = new RnOrderProductDetailVo();
        result.setLinkId(text(source, "linkId"));
        result.setName(text(source, "showNameCn", "nameCn"));
        result.setDescription(text(source, "descCn", "abbrDesc"));
        result.setImageUrl(image(source, "imageUrlNew", "imageUrl"));
        result.setPrice(money(source, false, "price"));
        result.setStandardPrice(money(source, true, "standardPrice"));
        result.setMinQuantity(integer(source, "minQty"));
        result.setMaxQuantity(integer(source, "maxQty"));
        result.setAvailable(available(source));
        JsonNode combo = source.path("superComboDetail");
        if (combo.isMissingNode() || combo.isNull()) combo = source.path("superCombo");
        result.setOptionGroups(optionGroups(combo.path("roundList")));
        return result;
    }

    private JsonNode data(JsonNode response) {
        return response == null ? MissingNode.getInstance() : response.path("data");
    }

    private List<RnOrderFlowVo.City> cities(JsonNode source) {
        List<RnOrderFlowVo.City> result = new ArrayList<>();
        if (source.isArray()) source.forEach(node -> result.add(city(node)));
        return result;
    }

    private RnOrderFlowVo.City city(JsonNode source) {
        if (!source.isObject()) return null;
        RnOrderFlowVo.City result = new RnOrderFlowVo.City();
        result.setCityCode(text(source, "cityCode"));
        result.setGbCityCode(text(source, "gbCityCode"));
        result.setNameZh(text(source, "cityNameZh"));
        result.setNameEn(text(source, "cityNameEn"));
        result.setAbbr(text(source, "abbr"));
        result.setLatitude(text(source, "latitude", "lat"));
        result.setLongitude(text(source, "longitude", "lng"));
        return result;
    }

    private List<RnOrderStoresVo.Store> stores(JsonNode source) {
        List<RnOrderStoresVo.Store> result = new ArrayList<>();
        if (source.isArray()) source.forEach(node -> result.add(store(node)));
        return result;
    }

    private RnOrderStoresVo.Store store(JsonNode source) {
        if (!source.isObject()) return null;
        RnOrderStoresVo.Store result = new RnOrderStoresVo.Store();
        result.setStoreCode(text(source, "storecode", "storeCode"));
        result.setStoreName(text(source, "storename", "storeName"));
        result.setAddress(text(source, "address"));
        result.setCityName(text(source, "cityName"));
        result.setLatitude(text(source, "lat", "latitude"));
        result.setLongitude(text(source, "lng", "longitude"));
        result.setDistance(text(source, "distance"));
        result.setPhoneDistance(text(source, "distancePhoneToStore"));
        result.setStartTime(text(source, "starttime", "startTime"));
        result.setEndTime(text(source, "endtime", "endTime"));
        result.setStatus(text(source, "status"));
        result.setStatusComments(text(source, "statuscomments", "statusComments"));
        JsonNode pictures = source.path("storePic");
        result.setImageUrl(image(pictures, "storeSelfPic", "storeEnvirPic", "storeBuzAreaPic"));
        result.setFavorite(truthy(source.path("favType")));
        JsonNode booking = source.path("bookingExt");
        result.setImmediate(truthy(booking.path("boolImmediate")));
        result.setBooking(truthy(booking.path("boolBooking")));
        return result;
    }

    private List<RnOrderMenuVo.MenuTab> tabs(JsonNode source) {
        List<RnOrderMenuVo.MenuTab> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderMenuVo.MenuTab tab = new RnOrderMenuVo.MenuTab();
            tab.setId(text(node, "id", "tabId", "type", "code"));
            tab.setName(text(node, "nameCn", "name", "title"));
            tab.setSort(integer(node, "showOrder", "sort"));
            result.add(tab);
        });
        return result;
    }

    private List<RnOrderMenuVo.Category> categories(JsonNode source) {
        List<RnOrderMenuVo.Category> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderMenuVo.Category category = new RnOrderMenuVo.Category();
            category.setClassId(text(node, "classId"));
            category.setName(text(node, "nameCn", "name"));
            category.setImageUrl(image(node, "imageCnUrl", "imageUrl"));
            category.setSort(integer(node, "showOrder", "sort"));
            category.setProducts(products(node.path("menuList")));
            result.add(category);
        });
        return result;
    }

    private List<RnOrderMenuVo.Product> products(JsonNode source) {
        List<RnOrderMenuVo.Product> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderMenuVo.Product product = new RnOrderMenuVo.Product();
            product.setLinkId(text(node, "linkId"));
            product.setClassId(text(node, "classId"));
            product.setName(text(node, "showNameCn", "nameCn"));
            product.setDescription(text(node, "abbrDesc", "descCn"));
            product.setImageUrl(image(node, "imageUrlNew", "imageUrl"));
            product.setPrice(money(node, false, "price"));
            product.setStandardPrice(money(node, true, "standardPrice"));
            product.setMinQuantity(integer(node, "minQty"));
            product.setMaxQuantity(integer(node, "maxQty"));
            product.setAvailable(available(node));
            JsonNode rounds = node.path("superCombo").path("roundList");
            product.setRequiresOptions(rounds.isArray() && !rounds.isEmpty());
            result.add(product);
        });
        return result;
    }

    private List<RnOrderMenuVo.Banner> banners(JsonNode source) {
        List<RnOrderMenuVo.Banner> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderMenuVo.Banner banner = new RnOrderMenuVo.Banner();
            banner.setId(text(node, "id", "bannerId", "activityId"));
            banner.setTitle(text(node, "title", "nameCn", "name"));
            banner.setImageUrl(image(node, "imageUrl", "imgUrl", "picUrl"));
            banner.setSort(integer(node, "showOrder", "sort"));
            result.add(banner);
        });
        return result;
    }

    private List<RnOrderProductDetailVo.OptionGroup> optionGroups(JsonNode source) {
        List<RnOrderProductDetailVo.OptionGroup> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderProductDetailVo.OptionGroup group = new RnOrderProductDetailVo.OptionGroup();
            group.setId(text(node, "roundId"));
            group.setName(text(node, "roundName"));
            group.setMinQuantity(integer(node, "riMinCount"));
            group.setMaxQuantity(integer(node, "riMaxCount"));
            group.setItems(optionItems(node.path("itemList")));
            result.add(group);
        });
        return result;
    }

    private List<RnOrderProductDetailVo.OptionItem> optionItems(JsonNode source) {
        List<RnOrderProductDetailVo.OptionItem> result = new ArrayList<>();
        if (!source.isArray()) return result;
        source.forEach(node -> {
            RnOrderProductDetailVo.OptionItem item = new RnOrderProductDetailVo.OptionItem();
            item.setLinkId(text(node, "linkId"));
            item.setName(text(node, "showNameCn", "nameCn"));
            item.setImageUrl(image(node, "imageUrlNew", "imageUrl"));
            item.setDefaultQuantity(integer(node, "defaultCount"));
            item.setMinQuantity(integer(node, "minCount"));
            item.setMaxQuantity(integer(node, "maxCount"));
            item.setAdjustPrice(money(node, false, "adjustPrice"));
            item.setAvailable(available(node));
            result.add(item);
        });
        return result;
    }

    private boolean available(JsonNode source) {
        String saleFlag = text(source, "saleFlag");
        String stockFlag = text(source, "stockFlag");
        return !"N".equalsIgnoreCase(saleFlag) && !"1".equals(stockFlag)
                && !"N".equalsIgnoreCase(stockFlag);
    }

    private boolean truthy(JsonNode source) {
        if (source.isBoolean()) return source.asBoolean();
        String value = source.asText("");
        return "1".equals(value) || "Y".equalsIgnoreCase(value)
                || "true".equalsIgnoreCase(value);
    }

    private String text(JsonNode source, String... fields) {
        if (source == null) return null;
        for (String field : fields) {
            JsonNode value = source.path(field);
            if (!value.isMissingNode() && !value.isNull() && !value.isContainerNode()) {
                String text = value.asText();
                if (!text.isBlank()) return text;
            }
        }
        return null;
    }

    private Integer integer(JsonNode source, String... fields) {
        String value = text(source, fields);
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String image(JsonNode source, String... fields) {
        String value = text(source, fields);
        if (value == null) return null;
        if (value.startsWith("https://")) return value;
        if (value.startsWith("//")) return "https:" + value;
        if (value.startsWith("/")) return ASSET_ORIGIN + value;
        return null;
    }

    private String money(JsonNode source, boolean omitZero, String... fields) {
        String value = text(source, fields);
        if (value == null) return null;
        try {
            BigDecimal amount = new BigDecimal(value);
            if (!value.contains(".")) amount = amount.movePointLeft(2);
            if (omitZero && amount.signum() == 0) return null;
            return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
