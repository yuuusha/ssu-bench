package com.diev.security;

import com.diev.entity.User;
import com.diev.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/openapi.yaml")
                || path.startsWith("/swagger")
                || path.startsWith("/webjars")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        Optional<JwtPrincipal> parsed = jwtService.parse(token);

        if (parsed.isEmpty()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or expired JWT.")
            );
            return;
        }

        JwtPrincipal claims = parsed.get();
        Optional<User> userOpt = userRepository.findById(claims.id());

        if (userOpt.isEmpty()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("User not found.")
            );
            return;
        }

        User user = userOpt.get();

        if (Boolean.TRUE.equals(user.getBlocked())) {
            accessDeniedHandler.handle(
                    request,
                    response,
                    new AccessDeniedException("User is blocked.")
            );
            return;
        }

        JwtPrincipal principal = new JwtPrincipal(user.getId(), user.getEmail(), user.getRole());
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}