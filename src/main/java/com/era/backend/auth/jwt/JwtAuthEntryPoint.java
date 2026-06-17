package com.era.backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.era.backend.common.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a clean ApiResponse-shaped 401 JSON body instead of Spring's
 * default HTML error page when an unauthenticated request hits a
 * protected endpoint.
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.fail("Unauthorized: " + authException.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
