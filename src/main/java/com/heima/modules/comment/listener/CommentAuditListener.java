package com.heima.modules.comment.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.entity.CommentEntities.Version;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.mapper.CommentMappers.VersionMapper;
import com.heima.modules.comment.service.CommentService;
import com.heima.modules.comment.service.ContentTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentAuditListener {
    private final CommentMapper commentMapper;
    private final VersionMapper versionMapper;
    private final ContentTargetService targetService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if (!CommentService.AUDIT_BIZ_TYPE.equals(event.bizType())) return;
        Comment comment = commentMapper.selectById(event.bizId());
        if (comment == null || comment.getDeleted() == 1) return;
        Version version = versionMapper.selectOne(new LambdaQueryWrapper<Version>().eq(Version::getCommentId, comment.getId())
                .eq(Version::getVersionNo, event.versionNo()));
        if (version == null) return;
        if (!comment.getLatestVersionNo().equals(event.versionNo())) {
            supersede(comment.getId(), event.versionNo());
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            int approved = versionMapper.update(null, new LambdaUpdateWrapper<Version>().eq(Version::getId, version.getId())
                    .in(Version::getStatus, "PENDING", "MANUAL_REVIEW", "REJECTED", "FAILED").set(Version::getStatus, "APPROVED").set(Version::getAuditReason, null));
            if (approved == 0) return;
            boolean firstPublic = comment.getPublishedVersionNo() == null;
            int published = commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, comment.getId())
                    .eq(Comment::getLatestVersionNo, event.versionNo()).ne(Comment::getStatus, "OFFLINE").eq(Comment::getDeleted, 0)
                    .set(Comment::getPublishedVersionNo, event.versionNo()).set(Comment::getStatus, "PUBLISHED").set(Comment::getAuditReason, null));
            if (published == 0) return;
            if (firstPublic) {
                targetService.changeCommentCount(comment.getTargetType(), comment.getTargetId(), 1);
                if (comment.getParentId() != null) {
                    commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, comment.getParentId())
                            .setSql("reply_count = reply_count + 1"));
                }
            }
            return;
        }
        String status = switch (event.status()) {
            case REJECTED -> "REJECTED";
            case MANUAL_REVIEW -> "MANUAL_REVIEW";
            case FAILED -> "FAILED";
            default -> null;
        };
        if (status == null) return;
        versionMapper.update(null, new LambdaUpdateWrapper<Version>().eq(Version::getId, version.getId())
                .in(Version::getStatus, "PENDING", "MANUAL_REVIEW", "REJECTED", "FAILED").set(Version::getStatus, status).set(Version::getAuditReason, event.reason()));
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, comment.getId())
                .eq(Comment::getLatestVersionNo, event.versionNo()).ne(Comment::getStatus, "OFFLINE")
                .set(Comment::getStatus, status).set(Comment::getAuditReason, event.reason()));
    }

    private void supersede(Long commentId, Integer versionNo) {
        versionMapper.update(null, new LambdaUpdateWrapper<Version>().eq(Version::getCommentId, commentId)
                .eq(Version::getVersionNo, versionNo).in(Version::getStatus, "PENDING", "MANUAL_REVIEW", "REJECTED", "FAILED")
                .set(Version::getStatus, "SUPERSEDED").set(Version::getAuditReason, "已有更新版本，本结果不再生效"));
    }
}
