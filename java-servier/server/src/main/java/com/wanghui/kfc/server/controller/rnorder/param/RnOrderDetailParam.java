package com.wanghui.kfc.server.controller.rnorder.param;

import com.wanghui.kfc.server.controller.common.param.BasePhoneParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 前端打开商品详情时提交的流程和商品标识。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RnOrderDetailParam", description = "RN 点餐商品详情查询参数")
public class RnOrderDetailParam extends BasePhoneParam {
    /** 供 JSON 框架或调用方逐项设置字段。 */
    public RnOrderDetailParam() { }

    /** 服务端签发且绑定当前手机号的随机流程标识。 */
    @NotBlank(message = "点餐流程不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "点餐流程格式不正确")
    @ToString.Exclude
    @Schema(description = "RN 点餐随机流程标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flowId;
    /** 菜单响应给出的商品链接标识。 */
    @NotBlank(message = "商品标识不能为空")
    @Size(max = 128, message = "商品标识过长")
    @Schema(description = "菜单商品 linkId", requiredMode = Schema.RequiredMode.REQUIRED)
    private String linkId;
}
