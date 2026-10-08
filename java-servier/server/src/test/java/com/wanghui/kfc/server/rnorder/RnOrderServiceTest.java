package com.wanghui.kfc.server.rnorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcPhoneInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.api.RnOrderApi;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderContext;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderLocation;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.param.RnOrderSession;
import com.wanghui.kfc.kfcapi.rnorderkfccomcn.vo.RnOrderInitResult;
import com.wanghui.kfc.server.context.KfcRequestContext;
import com.wanghui.kfc.server.context.KfcRequestContextHolder;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoreSearchVo;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RnOrderServiceTest {
    /** 不对应真实账号的测试手机号。 */
    private static final String PHONE = "13800000000";
    /** 符合格式的固定测试流程标识。 */
    private static final String FLOW_ID = "a".repeat(43);
    /** JSON 测试数据解析器。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void initializesAndStoresPhoneBoundUpstreamSession() throws Exception {
        RnOrderApi api = mock(RnOrderApi.class);
        KfcRequestContextHolder holder = mock(KfcRequestContextHolder.class);
        RnOrderFlowStore store = mock(RnOrderFlowStore.class);
        RnOrderViewMapper viewMapper = mock(RnOrderViewMapper.class);
        when(holder.require()).thenReturn(request());
        RnOrderInitResult initialized = new RnOrderInitResult();
        initialized.setResponse(json("{\"code\":0,\"data\":{\"sessionId\":\"internal\"}}"));
        initialized.setSession(session());
        when(api.initializePreorder(any(RnOrderContext.class), any(RnOrderLocation.class)))
                .thenReturn(initialized);
        JsonNode city = json("{\"code\":0,\"data\":{\"city\":{}}}");
        JsonNode cities = json("{\"code\":0,\"data\":{\"allCities\":[]}}");
        when(api.getCityByLocation(any(), any(), any())).thenReturn(city);
        when(api.cities(any(), any())).thenReturn(cities);
        when(store.issue(any())).thenReturn(FLOW_ID);
        when(store.expiresInSeconds()).thenReturn(900L);
        when(viewMapper.flow(city, cities)).thenReturn(new RnOrderFlowVo());
        RnOrderService service = new RnOrderService(api, holder, store, viewMapper);

        RnOrderFlowVo result = service.start(PHONE, "31.2", "121.4", "310000");

        assertThat(result.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(result.getExpiresInSeconds()).isEqualTo(900L);
        ArgumentCaptor<RnOrderFlowState> state = ArgumentCaptor.forClass(RnOrderFlowState.class);
        verify(store).issue(state.capture());
        assertThat(state.getValue().getPhoneHash()).isEqualTo(SecureUtil.sha256(PHONE));
        assertThat(state.getValue().getSession()).isSameAs(initialized.getSession());
        ArgumentCaptor<RnOrderContext> upstreamContext =
                ArgumentCaptor.forClass(RnOrderContext.class);
        verify(api).initializePreorder(upstreamContext.capture(), any());
        assertThat(upstreamContext.getValue().getDeviceId()).isEqualTo("device-id");
        assertThat(upstreamContext.getValue().getTicket()).isEqualTo("upstream-ticket");
    }

    @Test
    void rejectsExpiredOrCrossPhoneFlowBeforeCallingUpstream() {
        RnOrderApi api = mock(RnOrderApi.class);
        KfcRequestContextHolder holder = mock(KfcRequestContextHolder.class);
        RnOrderFlowStore store = mock(RnOrderFlowStore.class);
        when(holder.require()).thenReturn(request());
        when(store.resolve(FLOW_ID, SecureUtil.sha256(PHONE))).thenReturn(Optional.empty());
        RnOrderService service = new RnOrderService(api, holder, store,
                mock(RnOrderViewMapper.class));

        assertThatThrownBy(() -> service.detail(PHONE, FLOW_ID, "product-id"))
                .isInstanceOf(RnOrderException.class)
                .extracting("code").isEqualTo("RN_ORDER_FLOW_EXPIRED");
        verify(api, never()).menuDetail(any(), any(), anyString(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void searchesStoresWithPhoneBoundFlow() throws Exception {
        RnOrderApi api = mock(RnOrderApi.class);
        KfcRequestContextHolder holder = mock(KfcRequestContextHolder.class);
        RnOrderFlowStore store = mock(RnOrderFlowStore.class);
        RnOrderViewMapper viewMapper = mock(RnOrderViewMapper.class);
        RnOrderFlowState state = new RnOrderFlowState();
        state.setSession(session());
        when(holder.require()).thenReturn(request());
        when(store.resolve(FLOW_ID, SecureUtil.sha256(PHONE))).thenReturn(Optional.of(state));
        JsonNode response = json("{\"code\":0,\"data\":{\"stores\":[]}}");
        when(api.searchStoresByCityCodeAndKeyword(any(), any(), any(), anyString(),
                anyString())).thenReturn(response);
        RnOrderStoreSearchVo mapped = new RnOrderStoreSearchVo();
        when(viewMapper.searchStores(response)).thenReturn(mapped);
        RnOrderService service = new RnOrderService(api, holder, store, viewMapper);

        assertThat(service.searchStores(PHONE, FLOW_ID, "25.7", "113.0", "431000",
                " 郴州 ", " 龙泉 ")).isSameAs(mapped);
        verify(api).searchStoresByCityCodeAndKeyword(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("郴州"),
                org.mockito.ArgumentMatchers.eq("龙泉"));
        verify(store).save(FLOW_ID, state);
    }

    private KfcRequestContext request() {
        KfcInstallation installation = new KfcInstallation();
        installation.setDeviceId("device-id");
        KfcPhoneInstallation binding = new KfcPhoneInstallation();
        binding.setCityCode("310000");
        binding.setUserAgent("test-user-agent");
        KfcUser user = new KfcUser();
        user.setUserCode("user-code");
        KfcRequestContext request = new KfcRequestContext();
        request.setPhone(PHONE);
        request.setInstallation(installation);
        request.setPhoneInstallation(binding);
        request.setKfcUser(user);
        request.setUpstreamToken("upstream-ticket");
        return request;
    }

    private RnOrderSession session() {
        RnOrderSession session = new RnOrderSession();
        session.setSessionId("internal-session");
        session.setRouteCell("internal-route");
        LinkedHashMap<String, String> cookies = new LinkedHashMap<>();
        cookies.put("route-cell", "a");
        cookies.put("sessionIdCookie", "b");
        cookies.put("sessionIdCookie.sig", "c");
        session.setCookies(cookies);
        return session;
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }
}
