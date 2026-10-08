package com.wanghui.kfc.server.controller.login;

import static org.assertj.core.api.Assertions.assertThat;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import com.wanghui.kfc.server.controller.login.dto.LoginNextAction;
import com.wanghui.kfc.server.controller.login.dto.LoginSessionDto;
import com.wanghui.kfc.server.controller.login.param.SendSmsCodeParam;
import com.wanghui.kfc.server.controller.login.param.SmsCodeLoginParam;
import com.wanghui.kfc.server.controller.login.vo.SendSmsCodeVo;
import com.wanghui.kfc.server.controller.login.vo.SmsCodeLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** 防止登录接口和数据模型在后续修改中丢失 OpenAPI 3 注解。 */
class OpenApiContractTest {
    @Test
    void controllerAndPublicModelsDeclareOpenApiAnnotations() {
        assertThat(LoginController.class.getAnnotation(Tag.class)).isNotNull();
        for (Method method : LoginController.class.getDeclaredMethods()) {
            if (method.getName().equals("sendSmsCode") || method.getName().equals("loginBySmsCode")) {
                assertThat(method.getAnnotation(Operation.class)).isNotNull();
            }
        }
        assertSchema(BasePhoneParam.class);
        assertSchema(SendSmsCodeParam.class);
        assertSchema(SmsCodeLoginParam.class);
        assertSchema(LoginSessionDto.class);
        assertSchema(LoginNextAction.class);
        assertSchema(SendSmsCodeVo.class);
        assertSchema(SmsCodeLoginVo.class);
        assertThat(SendSmsCodeParam.class.getSuperclass()).isEqualTo(BasePhoneParam.class);
        assertThat(SmsCodeLoginParam.class.getSuperclass()).isEqualTo(BasePhoneParam.class);
    }

    private void assertSchema(Class<?> type) {
        assertThat(type.getAnnotation(Schema.class)).as(type.getName()).isNotNull();
    }
}
