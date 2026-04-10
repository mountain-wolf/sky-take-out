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
public class Coupon {
    /*
    *   优惠券类型状态：1-折扣券 2-满减券
    */
    public static final Integer DISCOUNT_COUPON = 1;
    public static final Integer CASH_COUPON = 2;

    /*
    *   优惠券状态：0-未开始 1-进行中 2-已结束 3-已作废
    */
    public static final Integer NOT_STARTED = 0;
    public static final Integer IN_PROGRESS = 1;
    public static final Integer ENDED = 2;
    public static final Integer CANCELED = 3;

    private static final long serialVersionUID = 1L;
    // 优惠券Id
    private Long couponId;
    // 优惠券名称
    private String couponName;
    // 优惠券类型：1-折扣券 2-满减券
    private Integer couponType;
    // 状态：0-未开始 1-进行中 2-已结束 3-已作废
    private Integer status;
    // 优惠券门槛金额
    private BigDecimal conditionAmount;
    // 优惠券面值
    private BigDecimal faceValue;
    // 折扣比例
    private BigDecimal discountRatio;
    // 特定产品的Id
    private Long dishId;
    private Long setmealId;
    // 生效开始时间
    private LocalDateTime enableStartTime;
    // 生效结束时间
    private LocalDateTime enableEndTime;
    // 领取后有效天数
    private Integer validDays;
    // 生效时间是否为永久：0-否，1-是
    private Boolean isPermanent;
    // 发行总量
    private Long publishCount;
    // 每人限领数量
    private Integer perLimitCount;
}
