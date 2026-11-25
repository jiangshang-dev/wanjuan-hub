package com.heima.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint,AccessDeniedHandler {
    @Override public void commence(HttpServletRequest request,HttpServletResponse response,AuthenticationException e) throws IOException { write(response,401,"请先登录"); }
    @Override public void handle(HttpServletRequest request,HttpServletResponse response,AccessDeniedException e) throws IOException { write(response,403,"没有操作权限"); }
    private void write(HttpServletResponse response,int code,String message) throws IOException {
        response.setStatus(code); response.setCharacterEncoding(StandardCharsets.UTF_8.name()); response.setContentType("application/json");
        response.getWriter().write("{\"code\":"+code+",\"message\":\""+message+"\",\"data\":null}");
    }
}
