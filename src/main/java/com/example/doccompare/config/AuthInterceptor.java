package com.example.doccompare.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 白名单路径无需登录即可访问
        if (path.startsWith("/css") || path.startsWith("/js") ||
                path.equals("/login") || path.equals("/register")) {
            return true;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("currentUser") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}