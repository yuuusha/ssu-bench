package com.diev.security;

import com.diev.api.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                403,
                "Forbidden",
                "ACCESS_DENIED",
                accessDeniedException.getMessage() == null || accessDeniedException.getMessage().isBlank()
                        ? "Access denied."
                        : accessDeniedException.getMessage(),
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
}