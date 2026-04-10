package com.sky.service;


import com.sky.dto.UserCouponReceiveDTO;
import com.sky.dto.UserUseCouponDTO;

import java.math.BigDecimal;

public interface CouponService {
    /*
     *   用户领取优惠券
     * @param couponId
     * @return
     */
    void receiveCoupon(UserCouponReceiveDTO userCouponReceiveDTO);
    /*
     * 用户使用优惠券
     * @param userCouponsId
     */
    public void useCoupon(UserUseCouponDTO userUseCouponDTO);
}
