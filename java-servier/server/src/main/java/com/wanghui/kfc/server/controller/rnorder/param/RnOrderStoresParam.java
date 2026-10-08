package com.wanghui.kfc.server.controller.rnorder.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端查询附近和常用门店时提交的流程与定位参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RnOrderStoresParam", description = "RN 点餐门店查询参数")
public class RnOrderStoresParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public RnOrderStoresParam() { }

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
    /** 门店查询使用的国标城市编码。 */
    @Pattern(regexp = "[0-9]{0,12}", message = "城市编码格式不正确")
    @Schema(description = "可选国标城市编码")
    private String gbCityCode;
    /** 前端当前已选择的门店编码；尚未选择时允许为空。 */
    @Size(max = 64, message = "门店编码过长")
    @Schema(description = "可选当前门店编码")
    private String storeCode;
}
