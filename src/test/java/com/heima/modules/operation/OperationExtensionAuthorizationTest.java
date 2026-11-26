package com.heima.modules.operation;

import com.heima.common.annotation.OperationLog;
import com.heima.modules.operation.controller.AdminFeedbackController;
import com.heima.modules.operation.controller.AdminSiteExperienceController;
import com.heima.modules.operation.service.SiteExperienceService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationExtensionAuthorizationTest {
    @Test
    void adminEndpointsHavePermissionsAndWritesHaveOperationLogs() {
        assertSecured(AdminSiteExperienceController.class);
        assertSecured(AdminFeedbackController.class);
    }

    @Test
    void interactionModuleCanQueryGrayDayThroughStableServiceContract() throws NoSuchMethodException {
        assertEquals(boolean.class, SiteExperienceService.class.getMethod("isGrayDay").getReturnType());
        assertEquals(String.class, SiteExperienceService.class.getMethod("grayReason").getReturnType());
    }

    private static void assertSecured(Class<?> controller) {
        for (Method method : controller.getDeclaredMethods()) {
            PreAuthorize permission = method.getAnnotation(PreAuthorize.class);
            assertNotNull(permission, method.getName() + "缺少权限注解");
            assertTrue(permission.value().startsWith("hasAuthority('operation:"), method.getName() + "权限码不正确");
            if (method.isAnnotationPresent(PostMapping.class) || method.isAnnotationPresent(PutMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class)) {
                assertNotNull(method.getAnnotation(OperationLog.class), method.getName() + "缺少操作日志");
            }
        }
    }
}
