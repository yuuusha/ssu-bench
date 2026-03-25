package com.diev.security;

import com.diev.api.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, AuthenticationException authException) throws IOException {
        write(
                response,
                request,
                authException.getMessage() == null || authException.getMessage().isBlank()
                        ? "Authentication required."
                        : authException.getMessage()
        );
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                401,
                HttpStatusText.reason(),
                "UNAUTHORIZED",
                message,
                request.getRequestURI(),
                requestId(),
                null
        );

        objectMapper.writeValue(response.getWriter(), body);
    }

    private String requestId() {
        String requestId = MDC.get("request_id");
        if (requestId == null) {
            requestId = MDC.get("requestId");
        }
        return requestId;
    }

    private static final class HttpStatusText {
        private static String reason() {
            return "Unauthorized";
        }
    }
}