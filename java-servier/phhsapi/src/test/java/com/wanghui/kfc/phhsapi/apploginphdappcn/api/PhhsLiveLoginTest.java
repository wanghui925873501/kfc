package com.wanghui.kfc.phhsapi.apploginphdappcn.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghui.kfc.basicapi.Brand;
import com.wanghui.kfc.basicapi.GzipResponseInterceptor;
import com.wanghui.kfc.basicapi.UpstreamGateway;
import com.wanghui.kfc.basicapi.UpstreamProperties;
import com.wanghui.kfc.phhsapi.PhhsUpstream;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginBySmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsLoginContext;
import com.wanghui.kfc.phhsapi.apploginphdappcn.param.PhhsSendSmsCodeParam;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginAuthentication;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginCrypto;
import com.wanghui.kfc.phhsapi.apploginphdappcn.support.PhhsLoginProperties;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsLoginBySmsCodeVo;
import com.wanghui.kfc.phhsapi.apploginphdappcn.vo.PhhsSendSmsCodeVo;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** 使用账号持有人当次授权手动验证 PHHS Java 发码和验证码登录。 */
class PhhsLiveLoginTest {
    @Test
    @Disabled("当次明确授权发送短信后，仅单独启用本方法")
    void sendSmsCodeOnce() throws IOException {
        String phone = requiredEnvironment("PHHS_LIVE_PHONE");
        lock(phone, "send");
        PhhsSendSmsCodeVo response = api().sendSmsCode(
                new PhhsSendSmsCodeParam(phone, context()));
        assertThat(response.getErrCode()).isZero();
        System.out.println("PHHS 短信请求成功，errCode=0");
    }

    @Test
    @Disabled("收到验证码并获得当次登录授权后，仅单独启用本方法")
    void loginBySmsCodeOnce() throws IOException {
        String phone = requiredEnvironment("PHHS_LIVE_PHONE");
        String smsCode = requiredEnvironment("PHHS_LIVE_SMS_CODE");
        lock(phone, "login");
        PhhsLoginBySmsCodeVo response = api().loginBySmsCode(
                new PhhsLoginBySmsCodeParam(phone, smsCode, context()));
        assertThat(response.getErrCode()).isZero();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getMainBrandData()).isNotNull();
        assertThat(response.getData().getMainBrandData().getToken()).isNotBlank();
        System.out.println("PHHS 验证码登录成功，敏感响应未输出");
    }

    private static PhhsLoginApi api() {
        Properties values = localProperties();
        PhhsLoginProperties login = new PhhsLoginProperties();
        login.setClientKey(required(values, "PHHS_CLIENT_KEY"));
        login.setClientSecret(required(values, "PHHS_CLIENT_SEC"));
        login.setDesKey(required(values, "PHHS_DES_KEY"));
        login.setRequestSecretKey(values.getProperty("PHHS_LOGIN_REQUEST_SECRET_KEY", "phhs"));
        UpstreamProperties upstream = new UpstreamProperties();
        upstream.getEnabled().put(Brand.PHHS, true);
        upstream.getUrls().put(PhhsUpstream.APP_LOGIN.configurationKey(),
                URI.create(values.getProperty(
                        "PHHS_APP_LOGIN_URL", "https://applogin.phdapp.cn")));
        RestClient client = RestClient.builder()
                .requestInterceptor(new GzipResponseInterceptor()).build();
        UpstreamGateway gateway = new UpstreamGateway(
                client, upstream, new PhhsLoginAuthentication(login));
        ObjectMapper mapper = new ObjectMapper();
        return new PhhsLoginApi(gateway, new PhhsLoginCrypto(login), login, mapper);
    }

    private static PhhsLoginContext context() {
        Properties values = localProperties();
        return new PhhsLoginContext(
                required(values, "PHHS_LOGIN_TDID"),
                required(values, "PHHS_LOGIN_DEVICE_ID"),
                values.getProperty("PHHS_LOGIN_JPUSH_ID", ""),
                required(values, "PHHS_LOGIN_CITY_CODE"),
                values.getProperty("PHHS_LOGIN_CHANNEL", "app"),
                values.getProperty("PHHS_LOGIN_USER_CODE", ""),
                required(values, "PHHS_LOGIN_RCSDCID"),
                required(values, "PHHS_LOGIN_RCSAV"),
                required(values, "PHHS_LOGIN_USER_AGENT"));
    }

    private static void lock(String phone, String phase) throws IOException {
        String runId = requiredEnvironment("PHHS_LIVE_RUN_ID");
        if (!runId.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("PHHS_LIVE_RUN_ID 格式无效");
        }
        Path directory = projectRoot().resolve(Path.of("抓包文件", "登录", "phhs-live-test"));
        Files.createDirectories(directory);
        Path lock = directory.resolve(sha256(phone) + "." + runId + "." + phase + ".lock");
        try {
            Files.createFile(lock);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("该手机号、本批次和阶段已经触发，拒绝重复请求", e);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be provided for this manual run");
        }
        return value;
    }

    private static String required(Properties values, String name) {
        String value = values.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is missing from .env.kfc-reverse.local");
        }
        return value;
    }

    private static Properties localProperties() {
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(
                projectRoot().resolve(".env.kfc-reverse.local"), StandardCharsets.UTF_8)) {
            values.load(reader);
            return values;
        } catch (IOException e) {
            throw new IllegalStateException("无法读取本机忽略配置", e);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve(".env.kfc-reverse.local"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("缺少本机忽略配置 .env.kfc-reverse.local");
        }
        return current;
    }
}
