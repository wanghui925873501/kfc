package com.wanghui.kfc.kfcapi.apploginkfcappcn.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.Captcha3ResultVo;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 只用本地模拟 HTTP 响应核验小辉服务协议。 */
class Captcha3ClientTest {
    @Test
    void acceptsNestedSuccessAndSendsApiKey() {
        Captcha3Properties properties = new Captcha3Properties();
        properties.setEnabled(true);
        properties.setUrl("http://192.168.1.8:16254/captcha3");
        properties.setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(properties.getUrl())).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andExpect(content().string("gt=test-gt&challenge=test-challenge"))
                .andRespond(withSuccess("{\"data\":{\"code\":{\"result\":\"success\","
                        + "\"challenge\":\"refreshed-challenge\",\"validate\":\"test-validate\"}}}",
                        MediaType.APPLICATION_JSON));
        Captcha3ResultVo result = new Captcha3Client(builder.build(), properties)
                .solve("test-gt", "test-challenge");
        assertThat(result.getChallenge()).isEqualTo("refreshed-challenge");
        assertThat(result.getSeccode()).isEqualTo("test-validate|jordan");
        server.verify();
    }

    @Test
    void stopsWhenServiceReturnsFailure() {
        Captcha3Properties properties = new Captcha3Properties();
        properties.setEnabled(true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(properties.getUrl())).andRespond(withSuccess(
                "{\"result\":\"fail\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new Captcha3Client(builder.build(), properties)
                .solve("test-gt", "test-challenge"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void rejectsUnkeyedRemoteAndDisabledServiceBeforeNetwork() {
        Captcha3Properties properties = new Captcha3Properties();
        properties.setUrl("http://192.168.1.8:16254/captcha3");
        Captcha3Client client = new Captcha3Client(RestClient.create(), properties);
        assertThatThrownBy(() -> client.solve("test-gt", "test-challenge"))
                .hasMessageContaining("尚未启用");
        properties.setEnabled(true);
        assertThatThrownBy(() -> client.solve("test-gt", "test-challenge"))
                .hasMessageContaining("API Key");
    }

    @Test
    void surfacesTransportTimeoutWithoutReturningAProof() {
        Captcha3Properties properties = new Captcha3Properties();
        properties.setEnabled(true);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(properties.getUrl())).andRespond(request -> {
            throw new IOException("synthetic timeout");
        });
        assertThatThrownBy(() -> new Captcha3Client(builder.build(), properties)
                .solve("test-gt", "test-challenge"))
                .hasMessageContaining("失败或超时");
        server.verify();
    }
}
