package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
//import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RIdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    /*@Autowired
    private ShoppingCartMapper shoppingCartMapper;*/
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RIdGenerator idGenerator;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    /*public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前加入到购物车中的商品是否已经存在了
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果已经存在了，只需要将数量加一
        if(list != null && list.size() > 0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);//update shopping_cart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        }else {
            //如果不存在，需要插入一条购物车数据
            //判断本次添加到购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                //本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }*/

    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 获取用户ID，构建Redis Key
        Long userId = BaseContext.getCurrentId();
        String key = "shopping_cart:" + userId;
        String flavor = shoppingCartDTO.getDishFlavor();
        // 构建Hash中的Field (商品唯一标识)
        // 逻辑：用id和flavor区分菜品和套餐，例如："dish_123" 或 "dish_123_abc"
        String hashKey;
        Long dishId = shoppingCartDTO.getDishId();
        if (dishId != null) {
            if (flavor != null && !flavor.isEmpty()) {
                hashKey = "dish_" + dishId + "_" + flavor;
            } else {
                hashKey = "dish_" + dishId;
            }
        } else {
            hashKey = "setmeal_" + shoppingCartDTO.getSetmealId();
        }

        // 判断购物车中是否已存在该商品
        ShoppingCart existingCart =  (ShoppingCart) redisTemplate.opsForHash().get(key, hashKey);

        if (existingCart != null) {
            // 如果已存在，数量+1
            existingCart.setNumber(existingCart.getNumber() + 1);
            // 更新Redis中的数据
            redisTemplate.opsForHash().put(key, hashKey, existingCart);

        } else {
            // 如果不存在，插入新数据
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
            shoppingCart.setUserId(userId);

            // 补充商品信息（从数据库查询名称、图片、价格）
            if (dishId != null) {
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            long orderId = idGenerator.nextId();
            shoppingCart.setId(orderId);

            // 存入Redis
            redisTemplate.opsForHash().put(key, hashKey, shoppingCart);
        }
    }
    public void addShoppingCart(List<ShoppingCart> shoppingCartList){
        // 获取用户ID，构建Redis Key
        Long userId = BaseContext.getCurrentId();
        String key = "shopping_cart:" + userId;

        for (ShoppingCart shoppingCart : shoppingCartList) {
            String flavor = shoppingCart.getDishFlavor();
            // 构建Hash中的Field (商品唯一标识)
            // 逻辑：区分菜品和套餐，例如 "dish_123" 或 "setmeal_456"
            String hashKey;
            Long dishId = shoppingCart.getDishId();
            if (dishId != null) {
                if (flavor != null && !flavor.isEmpty()) {
                    hashKey = "dish_" + dishId + "_" + flavor;
                } else {
                    hashKey = "dish_" + dishId;
                }
            } else {
                hashKey = "setmeal_" + shoppingCart.getSetmealId();
            }
            redisTemplate.opsForHash().put(key, hashKey, shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    /*public List<ShoppingCart> showShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }*/
    public List<ShoppingCart> showShoppingCart() {
        // 获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        String key = "shopping_cart:" + userId;
        List<Object> values = redisTemplate.opsForHash().values(key);

        if (values.isEmpty()) {
            return new ArrayList<>();
        }
        // 获取Redis中的数据
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof ShoppingCart) {
                shoppingCartList.add((ShoppingCart) value);
            }
        }
        return shoppingCartList;

    }
    /**
     * 清空购物车
     */
    /*public void cleanShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }*/
    public void cleanShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        String key = "shopping_cart:" + userId;
//        redisTemplate.opsForHash().delete(key);
        redisTemplate.delete(key);
    }
    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    /*public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        //设置查询条件，查询当前登录用户的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list != null && list.size() > 0){
            shoppingCart = list.get(0);

            Integer number = shoppingCart.getNumber();
            if(number == 1){
                //当前商品在购物车中的份数为1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }else {
                //当前商品在购物车中的份数不为1，修改份数即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }*/
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        String key = "shopping_cart:" + userId;
        String flavor = shoppingCartDTO.getDishFlavor();
        String hashKey;
        Long dishId = shoppingCartDTO.getDishId();
        if (dishId != null) {
            if (flavor != null && !flavor.isEmpty()) {
                hashKey = "dish_" + dishId + "_" + flavor;
            } else {
                hashKey = "dish_" + dishId;
            }
        } else {
            hashKey = "setmeal_" + shoppingCartDTO.getSetmealId();
        }
        ShoppingCart existingCart =  (ShoppingCart) redisTemplate.opsForHash().get(key, hashKey);
        if (existingCart != null){
            if (existingCart.getNumber() == 1) {
                redisTemplate.opsForHash().delete(key, hashKey);
            } else {
                existingCart.setNumber(existingCart.getNumber() - 1);
                redisTemplate.opsForHash().put(key, hashKey, existingCart);
            }
        }
    }

}
