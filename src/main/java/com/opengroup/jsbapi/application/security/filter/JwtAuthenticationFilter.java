package com.opengroup.jsbapi.application.security.filter;

import com.opengroup.jsbapi.application.error.UnauthorizedAccessException;
import com.opengroup.jsbapi.application.security.services.TokenService;
import com.opengroup.jsbapi.application.security.token.JwtToken;
import com.opengroup.jsbapi.domain.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtAuthenticationFilter implements Filter {


    @Autowired
    TokenService tokenService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {


        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // decode jwt token
        try{
            String token = request.getHeader("x-access-token");
            if (StringUtils.isEmpty(token)) {
                throw new UnauthorizedAccessException("No Token Provided");
            }
            JwtToken jwtToken = tokenService.accessToken(token);
            // verify
            Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
            for (String scope : jwtToken.getScopes()) {
                grantedAuthorities.add(new SimpleGrantedAuthority(scope));
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(jwtToken.getLogin(), "", grantedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }catch(UnauthorizedAccessException ex){

            response.sendError(HttpStatus.UNAUTHORIZED.value(),ex.getMessage());
            return;
        }


        filterChain.doFilter(request, response);
    }


}


