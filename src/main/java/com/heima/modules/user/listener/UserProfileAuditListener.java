package com.heima.modules.user.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserProfileAuditListener {
    public static final String BIZ_TYPE = "USER_PROFILE";

    private final UserProfileMapper profileMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if (!BIZ_TYPE.equals(event.bizType())) return;
        UserProfile profile = profileMapper.selectById(event.bizId());
        if (profile == null || !profile.getProfileVersion().equals(event.versionNo())) return;

        LambdaUpdateWrapper<UserProfile> update = new LambdaUpdateWrapper<UserProfile>()
                .eq(UserProfile::getUserId, event.bizId())
                .eq(UserProfile::getProfileVersion, event.versionNo())
                .set(UserProfile::getProfileAuditStatus, event.status().name())
                .set(UserProfile::getProfileAuditReason, event.reason());
        if (event.status() == AuditStatus.APPROVED) {
            update.set(UserProfile::getNickname, profile.getPendingNickname())
                    .set(UserProfile::getSignature, profile.getPendingSignature())
                    .set(UserProfile::getAvatarFileId, Boolean.TRUE.equals(profile.getPendingRemoveAvatar())
                            ? null : profile.getPendingAvatarFileId())
                    .set(UserProfile::getAvatar, null)
                    .set(UserProfile::getProfileAuditReason, null);
        }
        profileMapper.update(null, update);
    }
}
