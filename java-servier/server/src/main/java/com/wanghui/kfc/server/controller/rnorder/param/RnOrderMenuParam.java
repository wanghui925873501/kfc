package com.wanghui.kfc.server.controller.rnorder.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端进入菜单页时提交的流程、定位和门店参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RnOrderMenuParam", description = "RN 点餐菜单查询参数")
public class RnOrderMenuParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public RnOrderMenuParam() { }

    /** 服务端签发且绑定当前手机号的随机流程标识。 */
    @NotBlank(message = "点餐流程不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "点餐流程格式不正确")
    @ToString.Exclude
    @Schema(description = "RN 点餐随机流程标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flowId;
    /** 当前定位纬度十进制文本。 */
    @NotBlank(message = "纬度不能为空")
    @ToString.Exclude
    @Schema(description = "GCJ-02 纬度十进制文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;
    /** 当前定位经度十进制文本。 */
    @NotBlank(message = "经度不能为空")
    @ToString.Exclude
    @Schema(description = "GCJ-02 经度十进制文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;
    /** 门店所在国标城市编码。 */
    @Pattern(regexp = "[0-9]{0,12}", message = "城市编码格式不正确")
    @Schema(description = "可选国标城市编码")
    private String gbCityCode;
    /** 用户在选店页确认的门店编码。 */
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 64, message = "门店编码过长")
    @Schema(description = "已选择门店编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeCode;
}
