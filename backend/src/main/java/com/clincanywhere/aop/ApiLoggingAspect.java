package com.clincanywhere.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);
    @Around("within(com.clincanywhere.controller..*)")
    public Object logControllerCall(ProceedingJoinPoint invocation) throws Throwable {
        long started = System.nanoTime();
        String operation = invocation.getSignature().toShortString();
        String traceId = MDC.get("correlationId");
        try {
            Object result = invocation.proceed();
            log.info("api.operation traceId={} operation={} outcome=success durationMs={}", traceId, operation, (System.nanoTime() - started) / 1_000_000);
            return result;
        } catch (Throwable failure) {
            log.warn("api.operation traceId={} operation={} outcome=failure durationMs={} exceptionType={}", traceId, operation,
                    (System.nanoTime() - started) / 1_000_000, failure.getClass().getSimpleName());
            throw failure;
        }
    }
}
