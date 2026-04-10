package com.sky.mapper;

import com.sky.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CouponMapper {
    /*
     * 根据Id 查询优惠券
     * @param couponId
     * @return
     */
    @Select("select * from coupon where coupon_id = #{couponId}")
    Coupon getById(Long couponId);
    /*
     * 修改优惠券的库存
     * @param couponId
     * @param publishCount
     */
    @Select("update coupon set publish_count = publish_count - 1 where coupon_id = #{couponId}")
    void updatePublishCount(Long publishCount, Long couponId);
}
