package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);


        // 1. String 序列化器
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();

        // 2. 自定义 ObjectMapper 的 JSON 序列化器
        ObjectMapper om = new ObjectMapper();

        // 2.1 注册 JavaTimeModule，支持 Java 8 日期时间 API (解决 createTime 报错的关键)
        om.registerModule(new JavaTimeModule());

        // 2.2 禁用将日期写为时间戳
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2.3 设置所有属性的可见性
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 2.4 启用默认类型信息，这是 GenericJackson2JsonRedisSerializer 能够正确反序列化的关键
        // 这会使得存入 Redis 的 JSON 带有 @class 属性
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 3. 使用自定义的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(om);

        // 4. 设置 Key 和 Value 的序列化方式
        // Key 使用 String
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON (包含 Java 8 时间支持)
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RIdGenerator idGenerator(RedissonClient redissonClient) {
        // 直接注入 Spring 容器中已有的 RedissonClient
        RIdGenerator idGenerator = redissonClient.getIdGenerator("my-id-generator");
        idGenerator.tryInit(1, 10000);
        return idGenerator;
    }

    @Bean
    public RIdGenerator userCouponsIdGenerator(RedissonClient redissonClient) {
        RIdGenerator userCouponsIdGenerator = redissonClient.getIdGenerator("my-userCouponsId-generator");
        userCouponsIdGenerator.tryInit(1000000000, 10000);
        return userCouponsIdGenerator;
    }
}
