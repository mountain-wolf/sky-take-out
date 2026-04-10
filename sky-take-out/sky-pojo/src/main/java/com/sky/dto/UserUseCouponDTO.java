package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserUseCouponDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userCouponsId;
    private Integer status;
    private String orderNumber;
}
