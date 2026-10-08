package com.wanghui.kfc.server.rnorder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderMenuVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderProductDetailVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoreSearchVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoresVo;
import org.junit.jupiter.api.Test;

class RnOrderViewMapperTest {
    /** JSON 测试数据解析器。 */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** 被测白名单视图映射器。 */
    private final RnOrderViewMapper mapper = new RnOrderViewMapper();

    @Test
    void mapsOnlyFrontendSafeCityAndStoreFields() throws Exception {
        JsonNode located = json("""
                {"code":0,"data":{"city":{"cityCode":"021","gbCityCode":"310000",
                "cityNameZh":"上海市","cityNameEn":"Shanghai","abbr":"SH",
                "latitude":"31.2","longitude":"121.4","secret":"hidden"}}}
                """);
        JsonNode cities = json("""
                {"code":0,"data":{"allCities":[{"cityCode":"021","cityNameZh":"上海市"}],
                "hotCities":[{"cityCode":"010","cityNameZh":"北京市"}]}}
                """);
        RnOrderFlowVo flow = mapper.flow(located, cities);
        assertThat(flow.getCurrentCity().getNameZh()).isEqualTo("上海市");
        assertThat(flow.getAllCities()).hasSize(1);
        assertThat(flow.getHotCities()).extracting(RnOrderFlowVo.City::getCityCode)
                .containsExactly("010");

        JsonNode nearby = json("""
                {"code":0,"data":{"stores":[{"storecode":"001","storename":"测试店",
                "address":"测试路","lat":"31.1","lng":"121.1","distance":"1km",
                "favType":"1","bookingExt":{"boolImmediate":true,"boolBooking":"Y"},
                "storePic":{"storeSelfPic":"https://img.example/store.png"},
                "cookie":"must-not-leak"}]}}
                """);
        RnOrderStoresVo stores = mapper.stores(nearby,
                json("{" + "\"code\":0,\"data\":{\"stores\":[]}}"));
        assertThat(stores.getNearbyStores()).singleElement().satisfies(store -> {
            assertThat(store.getStoreCode()).isEqualTo("001");
            assertThat(store.isFavorite()).isTrue();
            assertThat(store.isImmediate()).isTrue();
            assertThat(store.isBooking()).isTrue();
        });
        assertThat(objectMapper.writeValueAsString(stores)).doesNotContain("cookie", "must-not-leak");
    }

    @Test
    void mapsMenuAndProductOptions() throws Exception {
        JsonNode store = json("{" + "\"storecode\":\"001\",\"storename\":\"测试店\"}");
        JsonNode menu = json("""
                {"code":0,"data":{"menuTab":[{"tabId":"all","nameCn":"全部","showOrder":1}],
                "menuData":[{"classId":"c1","nameCn":"汉堡","showOrder":2,"menuList":[{
                "linkId":"p1","classId":"c1","showNameCn":"香辣鸡腿堡","abbrDesc":"香辣",
                "imageUrlNew":"/images/product.png","price":"1990",
                "standardPrice":"0","minQty":"1","maxQty":"9","saleFlag":"Y",
                "stockFlag":"0","superCombo":{"roundList":[{"roundId":"r1"}]}}]}],
                "scenceBanners":[{"bannerId":"b1","title":"活动","imageUrl":"https://img.example/banner.png"}]}}
                """);
        RnOrderMenuVo mappedMenu = mapper.menu(store, menu);
        assertThat(mappedMenu.getStore().getStoreCode()).isEqualTo("001");
        assertThat(mappedMenu.getCategories()).singleElement().satisfies(category ->
                assertThat(category.getProducts()).singleElement().satisfies(product -> {
                    assertThat(product.getLinkId()).isEqualTo("p1");
                    assertThat(product.getImageUrl())
                            .isEqualTo("https://rnorder.kfc.com.cn/images/product.png");
                    assertThat(product.getPrice()).isEqualTo("19.90");
                    assertThat(product.getStandardPrice()).isNull();
                    assertThat(product.isAvailable()).isTrue();
                    assertThat(product.isRequiresOptions()).isTrue();
                }));

        JsonNode detail = json("""
                {"code":0,"data":{"linkId":"p1","showNameCn":"香辣鸡腿堡",
                "descCn":"商品说明","price":"19.00","standardPrice":"21.00",
                "saleFlag":"Y","stockFlag":"0","superComboDetail":{"roundList":[{
                "roundId":"r1","roundName":"饮料","riMinCount":"1","riMaxCount":"1",
                "itemList":[{"linkId":"i1","nameCn":"可乐","defaultCount":"1",
                "minCount":"0","maxCount":"1","adjustPrice":"0.00","saleFlag":"Y",
                "stockFlag":"0"}]}]},"sessionId":"must-not-leak"}}
                """);
        RnOrderProductDetailVo mappedDetail = mapper.detail(detail);
        assertThat(mappedDetail.getOptionGroups()).singleElement().satisfies(group -> {
            assertThat(group.getName()).isEqualTo("饮料");
            assertThat(group.getItems()).singleElement().satisfies(item ->
                    assertThat(item.getName()).isEqualTo("可乐"));
        });
        assertThat(objectMapper.writeValueAsString(mappedDetail))
                .doesNotContain("sessionId", "must-not-leak");
    }

    @Test
    void mapsKeywordSearchStoresWithoutLeakingUpstreamFields() throws Exception {
        JsonNode response = json("""
                {"code":0,"data":{"stores":[{"storeCode":"S001",
                "storeName":"龙泉示例店","address":"龙泉路 1 号","latitude":"25.7",
                "longitude":"113.0","sessionId":"must-not-leak"}]}}
                """);

        RnOrderStoreSearchVo result = mapper.searchStores(response);

        assertThat(result.getStores()).singleElement().satisfies(store -> {
            assertThat(store.getStoreCode()).isEqualTo("S001");
            assertThat(store.getStoreName()).isEqualTo("龙泉示例店");
        });
        assertThat(objectMapper.writeValueAsString(result))
                .doesNotContain("sessionId", "must-not-leak");
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
