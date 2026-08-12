package com.company.crm.config;

import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;

import javax.swing.Spring;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 类 SecurityConfig 是一个配置类，用于配置 Spring Security 的相关设置。里面包含了多个方法，用于配置不同的安全设置。
// Spring 启动时读取这个类，并根据里面定义的 Bean 组装认证和安全过滤链。
@Configuration
public class SecurityConfig {

        // 创建了一个名为 authenticationProvider 的 Bean，用于组装一个 DaoAuthenticationProvider，它内部需要两个核心东西：
        // CustomUserDetailsService→ 去哪里查用户
        // PasswordEncoder→ 怎么校验密码
        // 参数：CustomUserDetailsService 是一个自定义的用户服务类，用于从数据库或其他数据源中获取用户信息。
        // 参数：PasswordEncoder 是一个密码编码器，用于对用户密码进行编码和验证。
    @Bean
    public DaoAuthenticationProvider authenticationProvider( 
            CustomUserDetailsService userDetailsService,  
            PasswordEncoder passwordEncoder
    ) {
        // 创建一个 DaoAuthenticationProvider 对象，该对象里面包含信息：用户服务类和密码编码器。
        // DaoAuthenticationProvider 是用户名密码认证的具体执行者，它调用 CustomUserDetailsService 查询用户，再用 PasswordEncoder 验证用户输入的密码。
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // 将userDetailsService和passwordEncoder设置到provider中,以便provider能够使用它们来处理用户认证。
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // 创建了一个名为 authenticationManager 的 Bean，传入authenticationProvider，返回一个 AuthenticationManager 对象。
    // ProviderManager 是 Spring Security 提供的一个身份验证管理器，它可以将多个 AuthenticationProvider 组合在一起，实现多身份验证。
    // AuthenticationManager= 认证总入口
    // ProviderManager= AuthenticationManager 的常用实现
    // DaoAuthenticationProvider= 具体负责数据库用户名密码认证
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    // 创建了一个名为jwtFilterRegistration的 Bean，传入 JwtAuthenticationFilter，返回一个 FilterRegistrationBean 对象。
    // 传入 JwtAuthenticationFilter  对象，返回一个 FilterRegistrationBean 对象。
    // JwtAuthenticationFilter 对象里面包含了 JWT 认证的相关逻辑。
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        // 创建一个 FilterRegistrationBean 对象，用于注册自定义的过滤器。
        // 为什么需要过滤器？因为过滤器可以拦截请求，并在请求到达目标资源之前进行一些处理，例如认证、授权、日志记录等。
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);

        // 禁止 Spring Boot 把 JwtAuthenticationFilter
        // 自动注册为普通 Servlet Filter。
        // 因为它已经通过 SecurityFilterChain 的 addFilterBefore()
        // 注册到 Spring Security 过滤器链中。
        // 这样可以避免同一个 JWT Filter 执行两次。
        registration.setEnabled(false); 

        return registration; 
    }

        // 创建一个名为 securityFilterChain 的 Bean，用于配置 Spring Security 的过滤器链。过滤器链是一系列过滤器的集合，这些过滤器按照一定的顺序执行，用于处理HTTP请求。
    // 传入 HttpSecurity、DaoAuthenticationProvider、JwtAuthenticationFilter、RestAuthenticationEntryPoint 对象，返回一个 SecurityFilterChain 对象。
    // http 用来配置 Spring Security 的 HTTP 安全规则，例如设置哪些URL需要认证、哪些URL不需要认证、哪些URL需要授权等。
    // authenticationProvider 用来配置认证提供者，认证提供者用于验证用户的身份。
    // jwtAuthenticationFilter 用来配置 JWT 认证过滤器，JWT 认证过滤器用于验证 JWT 令牌的有效性。
    // authenticationEntryPoint 用来配置认证入口点，认证入口点用于处理未认证的请求。
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF 当前采用前后端分离 + Bearer JWT 无状态认证，不依赖 Cookie Session，因此关闭 CSRF。
                .cors(Customizer.withDefaults()) // 启用 CORS（跨域资源共享）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态会话管理，即不使用session
                .formLogin(AbstractHttpConfigurer::disable) // 禁用表单登录
                .httpBasic(AbstractHttpConfigurer::disable)  // 禁用 HTTP 基本认证
                .logout(AbstractHttpConfigurer::disable) // 禁用 Spring Security 默认的 Session Logout 机制。
                .requestCache(AbstractHttpConfigurer::disable)  // 不保存“用户认证前原本想访问哪个页面”的请求。
                .authenticationProvider(authenticationProvider) // 配置认证提供者，前面创建的：DaoAuthenticationProvider正式注册给 Spring Security。
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint)) // 当未认证用户访问受保护接口时，调用 RestAuthenticationEntryPoint 返回统一 401 JSON。
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()  // 放行 Servlet 容器内部的 ERROR 类型派发，防止原本的 404/500 错误在内部二次派发时，又被 Spring Security 拦截成 401。
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()  // 登录接口公开访问
                        .anyRequest().authenticated()  // 除明确放行的接口以外，目前其他所有接口都必须先认证。
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 把自定义 JWT Filter 插入 Spring Security 过滤器链，并让它在 UsernamePasswordAuthenticationFilter 之前执行。
                .build();
    }
}
