package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCouponReceiveDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long couponId;
    private Integer number;
}
