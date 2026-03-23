package com.diev.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "request_id";
    public static final String REQUEST_ID_MDC_COMPAT_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.currentTimeMillis();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(REQUEST_ID_MDC_COMPAT_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String requestLine = requestLine(request);
        log.info("incoming request id={} method={} path={}", requestId, request.getMethod(), requestLine);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            int status = response.getStatus();
            log.info(
                    "completed request id={} method={} path={} status={} duration_ms={}",
                    requestId,
                    request.getMethod(),
                    requestLine,
                    status,
                    durationMs
            );

            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_COMPAT_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String requestLine(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + query;
    }
}