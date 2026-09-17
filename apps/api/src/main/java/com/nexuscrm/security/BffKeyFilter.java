package com.nexuscrm.security;

import com.nexuscrm.config.NexusProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BffKeyFilter extends OncePerRequestFilter {
    private final NexusProperties properties;

    public BffKeyFilter(NexusProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String expected = properties.getBffApiKey();
        String provided = request.getHeader("X-BFF-Key");
        if (expected != null && !expected.isBlank() && !expected.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid BFF key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
