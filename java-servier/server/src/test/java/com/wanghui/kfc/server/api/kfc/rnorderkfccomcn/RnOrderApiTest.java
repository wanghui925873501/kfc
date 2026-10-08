package com.wanghui.kfc.server.api.kfc.rnorderkfccomcn;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.api.RnOrderApi;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderContext;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderLocation;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.vo.RnOrderInitResult;
import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 使用本机已登录账号手工验证 {@code rnorder.kfc.com.cn} 的只读选店与菜单接口。
 * 每个测试方法必须单独移除 {@link Disabled} 后运行，普通构建不会访问 KFC 上游。
 */
@SpringBootTest(properties = "basic.upstream.enabled.kfc=true")
public class RnOrderApiTest {
    /** RN 选店与菜单底层客户端。 */
    @Resource
    private RnOrderApi rnOrderApi;
    /** 按手机号读取已核对 token、安装和客户端档案的上下文服务。 */
    @Resource
    private KfcRequestContextService requestContexts;

    /**
     * 初始化 RN 点餐会话，并验证城市、附近门店和当前账号常用门店查询。
     */
    @Test
    @Disabled("填写本人授权手机号后，单独移除此方法的注解运行")
    void storeQueriesTest() {
        String phone = requiredLocalValue("KFC_RN_ORDER_TEST_PHONE");
        RnOrderLocation location = location("31.2304", "121.4737", "310000");
        RnOrderContext context = context(phone);

        RnOrderInitResult initialized = rnOrderApi.initializePreorder(context, location);
        assertSuccess("初始化", initialized.getResponse());
        RnOrderSession session = initialized.getSession();

        JsonNode city = rnOrderApi.getCityByLocation(context, session, location);
        assertSuccess("定位城市", city);
        printDataFields("定位城市", city);

        JsonNode cities = rnOrderApi.cities(context, session);
        assertSuccess("城市列表", cities);
        printDataFields("城市列表", cities);

        JsonNode nearby = rnOrderApi.searchStoresByLbs(context, session, location, "");
        assertSuccess("附近门店", nearby);
        int nearbyCount = stores(nearby).size();
        System.out.println("附近门店查询成功，返回门店数=" + nearbyCount);

        JsonNode customerStores = rnOrderApi.queryCustomerStores(context, session, location);
        assertSuccess("常用门店", customerStores);
        System.out.println("常用门店查询成功，返回门店数=" + stores(customerStores).size());
    }

    /**
     * 初始化 RN 点餐会话，并按所选城市和关键词验证门店搜索。
     */
    @Test
    @Disabled("填写本人授权手机号和搜索参数后，单独移除此方法的注解运行")
    void storeKeywordSearchTest() {
        String phone = requiredLocalValue("KFC_RN_ORDER_TEST_PHONE");
        RnOrderLocation location = location(requiredLocalValue("KFC_RN_ORDER_TEST_LATITUDE"),
                requiredLocalValue("KFC_RN_ORDER_TEST_LONGITUDE"),
                requiredLocalValue("KFC_RN_ORDER_TEST_GB_CITY_CODE"));
        String cityName = requiredLocalValue("KFC_RN_ORDER_TEST_CITY_NAME");
        String keyword = requiredLocalValue("KFC_RN_ORDER_TEST_KEYWORD");
        RnOrderContext context = context(phone);

        RnOrderInitResult initialized = rnOrderApi.initializePreorder(context, location);
        assertSuccess("初始化", initialized.getResponse());
        JsonNode response = rnOrderApi.searchStoresByCityCodeAndKeyword(context,
                initialized.getSession(), location, cityName, keyword);
        assertSuccess("门店关键词搜索", response);
        System.out.println("门店关键词搜索成功，返回门店数=" + stores(response).size());
    }

    /**
     * 校验门店并查询菜单；填写 {@code linkId} 时继续查询商品详情。
     */
    @Test
    @Disabled("填写本人授权手机号后，单独移除此方法的注解运行")
    void menuAndDetailTest() {
        String phone = requiredLocalValue("KFC_RN_ORDER_TEST_PHONE");
        String storeCode = localValue("KFC_RN_ORDER_TEST_STORE_CODE");
        String linkId = localValue("KFC_RN_ORDER_TEST_LINK_ID");
        RnOrderLocation location = location(requiredLocalValue("KFC_RN_ORDER_TEST_LATITUDE"),
                requiredLocalValue("KFC_RN_ORDER_TEST_LONGITUDE"),
                requiredLocalValue("KFC_RN_ORDER_TEST_GB_CITY_CODE"));
        RnOrderContext context = context(phone);

        RnOrderInitResult initialized = rnOrderApi.initializePreorder(context, location);
        assertSuccess("初始化", initialized.getResponse());
        RnOrderSession session = initialized.getSession();

        if (StrUtil.isBlank(storeCode)) {
            JsonNode nearby = rnOrderApi.searchStoresByLbs(context, session, location, "");
            assertSuccess("附近门店", nearby);
            storeCode = firstStoreCode(nearby);
        }

        JsonNode validated = rnOrderApi.validStore(context, session, location, storeCode);
        assertSuccess("门店校验", validated);
        JsonNode store = validated.path("data").path("store");
        if (!store.isObject()) {
            throw new IllegalStateException("门店校验成功但响应没有 data.store");
        }

        JsonNode menu = rnOrderApi.menuList(context, session, store);
        assertSuccess("菜单", menu);
        printDataFields("菜单", menu);

        if (StrUtil.isBlank(linkId)) {
            System.out.println("linkId 未填写，菜单验证完成，本次跳过商品详情查询");
            return;
        }
        JsonNode detail = rnOrderApi.menuDetail(context, session, linkId, true);
        assertSuccess("商品详情", detail);
        printDataFields("商品详情", detail);
    }

    private RnOrderContext context(String phone) {
        if (phone == null || !phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("先填写本人授权且已完成登录的手机号");
        }
        KfcRequestContext stored = requestContexts.load(phone)
                .orElseThrow(() -> new IllegalStateException("该手机号没有完整登录态，请先完成登录"));
        KfcUser user = stored.getKfcUser();
        KfcInstallation installation = stored.getInstallation();
        KfcPhoneInstallation profile = stored.getPhoneInstallation();
        if (user == null || installation == null || profile == null) {
            throw new IllegalStateException("本地登录态缺少用户或安装上下文");
        }
        RnOrderContext context = new RnOrderContext();
        context.setDeviceId(installation.getDeviceId());
        context.setUserCode(user.getUserCode());
        context.setTicket(stored.getUpstreamToken());
        context.setCityCode(profile.getCityCode());
        context.setUserAgent(profile.getUserAgent());
        return context.requireComplete();
    }

    private RnOrderLocation location(String latitude, String longitude, String gbCityCode) {
        RnOrderLocation location = new RnOrderLocation();
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setGbCityCode(gbCityCode);
        return location.requireValid();
    }

    private void assertSuccess(String stage, JsonNode response) {
        JsonNode code = response == null ? null : response.get("code");
        if (code == null || !code.canConvertToInt() || code.asInt() != 0) {
            String value = code == null ? "missing" : code.asText();
            throw new IllegalStateException(stage + "调用未成功，code=" + value
                    + "，请在 Reqable 查看本次响应");
        }
    }

    private JsonNode stores(JsonNode response) {
        JsonNode values = response.path("data").path("stores");
        if (!values.isArray()) {
            throw new IllegalStateException("门店响应没有 data.stores 数组");
        }
        return values;
    }

    private String firstStoreCode(JsonNode response) {
        JsonNode values = stores(response);
        if (values.isEmpty()) {
            throw new IllegalStateException("附近门店为空，请调整测试定位或手工填写 storeCode");
        }
        JsonNode first = values.get(0);
        String storeCode = first.path("storecode").asText("");
        if (StrUtil.isBlank(storeCode)) {
            storeCode = first.path("storeCode").asText("");
        }
        if (StrUtil.isBlank(storeCode)) {
            throw new IllegalStateException("第一家门店没有 storecode/storeCode");
        }
        return storeCode;
    }

    private void printDataFields(String stage, JsonNode response) {
        JsonNode data = response.path("data");
        if (!data.isObject()) {
            System.out.println(stage + "调用成功，响应没有 data 对象");
            return;
        }
        Iterable<String> names = () -> data.fieldNames();
        String fields = String.join(",", StreamSupport.stream(names.spliterator(), false).toList());
        System.out.println(stage + "调用成功，data 字段=" + fields);
    }

    /**
     * 为手工测试注入本机忽略配置中的密钥和数据库连接信息。
     *
     * @param registry Spring 测试属性注册器
     */
    @DynamicPropertySource
    static void localSecrets(DynamicPropertyRegistry registry) {
        Properties values = localProperties();
        values.forEach((key, value) -> registry.add((String) key, () -> value));
    }

    private static Properties localProperties() {
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(
                projectRoot().resolve(".env.kfc-reverse.local"), StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取本机忽略配置", e);
        }
        return values;
    }

    private static String requiredLocalValue(String name) {
        String value = localValue(name);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("本机忽略配置缺少 " + name);
        }
        return value;
    }

    private static String localValue(String name) {
        return localProperties().getProperty(name, "").trim();
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve(".env.kfc-reverse.local"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("缺少本机忽略配置");
        }
        return current;
    }
}
