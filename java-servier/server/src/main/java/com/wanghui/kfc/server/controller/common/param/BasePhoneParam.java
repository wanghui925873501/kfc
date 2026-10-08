package com.wanghui.kfc.server.controller.common.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

/** 所有前端业务请求共同携带的手机号参数，作为内部系统的统一通行标识。 */
@Data
@Schema(name = "BasePhoneParam", description = "内部接口统一手机号参数")
public class BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public BasePhoneParam() { }

    /** 当前请求对应的中国大陆手机号。 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "1[0-9]{10}", message = "手机号格式不正确")
    @ToString.Exclude
    @Schema(description = "内部业务使用的 11 位手机号",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = 11, maxLength = 11)
    private String phone;
}
