package com.wanghui.kfc.server.controller.rnorder.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端开始一次 RN 点餐浏览流程时提交的定位参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RnOrderStartParam", description = "开始 RN 点餐流程的定位参数")
public class RnOrderStartParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public RnOrderStartParam() { }

    /** 当前定位纬度十进制文本。 */
    @NotBlank(message = "纬度不能为空")
    @ToString.Exclude
    @Schema(description = "GCJ-02 纬度十进制文本", example = "31.2304",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;
    /** 当前定位经度十进制文本。 */
    @NotBlank(message = "经度不能为空")
    @ToString.Exclude
    @Schema(description = "GCJ-02 经度十进制文本", example = "121.4737",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;
    /** 已知的国标城市编码；首次定位时允许为空。 */
    @Pattern(regexp = "[0-9]{0,12}", message = "城市编码格式不正确")
    @Schema(description = "可选国标城市编码", example = "310000")
    private String gbCityCode;
}
