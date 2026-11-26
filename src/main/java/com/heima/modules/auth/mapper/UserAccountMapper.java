package com.heima.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    @Update("UPDATE user_account SET token_version=token_version+1,status=#{status} WHERE id=#{id}")
    int updateStatusAndVersion(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE user_account SET token_version=token_version+1 WHERE id=#{id}")
    int incrementTokenVersion(@Param("id") Long id);

    @Select("""
            SELECT EXISTS(SELECT 1 FROM blog_article WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM forum_topic WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM forum_post WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM content_comment WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_info WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM file_object WHERE owner_id=#{id})
                OR EXISTS(SELECT 1 FROM audit_task WHERE author_id=#{id})
                OR EXISTS(SELECT 1 FROM content_like WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM content_favorite WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM user_follow WHERE follower_id=#{id} OR followee_id=#{id})
                OR EXISTS(SELECT 1 FROM content_report WHERE reporter_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_entitlement WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_access_record WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_invalid_report WHERE reporter_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_point_account WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM resource_point_transaction WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM trade_order WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM trade_refund WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM points_account WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM points_record WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM user_sign_in WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM message_notification WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM message_preference WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM user_membership WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM membership_daily_usage WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM membership_resource_claim WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM wallet_account WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM wallet_ledger WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM wallet_withdrawal WHERE user_id=#{id})
                OR EXISTS(SELECT 1 FROM operation_feedback WHERE user_id=#{id})
            """)
    boolean hasBusinessReferences(@Param("id") Long id);
}
