package com.heima.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.system.entity.SysAdmin;
import com.heima.modules.system.entity.SysPermission;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {
    @Select("SELECT role_id FROM sys_admin_role WHERE admin_id=#{adminId}") List<Long> selectRoleIds(Long adminId);
    @Select("SELECT DISTINCT p.code FROM sys_permission p JOIN sys_role_permission rp ON rp.permission_id=p.id JOIN sys_admin_role ar ON ar.role_id=rp.role_id JOIN sys_role r ON r.id=ar.role_id WHERE ar.admin_id=#{adminId} AND p.status=1 AND r.status=1")
    List<String> selectPermissionCodes(Long adminId);
    @Select("SELECT DISTINCT p.* FROM sys_permission p JOIN sys_role_permission rp ON rp.permission_id=p.id JOIN sys_admin_role ar ON ar.role_id=rp.role_id JOIN sys_role r ON r.id=ar.role_id WHERE ar.admin_id=#{adminId} AND p.status=1 AND r.status=1 ORDER BY p.sort_order,p.id")
    List<SysPermission> selectPermissions(Long adminId);
    @Update("UPDATE sys_admin SET status=#{status},token_version=token_version+1 WHERE id=#{id}") int updateStatusAndVersion(@Param("id") Long id,@Param("status") int status);
    @Update("UPDATE sys_admin SET token_version=token_version+1 WHERE id=#{id}") int incrementTokenVersion(Long id);

    @Select("""
            SELECT EXISTS(SELECT 1 FROM forum_moderator WHERE admin_id=#{id})
                OR EXISTS(SELECT 1 FROM content_report_handle WHERE handler_id=#{id})
                OR EXISTS(SELECT 1 FROM audit_manual_record WHERE reviewer_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_invalid_report WHERE handler_id=#{id})
                OR EXISTS(SELECT 1 FROM trade_refund WHERE reviewer_id=#{id})
                OR EXISTS(SELECT 1 FROM trade_reconciliation WHERE operator_id=#{id})
                OR EXISTS(SELECT 1 FROM wallet_withdrawal WHERE handled_by=#{id})
                OR EXISTS(SELECT 1 FROM operation_feedback WHERE reply_admin_id=#{id})
            """)
    boolean hasBusinessReferences(@Param("id") Long id);
}
