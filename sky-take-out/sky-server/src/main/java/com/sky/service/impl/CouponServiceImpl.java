package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.UserCouponReceiveDTO;
import com.sky.dto.UserUseCouponDTO;
import com.sky.entity.Coupon;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.UserCoupon;
import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.CouponMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CouponServiceImpl implements CouponService {
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RIdGenerator userCouponsIdGenerator;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /*
     * 用户领取优惠券
     * @param couponId
     * @return
     */
    @Transactional
    public void receiveCoupon(UserCouponReceiveDTO userCouponReceiveDTO) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        // 定义锁的Key，格式：业务前缀:优惠券Id
        String lockKey = "lock:coupon:receive:" + userCouponReceiveDTO.getCouponId();
        // 获取锁对象
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            // 参数1：等待时间；参数2：锁自动释放时间。
            boolean isLocked = lock.tryLock(10, 15, TimeUnit.SECONDS);
            if (!isLocked) {
                // 获取锁失败，抛出异常
                throw new OrderBusinessException("操作频繁，请稍后再试");
            }
            try {
                String key = "coupon:" + userId;

                Coupon coupon = couponMapper.getById(userCouponReceiveDTO.getCouponId());
                if(coupon == null){
                    throw new BaseException("优惠券不存在");
                }
                if(coupon.getStatus().equals(Coupon.NOT_STARTED)){
                    throw new BaseException("优惠券未开始");
                }
                if(coupon.getStatus().equals(Coupon.ENDED)){
                    throw new BaseException("优惠券已结束");
                }
                if(coupon.getStatus().equals(Coupon.CANCELED)){
                    throw new BaseException("优惠券已作废");
                }
                if(coupon.getStatus().equals(Coupon.IN_PROGRESS)){
                    if(coupon.getPublishCount()>0) {
                        Long userCouponsId = userCouponsIdGenerator.nextId();
                        // 给用户优惠券赋值
                        UserCoupon userCoupon = UserCoupon.builder()
                                .userCouponsId(userCouponsId)
                                .userId(userId)
                                .couponId(userCouponReceiveDTO.getCouponId())
                                .status(UserCoupon.NOT_USED)
                                .getTime(LocalDateTime.now())
                                .isPermanent(coupon.getIsPermanent())
                                .number(userCouponReceiveDTO.getNumber())
                                .build();
                        // 判断用户优惠券的结束时间
                        if(!coupon.getIsPermanent()){
                            LocalDateTime limitTime = coupon.getEnableEndTime();
                            Integer validDays = coupon.getValidDays();
                            if(limitTime == null && validDays == null){
                                throw new BaseException("优惠券结束时间错误");
                            }
                            LocalDateTime endTime = LocalDateTime.now().plusDays(validDays);
                            if(limitTime == null || endTime.isBefore(limitTime)){
                                userCoupon.setEndTime(endTime);
                            }else {
                                userCoupon.setEndTime(limitTime);
                            }
                        }
                        // 优惠券库存-1
                        couponMapper.updatePublishCount(coupon.getPublishCount() - 1, userCouponReceiveDTO.getCouponId());
                        // 向redis的用户优惠券表中插入数据
                        String hashKey = userCouponsId+"_"+userCoupon.getStatus();
                        redisTemplate.opsForHash().put(key, hashKey, userCoupon);
                    }else {
                        throw new BaseException("库存不足");
                    }
                }
            }finally {
                // 判断当前线程是否持有锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }catch (InterruptedException e) {
            throw new OrderBusinessException("服务器繁忙，请重试");
        }
    }

    /*
    * 用户使用优惠券
    * @param userCouponsId
    */
    @Transactional
    public void useCoupon(UserUseCouponDTO userUseCouponDTO) {
        Long userId = BaseContext.getCurrentId();
        String lockKey = "lock:order:receive_coupon:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            // 参数1：等待时间；参数2：锁自动释放时间。
            boolean isLocked = lock.tryLock(10, 15, TimeUnit.SECONDS);
            if (!isLocked) {
                // 获取锁失败，抛出异常
                throw new OrderBusinessException("操作频繁，请稍后再试");
            }
            try {
                // key："coupon:userId"
                // hashKey："userCouponsId_status"
                String key = "coupon:" + userId;
                String hashKey = userUseCouponDTO.getUserCouponsId()+"_"+userUseCouponDTO.getStatus();
                UserCoupon userCoupon = (UserCoupon) redisTemplate.opsForHash().get(key, hashKey);
                // 库存-1
                if(userCoupon == null){
                    throw new OrderBusinessException("用户优惠券不存在");
                }else if(userCoupon.getNumber() > 0){
                    userCoupon.setNumber(userCoupon.getNumber() - 1);
                }else{
                    throw new OrderBusinessException("用户优惠券不足");
                }
                Coupon coupon = couponMapper.getById(userCoupon.getCouponId());
                Integer status = coupon.getStatus();
                if(status == 1){
                    if(coupon.getCouponType().equals(1)){
                        userCoupon.setActualAmount(getDiscountAmount(coupon, userUseCouponDTO));
                    }else if(coupon.getCouponType().equals(2)){
                        userCoupon.setActualAmount(getReduceAmount(coupon, userUseCouponDTO));
                    }
                    redisTemplate.opsForHash().put(key, hashKey, userCoupon);
                    // 新建已经使用的优惠券
                    String newHashKey = userUseCouponDTO.getUserCouponsId()+"_"+1;
                    UserCoupon newUserCoupon = new UserCoupon();
                    BeanUtils.copyProperties(userCoupon, newUserCoupon);
                    newUserCoupon.setNumber(1);
                    newUserCoupon.setStatus(UserCoupon.USED);
                    newUserCoupon.setOrderNumber(userUseCouponDTO.getOrderNumber());
                    redisTemplate.opsForHash().put(key, newHashKey, newUserCoupon);
                }

            }finally {
                // 判断当前线程是否持有锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }catch (InterruptedException e) {
            throw new OrderBusinessException("服务器繁忙，请重试");
        }
    }
    /*
    * 折扣券的优惠金额计算
    */
    private BigDecimal getDiscountAmount(Coupon coupon, UserUseCouponDTO userUseCouponDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("查询订单入参 - orderNumber: {}, userId: {}", userUseCouponDTO.getOrderNumber(), userId);
        Orders orders = orderMapper.getByNumberAndUserId(userUseCouponDTO.getOrderNumber(), userId);
        BigDecimal decreasedAmount = new BigDecimal("0");
        BigDecimal allAmount = new BigDecimal("0");
        BigDecimal num = new BigDecimal("0");
        BigDecimal decrease = BigDecimal.valueOf(1).subtract(coupon.getDiscountRatio());
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        for (OrderDetail orderDetail : orderDetailList) {
            allAmount = allAmount.add(orderDetail.getAmount().multiply(BigDecimal.valueOf(orderDetail.getNumber())));
        }
        // 判断订单金额是否满足优惠券使用条件
        if(coupon.getConditionAmount() == null || allAmount.compareTo(coupon.getConditionAmount()) >= 0){
            // 通用折扣券
            if(coupon.getSetmealId() ==  null && coupon.getDishId() == null){
                return allAmount.multiply(decrease);
            }
            // 套餐折扣券
            else if(coupon.getSetmealId() != null && coupon.getDishId() == null){
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getSetmealId().equals(coupon.getSetmealId())){
                        //
                        num = BigDecimal.valueOf(orderDetail.getNumber());
                        decreasedAmount = decreasedAmount.add(num.multiply(decrease.multiply(orderDetail.getAmount())));
                    }
                }
                return decreasedAmount;
            }
            // 菜品折扣券
            else if(coupon.getSetmealId() == null && coupon.getDishId() != null){
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getDishId().equals(coupon.getDishId()) && orderDetail.getSetmealId() == null){
                        num = BigDecimal.valueOf(orderDetail.getNumber());
                        decreasedAmount = decreasedAmount.add(num.multiply(decrease.multiply(orderDetail.getAmount())));
                    }
                }
                return decreasedAmount;
            }
            // 套餐内指定菜品折扣券
            else {
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getSetmealId().equals(coupon.getSetmealId()) && orderDetail.getDishId().equals(coupon.getDishId())){
                        num = BigDecimal.valueOf(orderDetail.getNumber());
                        decreasedAmount = decreasedAmount.add(num.multiply(decrease.multiply(orderDetail.getAmount())));
                    }
                }
                return decreasedAmount;
            }
        }else {
            throw new BaseException("优惠券不可用");
        }
    }
    /*
    * 满减券的优惠金额计算
    */
    private BigDecimal getReduceAmount(Coupon coupon, UserUseCouponDTO userUseCouponDTO) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumberAndUserId(userUseCouponDTO.getOrderNumber(), userId);
        BigDecimal allAmount = new BigDecimal("0");
        BigDecimal discountAmount = new BigDecimal("0");
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        for (OrderDetail orderDetail : orderDetailList) {
            allAmount = allAmount.add(orderDetail.getAmount().multiply(BigDecimal.valueOf(orderDetail.getNumber())));
        }
        // 判断订单金额是否满足优惠券使用条件
        // 无门槛优惠券
        if(coupon.getConditionAmount() == null || coupon.getConditionAmount().intValue() == 0){
            discountAmount = allAmount;
            return discountAmount.min(coupon.getFaceValue());
        }
        // 满减券
        if(allAmount.compareTo(coupon.getConditionAmount()) >= 0){
            // 套餐满减券
            if(coupon.getDishId() == null && coupon.getSetmealId() != null){
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getSetmealId().equals(coupon.getSetmealId())){
                        discountAmount = discountAmount.add(orderDetail.getAmount().multiply(BigDecimal.valueOf(orderDetail.getNumber())));
                    }
                }
                return discountAmount.min(coupon.getFaceValue());
            }
            //菜品满减券
            else if(coupon.getDishId() != null && coupon.getSetmealId() == null){
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getDishId().equals(coupon.getDishId()) && orderDetail.getSetmealId() == null){
                        discountAmount = discountAmount.add(orderDetail.getAmount().multiply(BigDecimal.valueOf(orderDetail.getNumber())));
                    }
                }
                return discountAmount.min(coupon.getFaceValue());
            }
            // 套餐内指定菜品满减券
            else {
                for (OrderDetail orderDetail : orderDetailList) {
                    if(orderDetail.getSetmealId().equals(coupon.getSetmealId()) && orderDetail.getDishId().equals(coupon.getDishId())){
                        discountAmount = discountAmount.add(orderDetail.getAmount().multiply(BigDecimal.valueOf(orderDetail.getNumber())));
                    }
                }
                return discountAmount.min(coupon.getFaceValue());
            }
        }else {
            throw new BaseException("优惠券不可用");
        }
    }

}
