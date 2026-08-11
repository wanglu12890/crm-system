package com.company.crm.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
// @Validated启用 Spring 的校验功能，让类内部(@NotBlank\@Positive)的校验注解生效
@Validated
// @Component 将该类注册为 Spring 容器的一个 Bean，其他地方可以通过 @Autowired 注入使用
@Component
// 配置绑定注解，告诉 Spring 将配置文件中 jwt 开头的配置项绑定到这个类的字段；如果配置文件中有相关信息，则secret 会被赋值为 "xxxx"，expire 会被赋值为 7200
@ConfigurationProperties(prefix = "jwt") 
public class JwtProperties {

    @NotBlank //校验 secret 不能为 null、不能为空字符串、不能全是空格
    private String secret;

    /** Access token lifetime in seconds. */
    @Positive // 校验 expire 必须是正数（大于 0）
    private long expire;
}
