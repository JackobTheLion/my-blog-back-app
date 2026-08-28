package ru.practicum.yakovlev.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = RequestLoggingInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        log.info("HTTP request started: method={}, path={}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        long durationMillis = elapsedMillis(request);
        HttpStatusCode status = HttpStatusCode.valueOf(response.getStatus());
        String message = "HTTP request completed: method={}, path={}, status={}, durationMs={}";

        if (exception != null) {
            log.error(message, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis,
                    exception);
        } else if (status.is5xxServerError()) {
            log.error(message, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
        } else if (status.is4xxClientError()) {
            log.warn(message, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
        } else {
            log.info(message, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMillis);
        }
    }

    private long elapsedMillis(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        if (!(startTime instanceof Long startTimeNanos)) {
            return -1;
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
    }
}
