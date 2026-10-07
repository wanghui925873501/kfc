package com.wanghui.kfc.server.api.apploginkfcappcn;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.wanghui.kfc.db.entity.AppUser;
import com.wanghui.kfc.db.entity.KfcInstallation;
import com.wanghui.kfc.db.entity.KfcUser;
import com.wanghui.kfc.db.service.AppUserService;
import com.wanghui.kfc.db.service.KfcInstallationService;
import com.wanghui.kfc.db.service.KfcUserService;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.api.AppLoginApi;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.AppLoginContext;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.LoginBySmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.param.SendSmsCodeParam;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.support.AppLoginCrypto;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.LoginBySmsCodeVo;
import com.wanghui.kfc.kfcapi.apploginkfcappcn.vo.SendSmsCodeVo;
import com.wanghui.kfc.server.identity.IssuedKfcSession;
import com.wanghui.kfc.server.identity.KfcIdentityService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * 使用用户本人账号分别手动验证发送验证码与验证码登录。
 * 两个测试方法独立启用，HTTP 报文仅在手动运行时打印到本机控制台。
 */
@SpringBootTest(properties = "kfc.upstream.enabled=true")
@Import(AppLoginApiTest.PacketLoggingConfiguration.class)
public class AppLoginApiTest {
    /** 已抓包确认的上游短信登录客户端。 */
    @Resource private AppLoginApi appLoginApi;
    /** 核对测试手机号与本机抓包密文是否属于同一账号。 */
    @Resource private AppLoginCrypto crypto;
    /** 登录身份和本地会话编排入口。 */
    @Resource private KfcIdentityService identityService;
    /** 本地账号持久化入口。 */
    @Resource private AppUserService appUsers;
    /** 安装记录持久化入口。 */
    @Resource private KfcInstallationService installations;
    /** 上游账号持久化入口。 */
    @Resource private KfcUserService kfcUsers;

    /**
     * 本地无 token 时，用本机授权抓包配置登记测试安装并发送一次验证码。
     * @throws IOException 本机单次触发锁无法建立时
     */
    @Test
    @Disabled("当次授权发送短信后，单独移除此方法的注解运行")
    void sendSmsCodeTest() throws IOException {
        String phone = "18229301217";
        String phoneHash = SecureUtil.sha256(phone);
        if (hasStoredToken(phone)) {
            System.out.println("kfc_user 已有 token，跳过发送验证码");
            return;
        }
        Properties values = localProperties();
        String cityCode = requiredLocal(values, "KFC_TEST_CITY_CODE");
        String userAgent = requiredLocal(values, "KFC_TEST_USER_AGENT");
        String rcsdcid = requiredLocal(values, "KFC_TEST_RCSDCID");
        String userCode = values.getProperty("KFC_TEST_USER_CODE", "");
        String runId = requiredLocal(values, "KFC_TEST_RUN_ID");
        requireCapturedPhone(values, phone);
        AppLoginContext context = testContext(capturedInstallation(values),
                cityCode, userAgent, rcsdcid, userCode);
        lock(phoneHash, "send", runId);
        SendSmsCodeParam param = new SendSmsCodeParam();
        param.setPhone(phone);
        param.setContext(context);
        SendSmsCodeVo response = appLoginApi.sendSmsCode(param);
        if (response != null && response.requiresHumanVerification()) {
            throw new IllegalStateException("上游要求人工验证，errCode=" + response.getErrCode()
                    + "；请由账号持有人在官方客户端完成，本测试不会自动重试");
        }
        if (response == null || response.getErrCode() == null || response.getErrCode() != 0) {
            throw new IllegalStateException("短信响应未确认成功，请查看上方完整响应报文");
        }
    }

    /**
     * 使用本方法内填写的验证码登录，并校验用户、token 和本地会话已持久化。
     * @throws IOException 本机单次触发锁无法建立时
     */
    @Test
    @Disabled("收到验证码并获得当次登录授权后，单独移除此方法的注解运行")
    void loginBySmsCodeTest() throws IOException {
        String phone = "18229301217";
        String smsCode = "";
        String phoneHash = SecureUtil.sha256(phone);
        if (hasStoredToken(phone)) {
            System.out.println("kfc_user 已有 token，跳过验证码登录");
            return;
        }
        if (!smsCode.matches("[0-9]{6}")) throw new IllegalArgumentException("验证码需要六位数字");
        Properties values = localProperties();
        String cityCode = requiredLocal(values, "KFC_TEST_CITY_CODE");
        String userAgent = requiredLocal(values, "KFC_TEST_USER_AGENT");
        String rcsdcid = requiredLocal(values, "KFC_TEST_LOGIN_RCSDCID");
        String userCode = values.getProperty("KFC_TEST_USER_CODE", "");
        String runId = requiredLocal(values, "KFC_TEST_RUN_ID");
        requireCapturedPhone(values, phone);
        KfcInstallation installation = capturedInstallation(values);
        AppLoginContext context = testContext(installation, cityCode, userAgent, rcsdcid, userCode);
        lock(phoneHash, "login", runId);
        LoginBySmsCodeParam param = new LoginBySmsCodeParam();
        param.setPhone(phone);
        param.setSmsCode(smsCode);
        param.setContext(context);
        LoginBySmsCodeVo response = appLoginApi.loginBySmsCode(param);
        if (response == null || response.getErrCode() == null || response.getErrCode() != 0) {
            throw new IllegalStateException("登录未成功，请查看上方完整响应报文");
        }
        AppUser appUser = findOrCreateUser(phoneHash);
        IssuedKfcSession session = identityService.recordLoginSuccess(
                appUser.getId(), installation.getInstallationId(), phone, response);
        KfcUser stored = kfcUsers.getById(session.getKfcUserId());
        String token = response.getData().getMainBrandData().getToken();
        if (stored == null || !phone.equals(stored.getPhonePlain())
                || StrUtil.isBlank(stored.getPhoneCiphertext())
                || !token.equals(stored.getTokenPlain())
                || StrUtil.isBlank(stored.getTokenCiphertext())
                || stored.getTokenUpdatedAt() == null
                || identityService.resolveUpstreamContext(session.getSessionId(),
                installation.getInstallationId()).isEmpty()) {
            throw new IllegalStateException("登录成功但数据库或本地会话校验失败");
        }
        System.out.println("登录成功，手机号和 token 明密文字段已写入 kfc_user");
    }

    private boolean hasStoredToken(String phone) {
        KfcUser existing = kfcUsers.lambdaQuery().eq(KfcUser::getBrand, "KFC")
                .eq(KfcUser::getPhonePlain, phone).one();
        return existing != null && StrUtil.isNotBlank(existing.getTokenPlain())
                && StrUtil.isNotBlank(existing.getTokenCiphertext());
    }

    private KfcInstallation capturedInstallation(Properties values) {
        String installationId = requiredLocal(values, "KFC_TEST_INSTALLATION_ID");
        String deviceId = requiredLocal(values, "KFC_TEST_DEVICE_ID");
        String tdid = requiredLocal(values, "KFC_TEST_TDID");
        String appVersion = requiredLocal(values, "KFC_TEST_APP_VERSION");
        String jpushRegId = values.getProperty("KFC_TEST_JPUSH_REG_ID", "");
        KfcInstallation installation = installations.findByInstallationId(installationId);
        if (installation == null) {
            installation = new KfcInstallation();
            installation.setInstallationId(installationId);
            installation.setDeviceId(deviceId);
            installation.setTdid(tdid);
            installation.setJpushRegId(jpushRegId);
            installation.setPlatform("android-apk-test");
            installation.setAppVersion(appVersion);
            installation.setCreatedAt(LocalDateTime.now());
            installation.setLastSeenAt(LocalDateTime.now());
            if (!installations.save(installation)) {
                throw new IllegalStateException("本机授权测试安装记录保存失败");
            }
        } else if (!Objects.equals(installation.getDeviceId(), deviceId)
                || !Objects.equals(installation.getTdid(), tdid)
                || !Objects.equals(installation.getAppVersion(), appVersion)
                || !"android-apk-test".equalsIgnoreCase(installation.getPlatform())) {
            throw new IllegalStateException("已登记安装记录与本机抓包设备上下文不一致");
        }
        return installation;
    }

    private AppLoginContext testContext(KfcInstallation installation, String cityCode,
                                        String userAgent, String rcsdcid, String userCode) {
        if (StrUtil.isBlank(cityCode) || StrUtil.isBlank(userAgent) || StrUtil.isBlank(rcsdcid)) {
            throw new IllegalStateException("先填写当前客户端的城市编码、User-Agent 和当次 SDK rcsdcid");
        }
        AppLoginContext context = new AppLoginContext();
        context.setDeviceId(installation.getDeviceId());
        context.setTdid(installation.getTdid());
        context.setJPushRegId(StrUtil.nullToEmpty(installation.getJpushRegId()));
        context.setCityCode(cityCode);
        context.setChannel("app");
        context.setUserCode(userCode);
        context.setRcsdcid(rcsdcid);
        context.setRcsav(installation.getAppVersion());
        context.setUserAgent(userAgent);
        context.requireComplete();
        return context;
    }

    private static String requiredLocal(Properties values, String key) {
        String value = values.getProperty(key);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("本机忽略配置缺少 " + key);
        }
        return value;
    }

    private void requireCapturedPhone(Properties values, String phone) {
        if (!crypto.encrypt(phone).equals(requiredLocal(values, "KFC_TEST_PHONE_CIPHERTEXT"))) {
            throw new IllegalStateException("测试手机号与本机抓包中的加密手机号不一致");
        }
    }

    private AppUser findOrCreateUser(String phoneHash) {
        AppUser user = appUsers.lambdaQuery().eq(AppUser::getPhoneHash, phoneHash).one();
        if (user != null) return user;
        user = new AppUser();
        user.setNickname("KFC用户");
        user.setPhoneHash(phoneHash);
        if (!appUsers.save(user) || user.getId() == null) {
            throw new IllegalStateException("本地账号保存失败");
        }
        return user;
    }

    private void lock(String phoneHash, String phase, String runId) throws IOException {
        if (!runId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalStateException("KFC_TEST_RUN_ID 需要是本机生成的 UUID");
        }
        Path directory = projectRoot().resolve(Path.of("抓包文件", "登录", "live-test"));
        Files.createDirectories(directory);
        try {
            Files.createFile(directory.resolve(phoneHash + "." + runId + "." + phase + ".lock"));
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("该手机号本阶段已触发过真实请求，不能自动重试", e);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve(".env.kfc-reverse.local"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("缺少本机忽略配置");
        return current;
    }

    /**
     * 为手动测试注入本机密钥；设备上下文由本机忽略配置读取。
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

    /** 只在此手动测试中记录完整 HTTP 请求和响应。 */
    @TestConfiguration(proxyBeanMethods = false)
    public static class PacketLoggingConfiguration {
        /** 供 Spring 创建测试专用 HTTP 客户端配置。 */
        public PacketLoggingConfiguration() { }

        /**
         * 创建可重复读取响应正文的 HTTP 客户端，打印完整头和原始正文。
         * @param builder Spring 提供的 HTTP 客户端构建器
         * @return 仅供此测试使用的客户端
         */
        @Bean
        @Primary
        RestClient packetLoggingRestClient(RestClient.Builder builder) {
            SimpleClientHttpRequestFactory transport = new SimpleClientHttpRequestFactory();
            transport.setConnectTimeout(Duration.ofSeconds(3));
            transport.setReadTimeout(Duration.ofSeconds(8));
            return builder.requestFactory(new BufferingClientHttpRequestFactory(transport))
                    .requestInterceptor((request, body, execution) -> {
                        System.out.println("===== HTTP REQUEST " + request.getMethod()
                                + " " + request.getURI() + " =====");
                        request.getHeaders().forEach((name, values) ->
                                System.out.println(name + ": " + String.join(", ", values)));
                        printBody(body);
                        ClientHttpResponse response = execution.execute(request, body);
                        System.out.println("===== HTTP RESPONSE " + response.getStatusCode() + " =====");
                        response.getHeaders().forEach((name, values) ->
                                System.out.println(name + ": " + String.join(", ", values)));
                        printBody(response.getBody().readAllBytes());
                        return response;
                    }).build();
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
