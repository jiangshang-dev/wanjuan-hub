package com.heima.security.filter;

import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.system.entity.SysAdmin;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.security.context.LoginUser;
import com.heima.security.token.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserAccountMapper userMapper;
    private final SysAdminMapper adminMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if (header!=null && header.startsWith("Bearer ")) {
            try {
                Claims claims=jwt.parse(header.substring(7),JwtService.ACCESS);
                Long id=Long.valueOf(claims.getSubject());
                Number versionClaim=claims.get("ver",Number.class);
                if (versionClaim==null) throw new ServiceException(401,"登录状态已失效");
                int version=versionClaim.intValue();
                String type=claims.get("typ",String.class); LoginUser principal;
                if ("USER".equals(type)) {
                    UserAccount user=userMapper.selectById(id);
                    if (user==null || user.getStatus()!=1 || user.getTokenVersion()!=version) throw new ServiceException(401,"登录状态已失效");
                    principal=new LoginUser(id,user.getUsername(),user.getUsername(),"USER",true,List.of(new SimpleGrantedAuthority("ROLE_USER")));
                } else if ("ADMIN".equals(type)) {
                    SysAdmin admin=adminMapper.selectById(id);
                    if (admin==null || admin.getStatus()!=1 || admin.getTokenVersion()!=version) throw new ServiceException(401,"登录状态已失效");
                    List<SimpleGrantedAuthority> authorities=new java.util.ArrayList<>(adminMapper.selectPermissionCodes(id).stream().map(SimpleGrantedAuthority::new).toList());
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    principal=new LoginUser(id,admin.getUsername(),admin.getNickname(),"ADMIN",true,authorities);
                } else throw new ServiceException(401,"令牌身份错误");
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            } catch (io.jsonwebtoken.JwtException | ServiceException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request,response);
    }
}
