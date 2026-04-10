package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
// 将配置文件中以 "sky.jwt" 为前缀的属性值自动绑定到当前类的对应字段上
@ConfigurationProperties(prefix = "sky.jwt")
// Lombok 注解，自动生成 getter、setter、toString、equals 和 hashCode 方法，减少样板代码
@Data
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

}
