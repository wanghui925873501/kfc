package com.wanghui.kfc.kfcapi.rnorderkfccomcn.param;

import java.math.BigDecimal;
import lombok.Data;
import lombok.ToString;

/** 保存一次门店查询使用的经纬度，发送前由底层客户端加密。 */
@Data
public class RnOrderLocation {
    /** 供调用方逐项设置定位。 */
    public RnOrderLocation() { }

    /** 纬度原始十进制文本。 */
    @ToString.Exclude
    private String latitude;
    /** 经度原始十进制文本。 */
    @ToString.Exclude
    private String longitude;
    /** 门店查询使用的国标城市编码，允许为空。 */
    private String gbCityCode;

    /**
     * 校验经纬度格式和合法范围。
     *
     * @return 当前合法定位
     * @throws IllegalArgumentException 经纬度缺失、格式错误或越界时
     */
    public RnOrderLocation requireValid() {
        try {
            BigDecimal lat = new BigDecimal(latitude);
            BigDecimal lng = new BigDecimal(longitude);
            if (lat.compareTo(BigDecimal.valueOf(-90)) < 0
                    || lat.compareTo(BigDecimal.valueOf(90)) > 0
                    || lng.compareTo(BigDecimal.valueOf(-180)) < 0
                    || lng.compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new IllegalArgumentException("RN order location is out of range");
            }
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException("RN order location is invalid", e);
        }
        return this;
    }
}
