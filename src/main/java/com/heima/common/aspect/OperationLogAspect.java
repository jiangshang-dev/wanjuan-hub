package com.heima.common.aspect;

import com.heima.common.annotation.OperationLog;
import com.heima.infrastructure.location.IpLocationService;
import com.heima.modules.system.entity.OperationLogEntity;
import com.heima.security.config.SecurityProperties;
import com.heima.security.context.LoginUser;
import com.heima.utils.IpUtils;
import com.heima.utils.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private static final Pattern SENSITIVE = Pattern.compile("(?i)(password|code|token|secret)\\s*[=:]\\s*(\\\"[^\\\"]*\\\"|[^,\\s}\\)]+)");
    private final OperationLogWriter writer;
    private final SecurityProperties securityProperties;
    private final IpLocationService locationService;

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint point, OperationLog annotation) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogEntity entity = new OperationLogEntity();
        LoginUser user = UserUtil.getUser();
        entity.setOperatorType(user == null ? "ANONYMOUS" : user.getRole());
        entity.setOperatorId(user == null ? null : user.getId());
        entity.setOperatorName(user == null ? "anonymous" : user.getUsername());
        entity.setModule(annotation.module());
        entity.setContent(limit(annotation.value() + " " + mask(Arrays.toString(point.getArgs())), 1000));
        entity.setTargetId(target(point, annotation.targetId()));
        String ip = ip();
        entity.setIp(ip);
        entity.setLocation(locationService.locate(ip));
        entity.setTraceId(MDC.get("traceId"));
        entity.setCreateTime(LocalDateTime.now());
        try {
            Object result = point.proceed();
            entity.setSuccess(true);
            entity.setCostMs(System.currentTimeMillis() - start);
            writer.save(entity);
            return result;
        } catch (Throwable e) {
            entity.setSuccess(false);
            entity.setErrorMessage(limit(e.getMessage(), 500));
            entity.setCostMs(System.currentTimeMillis() - start);
            writer.save(entity);
            throw e;
        }
    }

    public static String mask(String value) {
        if (value == null) return "";
        Matcher matcher = SENSITIVE.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "=******"));
        matcher.appendTail(out);
        return out.toString();
    }

    private String target(ProceedingJoinPoint point, String expression) {
        if (expression == null || expression.isBlank()) return null;
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = new DefaultParameterNameDiscoverer().getParameterNames(signature.getMethod());
            Object[] args = point.getArgs();
            if (names != null) for (int i = 0; i < names.length; i++) context.setVariable(names[i], args[i]);
            Object value = new SpelExpressionParser().parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String ip() {
        ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest r = a == null ? null : a.getRequest();
        return IpUtils.getIpAddr(r, securityProperties.isTrustForwardedHeaders());
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
