package com.wanghui.kfc.server.web.logging;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 将 API 日志中的手机号、验证码、token 和设备标识替换为不可直接使用的值。 */
@Component
public class ApiLogSanitizer {
    /** 供 Spring 创建日志脱敏器。 */
    public ApiLogSanitizer() { }

    /**
     * 脱敏 JSON 文本并限制最终日志长度。
     *
     * @param json 原始 JSON 文本
     * @param maxLength 最大输出字符数
     * @return 可安全写入普通应用日志的单行文本
     */
    public String sanitizeJson(String json, int maxLength) {
        if (StrUtil.isBlank(json)) return "<empty>";
        try {
            Object parsed = JSON.parse(json);
            sanitizeValue(parsed);
            return truncate(JSON.toJSONString(parsed), maxLength);
        } catch (JSONException | IllegalArgumentException e) {
            return "<invalid-json>";
        }
    }

    /**
     * 脱敏查询参数并限制最终日志长度。
     *
     * @param parameters Servlet 查询参数映射
     * @param maxLength 最大输出字符数
     * @return 可安全写入普通应用日志的单行 JSON
     */
    public String sanitizeParameters(Map<String, String[]> parameters, int maxLength) {
        if (parameters == null || parameters.isEmpty()) return "{}";
        JSONObject json = new JSONObject();
        parameters.forEach((key, values) -> {
            if (values == null) {
                json.put(key, null);
            } else if (values.length == 1) {
                json.put(key, values[0]);
            } else {
                json.put(key, values);
            }
        });
        sanitizeValue(json);
        return truncate(JSON.toJSONString(json), maxLength);
    }

    private void sanitizeValue(Object value) {
        if (value instanceof JSONObject object) {
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                String key = entry.getKey();
                Object current = entry.getValue();
                if (isPhoneKey(key)) {
                    entry.setValue(maskPhone(current));
                } else if (isSecretKey(key)) {
                    entry.setValue(maskValue(current));
                } else {
                    sanitizeValue(current);
                }
            }
        } else if (value instanceof JSONArray array) {
            array.forEach(this::sanitizeValue);
        }
    }

    private boolean isPhoneKey(String key) {
        String normalized = normalize(key);
        return normalized.equals("phone") || normalized.equals("mobile")
                || normalized.contains("phoneplain") || normalized.contains("mobileplain");
    }

    private boolean isSecretKey(String key) {
        String normalized = normalize(key);
        return normalized.equals("smscode") || normalized.equals("verificationcode")
                || normalized.contains("token") || normalized.contains("secret")
                || normalized.contains("password") || normalized.equals("pwd")
                || normalized.contains("ciphertext") || normalized.equals("authorization")
                || normalized.equals("cookie") || normalized.equals("sessionid")
                || normalized.equals("installationid") || normalized.equals("deviceid")
                || normalized.equals("tdid") || normalized.equals("rcsdcid")
                || normalized.equals("apikey");
    }

    private String normalize(String key) {
        if (key == null) return "";
        return key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String maskPhone(Object value) {
        String phone = value == null ? "" : String.valueOf(value);
        if (phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Object maskValue(Object value) {
        if (value == null) return null;
        if (value instanceof JSONArray array) {
            JSONArray masked = new JSONArray();
            for (int index = 0; index < array.size(); index++) masked.add("***");
            return masked;
        }
        return "***";
    }

    private String truncate(String value, int maxLength) {
        int safeLength = Math.max(128, maxLength);
        if (value.length() <= safeLength) return value;
        return value.substring(0, safeLength) + "...<truncated>";
    }
}
