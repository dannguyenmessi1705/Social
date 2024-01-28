package com.didan.social.security;

import com.didan.social.entity.Users;
import com.didan.social.repository.UserRepository;
import com.didan.social.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    @Autowired
    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserRepository userRepository){
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = getTokenFromHeader(request);
        if(StringUtils.hasText(accessToken)){
            try {
                jwtUtils.validateAccessToken(accessToken);
                String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
                Users user = userRepository.findFirstByEmail(email);
                if (user != null){
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(null, null, new ArrayList<>());
                    SecurityContext securityContext = SecurityContextHolder.getContext();
                    securityContext.setAuthentication(usernamePasswordAuthenticationToken);
                }
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    // Lấy token từ header
    private String getTokenFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization"); // Lấy token từ header
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){ // Kiểm tra bearerToken khác null và có bắt đầu bằng Bearer
            return bearerToken.substring(7); // Trả về token
        }
        return null; // Nếu token không hợp lệ thì trả về null
    }
}
