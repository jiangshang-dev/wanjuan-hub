package com.heima.modules.payment;

import com.heima.common.annotation.OperationLog;
import com.heima.modules.order.controller.admin.AdminOrderController;
import com.heima.modules.payment.controller.AdminPaymentController;
import com.heima.modules.resource.controller.admin.AdminResourceEntitlementController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTradeSecurityTest {
    @Test
    void adminEndpointsRequireExactButtonPermissions() {
        assertAuthorities(AdminOrderController.class, Map.of(
                "page", "order:list",
                "detail", "order:detail"));
        assertAuthorities(AdminPaymentController.class, Map.of(
                "payments", "payment:list",
                "paymentDetail", "payment:list",
                "notifications", "payment:notification:list",
                "notificationDetail", "payment:notification:list",
                "reconciliations", "payment:reconciliation:list",
                "reconciliationDetail", "payment:reconciliation:list",
                "reconcile", "payment:reconcile",
                "refunds", "refund:list",
                "review", "refund:review",
                "syncRefund", "refund:sync"));
        assertAuthorities(AdminResourceEntitlementController.class, Map.of(
                "page", "resource:entitlement:list",
                "detail", "resource:entitlement:list"));
    }

    @Test
    void everyAdminWriteEndpointHasOperationLog() {
        assertOperationLog(AdminPaymentController.class, "reconcile", "#orderNo");
        assertOperationLog(AdminPaymentController.class, "review", "#refundId");
        assertOperationLog(AdminPaymentController.class, "syncRefund", "#refundNo");
    }

    @Test
    void entitlementAdminControllerIsReadOnly() {
        Method[] methods = AdminResourceEntitlementController.class.getDeclaredMethods();
        assertEquals(2, Arrays.stream(methods).filter(method -> method.isAnnotationPresent(GetMapping.class)).count());
        assertTrue(Arrays.stream(methods).noneMatch(method -> method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)));
    }

    @Test
    void migrationContainsTradePagesButtonsAndSuperAdminGrant() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V12__s5_trade_menu_button_permissions.sql"));
        List<String> paths = List.of("/trade", "/trade/orders", "/trade/payments", "/trade/payment-notifications",
                "/trade/reconciliations", "/trade/refunds", "/trade/entitlements");
        List<String> permissions = List.of("order:list", "order:detail", "payment:list", "payment:reconcile",
                "payment:notification:list", "payment:reconciliation:list", "refund:list", "refund:review",
                "refund:sync", "resource:entitlement:list");

        paths.forEach(path -> assertTrue(sql.contains("'" + path + "'"), path));
        permissions.forEach(permission -> assertTrue(sql.contains("'" + permission + "'"), permission));
        assertTrue(sql.contains("SELECT 1, id FROM sys_permission WHERE id BETWEEN 230 AND 246"));
    }

    private static void assertAuthorities(Class<?> controller, Map<String, String> expected) {
        expected.forEach((methodName, authority) -> {
            PreAuthorize annotation = method(controller, methodName).getAnnotation(PreAuthorize.class);
            assertNotNull(annotation, controller.getSimpleName() + "." + methodName);
            assertEquals("hasAuthority('" + authority + "')", annotation.value());
        });
    }

    private static void assertOperationLog(Class<?> controller, String methodName, String targetId) {
        OperationLog annotation = method(controller, methodName).getAnnotation(OperationLog.class);
        assertNotNull(annotation, controller.getSimpleName() + "." + methodName);
        assertEquals(targetId, annotation.targetId());
        assertFalse(annotation.module().isBlank());
        assertFalse(annotation.value().isBlank());
    }

    private static Method method(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type.getSimpleName() + "." + name));
    }
}
