package com.sky.controller.user;

import com.sky.dto.UserCouponReceiveDTO;
import com.sky.dto.UserUseCouponDTO;
import com.sky.result.Result;
import com.sky.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userCouponController")
@RequestMapping("/user/coupon")
@Api(tags = "用户端优惠券相关接口")
@Slf4j
public class CouponController {
    @Autowired
    private CouponService couponService;

    /*
     *   用户领取优惠券
     * @param couponId
     * @return
     */
    @PostMapping("/receive")
    @ApiOperation("用户领取优惠券")
    public Result receive(@RequestBody UserCouponReceiveDTO userCouponReceiveDTO){
        log.info("用户领取优惠券：{}",userCouponReceiveDTO);
        couponService.receiveCoupon(userCouponReceiveDTO);
        return Result.success();
    }
    /*
     * 用户使用优惠券
     * @param userCouponsId
     */
    @PostMapping("/use")
    @ApiOperation("用户使用优惠券")
    public Result use(@RequestBody UserUseCouponDTO userUseCouponDTO){
        log.info("用户使用优惠券：{}",userUseCouponDTO);
        couponService.useCoupon(userUseCouponDTO);
        return Result.success();
    }
}
