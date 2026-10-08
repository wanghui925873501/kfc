package com.wanghui.kfc.server.api.phhs.apploginphdappcn;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.wanghui.kfc.basicapi.GzipResponseInterceptor;
import com.wanghui.kfc.db.entity.PhhsPhoneInstallation;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsSendSmsCodeVo;
import com.wanghui.kfc.server.identity.phhs.PhhsPhoneDeviceService;
import com.wanghui.kfc.server.login.phhs.PhhsLoginCaptchaService;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 使用账号持有人本人账号分别手动验证 PHHS 发送验证码与验证码登录。
 * 两个测试方法必须独立启用，完整 HTTP 报文只在手动运行时输出到本机控制台。
 */
@SpringBootTest(properties = "basic.upstream.enabled.phhs=true")
@Import(PhhsLoginApiTest.PacketLoggingConfiguration.class)
public class PhhsLoginApiTest {
    /** 必胜客统一登录与小辉版图形验证编排入口。 */
    @Resource private PhhsLoginCaptchaService captchaLogin;
    /** 按手机号生成并复用 PHHS 独占虚拟安装。 */
    @Resource private PhhsPhoneDeviceService phoneDevices;

    /**
     * 为本方法内填写的授权手机号发送一次验证码。
     *
     * @throws IOException 本机单次触发锁无法建立时
     */
    @Test
    @Disabled("当次明确授权发送短信后，单独移除此方法的注解运行")
    void sendSmsCodeTest() throws IOException {
        String phone = "18229301217";
        String runId = "phhs-virtual-installation-20261008-v1";
        if (!phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("先填写本人授权的手机号");
        }
        PhhsPhoneInstallation binding = testBinding(phone);
        PhhsLoginContext context = phoneDevices.loginContext(binding, "");
        PhhsSendSmsCodeParam param = new PhhsSendSmsCodeParam(phone, context);
        param.requireComplete();
        lock(SecureUtil.sha256(phone), "send", runId);

        PhhsSendSmsCodeVo response = captchaLogin.sendSmsCode(param);
        if (response == null || response.getErrCode() == null || response.getErrCode() != 0) {
            throw new IllegalStateException("短信响应未确认成功，请查看上方完整响应报文");
        }
        System.out.println("PHHS 短信请求成功，errCode=0");
    }

    /**
     * 使用本方法内填写的同一手机号和六位验证码执行一次登录。
     *
     * @throws IOException 本机单次触发锁无法建立时
     */
    @Test
    @Disabled("收到验证码并获得当次登录授权后，单独移除此方法的注解运行")
    void loginBySmsCodeTest() throws IOException {
        String phone = "";
        String smsCode = "";
        String runId = "phhs-virtual-installation-20261008-v1";
        if (!phone.matches("1[0-9]{10}")) {
            throw new IllegalArgumentException("先填写本人授权的手机号");
        }
        if (!smsCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("验证码需要六位数字");
        }
        PhhsPhoneInstallation binding = testBinding(phone);
        PhhsLoginContext context = phoneDevices.loginContext(binding, "");
        PhhsLoginBySmsCodeParam param = new PhhsLoginBySmsCodeParam(phone, smsCode, context);
        param.requireComplete();
        lock(SecureUtil.sha256(phone), "login", runId);

        PhhsLoginBySmsCodeVo response = captchaLogin.loginBySmsCode(param);
        if (response == null || response.getErrCode() == null || response.getErrCode() != 0
                || response.getData() == null || response.getData().getMainBrandData() == null
                || response.getData().getMainBrandData().getToken() == null
                || response.getData().getMainBrandData().getToken().isBlank()) {
            throw new IllegalStateException("登录未成功或响应缺少 token，请查看上方完整响应报文");
        }
        System.out.println("PHHS 验证码登录成功，敏感登录数据未再次输出");
    }

    private PhhsPhoneInstallation testBinding(String phone) {
        return phoneDevices.getOrCreate(phone, "430800",
                "Dalvik/2.1.0 (Linux; U; Android 9; Local Test Device Build/LOCAL) "
                        + "PH Mobile Android Client PHSuperAPP v6.59.1", "6.59.1");
    }

    private static void lock(String phoneHash, String phase, String runId) throws IOException {
        if (!runId.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new IllegalStateException("测试批次标识只能使用字母、数字、下划线和短横线");
        }
        Path directory = projectRoot().resolve(Path.of("抓包文件", "登录", "phhs-live-test"));
        Files.createDirectories(directory);
        try {
            Files.createFile(directory.resolve(phoneHash + "." + runId + "." + phase + ".lock"));
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("该手机号、本批次和阶段已触发过真实请求，不能自动重试", e);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("PROJECT_HANDOFF.md"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("无法定位项目根目录");
        }
        return current;
    }

    /** 只在当前手工测试中记录完整 HTTP 请求和响应。 */
    @TestConfiguration(proxyBeanMethods = false)
    public static class PacketLoggingConfiguration {
        /** 供 Spring 创建测试专用 HTTP 客户端配置。 */
        public PacketLoggingConfiguration() { }

        /**
         * 创建可重复读取响应正文的 HTTP 客户端，打印完整头和原始正文。
         *
         * @param builder Spring 提供的 HTTP 客户端构建器
         * @return 仅供当前 PHHS 手工测试使用的客户端
         */
        @Bean
        @Primary
        RestClient packetLoggingRestClient(RestClient.Builder builder) {
            SimpleClientHttpRequestFactory transport = new SimpleClientHttpRequestFactory();
            transport.setConnectTimeout(Duration.ofSeconds(3));
            transport.setReadTimeout(Duration.ofSeconds(8));
            return builder.requestFactory(new BufferingClientHttpRequestFactory(transport))
                    .requestInterceptor(new GzipResponseInterceptor())
                    .requestInterceptor((request, body, execution) -> {
                        System.out.println("===== HTTP REQUEST " + request.getMethod()
                                + " " + request.getURI() + " =====");
                        request.getHeaders().forEach((name, values) ->
                                System.out.println(name + ": " + String.join(", ", values)));
                        printBody(body);
                        ClientHttpResponse response = execution.execute(request, body);
                        System.out.println("===== HTTP RESPONSE "
                                + response.getStatusCode() + " =====");
                        response.getHeaders().forEach((name, values) ->
                                System.out.println(name + ": " + String.join(", ", values)));
                        printBody(decodedBody(response));
                        return response;
                    }).build();
        }

        private static byte[] decodedBody(ClientHttpResponse response) throws IOException {
            byte[] wireBody = response.getBody().readAllBytes();
            if (!"gzip".equalsIgnoreCase(
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
                return wireBody;
            }
            try (GZIPInputStream gzip = new GZIPInputStream(
                    new ByteArrayInputStream(wireBody))) {
                return gzip.readAllBytes();
            }
        }

        private static void printBody(byte[] body) {
            String raw = new String(body, StandardCharsets.UTF_8);
            System.out.println("body(raw): " + raw);
            try {
                System.out.println("body(pretty): "
                        + JSON.toJSONString(JSON.parse(raw), JSONWriter.Feature.PrettyFormat));
            } catch (JSONException ignored) {
                // 非 JSON 响应仍已在 raw 中完整输出。
            }
        }
    }
}
