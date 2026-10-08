package com.tenco.spring_blog._core.interceptor;

import com.tenco.spring_blog._core.error.Exception403;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

// 특정 IP를 차단 하는 인터셉터를 구현해주세요 단, 여러개 가능 (조원들 IP 차단)
@Slf4j
@Component
public class IpBlockInterceptor implements HandlerInterceptor {
    // IP 목록
    private static final List<String> BLOCK_IP = List.of("192.168.7.237", "192.168.5.16");

    // 요청
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        log.info("현재 접속 IP = {} ", clientIp);
        log.info("차단 목록 포함 여부 = {}", BLOCK_IP.contains(clientIp));

        if (BLOCK_IP.contains(clientIp)) {
            throw new Exception403("접근이 차단된 IP 입니다");
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}