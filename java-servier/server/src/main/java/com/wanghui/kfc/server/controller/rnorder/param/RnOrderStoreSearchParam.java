package com.wanghui.kfc.server.controller.rnorder.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端按所选城市和关键词查询门店时提交的参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RnOrderStoreSearchParam", description = "RN 点餐门店关键词搜索参数")
public class RnOrderStoreSearchParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public RnOrderStoreSearchParam() { }

    /** 服务端签发且绑定当前手机号的随机流程标识。 */
    @NotBlank(message = "点餐流程不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "点餐流程格式不正确")
    @ToString.Exclude
    @Schema(description = "RN 点餐随机流程标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flowId;
    /** 所选城市中心纬度十进制文本。 */
    @NotBlank(message = "纬度不能为空")
    @ToString.Exclude
    @Schema(description = "所选城市中心纬度", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;
    /** 所选城市中心经度十进制文本。 */
    @NotBlank(message = "经度不能为空")
    @ToString.Exclude
    @Schema(description = "所选城市中心经度", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;
    /** 所选城市的国标城市编码。 */
    @NotBlank(message = "城市编码不能为空")
    @Pattern(regexp = "[0-9]{1,12}", message = "城市编码格式不正确")
    @Schema(description = "所选城市国标编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gbCityCode;
    /** 所选城市的中文展示名称。 */
    @NotBlank(message = "城市名称不能为空")
    @Size(max = 64, message = "城市名称过长")
    @Schema(description = "所选城市中文名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cityName;
    /** 用户主动提交的餐厅名称或地址关键词。 */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 50, message = "搜索关键词过长")
    @Schema(description = "餐厅名称或地址关键词", requiredMode = Schema.RequiredMode.REQUIRED)
    private String keyword;
}
