package com.heima.modules.operation;

import com.heima.common.annotation.OperationLog;
import com.heima.modules.operation.controller.AdminOperationController;
import com.heima.modules.statistics.controller.AdminStatisticsController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationAuthorizationTest {
    @Test
    void everyOperationEndpointHasExactPermissionAndEveryWriteHasOperationLog() {
        Method[] methods = AdminOperationController.class.getDeclaredMethods();
        assertEquals(30, methods.length);
        for (Method method : methods) {
            PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
            assertNotNull(permission, method.getName() + "缺少权限注解");
            assertTrue(permission.value().matches("hasAuthority\\('operation:[a-z-]+:(list|create|update|status|delete)'\\)"),
                    method.getName() + "权限码不稳定: " + permission.value());
            if (method.isAnnotationPresent(PostMapping.class) || method.isAnnotationPresent(PutMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class)) {
                assertNotNull(method.getAnnotation(OperationLog.class), method.getName() + "缺少操作日志");
            }
        }
    }

    @Test
    void statisticsEndpointsUseDashboardPermissions() throws NoSuchMethodException {
        Method dashboard = AdminStatisticsController.class.getMethod("dashboard", int.class);
        Method refresh = AdminStatisticsController.class.getMethod("refreshDaily", java.time.LocalDate.class);

        assertEquals("hasAuthority('statistics:dashboard:view')",
                dashboard.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasAuthority('statistics:dashboard:refresh')",
                refresh.getAnnotation(PreAuthorize.class).value());
        assertNotNull(refresh.getAnnotation(OperationLog.class));
        assertTrue(Arrays.stream(AdminStatisticsController.class.getDeclaredMethods())
                .allMatch(method -> method.isAnnotationPresent(PreAuthorize.class)));
    }
}
