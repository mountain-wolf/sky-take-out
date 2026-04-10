package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCouponPaymentDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    // 用户领取的优惠券Id
    private Long userCouponsId;
    // 状态： 0：未使用 1：已使用 2：已过期
    private Integer status;
    // 订单Id
    private Long orderId;
}
