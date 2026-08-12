package com.company.crm.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 用于验证请求中的JWT令牌，并设置用户认证信息
 */
@Component
@RequiredArgsConstructor
// 为什么用 OncePerRequestFilter？确保每个请求只执行一次过滤，避免在请求转发（forward）时重复执行
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer "; // JWT令牌前缀，用于标识JWT令牌（表明这是JWT认证）

    private final JwtService jwtService; 
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 用于获取请求中的信息，请求信息中包含了请求头、请求参数等
            HttpServletResponse response, // 用于设置响应中的信息
            FilterChain filterChain // 用于将请求传递给下一个过滤器或目标资源
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);  // 获取请求头中的Authorization字段，包含JWT令牌和前缀
        // 如果请求头中没有Authorization字段或者Authorization字段不包含JWT令牌前缀，则直接将请求传递给下一个过滤器或目标资源？？？
        // 为什么需要filterChain.doFilter(request, response);，而不是直接return？
        // 因为，如果请求头中没有Authorization字段或者Authorization字段不包含JWT令牌前缀，则直接将请求传递给下一个过滤器或目标资源，而不是直接返回，这样可以确保请求能够正常处理，而不会因为缺少JWT令牌而中断请求处理流程。
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {  
            filterChain.doFilter(request, response); 
            return;
        }
        // 如果请求头中有Authorization字段并且包含JWT令牌前缀，则进行JWT令牌验证，如果验证通过则将用户信息存入SecurityContextHolder中，否则返回401 Unauthorized错误
        // 该检查的作用是避免重复认证。如果当前请求已经有认证信息（比如在同一个请求中多次经过此过滤器），就直接放行，不再重复解析和验证 JWT。
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取JWT令牌，通过截取字符串的方式获取JWT令牌，从第7个字符开始截取，因为前缀"Bearer "有7个字符
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            // 验证JWT令牌是否有效，如果无效则抛出异常
            if (!StringUtils.hasText(token) || !jwtService.isTokenValid(token)) {
                throw new BadCredentialsException("JWT无效或已过期");
            }

            // JWT令牌验证通过，从JWT令牌中获取用户名
            String username = jwtService.getUsername(token);
            // 验证用户名是否为空，如果为空则抛出异常
            if (!StringUtils.hasText(username)) {
                throw new BadCredentialsException("JWT中缺少用户标识");
            }

            // 用户名不为空，则从数据库中查询用户信息，并将用户信息封装成SecurityUser对象，如果查询不到用户信息则抛出异常
            // instanceof SecurityUser securityUser 的作用是判断userDetailsService.loadUserByUsername(username)返回的对象是否是SecurityUser类型，如果是则将其转换为securityUser变量
            if (!(userDetailsService.loadUserByUsername(username) instanceof SecurityUser securityUser)) {
                throw new AuthenticationServiceException("认证主体类型异常");
            }

            // 定义一个UsernamePasswordAuthenticationToken对象，调用其authenticated方法将用户信息封装进去，包含用户名、密码、权限等信息
            // authenticated() 是静态工厂方法，创建已认证的令牌对象
            // 第一个参数 principal：用户主体（SecurityUser）
            // 第二个参数 credentials：凭证（null，因为 JWT 已经验证过了）
            // 第三个参数 authorities：用户权限列表 
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated( 
                            securityUser,
                            null,
                            securityUser.getAuthorities()  
                    );
            // 设置认证信息，将认证信息放入SecurityContextHolder中，以便后续的过滤器可以获取到认证信息
            // 通过创建WebAuthenticationDetailsSource对象，调用其buildDetails方法获取请求信息，并将其封装成WebAuthenticationDetails对象，然后将WebAuthenticationDetails对象设置到authentication对象的details属性中
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 将authentication对象设置到SecurityContextHolder中，以便后续的过滤器可以获取到认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException | JwtException | IllegalArgumentException exception) {
            // ✅ 捕获异常后要做三件事：
            // 1. 清除上下文 - 防止污染
            // 2. 调用认证入口点 - 返回 401 响应
            // 3. 不调用 filterChain.doFilter() - 中断请求处理
            SecurityContextHolder.clearContext(); 
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("JWT认证失败", exception)
            );
        }
    }
}
