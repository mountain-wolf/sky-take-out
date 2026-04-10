package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
     /*
     *   用户优惠券状态：0-未使用 1-已使用 2-已过期 3-已删除
     */
    public static final Integer NOT_USED = 0;
    public static final Integer USED = 1;
    public static final Integer EXPIRED = 2;
    public static final Integer DELETED = 3;

    private static final long serialVersionUID = 1L;
    // 用户领取的优惠券Id
    private Long userCouponsId;
    // 用户Id
    private Long userId;
    // 优惠券Id
    private Long couponId;
    // 状态：0-未使用 1-已使用 2-已过期
    private Integer status;
    // 领取时间
    private LocalDateTime getTime;
    // 使用时间
    private LocalDateTime useTime;
    // 失效时间
    private LocalDateTime endTime;
    // 生效时间是否为永久：0否，1-是
    private Boolean isPermanent;
    // 订单number
    private String orderNumber;
    // 实际优惠金额
    private BigDecimal actualAmount;
    // 数量
    private Integer number;
}
