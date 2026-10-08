package com.wanghui.kfc.server.controller.rnorder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderFlowVo;
import com.wanghui.kfc.server.controller.rnorder.vo.RnOrderStoreSearchVo;
import com.wanghui.kfc.server.rnorder.RnOrderService;
import com.wanghui.kfc.server.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RnOrderControllerTest {
    @Test
    void startsFlowWithoutExposingUpstreamSessionFields() throws Exception {
        RnOrderService service = mock(RnOrderService.class);
        RnOrderFlowVo response = new RnOrderFlowVo();
        response.setFlowId("a".repeat(43));
        response.setExpiresInSeconds(900L);
        when(service.start("13800000000", "31.2", "121.4", "310000"))
                .thenReturn(response);

        mvc(service).perform(post("/api/v1/rn-order/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000000","latitude":"31.2",
                                "longitude":"121.4","gbCityCode":"310000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowId").value("a".repeat(43)))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sessionId"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("cookies"))));
    }

    @Test
    void rejectsMalformedFlowBeforeServiceCall() throws Exception {
        RnOrderService service = mock(RnOrderService.class);

        mvc(service).perform(post("/api/v1/rn-order/products/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000000","flowId":"bad","linkId":"p1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void searchesStoresWithValidatedCityAndKeyword() throws Exception {
        RnOrderService service = mock(RnOrderService.class);
        when(service.searchStores("13800000000", "a".repeat(43), "25.7", "113.0",
                "431000", "郴州", "龙泉")).thenReturn(new RnOrderStoreSearchVo());

        mvc(service).perform(post("/api/v1/rn-order/stores/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000000","flowId":"%s",
                                "latitude":"25.7","longitude":"113.0",
                                "gbCityCode":"431000","cityName":"郴州","keyword":"龙泉"}
                                """.formatted("a".repeat(43))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores").isArray());
    }

    @Test
    void rejectsBlankStoreSearchKeyword() throws Exception {
        RnOrderService service = mock(RnOrderService.class);

        mvc(service).perform(post("/api/v1/rn-order/stores/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000000","flowId":"%s",
                                "latitude":"25.7","longitude":"113.0",
                                "gbCityCode":"431000","cityName":"郴州","keyword":" "}
                                """.formatted("a".repeat(43))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private MockMvc mvc(RnOrderService service) {
        return MockMvcBuilders.standaloneSetup(new RnOrderController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
}
