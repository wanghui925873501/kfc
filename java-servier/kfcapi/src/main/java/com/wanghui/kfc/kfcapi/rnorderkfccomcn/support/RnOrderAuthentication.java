package com.wanghui.kfc.kfcapi.rnorderkfccomcn.support;

import com.wanghui.kfc.kfcapi.KfcRequestSignature;
import com.wanghui.kfc.kfcapi.Upstream;
import com.wanghui.kfc.kfcapi.UpstreamAuthentication;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginProperties;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/** 按 Reqable 证据为 {@code rnorder.kfc.com.cn} 的固定路径选择签名或无签名协议。 */
@Component
public class RnOrderAuthentication implements UpstreamAuthentication {
    /** 已确认携带 {@code kbck/kbcts/kbsv} 的传输路径及其短签名路径。 */
    private static final Map<String, String> SIGNED_PATHS = Map.of(
            "/preorder-portal/api/v2/init/combine/preorder", "/api/v2/init/combine/preorder",
            "/store-portal/api/v2/store/recommendStorePop", "/api/v2/store/recommendStorePop",
            "/preorder-portal/api/v2/menu/list", "/api/v2/menu/list",
            "/preorder-portal/api/v3/menu/detail", "/api/v3/menu/detail");
    /** 已确认未携带 KFC 签名头的门店查询路径。 */
    private static final Set<String> UNSIGNED_PATHS = Set.of(
            "/store-portal/api/v2/city/getCityByRgeoCode",
            "/store-portal/api/v2/city/cities",
            "/store-portal/api/v2/store/searchByLbs",
            "/store-portal/api/v2/store/searchByCityCodeAndKeyword",
            "/store-portal/api/v2/customer/queryStores",
            "/store-portal/api/v2/store/validStore");
    /** 复用抓包已证明一致的 KFC 客户端签名材料。 */
    private final AppLoginProperties credentials;
    /** 生成毫秒签名时间戳的时钟。 */
    private final Clock clock;

    /**
     * 创建生产环境 RN 点餐鉴权器。
     *
     * @param credentials 本机 KFC 客户端签名材料
     */
    @Autowired
    public RnOrderAuthentication(AppLoginProperties credentials) {
        this(credentials, Clock.systemUTC());
    }

    RnOrderAuthentication(AppLoginProperties credentials, Clock clock) {
        this.credentials = credentials;
        this.clock = clock;
    }

    /**
     * 仅处理真实抓包确认的 RN 点餐上游。
     *
     * @param upstream 目标上游
     * @return 目标是否为 RN 点餐服务
     */
    @Override
    public boolean supports(Upstream upstream) {
        return upstream == Upstream.RN_ORDER;
    }

    /**
     * 为已验证的签名路径添加请求头，并对已验证无签名路径保持无操作。
     *
     * <p>APK/Reqable 证据：{@code 抓包文件/选店/01-[6736]-init-combine-preorder}、
     * {@code 02-[6738]-recommend-store-pop}、{@code 菜单首页/03-[6811]-menu-list} 和
     * {@code 商品详情/01-[7153]-menu-detail}。签名路径不包含 portal 前缀。</p>
     *
     * @param upstream 固定 RN 点餐上游
     * @param path 带 portal 前缀的传输路径
     * @param bodyJson 实际发送的 JSON 文本
     * @param headers 待补充的请求头
     * @throws IllegalStateException 路径尚无明确鉴权证据或签名材料缺失时
     */
    @Override
    public void apply(Upstream upstream, String path, String bodyJson, HttpHeaders headers) {
        if (upstream != Upstream.RN_ORDER) {
            throw new IllegalStateException("RN order authentication received the wrong upstream");
        }
        String signPath = SIGNED_PATHS.get(path);
        if (signPath == null) {
            if (UNSIGNED_PATHS.contains(path)) {
                return;
            }
            throw new IllegalStateException("RN order authentication is not configured for this request");
        }
        String key = credentials.requireClientKey();
        String secret = credentials.requireClientSecret();
        String timestamp = Long.toString(clock.millis());
        headers.set("kbck", key);
        headers.set("kbcts", timestamp);
        headers.set("kbsv", KfcRequestSignature.signPost(key, secret, timestamp, signPath, bodyJson));
    }
}
